# Android host runtime

`build-proot.sh` builds pinned ARM64 Android PRoot and its static loader using
NDK 29. It also builds talloc and libandroid-shmem, patches temporary-directory
handling, and copies those dependencies into `build/host/lib`.

```sh
ANDROID_NDK_ROOT=/path/to/ndk/29.0.14206865 ./native/build-proot.sh
```

Outputs:

- `build/jniLibs/arm64-v8a/libproot.so`: Android PIE executable.
- `build/jniLibs/arm64-v8a/libproot-loader.so`: static guest loader executable.
- `build/host/lib`: loader dependencies included in the runtime archive.

The `lib*.so` names make Android package/install these executables in
`nativeLibraryDir`. They are executed as processes, not loaded as JNI libraries.
`verify-elf.sh` checks these rebuilt PRoot artifacts; it is not a relocation
validator for arbitrary Termux packages.

Graphics/audio packages are assembled by `../packaging/host_packages.py` from
`../packaging/host-packages.lock.json`. See `../UPSTREAM.md` for provenance.
The Java `RuntimeService` owns the actual path/environment/launch contract;
there is no separate duplicate native contract library.

PRoot version and a minimal bind/guest command were verified on an Android 13
ARM64 emulator. Full game validation is recorded in the standalone test report.
