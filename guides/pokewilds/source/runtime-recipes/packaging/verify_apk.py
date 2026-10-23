#!/usr/bin/env python3
"""Static, bounded-memory verification for the standalone APK.

The payload is deliberately checked as a stream.  This keeps verification useful on
the build host even when the runtime archive is several hundred megabytes compressed.
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import io
import json
import os
from pathlib import Path
import posixpath
import re
import shutil
import subprocess
import sys
import tarfile
import zipfile


PACKAGE = "local.pokewilds.standalone"
ABI = "arm64-v8a"
NATIVE_LIBS = (
    "libproot.so",
    "libproot-loader.so",
    "libvirgl_test_server_android.so",
    "libpulseaudio.so",
    "libXlorie.so",
)
# aapt2 treats a .gz asset as a compressed resource and strips the suffix.  The
# build therefore uses .bin to preserve the gzip bytes exactly.
PAYLOAD_ASSETS = ("assets/runtime.bin", "assets/runtime.tar.gz", "assets/runtime.tar")
GAME_SOURCE_ASSET = "assets/game-source.json"
PATCH_AGENT_ASSET = "assets/bugfix.jar"
CONTROL_AGENT_ASSET = "assets/control-patches.jar"
CHUNK = 1024 * 1024
ELF_AARCH64 = 183


def locate_tool(name: str, requested: str | None) -> str | None:
    if requested:
        return requested
    found = shutil.which(name)
    if found:
        return found
    roots = []
    for variable in ("ANDROID_SDK_ROOT", "ANDROID_HOME"):
        value = os.environ.get(variable)
        if value:
            roots.append(Path(value))
    roots.append(Path.home() / "Library" / "Android" / "sdk")
    candidates = []
    for root in roots:
        candidates.extend((root / "build-tools").glob("*/" + name))
    return str(sorted((p for p in candidates if p.is_file()), key=lambda p: str(p))[-1]) if candidates else None


def run_tool(argv: list[str]) -> tuple[int, str]:
    try:
        result = subprocess.run(argv, check=False, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    except OSError as exc:
        return 127, str(exc)
    return result.returncode, result.stdout


def inspect_patch_agent(zf: zipfile.ZipFile, errors: list[str]) -> dict[str, object] | None:
    try:
        with zf.open(PATCH_AGENT_ASSET) as stream:
            data = stream.read(4 * 1024 * 1024 + 1)
    except KeyError:
        errors.append("missing built-in patch agent")
        return None
    if len(data) > 4 * 1024 * 1024:
        errors.append("built-in patch agent is unexpectedly large")
        return None
    try:
        with zipfile.ZipFile(io.BytesIO(data)) as agent:
            names = set(agent.namelist())
            required = {
                "META-INF/MANIFEST.MF",
                "local/pokewilds/bugfix/BugFixAgent.class",
                "local/pokewilds/bugfix/Hooks.class",
                "local/pokewilds/bugfix/asm/ClassReader.class",
            }
            if not required.issubset(names):
                errors.append("built-in patch agent is missing required classes or manifest")
            if "META-INF/MANIFEST.MF" in names:
                manifest = agent.read("META-INF/MANIFEST.MF").decode("utf-8", errors="replace")
                if "Premain-Class: local.pokewilds.bugfix.BugFixAgent" not in manifest:
                    errors.append("built-in patch agent has the wrong premain class")
    except (OSError, zipfile.BadZipFile):
        errors.append("built-in patch agent is not a readable JAR")
    return {"asset": PATCH_AGENT_ASSET, "sha256": hashlib.sha256(data).hexdigest()}


def inspect_control_agent(zf: zipfile.ZipFile, errors: list[str]) -> dict[str, object] | None:
    try:
        with zf.open(CONTROL_AGENT_ASSET) as stream:
            data = stream.read(1024 * 1024 + 1)
    except KeyError:
        errors.append("missing built-in control agent")
        return None
    if len(data) > 1024 * 1024:
        errors.append("built-in control agent is unexpectedly large")
        return None
    try:
        with zipfile.ZipFile(io.BytesIO(data)) as agent:
            names = set(agent.namelist())
            if not {"META-INF/MANIFEST.MF", "com/pkmngen/game/OdinAgent.class"}.issubset(names):
                errors.append("built-in control agent is missing its class or manifest")
            if "com/pkmngen/game/OdinInputHook.class" in names:
                errors.append("built-in control agent contains the donor input replacement hook")
            if "META-INF/MANIFEST.MF" in names:
                manifest = agent.read("META-INF/MANIFEST.MF").decode("utf-8", errors="replace")
                if "Premain-Class: com.pkmngen.game.OdinAgent" not in manifest:
                    errors.append("built-in control agent has the wrong premain class")
    except (OSError, zipfile.BadZipFile):
        errors.append("built-in control agent is not a readable JAR")
    return {"asset": CONTROL_AGENT_ASSET, "sha256": hashlib.sha256(data).hexdigest()}


def read_properties(zf: zipfile.ZipFile, errors: list[str]) -> dict[str, str]:
    try:
        with zf.open("assets/payload.properties") as stream:
            data = stream.read(64 * 1024)
    except KeyError:
        errors.append("missing assets/payload.properties")
        return {}
    if len(data) >= 64 * 1024:
        errors.append("assets/payload.properties exceeds 64 KiB")
    properties: dict[str, str] = {}
    for line in data.decode("utf-8", "replace").splitlines():
        if not line or line.startswith(('#', '!')):
            continue
        if "=" not in line:
            errors.append(f"malformed payload property: {line[:80]!r}")
            continue
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
    declared = properties.get("sha256", "")
    if not re.fullmatch(r"[0-9a-fA-F]{64}", declared):
        errors.append("payload.properties sha256 is missing or invalid")
    return properties


def stream_hash(zf: zipfile.ZipFile, member: str) -> str:
    digest = hashlib.sha256()
    with zf.open(member) as stream:
        while True:
            block = stream.read(CHUNK)
            if not block:
                break
            digest.update(block)
    return digest.hexdigest()


def elf_machine(zf: zipfile.ZipFile, member: str) -> int | None:
    with zf.open(member) as stream:
        header = stream.read(20)
    if len(header) < 20 or header[:4] != b"\x7fELF":
        return None
    # e_machine is two bytes at offset 18 for both ELF32 and ELF64.
    return int.from_bytes(header[18:20], "little")


def normalized_tar_name(name: str) -> str | None:
    if "\x00" in name or name.startswith("/"):
        return None
    normalized = posixpath.normpath(name)
    if normalized in ("", "."):
        return normalized or "."
    if normalized == ".." or normalized.startswith("../"):
        return None
    return normalized[2:] if normalized.startswith("./") else normalized


def inspect_payload(zf: zipfile.ZipFile, member: str, errors: list[str]) -> dict[str, object]:
    # Keep only the required hits, rather than retaining potentially millions of names.
    found: set[str] = set()
    tar_entries = 0
    tar_bytes = 0
    bundled_game = False
    try:
        with zf.open(member) as raw, gzip.GzipFile(fileobj=raw) as compressed, tarfile.open(fileobj=compressed, mode="r|") as archive:
            for entry in archive:
                tar_entries += 1
                name = normalized_tar_name(entry.name)
                if name is None:
                    errors.append(f"payload contains an unsafe path: {entry.name!r}")
                    continue
                if name == "game" or name.startswith("game/"):
                    bundled_game = True
                if entry.isfile() and entry.size > 0:
                    tar_bytes += entry.size
                if name in {
                    "rootfs/opt/pokewilds/jre/bin/java",
                    "rootfs/usr/bin/java",
                    "rootfs/usr/lib/jvm/pokewilds-jre/bin/java",
                    "rootfs/usr/local/bin/pokewilds-close",
                    "host/lib/libandroid-shmem.so",
                    "host/lib/libtalloc.so.2",
                    "host/opt/virglrenderer-android/libexec/virgl_render_server",
                    "host/opt/virglrenderer-android/lib/libvirglrenderer.so",
                    "host/opt/angle-android/vulkan/libGLESv2_angle.so",
                    "host/etc/pulse/default.pa",
                }:
                    found.add(name)
                if name.startswith("rootfs/usr/share/X11/xkb/"):
                    found.add("rootfs/usr/share/X11/xkb/")
    except (OSError, EOFError, tarfile.TarError, gzip.BadGzipFile, zipfile.BadZipFile) as exc:
        errors.append(f"cannot stream payload archive: {exc}")

    alternatives = {
        "java": {
            "rootfs/opt/pokewilds/jre/bin/java",
            "rootfs/usr/bin/java",
            "rootfs/usr/lib/jvm/pokewilds-jre/bin/java",
        },
        "close-helper": {"rootfs/usr/local/bin/pokewilds-close"},
        "xkb": {"rootfs/usr/share/X11/xkb/"},
        "host-shmem": {"host/lib/libandroid-shmem.so"},
        "host-talloc": {"host/lib/libtalloc.so.2"},
        "virgl-server": {"host/opt/virglrenderer-android/libexec/virgl_render_server"},
        "virgl-library": {"host/opt/virglrenderer-android/lib/libvirglrenderer.so"},
        "angle": {"host/opt/angle-android/vulkan/libGLESv2_angle.so"},
        "pulse-config": {"host/etc/pulse/default.pa"},
    }
    missing = [label for label, choices in alternatives.items() if not found.intersection(choices)]
    errors.extend(f"payload missing required {label}" for label in missing)
    if bundled_game:
        errors.append("runtime payload contains bundled game files")
    return {"entries": tar_entries, "expandedFileBytes": tar_bytes, "found": sorted(found), "missing": missing, "containsGame": bundled_game}


def inspect_game_source(zf: zipfile.ZipFile, errors: list[str]) -> dict[str, object] | None:
    try:
        with zf.open(GAME_SOURCE_ASSET) as stream:
            data = stream.read(64 * 1024 + 1)
    except KeyError:
        errors.append(f"missing {GAME_SOURCE_ASSET}")
        return None
    if len(data) > 64 * 1024:
        errors.append(f"{GAME_SOURCE_ASSET} exceeds 64 KiB")
        return None
    try:
        source = json.loads(data.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        errors.append(f"{GAME_SOURCE_ASSET} is not valid UTF-8 JSON")
        return None
    if not isinstance(source, dict) or source.get("schema") != 1:
        errors.append(f"{GAME_SOURCE_ASSET} has an unsupported schema")
        return source if isinstance(source, dict) else None
    for field in ("version", "url", "archiveMember"):
        if not isinstance(source.get(field), str) or not source[field]:
            errors.append(f"{GAME_SOURCE_ASSET} is missing {field}")
    for field in ("sha256", "jarSha256"):
        if not isinstance(source.get(field), str) or not re.fullmatch(r"[0-9a-fA-F]{64}", source[field]):
            errors.append(f"{GAME_SOURCE_ASSET} has an invalid {field}")
    if not isinstance(source.get("url"), str) or not source.get("url", "").startswith("https://"):
        errors.append(f"{GAME_SOURCE_ASSET} url must use HTTPS")
    required = source.get("requiredFiles")
    if not isinstance(required, list) or "pokewilds.jar" not in required or any(not isinstance(item, str) or not item for item in required):
        errors.append(f"{GAME_SOURCE_ASSET} requiredFiles must include pokewilds.jar")
    try:
        expected = json.loads((Path(__file__).with_name("game-source.json")).read_text(encoding="utf-8"))
        if source != expected:
            errors.append(f"{GAME_SOURCE_ASSET} does not match the pinned packaging/game-source.json")
    except (OSError, json.JSONDecodeError):
        errors.append("cannot read pinned packaging/game-source.json")
    return {"version": source.get("version"), "url": source.get("url"), "sha256": source.get("sha256"), "jarSha256": source.get("jarSha256"), "archiveMember": source.get("archiveMember"), "requiredFiles": required}


def inspect_manifest(aapt2: str | None, apk: str, result: dict[str, object], errors: list[str], warnings: list[str]) -> None:
    if not aapt2:
        warnings.append("aapt2 unavailable; package, target SDK, and manifest component checks were skipped")
        return
    code, badging = run_tool([aapt2, "dump", "badging", apk])
    if code:
        errors.append(f"aapt2 dump badging failed (exit {code})")
        return
    package_match = re.search(r"package: name='([^']+)'", badging)
    target_match = re.search(r"targetSdkVersion:'([^']+)'", badging)
    launchers = re.findall(r"launchable-activity: name='([^']+)'", badging)
    result["package"] = package_match.group(1) if package_match else None
    result["targetSdk"] = int(target_match.group(1)) if target_match and target_match.group(1).isdigit() else (target_match.group(1) if target_match else None)
    result["launchers"] = launchers
    if not package_match or package_match.group(1) != PACKAGE:
        errors.append(f"APK package is {package_match.group(1) if package_match else 'unreadable'}, expected {PACKAGE}")
    if len(launchers) != 1:
        errors.append(f"expected one launcher activity, found {len(launchers)}")
    elif not launchers[0].startswith(PACKAGE + "."):
        errors.append(f"launcher activity is outside the standalone package: {launchers[0]}")
    code, permissions = run_tool([aapt2, "dump", "permissions", apk])
    if code == 0 and re.search(r"com\.termux\.permission\.RUN_COMMAND|RUN_COMMAND", permissions):
        errors.append("APK declares the Termux RUN_COMMAND permission")
    code, manifest = run_tool([aapt2, "dump", "xmltree", apk, "--file", "AndroidManifest.xml"])
    if code == 0:
        forbidden = ("com.termux.permission.RUN_COMMAND", "com.termux.x11/.MainActivity", "proot-distro")
        for token in forbidden:
            if token in manifest:
                errors.append(f"manifest contains forbidden external Termux dependency: {token}")
    else:
        warnings.append(f"aapt2 manifest xmltree unavailable (exit {code})")


def inspect_signature(apksigner: str | None, apk: str, result: dict[str, object], errors: list[str], warnings: list[str]) -> None:
    if not apksigner:
        warnings.append("apksigner unavailable; APK signature was not checked")
        return
    code, output = run_tool([apksigner, "verify", "--verbose", apk])
    verified = code == 0 and bool(re.search(r"Verified using v(?:1|2|3)(?:\.1)? scheme[^:]*:\s*true", output))
    result["signature"] = {"verified": verified}
    if not verified:
        errors.append(f"apksigner verification failed (exit {code})")


def verify(apk_path: Path, aapt2: str | None, apksigner: str | None) -> tuple[dict[str, object], int]:
    result: dict[str, object] = {"ok": False, "apk": str(apk_path), "size": 0, "native": [], "errors": [], "warnings": []}
    errors = result["errors"]
    warnings = result["warnings"]
    assert isinstance(errors, list) and isinstance(warnings, list)
    if not apk_path.is_file():
        errors.append("APK does not exist or is not a regular file")
        return result, 1
    result["size"] = apk_path.stat().st_size
    try:
        with zipfile.ZipFile(apk_path) as zf:
            names = set(zf.namelist())
            direct_game_files = sorted(name for name in names if name.startswith("assets/") and (
                name.startswith("assets/game/") or name.endswith("/pokewilds.jar")
                or name.endswith("/pokewilds-otherplatforms.zip")))
            if direct_game_files:
                errors.append("APK contains direct game files: " + ", ".join(direct_game_files[:5]))
            result["patchAgent"] = inspect_patch_agent(zf, errors)
            result["controlAgent"] = inspect_control_agent(zf, errors)
            native_result = result["native"]
            assert isinstance(native_result, list)
            for library in NATIVE_LIBS:
                member = f"lib/{ABI}/{library}"
                item: dict[str, object] = {"name": member, "present": member in names}
                if member not in names:
                    errors.append(f"missing native library: {member}")
                else:
                    machine = elf_machine(zf, member)
                    item["elfMachine"] = machine
                    if machine != ELF_AARCH64:
                        errors.append(f"native library is not AArch64 ELF: {member}")
                native_result.append(item)
            unexpected_abis = sorted({name.split("/")[1] for name in names if name.startswith("lib/") and name.count("/") >= 2} - {ABI})
            if unexpected_abis:
                errors.append("unexpected native ABIs: " + ", ".join(unexpected_abis))
            properties = read_properties(zf, errors)
            result["gameSource"] = inspect_game_source(zf, errors)
            payload_member = next((name for name in PAYLOAD_ASSETS if name in names), None)
            if payload_member is None:
                errors.append("missing assets/runtime.tar.gz (or compatibility name assets/runtime.tar)")
            else:
                actual = stream_hash(zf, payload_member)
                declared = properties.get("sha256", "").lower()
                payload_result = {"asset": payload_member, "sha256": actual, "declaredSha256": declared, "hashMatches": actual == declared}
                if actual != declared:
                    errors.append("payload archive SHA-256 does not match payload.properties")
                payload_result.update(inspect_payload(zf, payload_member, errors))
                result["payload"] = payload_result
            inspect_manifest(aapt2, str(apk_path), result, errors, warnings)
    except (OSError, zipfile.BadZipFile) as exc:
        errors.append(f"cannot read APK as ZIP: {exc}")
    inspect_signature(apksigner, str(apk_path), result, errors, warnings)
    result["ok"] = not errors
    return result, 0 if result["ok"] else 1


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--aapt2", help="path to aapt2; auto-discovered from the Android SDK when omitted")
    parser.add_argument("--apksigner", help="path to apksigner; auto-discovered from the Android SDK when omitted")
    args = parser.parse_args(argv)
    aapt2 = locate_tool("aapt2", args.aapt2)
    apksigner = locate_tool("apksigner", args.apksigner)
    result, status = verify(args.apk.resolve(), aapt2, apksigner)
    print(json.dumps(result, sort_keys=True, separators=(",", ":")))
    return status


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
