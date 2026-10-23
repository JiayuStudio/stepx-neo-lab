#!/bin/sh
set -eu

runtime_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_dir=$(CDPATH= cd -- "$runtime_dir/../.." && pwd)
source_file="$repo_dir/pokewilds-close.c"
output=${1:?usage: build-close-helper.sh OUTPUT}
cc=${CROSS_CC:-aarch64-linux-gnu-gcc}

if [ ! -f "$source_file" ]; then
    echo "missing close-helper source: $source_file" >&2
    exit 2
fi
if ! command -v "$cc" >/dev/null 2>&1; then
    echo "missing ARM64 Linux cross compiler: $cc (set CROSS_CC to a compiler with an ARM64 glibc/X11 sysroot)" >&2
    echo "on macOS, use build-close-helper-docker.sh for the pinned ARM64 Docker build" >&2
    exit 2
fi

mkdir -p "$(dirname -- "$output")"
"$cc" -O2 -Wall -Wextra "$source_file" -lX11 -o "$output"
if ! command -v readelf >/dev/null 2>&1; then
    echo "built $output; install readelf to verify its ARM64 ELF header" >&2
    exit 0
fi
if ! readelf -h "$output" | grep -Eq 'Machine:[[:space:]]+AArch64'; then
    echo "close helper is not an AArch64 ELF executable: $output" >&2
    exit 1
fi
echo "built verified ARM64 close helper: $output"
