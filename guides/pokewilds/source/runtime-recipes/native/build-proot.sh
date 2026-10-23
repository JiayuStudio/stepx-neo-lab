#!/usr/bin/env bash
set -euo pipefail

# Build the Android/Bionic PRoot host and its unbundled ARM64 loader.
# Outputs are deliberately named lib*.so so Gradle packages them in
# lib/arm64-v8a; they remain executables and are launched by absolute path.

script_dir="$(cd "$(dirname "$0")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
build_root="${POKEWILDS_NATIVE_BUILD_ROOT:-$script_dir/.build}"
source_root="$build_root/sources"
out_dir="$script_dir/build/jniLibs/arm64-v8a"
deps_dir="$script_dir/build/host-deps/arm64-v8a"
host_lib_dir="$script_dir/build/host/lib"
ndk_root="${ANDROID_NDK_ROOT:-${ANDROID_NDK_HOME:-$HOME/Library/Android/sdk/ndk/29.0.14206865}}"
ndk_bin="$ndk_root/toolchains/llvm/prebuilt/darwin-x86_64/bin"
api="${ANDROID_API_LEVEL:-28}"

proot_tag="v5.1.107.92"
proot_commit="7266fb3e8516535682f5a9c8f3a7e70f6506eddb"
talloc_version="2.4.2"
talloc_sha256="85ecf9e465e20f98f9950a52e9a411e14320bc555fa257d87697b7e7a9b1d8a6"
shmem_version="0.7"
shmem_sha256="1e5ff8459bc0a8c229dd8a94b27d119987e09ef3414331c2b5ebfff20b98e867"

for tool in git curl tar shasum make patch; do
    command -v "$tool" >/dev/null || { echo "missing build tool: $tool" >&2; exit 2; }
done
[[ -x "$ndk_bin/aarch64-linux-android${api}-clang" ]] || {
    echo "missing Android NDK toolchain: $ndk_bin" >&2
    exit 2
}

# PRoot's GNUmakefile invokes `readelf` while generating loader metadata;
# NDK r29 ships the same tool as llvm-readelf.
tool_shim="$build_root/tool-shims"
mkdir -p "$tool_shim"
ln -sf "$ndk_bin/llvm-readelf" "$tool_shim/readelf"
export PATH="$tool_shim:$ndk_bin:$PATH"

mkdir -p "$source_root" "$out_dir" "$deps_dir"

proot_dir="$source_root/proot"
if [[ ! -d "$proot_dir/.git" ]]; then
    git clone --depth 1 --branch "$proot_tag" https://github.com/termux/proot.git "$proot_dir"
fi
[[ "$(git -C "$proot_dir" rev-parse HEAD)" == "$proot_commit" ]] || {
    echo "unexpected PRoot revision" >&2
    exit 1
}

download_checked() {
    local url="$1" output="$2" expected="$3"
    if [[ ! -f "$output" ]]; then
        curl -fsSL "$url" -o "$output"
    fi
    local actual
    actual="$(shasum -a 256 "$output" | awk '{print $1}')"
    [[ "$actual" == "$expected" ]] || {
        echo "SHA-256 mismatch for $output" >&2
        exit 1
    }
}

talloc_archive="$source_root/talloc-${talloc_version}.tar.gz"
download_checked \
    "https://www.samba.org/ftp/talloc/talloc-${talloc_version}.tar.gz" \
    "$talloc_archive" "$talloc_sha256"
talloc_dir="$source_root/talloc-${talloc_version}"
if [[ ! -f "$talloc_dir/wscript" ]]; then
    tar -xzf "$talloc_archive" -C "$source_root"
fi

shmem_archive="$source_root/libandroid-shmem-${shmem_version}.tar.gz"
download_checked \
    "https://github.com/termux/libandroid-shmem/archive/refs/tags/v${shmem_version}.tar.gz" \
    "$shmem_archive" "$shmem_sha256"
shmem_dir="$source_root/libandroid-shmem-${shmem_version}"
if [[ ! -f "$shmem_dir/Makefile" ]]; then
    tar -xzf "$shmem_archive" -C "$source_root"
fi

if ! rg -q 'ashv_key_path' "$shmem_dir/shmem.c"; then
    patch -d "$shmem_dir" -p0 < "$script_dir/patches/libandroid-shmem-runtime-tmp.patch"
fi
if ! rg -q '#include <string.h>' "$proot_dir/src/extension/ashmem_memfd/ashmem_memfd.c"; then
    patch -d "$proot_dir" -p0 < "$script_dir/patches/proot-android-includes.patch"
fi

