#!/usr/bin/env python3
"""Download three pinned public documents, verify trusted SHA-256, never execute them.

Example (replace every uppercase value with a trusted release value):
  python3 bootstrap.py --revision FULL_COMMIT_SHA \
    --sha256 README.md=README_SHA256 --sha256 ai.md=AI_SHA256 \
    --sha256 knowledge.json=KNOWLEDGE_SHA256 --output agent-reading

No hash is learned from the same untrusted remote document. All three hashes
are required. --verify-local DIRECTORY performs the same checks without network.
"""

import argparse
import hashlib
import hmac
import http.client
import json
from pathlib import Path
import re
import shutil
import ssl
import sys
import time
import urllib.error
import urllib.request

FILES = ("README.md", "ai.md", "knowledge.json")
RAW_ROOT = "https://raw.githubusercontent.com/JiayuStudio/stepx-neo-lab"
ENTRY_PATH = "guides/neo-agent-touch"
MAX_BYTES = 2 * 1024 * 1024


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, newurl):
        raise ValueError("redirects are not allowed")


def expected_hashes(values):
    result = {}
    for value in values:
        name, separator, digest = value.partition("=")
        if not separator or name not in FILES or name in result:
            raise ValueError("supply each allowlisted filename exactly once")
        if not re.fullmatch(r"[0-9a-fA-F]{64}", digest):
            raise ValueError("expected SHA-256 must contain 64 hexadecimal characters")
        result[name] = digest.lower()
    if set(result) != set(FILES):
        raise ValueError("trusted SHA-256 is required for all three documents")
    return result


def verify_document(name, content, expected):
    if len(content) > MAX_BYTES:
        raise ValueError("document exceeds size limit")
    actual = hashlib.sha256(content).hexdigest()
    if not hmac.compare_digest(actual, expected):
        raise ValueError("SHA-256 mismatch")
    text = content.decode("utf-8")
    if name == "knowledge.json" and not isinstance(json.loads(text), dict):
        raise ValueError("knowledge.json must contain an object")
    return content


def read_bounded(response):
    """A successful read may return fewer bytes than requested; continue to EOF."""
    chunks = []
    size = 0
    deadline = time.monotonic() + 20
    while True:
        if time.monotonic() > deadline:
            raise ValueError("document read exceeded time limit")
        chunk = response.read(min(64 * 1024, MAX_BYTES + 1 - size))
        if time.monotonic() > deadline:
            raise ValueError("document read exceeded time limit")
        if not isinstance(chunk, bytes):
            raise ValueError("document response must contain bytes")
        if not chunk:
            return b"".join(chunks)
        size += len(chunk)
        if size > MAX_BYTES:
            raise ValueError("document exceeds size limit")
        chunks.append(chunk)


def read_remote(revision):
    # A fixed origin, HTTPS verification and no redirects/proxy credentials.
    opener = urllib.request.build_opener(
        urllib.request.ProxyHandler({}), NoRedirect(),
        urllib.request.HTTPSHandler(context=ssl.create_default_context()))
    result = {}
    for name in FILES:
        url = f"{RAW_ROOT}/{revision}/{ENTRY_PATH}/{name}"
        request = urllib.request.Request(url, headers={
            "User-Agent": "NeoAgentTouchResearchReader/1.0", "Accept": "text/plain",
            "Accept-Encoding": "identity", "Cache-Control": "no-cache"})
        for attempt in range(3):
            try:
                with opener.open(request, timeout=20) as response:
                    if response.status != 200:
                        raise ValueError("document request did not return HTTP 200")
                    declared = response.headers.get("Content-Length")
                    if declared is not None and (
                            not declared.isdecimal() or int(declared) > MAX_BYTES):
                        raise ValueError("invalid or excessive document length")
                    content = read_bounded(response)
                    if declared is not None and len(content) != int(declared):
                        raise OSError("document response was incomplete")
                    result[name] = content
                break
            except urllib.error.HTTPError:
                raise
            except (OSError, http.client.IncompleteRead):
                if attempt == 2:
                    raise
    return result


def read_local(directory):
    result = {}
    for name in FILES:
        path = directory / name
        if path.is_symlink() or not path.is_file():
            raise ValueError("local document must be a regular non-symlink file")
        with path.open("rb") as source:
            result[name] = source.read(MAX_BYTES + 1)
    return result


def save_new_directory(directory, documents):
    if directory.exists() or directory.is_symlink():
        raise ValueError("output already exists; choose a new directory")
    directory.mkdir()  # Do not create or replace arbitrary ancestor directories.
    try:
        for name in FILES:
            with (directory / name).open("xb") as destination:
                destination.write(documents[name])
    except BaseException:
        shutil.rmtree(directory)  # Only the newly created, owned output directory.
        raise


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--revision", help="trusted full 40-hex Git commit; required for network mode")
    parser.add_argument("--sha256", action="append", default=[], metavar="FILE=HEX", required=True)
    parser.add_argument("--output", type=Path, default=Path("agent-reading"))
    parser.add_argument("--verify-local", type=Path, metavar="DIRECTORY",
                        help="verify local files only; do not download or write")
    options = parser.parse_args(argv)
    expected = expected_hashes(options.sha256)
    if options.verify_local is not None:
        documents = read_local(options.verify_local)
    else:
        if not options.revision or not re.fullmatch(r"[0-9a-fA-F]{40}", options.revision):
            raise ValueError("network mode requires a trusted full 40-hex commit")
        documents = read_remote(options.revision.lower())
    verified = {name: verify_document(name, documents[name], expected[name]) for name in FILES}
    if options.verify_local is None:
        save_new_directory(options.output, verified)
    print("verified: README.md, ai.md, knowledge.json; no remote code executed")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception:
        # Keep diagnostics free of local paths, network settings or response bodies.
        print("bootstrap failed: input, integrity, network or output check failed", file=sys.stderr)
        raise SystemExit(1)
