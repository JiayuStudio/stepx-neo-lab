#!/bin/bash
# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Divinakra
#
# Builds everything and runs the offline checks against the official PokeWilds 0.8.11 jar.
# Usage: ./test.sh /path/to/pokewilds.jar
set -euo pipefail
[ $# -eq 1 ] || { echo "usage: $0 /path/to/official/pokewilds.jar" >&2; exit 1; }
cd "$(dirname "$0")"
cleanup() { rm -rf build/test build/patched.jar build/again.jar build/inplace; }
trap cleanup EXIT
GAME="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
./build.sh
rm -rf build/test build/patched.jar && mkdir -p build/test
javac -d build/test -cp "dist/bugfix.jar:$GAME" tests/*.java

echo; echo "== 0. all 32 patch-toggle combinations"
java -cp "build/test:dist/bugfix.jar:$GAME" local.pokewilds.bugfix.TestToggleMatrix "$GAME" | grep '^PASS\|^FAIL'

echo; echo "== 0a. controller wording and configured-Start editor hooks"
java -cp "build/test:dist/bugfix.jar:$GAME" local.pokewilds.bugfix.TestPrompts "$GAME" | grep '^PASS\|^FAIL'

echo; echo "== 1. bytecode patches: every patched site accounted for, every class passes the verifier"
java -Xverify:all -cp "build/test:dist/bugfix.jar:$GAME" local.pokewilds.bugfix.TestFloors "$GAME" 2>&1 | grep -E "^(PASS|FAIL|reads|ALL OK|FAILURES)" | grep -v "^PASS untouched" | grep -v "tiles assignments hooked"
echo; echo "== 2. per-floor monster maps: logic"
java -cp "build/test:dist/bugfix.jar" local.pokewilds.bugfix.TestFloorMaps 2>&1 | grep -E "^(FAIL|ALL OK|FAILURES)|randomized"
echo; echo "== 3. as a -javaagent: all game classes load with the strict verifier"
java -Xverify:all -javaagent:dist/bugfix.jar -cp "$GAME:build/test" local.pokewilds.bugfix.LoadAll "$GAME" 2>&1 | grep -E "^\[bugfix\] (floors: game jar|agent)|loaded|VERIFY|reflection"
java -Xverify:all -javaagent:dist/bugfix.jar -cp "$GAME:build/test" HoOhCheck 2>&1 | tail -1
echo; echo "== 3a. the enabled prompt patch passes the strict agent verifier"
java -Xverify:all -Dbugfix.prompts=true -javaagent:dist/bugfix.jar -cp "$GAME:build/test" local.pokewilds.bugfix.LoadAll "$GAME" 2>&1 | grep -E "^\[bugfix\] (floors: game jar|agent)|loaded|VERIFY|reflection"
echo; echo "== 4. as an offline patch: patch the jar, then load it with NO agent"
java -jar dist/bugfix.jar "$GAME" build/patched.jar 2>&1 | grep -E "^(Patched|Wrote|ERROR)"
java -Xverify:all -cp "build/patched.jar:build/test" local.pokewilds.bugfix.LoadAll build/patched.jar 2>&1 | grep -E "loaded|VERIFY|reflection"
java -Xverify:all -cp "build/patched.jar:build/test" HoOhCheck 2>&1 | tail -1
echo; echo "== 4a. the offline prompt patch includes its helper and passes the strict verifier without an agent"
java -jar dist/bugfix.jar --prompts "$GAME" build/prompts-patched.jar 2>&1 | grep -E "^(Patched|Wrote|ERROR)"
jar tf build/prompts-patched.jar | grep -qx 'local/pokewilds/bugfix/ControllerConfirm.class' && echo "PASS ControllerConfirm is bundled" || { echo "FAIL ControllerConfirm is missing"; exit 1; }
java -Xverify:all -cp "build/prompts-patched.jar:build/test" local.pokewilds.bugfix.LoadAll build/prompts-patched.jar 2>&1 | grep -E "loaded|VERIFY|reflection"
echo; echo "== 5. the patcher refuses anything but the official jar"
AGAIN=$(java -jar dist/bugfix.jar build/patched.jar build/again.jar 2>&1 || true)
echo "$AGAIN" | grep -q "^ERROR" && echo "PASS an already patched jar is refused" || { echo "FAIL an already patched jar was accepted"; exit 1; }
[ ! -e build/again.jar ] && echo "PASS nothing was written" || { echo "FAIL a file was written"; exit 1; }
echo; echo "== 6. patch in place, keep a backup, restore"
rm -rf build/inplace && mkdir -p build/inplace && cp "$GAME" build/inplace/pokewilds.jar
ORIG_SUM=$(shasum -a 256 build/inplace/pokewilds.jar | cut -d' ' -f1)
java -jar dist/bugfix.jar build/inplace/pokewilds.jar 2>&1 | grep -E "^(Patched|Patch fingerprint|Done|Your original|ERROR)"
[ -f build/inplace/pokewilds-original.jar.bak ] && [ "$(shasum -a 256 build/inplace/pokewilds-original.jar.bak | cut -d' ' -f1)" = "$ORIG_SUM" ] && echo "PASS the backup is byte-identical to the original"
TWICE=$(java -jar dist/bugfix.jar build/inplace/pokewilds.jar 2>&1 || true)
echo "$TWICE" | grep -q "already patched" && echo "PASS patching twice is refused, the patched jar is left alone" || { echo "FAIL patching twice was not refused"; exit 1; }
java -Xverify:all -cp "build/inplace/pokewilds.jar:build/test" HoOhCheck 2>&1 | tail -1
java -jar dist/bugfix.jar --restore build/inplace/pokewilds.jar 2>&1
[ "$(shasum -a 256 build/inplace/pokewilds.jar | cut -d' ' -f1)" = "$ORIG_SUM" ] && [ -f build/inplace/pokewilds-bugfix.jar ] && echo "PASS restore puts the byte-identical original back and keeps the patched jar"
echo; echo "All checks passed."
