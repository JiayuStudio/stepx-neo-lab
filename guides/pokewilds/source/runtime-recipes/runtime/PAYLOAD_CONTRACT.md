# Standalone payload contract

The runtime archive is an immutable, offline input to the Android packaging
step. It contains exactly these top-level entries:

```text
payload-manifest.json  # generated manifest, including every archive member hash
rootfs/                # ARM64 Linux/glibc guest filesystem
notices/               # upstream notices copied from the pinned inputs
host/                  # optional Android host closure supplied by native build
```

The Android supervisor owns the process that enters `rootfs/`. Game files are
acquired separately at first launch using the pinned source metadata in
`standalone/packaging/game-source.json`; they are never included in this runtime
archive. Worlds, settings, logs, sockets, and runtime staging data are outside
this archive and must never be added by the builder.

The service-facing guest paths are `/usr/bin/java`, `/usr/bin/xdotool`, and
`/usr/local/bin/pokewilds-close`. The JRE itself is rooted at
`/opt/pokewilds/jre`; the builder creates compatibility links at
`/usr/lib/jvm/pokewilds-jre` and `/usr/bin/java` only after the JRE archive has
been verified. `pokewilds-close` must be a
Linux ARM64/glibc build of the checked-in X11 helper. A macOS host cannot use a
native macOS build for that slot, so the builder accepts an explicit
`--close-helper` cross-build output and fails if it is absent.

`manifest.json` is the source lock file. Every downloadable input has a
release-pinned HTTPS URL and a 64-character SHA-256 digest. The package
closure is represented by one aggregate lock plus an individual digest for
each of its 131 official Ubuntu Ports `.deb` artifacts. The aggregate input
has no URL because it is the checked-in set of those artifact URLs; it is not
an instruction to read packages from a local machine.

The runtime lock names Canonical's Ubuntu Base 24.04.3 ARM64 archive, Eclipse
Temurin JRE 17.0.15+6 for Linux AArch64, and the Ubuntu Noble ARM64 Mesa/X11
package closure needed by the known-good virpipe launch. PokeWilds v0.8.11 is
pinned separately in `standalone/packaging/game-source.json`, including both
the upstream archive SHA-256 and the extracted JAR SHA-256. Current Ubuntu Mesa
names the DRI entrypoint
`virtio_gpu_dri.so`; this is the virpipe driver slot formerly called
`virgl_dri.so`. The JRE and game digests are pinned from their official
release assets, and every Ubuntu package version and digest is recorded in the
lock. URLs containing an encoded Debian epoch such as `%3a` are intentional;
the builder uses the manifest filename when locating a cached artifact.

`build-payload.sh` accepts a cache directory and writes a deterministic gzip
tar archive plus a `payload.properties` sidecar containing the final archive
SHA-256 and expanded byte count. The legacy `gameBytes` property is zero. It verifies every
source before extraction, rejects absolute or escaping archive paths, rejects
unsupported architectures, and emits a manifest containing the SHA-256 of
every produced member. `verify-payload.sh` can be run independently by the
Android packaging build and on CI.

The exact fresh-machine sequence is:

```sh
mkdir -p standalone/runtime/cache standalone/runtime/build/assets
standalone/runtime/build-close-helper-docker.sh \
  standalone/runtime/cache/pokewilds-close
standalone/runtime/build-payload.sh \
  --cache standalone/runtime/cache \
  --output standalone/runtime/build/assets/runtime.tar.gz \
  --close-helper standalone/runtime/cache/pokewilds-close \
  --host-dir standalone/native/build/host \
  --properties standalone/runtime/build/assets/payload.properties \
  --log standalone/standalone-build-logs/payload-build.log
standalone/runtime/verify-payload.sh \
  standalone/runtime/build/assets/runtime.tar.gz
```

The first payload build downloads the three pinned runtime inputs and their package
artifacts into the cache and verifies each digest. The game ZIP is fetched by the
Android app at first launch and checked against the APK's pinned game-source metadata. Add `--offline` on later
rebuilds to require that cache to be complete. `--host-dir` is the separately
built Android host closure from `standalone/native`; it is an explicit input,
so a fresh machine must build that closure before invoking the command above.

`build-close-helper-docker.sh` is the source-reproducible path for macOS. It
uses the pinned official `ubuntu:24.04` ARM64 image digest
`sha256:008173c23f95b170204355c12626cb5a965d779a7e1283b09e9cffbb1bf33ca3`,
mounts the checked-in `pokewilds-close.c` read-only, installs the image's
glibc/X11 development packages, and compiles with `gcc -O2 -Wall -Wextra`.
The source digest is
`c4cdb336033eb873f131156198d542e922eaceb4e080b35b900a4660b25aa8ee`; the
expected ARM64 helper digest is
`cacdea4f6076c78466708631e22394f09c8df1caa5e3047ff08ceade51e3e4b3`.
The script fails if either the source or resulting helper differs, so source
changes require an intentional provenance update.

The optional `--host-dir` argument copies the already-built Android/Bionic
host closure under `host/` (including symlinks) so the APK packaging step can
ship one archive. It is deliberately an explicit input: the Linux guest
rootfs and Android host libraries must not be mixed or silently synthesized by
this builder.

This archive is a Linux guest payload. It does not claim that Android can
execute files extracted to writable app data: the host-side Android bootstrap,
PRoot placement, VirGL server, audio bridge, and embedded X11 surface remain
separate implementation gates.
