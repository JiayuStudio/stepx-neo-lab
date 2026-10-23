#!/usr/bin/env python3
"""Fetch, verify, assemble, and inspect the standalone Linux payload.

The tool intentionally fails closed when a source lock entry has no digest.
The Android app must never be responsible for resolving floating downloads.
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import io
import json
import os
import shutil
import stat
import subprocess
import sys
import tarfile
import tempfile
import urllib.error
import urllib.request
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parent
DEFAULT_MANIFEST = ROOT / "manifest.json"
LOG_DIR = ROOT.parent / "standalone-build-logs"


class PayloadError(RuntimeError):
    pass


def log(message: str, log_file: Path | None) -> None:
    line = f"{message}\n"
    print(line, end="", file=sys.stderr)
    if log_file:
        log_file.parent.mkdir(parents=True, exist_ok=True)
        with log_file.open("a", encoding="utf-8") as stream:
            stream.write(line)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def safe_member_name(name: str) -> Path:
    # Archive names are always POSIX paths, even when built on Windows.
    normalized = name.replace("\\", "/")
    candidate = Path(normalized)
    if candidate.is_absolute() or normalized.startswith("/"):
        raise PayloadError(f"archive contains absolute path: {name!r}")
    if any(part in ("", ".", "..") for part in candidate.parts):
        raise PayloadError(f"archive contains unsafe path: {name!r}")
    return candidate


def safe_destination(root: Path, member_name: str, strip_components: int = 0) -> Path | None:
    parts = safe_member_name(member_name).parts
    if len(parts) <= strip_components:
        return None
    relative = Path(*parts[strip_components:])
    destination = root / relative
    if os.path.commonpath((str(root.resolve()), str(destination.parent.resolve()))) != str(root.resolve()):
        raise PayloadError(f"archive member escapes destination: {member_name!r}")
    return destination


def extract_tar(archive: Path, destination: Path, strip_components: int = 0) -> None:
    destination.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive, "r:*") as stream:
        members = stream.getmembers()
        for member in members:
            target = safe_destination(destination, member.name, strip_components)
            if target is None:
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            if member.isdir():
                target.mkdir(exist_ok=True)
                continue
            if member.issym():
                # Absolute links are valid inside a Linux rootfs; they resolve
                # within the guest root, not the Android host. Relative links
                # are constrained to the destination by the archive digest.
                if target.exists() or target.is_symlink():
                    target.unlink()
                target.symlink_to(member.linkname)
                continue
            if member.islnk():
                link_target = safe_destination(destination, member.linkname, strip_components)
                if link_target is None:
                    raise PayloadError(f"hard link escapes destination: {member.name!r}")
                if target.exists() or target.is_symlink():
                    target.unlink()
                os.link(link_target, target)
                continue
            if not member.isfile():
                raise PayloadError(f"unsupported tar member {member.name!r}")
            if target.exists() or target.is_symlink():
                target.unlink()
            source = stream.extractfile(member)
            if source is None:
                raise PayloadError(f"cannot read tar member {member.name!r}")
            with target.open("wb") as output:
                shutil.copyfileobj(source, output)
            target.chmod(member.mode & 0o7777)


def extract_zip(archive: Path, destination: Path, prefix: str | None = None) -> None:
    destination.mkdir(parents=True, exist_ok=True)
    prefix_parts = tuple(safe_member_name(prefix).parts) if prefix else ()
    with zipfile.ZipFile(archive) as stream:
        for info in stream.infolist():
            parts = safe_member_name(info.filename).parts
            if prefix_parts:
                if parts[: len(prefix_parts)] != prefix_parts:
                    continue
                parts = parts[len(prefix_parts) :]
            if not parts:
                continue
            target = destination / Path(*parts)
            if os.path.commonpath((str(destination.resolve()), str(target.parent.resolve()))) != str(destination.resolve()):
                raise PayloadError(f"zip member escapes destination: {info.filename!r}")
            if info.is_dir():
                target.mkdir(parents=True, exist_ok=True)
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            if target.exists() or target.is_symlink():
                target.unlink()
            with stream.open(info) as source, target.open("wb") as output:
                shutil.copyfileobj(source, output)
            mode = (info.external_attr >> 16) & 0o7777
            if mode:
                target.chmod(mode)


def extract_deb(archive: Path, destination: Path) -> None:
    """Extract only data.tar.* from a Debian archive without running dpkg."""
    raw = archive.read_bytes()
    if not raw.startswith(b"!<arch>\n"):
        raise PayloadError(f"{archive.name}: not an ar/deb archive")
    offset = 8
    data = None
    while offset < len(raw):
        if offset + 60 > len(raw) or raw[offset + 58 : offset + 60] != b"`\n":
            raise PayloadError(f"{archive.name}: malformed ar member")
        name = raw[offset : offset + 16].decode("ascii", "replace").strip().rstrip("/")
        size_text = raw[offset + 48 : offset + 58].decode("ascii", "replace").strip()
        try:
            size = int(size_text)
        except ValueError as exc:
            raise PayloadError(f"{archive.name}: malformed ar member size") from exc
        body_start = offset + 60
        body_end = body_start + size
        if body_end > len(raw):
            raise PayloadError(f"{archive.name}: truncated ar member")
        if name.startswith("data.tar"):
            data = raw[body_start:body_end]
            break
        offset = body_end + (size & 1)
    if data is None:
        raise PayloadError(f"{archive.name}: missing data.tar member")
    try:
        with tarfile.open(fileobj=io.BytesIO(data), mode="r:*") as stream:
            # Reuse the same lexical safety checks as a downloaded rootfs.
            destination.mkdir(parents=True, exist_ok=True)
            for member in stream.getmembers():
                target = safe_destination(destination, member.name)
                if target is None:
                    continue
                target.parent.mkdir(parents=True, exist_ok=True)
                if member.isdir():
                    target.mkdir(exist_ok=True)
                    continue
                if member.issym():
                    if target.exists() or target.is_symlink():
                        target.unlink()
                    target.symlink_to(member.linkname)
                    continue
                if member.islnk():
                    link_target = safe_destination(destination, member.linkname)
                    if link_target is None:
                        raise PayloadError(f"{archive.name}: hard link escapes destination")
                    if target.exists() or target.is_symlink():
                        target.unlink()
                    os.link(link_target, target)
                    continue
                if not member.isfile():
                    raise PayloadError(f"{archive.name}: unsupported data member {member.name!r}")
                if target.exists() or target.is_symlink():
                    target.unlink()
                source = stream.extractfile(member)
                if source is None:
                    raise PayloadError(f"{archive.name}: cannot read {member.name!r}")
                with target.open("wb") as output:
                    shutil.copyfileobj(source, output)
                target.chmod(member.mode & 0o7777)
    except tarfile.ReadError as exc:
        raise PayloadError(f"{archive.name}: unsupported data.tar compression (install tar/zstd support)") from exc


def validate_lock(manifest: dict) -> list[str]:
    errors: list[str] = []
    if manifest.get("schema") != 1:
        errors.append("unsupported manifest schema")
    target = manifest.get("target", {})
    if target.get("arch") != "aarch64" or target.get("libc") != "glibc":
        errors.append("payload target must be Linux aarch64/glibc")
    inputs = manifest.get("inputs")
    if not isinstance(inputs, list) or not inputs:
        errors.append("manifest has no inputs")
        return errors
    for item in inputs:
        item_id = item.get("id", "<unnamed>")
        extract_to = str(item.get("extract_to", "")).strip("/")
        if item.get("role") == "game-release" or extract_to == "game" or extract_to.startswith("game/"):
            errors.append(f"{item_id}: game files must be acquired separately from the runtime payload")
        digest = item.get("sha256")
        if not isinstance(digest, str) or len(digest) != 64 or any(c not in "0123456789abcdef" for c in digest.lower()):
            errors.append(f"{item_id}: missing pinned SHA-256")
        if item.get("format") == "deb-set":
            artifacts = item.get("artifacts")
            if not artifacts:
                errors.append(f"{item_id}: package closure has no pinned artifact list")
            for artifact in artifacts or []:
                artifact_id = artifact.get("id", "<unnamed package>")
                digest = artifact.get("sha256")
                if not isinstance(digest, str) or len(digest) != 64 or any(c not in "0123456789abcdef" for c in digest.lower()):
                    errors.append(f"{item_id}/{artifact_id}: missing pinned SHA-256")
                if not isinstance(artifact.get("url"), str):
                    errors.append(f"{item_id}/{artifact_id}: missing official download URL")
        if item.get("format") != "deb-set" and not isinstance(item.get("url"), str):
            errors.append(f"{item_id}: missing official download URL")
    return errors


def find_cached(cache: Path, item: dict) -> Path | None:
    candidates = [cache / item["id"]]
    if item.get("filename"):
        candidates.append(cache / item["filename"])
    if item.get("url"):
        candidates.append(cache / Path(item["url"].split("/")[-1]))
    for candidate in candidates:
        if candidate.is_file():
            return candidate
    if item.get("url"):
        basenames = [Path(item["url"].split("/")[-1]).name]
        if item.get("filename"):
            basenames.append(item["filename"])
        matches = sorted(path for path in cache.rglob("*") if path.is_file() and path.name in basenames)
        if matches:
            return matches[0]
    return None


def obtain(item: dict, cache: Path, offline: bool, log_file: Path | None) -> Path:
    cached = find_cached(cache, item)
    if cached:
        log(f"cache: {item['id']} -> {cached}", log_file)
        return cached
    if offline:
        raise PayloadError(f"offline cache miss for {item['id']}")
    url = item.get("url")
    if not url:
        raise PayloadError(f"{item['id']} has no downloadable artifact list")
    cache.mkdir(parents=True, exist_ok=True)
    target = cache / item["id"]
    log(f"download: {item['id']} from {url}", log_file)
    try:
        try:
            source = urllib.request.urlopen(url, timeout=60)
        except urllib.error.HTTPError as exc:
            # Ubuntu's rolling pool removes superseded revisions. Launchpad
            # keeps the same pinned package file in its primary archive.
            if exc.code != 404 or not url.startswith("https://ports.ubuntu.com/ubuntu-ports/pool/") or not url.endswith(".deb"):
                raise
            archive_url = "https://launchpad.net/ubuntu/+archive/primary/+files/" + Path(url).name
            log(f"archive: {item['id']} from {archive_url}", log_file)
            source = urllib.request.urlopen(archive_url, timeout=60)
        with source, target.open("wb") as output:
            shutil.copyfileobj(source, output)
    except Exception as exc:
        target.unlink(missing_ok=True)
        raise PayloadError(f"download failed for {item['id']}: {exc}") from exc
    return target


def verify_input(item: dict, source: Path) -> None:
    expected = item["sha256"].lower()
    actual = sha256(source)
    if actual != expected:
        raise PayloadError(f"{item['id']}: SHA-256 mismatch (expected {expected}, got {actual})")


def required_paths_exist(staging: Path, manifest: dict) -> None:
    missing = [path for path in manifest.get("required_paths", []) if not (staging / path).exists()]
    if missing:
        raise PayloadError("assembled payload is incomplete; missing: " + ", ".join(missing))


def install_service_paths(staging: Path, close_helper: str | None) -> None:
    rootfs = staging / "rootfs"
    jre_link = rootfs / "usr/lib/jvm/pokewilds-jre"
    jre_link.parent.mkdir(parents=True, exist_ok=True)
    if not jre_link.exists() and not jre_link.is_symlink():
        jre_link.symlink_to("../../../opt/pokewilds/jre")
    java = rootfs / "usr/bin/java"
    java.parent.mkdir(parents=True, exist_ok=True)
    if not java.exists() and not java.is_symlink():
        java.symlink_to("../lib/jvm/pokewilds-jre/bin/java")
    if close_helper:
        source = Path(close_helper).resolve()
        if not source.is_file() or source.is_symlink():
            raise PayloadError(f"close helper is not a regular file: {source}")
        destination = rootfs / "usr/local/bin/pokewilds-close"
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        destination.chmod(0o755)


def install_host_tree(staging: Path, host_dir: str | None) -> None:
    if not host_dir:
        return
    source = Path(host_dir).resolve()
    if not source.is_dir():
        raise PayloadError(f"host directory is not a directory: {source}")
    destination = staging / "host"
    shutil.copytree(source, destination, symlinks=True, dirs_exist_ok=False)


def archive_member_hashes(staging: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for path in sorted(staging.rglob("*")):
        relative = path.relative_to(staging).as_posix()
        if relative in ("payload-manifest.json", "payload.properties"):
            continue
        if path.is_file() and not path.is_symlink():
            result[relative] = sha256(path)
        elif path.is_symlink():
            result[relative] = hashlib.sha256(("symlink:" + os.readlink(path)).encode()).hexdigest()
    return result


def deterministic_tar(source: Path, output: Path) -> None:
    with output.open("wb") as raw, gzip.GzipFile(filename="", fileobj=raw, mode="wb", compresslevel=9, mtime=0) as compressed:
        with tarfile.open(fileobj=compressed, mode="w", format=tarfile.PAX_FORMAT) as stream:
            paths = [source] + sorted(source.rglob("*"), key=lambda p: p.relative_to(source).as_posix())
            for path in paths:
                arcname = "." if path == source else path.relative_to(source).as_posix()
                info = stream.gettarinfo(str(path), arcname=arcname)
                info.uid = 0
                info.gid = 0
                info.uname = ""
                info.gname = ""
                info.mtime = 0
                if info.isreg():
                    with path.open("rb") as input_stream:
                        stream.addfile(info, input_stream)
                else:
                    stream.addfile(info)


def build(args: argparse.Namespace) -> int:
    manifest_path = Path(args.manifest).resolve()
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    log_file = Path(args.log).resolve() if args.log else LOG_DIR / "payload-build.log"
    errors = validate_lock(manifest)
    if errors:
        for error in errors:
            log("LOCK ERROR: " + error, log_file)
        return 2
    cache = Path(args.cache).resolve()
    output = Path(args.output).resolve()
    properties = Path(args.properties).resolve() if args.properties else output.parent / "payload.properties"
    staging_parent = Path(tempfile.mkdtemp(prefix="pokewilds-payload-"))
    staging = staging_parent / "payload"
    staging.mkdir()
    try:
        for item in manifest["inputs"]:
            fmt = item["format"]
            if fmt == "deb-set":
                for artifact in item["artifacts"]:
                    source = obtain(artifact, cache, args.offline, log_file)
                    verify_input(artifact, source)
                    extract_deb(source, staging / item["extract_to"])
            else:
                source = obtain(item, cache, args.offline, log_file)
                verify_input(item, source)
            if fmt == "tar.gz":
                destination = staging / item["extract_to"]
                extract_tar(source, destination, int(item.get("strip_components", 0)))
            elif fmt == "zip":
                extract_zip(source, staging / item["extract_to"], manifest["game"].get("archive_member"))
            elif fmt == "deb-set":
                pass
            else:
                raise PayloadError(f"{item['id']}: unsupported input format {fmt!r}")
        install_service_paths(staging, args.close_helper)
        install_host_tree(staging, args.host_dir)
        required_paths_exist(staging, manifest)
        notices = staging / "notices"
        notices.mkdir(parents=True, exist_ok=True)
        (notices / "SOURCES.json").write_text(
            json.dumps({"payload_id": manifest["payload_id"], "inputs": manifest["inputs"]}, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        generated = dict(manifest)
        generated["source_manifest_sha256"] = sha256(manifest_path)
        generated["members_sha256"] = archive_member_hashes(staging)
        (staging / "payload-manifest.json").write_text(json.dumps(generated, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        expanded_bytes = sum(path.stat().st_size for path in staging.rglob("*") if path.is_file() and not path.is_symlink())
        output.parent.mkdir(parents=True, exist_ok=True)
        temporary_output = output.with_suffix(output.suffix + ".tmp")
        temporary_output.unlink(missing_ok=True)
        deterministic_tar(staging, temporary_output)
        os.replace(temporary_output, output)
        properties.parent.mkdir(parents=True, exist_ok=True)
        properties.write_text(
            "schema=1\n"
            f"sha256={sha256(output)}\n"
            f"expandedBytes={expanded_bytes}\n"
            "gameBytes=0\n"
            f"payloadId={manifest['payload_id']}\n",
            encoding="utf-8",
        )
        log(f"BUILT: {output} ({output.stat().st_size} bytes)", log_file)
        return 0
    except PayloadError as exc:
        log("BLOCKED: " + str(exc), log_file)
        return 3
    finally:
        shutil.rmtree(staging_parent, ignore_errors=True)


def verify_archive(args: argparse.Namespace) -> int:
    archive = Path(args.archive).resolve()
    with tempfile.TemporaryDirectory(prefix="pokewilds-verify-") as temporary:
        destination = Path(temporary)
        try:
            extract_tar(archive, destination)
            manifest = json.loads((destination / "payload-manifest.json").read_text(encoding="utf-8"))
            if any(name == "game" or name.startswith("game/") for name in archive_member_hashes(destination)):
                raise PayloadError("runtime payload must not contain game files")
            if any(item.get("role") == "game-release" for item in manifest.get("inputs", [])):
                raise PayloadError("runtime payload manifest includes a game release input")
            required_paths_exist(destination, manifest)
            expected = manifest.get("members_sha256", {})
            actual = archive_member_hashes(destination)
            if expected != actual:
                raise PayloadError("payload member hash map does not match archive contents")
        except (OSError, tarfile.TarError, json.JSONDecodeError, PayloadError) as exc:
            print(f"INVALID: {exc}", file=sys.stderr)
            return 1
    print(f"VALID: {archive}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    build_parser = subparsers.add_parser("build")
    build_parser.add_argument("--manifest", default=str(DEFAULT_MANIFEST))
    build_parser.add_argument("--cache", required=True)
    build_parser.add_argument("--output", required=True)
    build_parser.add_argument("--log")
    build_parser.add_argument("--offline", action="store_true")
    build_parser.add_argument("--close-helper", help="verified Linux ARM64 pokewilds-close executable")
    build_parser.add_argument("--properties", help="sidecar properties path (defaults beside --output)")
    build_parser.add_argument("--host-dir", help="optional verified Android host tree to store as top-level host/")
    build_parser.set_defaults(function=build)
    verify_parser = subparsers.add_parser("verify")
    verify_parser.add_argument("archive")
    verify_parser.set_defaults(function=verify_archive)
    args = parser.parse_args()
    return args.function(args)


if __name__ == "__main__":
    raise SystemExit(main())
