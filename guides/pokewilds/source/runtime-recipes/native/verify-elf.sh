#!/usr/bin/env bash
set -euo pipefail

# Verify Android-side ELF payloads before APK packaging.  Guest glibc files
# are intentionally excluded: they are inspected by the Linux payload build,
# not by Android's linker.
if [[ $# -lt 1 ]]; then
    printf 'usage: %s ELF...\n' "$0" >&2
    exit 2
fi

readelf_bin="${READELF:-readelf}"
for elf in "$@"; do
    [[ -f "$elf" ]] || { printf 'missing ELF: %s\n' "$elf" >&2; exit 1; }
    file "$elf" | grep -Eq 'ELF 64-bit LSB (shared object|pie executable|executable), ARM aarch64' || {
        printf 'wrong ABI or file type: %s\n' "$elf" >&2
        exit 1
    }
    if ! file "$elf" | grep -q 'statically linked'; then
        "$readelf_bin" -d "$elf" | grep -qE 'NEEDED.*(libc\.so|libdl\.so|liblog\.so|libandroid\.so|libEGL\.so|libGLESv2\.so)' || {
            printf 'no expected Android dependency in: %s\n' "$elf" >&2
            exit 1
        }
    fi
    if "$readelf_bin" -l "$elf" | grep -qE '/data/data/com\.termux|/data/data/[^ ]+/files' ||
       "$readelf_bin" -d "$elf" | grep -qE '/data/data/com\.termux|/data/data/[^ ]+/files'; then
        printf 'absolute app-data interpreter/RPATH in: %s\n' "$elf" >&2
        exit 1
    fi
done
