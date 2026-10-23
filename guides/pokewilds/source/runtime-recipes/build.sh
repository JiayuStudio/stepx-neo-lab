#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK with platform 36 and NDK 29.0.14206865}"
export ANDROID_NDK_ROOT="${ANDROID_NDK_ROOT:-$ANDROID_HOME/ndk/29.0.14206865}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PWD/.cache/gradle}"
if [[ -z "${X11_SOURCE_DIR:-}" ]]; then ./prepare.sh; fi
python3 packaging/host_packages.py build packaging/host-packages.lock.json
./native/build-proot.sh
# The guest helper is Linux/glibc, not Android/Bionic. Build it through the
# pinned Docker recipe by default; an ARM64 Linux build may supply it directly.
close_helper="${POKEWILDS_CLOSE_HELPER:-runtime/cache/pokewilds-close}"
if [[ -z "${POKEWILDS_CLOSE_HELPER:-}" ]]; then
  ./runtime/build-close-helper-docker.sh "$close_helper"
fi
./runtime/build-payload.sh --cache runtime/cache --output runtime/build/assets/runtime.tar.gz \
  --properties runtime/build/assets/payload.properties --close-helper "$close_helper" --host-dir native/build/host
./gradlew :android-app:testDebugUnitTest :android-app:assembleDebug "$@"
printf 'APK: %s/android-app/build/outputs/apk/debug/android-app-debug.apk\n' "$PWD"
