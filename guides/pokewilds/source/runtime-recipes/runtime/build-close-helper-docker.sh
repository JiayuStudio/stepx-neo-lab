#!/bin/sh
set -eu

# Pinned ARM64 image for a Linux/glibc/X11 helper build on macOS.
IMAGE='ubuntu@sha256:008173c23f95b170204355c12626cb5a965d779a7e1283b09e9cffbb1bf33ca3'
SOURCE_SHA256='c4cdb336033eb873f131156198d542e922eaceb4e080b35b900a4660b25aa8ee'
EXPECTED_BINARY_SHA256='cacdea4f6076c78466708631e22394f09c8df1caa5e3047ff08ceade51e3e4b3'

runtime_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_dir=$(CDPATH= cd -- "$runtime_dir/../.." && pwd)
source_file="$repo_dir/pokewilds-close.c"
output=${1:?usage: build-close-helper-docker.sh OUTPUT}

sha256() {
    if command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$1" | awk '{print $1}'
    else
        sha256sum "$1" | awk '{print $1}'
    fi
}

if ! command -v docker >/dev/null 2>&1; then
    echo 'docker is required (Docker Desktop or an ARM64-capable Docker Engine)' >&2
    exit 2
fi
if [ ! -f "$source_file" ]; then
    echo "missing close-helper source: $source_file" >&2
    exit 2
fi
actual_source_sha=$(sha256 "$source_file")
if [ "$actual_source_sha" != "$SOURCE_SHA256" ]; then
    echo "pokewilds-close.c changed: expected $SOURCE_SHA256, got $actual_source_sha" >&2
    exit 2
fi

mkdir -p "$(dirname -- "$output")"
output_dir=$(CDPATH= cd -- "$(dirname -- "$output")" && pwd)
output_name=$(basename -- "$output")

docker run --rm --platform linux/arm64 \
    -v "$repo_dir:/src:ro" \
    -v "$output_dir:/out" \
    "$IMAGE" sh -eu -c \
    'apt-get update -qq
     apt-get install -y -qq --no-install-recommends gcc libc6-dev libx11-dev
     gcc -O2 -Wall -Wextra /src/pokewilds-close.c -lX11 -o "/out/$1"' \
    sh "$output_name"

actual_binary_sha=$(sha256 "$output")
if [ "$actual_binary_sha" != "$EXPECTED_BINARY_SHA256" ]; then
    echo "helper build changed: expected $EXPECTED_BINARY_SHA256, got $actual_binary_sha" >&2
    exit 1
fi
echo "built reproducible ARM64/glibc X11 helper: $output"
echo "sha256=$actual_binary_sha"