host_cc="$ndk_bin/aarch64-linux-android${api}-clang"
host_ar="$ndk_bin/llvm-ar"
host_ranlib="$ndk_bin/llvm-ranlib"
host_strip="$ndk_bin/llvm-strip"
host_objcopy="$ndk_bin/llvm-objcopy"
host_objdump="$ndk_bin/llvm-objdump"

talloc_prefix="$build_root/talloc-stage/opt/pokewilds/host"
if [[ ! -f "$talloc_prefix/lib/libtalloc.so.2.4.2" ]]; then
    rm -rf "$build_root/talloc-build" "$build_root/talloc-stage"
    (
        cd "$talloc_dir"
        CC="$host_cc" AR="$host_ar" RANLIB="$host_ranlib" STRIP="$host_strip" CFLAGS='-fPIC' \
            ./configure --out="$build_root/talloc-build" --prefix=/opt/pokewilds/host \
            --disable-python --without-gettext --bundled-libraries=ALL \
            --disable-symbol-versions --cross-compile --cross-execute=/usr/bin/true --hostcc=clang
        PYTHONHASHSEED=1 CC="$host_cc" AR="$host_ar" RANLIB="$host_ranlib" STRIP="$host_strip" \
            ./buildtools/bin/waf build --targets=talloc -j2
        PYTHONHASHSEED=1 CC="$host_cc" AR="$host_ar" RANLIB="$host_ranlib" STRIP="$host_strip" \
            ./buildtools/bin/waf install --destdir="$build_root/talloc-stage"
    )
fi

shmem_prefix="$build_root/shmem-stage/opt/pokewilds/host"
if [[ ! -f "$shmem_prefix/lib/libandroid-shmem.so" ]]; then
    mkdir -p "$shmem_prefix/lib" "$shmem_prefix/include/sys"
    make -C "$shmem_dir" clean >/dev/null 2>&1 || true
    make -C "$shmem_dir" CC="$host_cc" AR="$host_ar" \
        CFLAGS='-fPIC -std=c11 -Wall -Wextra' libandroid-shmem.a libandroid-shmem.so
    cp "$shmem_dir/libandroid-shmem.a" "$shmem_prefix/lib/"
    cp "$shmem_dir/libandroid-shmem.so" "$shmem_prefix/lib/"
    cp "$shmem_dir/shm.h" "$shmem_prefix/include/sys/"
fi

proot_src="$proot_dir/src"
make -C "$proot_src" -f GNUmakefile clean >/dev/null 2>&1 || true
proot_runpath="-L${talloc_prefix}/lib -L${shmem_prefix}/lib -Wl,-rpath,"'\$$ORIGIN'" -ltalloc -landroid-shmem -llog -landroid -Wl,-z,noexecstack"
make -C "$proot_src" -f GNUmakefile -j2 \
    CC="$host_cc" LD="$host_cc" STRIP="$host_strip" OBJCOPY="$host_objcopy" OBJDUMP="$host_objdump" \
    CPPFLAGS="-D_FILE_OFFSET_BITS=64 -D_GNU_SOURCE -I. -I$proot_src -I$talloc_prefix/include -I$shmem_prefix/include" \
    LDFLAGS="$proot_runpath" PROOT_WITH_LIBANDROID_SHMEM=true PROOT_UNBUNDLE_LOADER=.

cp "$proot_src/proot" "$out_dir/libproot.so"
cp "$proot_src/loader/loader" "$out_dir/libproot-loader.so"
"$host_strip" --strip-unneeded "$out_dir/libproot.so"
"$host_strip" --strip-all "$out_dir/libproot-loader.so"

# Keep the PRoot runtime dependencies beside the executable for nativeLibraryDir.
cp "$talloc_prefix/lib/libtalloc.so.2.4.2" "$deps_dir/"
ln -sf libtalloc.so.2.4.2 "$deps_dir/libtalloc.so.2"
cp "$shmem_prefix/lib/libandroid-shmem.so" "$deps_dir/"

mkdir -p "$host_lib_dir"
install_dependency() {
    local source="$1" target="$host_lib_dir/$(basename "$1")"
    if [[ -e "$target" ]] && ! cmp -s "$source" "$target"; then
        echo "refusing to replace differing host dependency: $target" >&2
        exit 1
    fi
    cp "$source" "$target"
}
install_dependency "$talloc_prefix/lib/libtalloc.so.2.4.2"
install_dependency "$shmem_prefix/lib/libandroid-shmem.so"
ln -sf libtalloc.so.2.4.2 "$host_lib_dir/libtalloc.so.2"

file "$out_dir/libproot.so" "$out_dir/libproot-loader.so"
"$ndk_bin/llvm-readelf" -d "$out_dir/libproot.so" | rg 'RUNPATH|NEEDED'
