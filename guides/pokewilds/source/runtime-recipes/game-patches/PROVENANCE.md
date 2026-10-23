# PokeWilds 0.8.11 bug fixes

This directory vendors the four-fix implementation from
[`Divinakra140/PokeWilds-0.8.11-BugFix-Patcher`](https://github.com/Divinakra140/PokeWilds-0.8.11-BugFix-Patcher),
commit `93f633a0fa7b6afe5b5318cff7868320fdddb505` (version 1.0, 2026-09-25).
The Java sources and source tests are kept under their original package and
retain their original copyright and SPDX notices. The floor patch is extended
locally with a versioned exact-placement entry in map save ZIPs and a loader
for that entry. The legacy `data.json` remains present. These changes make the
patched bytecode intentionally different from upstream v1.0. Sources are
licensed under MIT; see [LICENSE](LICENSE).

`PromptPatch.java` is a locally authored, opt-in patch. It changes the listed
controller instruction literals in the PokeWilds 0.8.11 game classes and lets
the nickname and sign editors accept a one-shot controller/touch confirmation
while retaining keyboard Enter. The agent applies it only when launched with
`-Dbugfix.prompts=true`; the standalone patcher keeps its existing default and
requires `--prompts`. The Android host enables it for the guest JVM through
`bugfix.prompts` and routes Start to the active editor through
`ControllerConfirm`. The transformation, confirmation identity/one-shot
behavior, and independent toggle are covered by `TestPrompts` and
`TestToggleMatrix`.

`build.sh` builds `dist/bugfix.jar`, which is both a Java agent and includes the
upstream offline patcher entry point. ASM 9.7 (`asm`, `asm-commons`, and
`asm-tree`) is fetched from Maven Central and checked against the SHA-256 values
pinned in the script. ASM is relocated into `local.pokewilds.bugfix.asm` to
avoid conflicts with the ASM copy bundled in PokeWilds. Its BSD-3-Clause notice
is included in the resulting JAR at `META-INF/licenses/ASM-LICENSE.txt`.

Build and run the bytecode, class-verification, agent-loading, floor-map logic,
and prompt transformation checks against the locally obtained official 0.8.11 JAR:

```sh
./build.sh
./test.sh /path/to/pokewilds.jar
```

The generated `dist/bugfix.jar` is the asset the Android app can bundle and
stage for the guest JVM. No PokeWilds game files are included here.
