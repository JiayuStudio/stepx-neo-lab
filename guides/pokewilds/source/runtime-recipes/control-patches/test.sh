#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
game="${1:-../../../PokeWilds-v0.8.11-decompiled/release/pokewilds.jar}"
POKEWILDS_GAME_JAR="$game" bash build.sh
mkdir -p build/test build/fixtures
asm=../game-patches/dist/bugfix.jar
if [[ ! -f "$asm" ]]; then bash ../game-patches/build.sh; fi
javac -cp "$asm" -d build/test tests/PrepareHeadlessGame.java
java -cp "$asm:build/test" PrepareHeadlessGame "$game" build/fixtures
javac -cp "$asm:$game:dist/control-patches.jar" -d build/test tests/com/pkmngen/game/*.java
java -Xverify:all -cp "build/fixtures:$asm:$game:dist/control-patches.jar:build/test" com.pkmngen.game.MapShortcutTest
java -Xverify:all -cp "build/fixtures:$asm:$game:dist/control-patches.jar:build/test" com.pkmngen.game.ControllerStartTest
