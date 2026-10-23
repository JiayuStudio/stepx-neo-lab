#!/usr/bin/env python3
"""Generate this app's public update channel from a signed release APK."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--notes", default="")
    args = parser.parse_args()
    tools = Path(os.environ["ANDROID_SDK_ROOT"]) / "build-tools" / "36.0.0"
    verified = subprocess.run([str(tools / "apksigner"), "verify", str(args.apk)],
                              capture_output=True, timeout=30)
    if verified.returncode:
        raise ValueError("APK signature verification failed")
    metadata = subprocess.run([str(tools / "aapt2"), "dump", "badging", str(args.apk)],
                              capture_output=True, timeout=30, check=True).stdout.decode("utf-8")
    package = re.search(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'", metadata, re.M)
    if not package or package[1] != "studio.jiayu.neoagent" or int(package[2]) < 1:
        raise ValueError("Unexpected release package identity")
    version = package[3]
    if not re.fullmatch(r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?", version):
        raise ValueError("Invalid release version")
    tag = "neo-agent-touch-v" + version
    name = "Neo-Agent-Touch-" + version + ".apk"
    payload = args.apk.read_bytes()
    if not 0 < len(payload) <= 150 * 1024 * 1024:
        raise ValueError("Release package size is out of range")
    feed = {
        "schema": 1, "packageName": package[1], "versionName": version,
        "versionCode": int(package[2]), "tag": tag, "assetName": name,
        "downloadUrl": "https://github.com/JiayuStudio/stepx-neo-lab/releases/download/" + tag + "/" + name,
        "sha256": hashlib.sha256(payload).hexdigest(), "size": len(payload),
        "notes": args.notes[:1600],
    }
    args.output.write_text(json.dumps(feed, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Update feed generated; APK signature verified. Publish the APK before replacing the live feed.")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        raise SystemExit("Update feed generation failed; no release was published.")
