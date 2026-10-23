#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
commit=bd1cfadd74f98b548662a48b6e1d564aa434c86e
mkdir -p .cache
if [[ ! -e .cache/termux-x11/.git ]]; then
 git clone https://github.com/termux/termux-x11.git .cache/termux-x11
fi
git -C .cache/termux-x11 checkout --detach "$commit"
git -C .cache/termux-x11 submodule update --init --recursive
git -C .cache/termux-x11 apply --reverse --check "$(pwd)/patches/x11-gl-header.patch" 2>/dev/null || git -C .cache/termux-x11 apply "$(pwd)/patches/x11-gl-header.patch"
git -C .cache/termux-x11 apply --reverse --check "$(pwd)/patches/x11-hide-cursor.patch" 2>/dev/null || git -C .cache/termux-x11 apply "$(pwd)/patches/x11-hide-cursor.patch"
git -C .cache/termux-x11/lorie/src/main/cpp/libepoxy apply --reverse --check ../patches/libepoxy.patch 2>/dev/null || git -C .cache/termux-x11/lorie/src/main/cpp/libepoxy apply ../patches/libepoxy.patch
printf 'X11 source ready at %s\n' "$commit"
