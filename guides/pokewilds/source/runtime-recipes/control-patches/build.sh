#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

expected=0a72e17bf5cc3bd14ac97a6d001fe16c32cf8bd2d6caf1271bb2d3f234a6f248
game_jar="${POKEWILDS_GAME_JAR:-}"
if [[ -z "$game_jar" && -f ../../../PokeWilds-v0.8.11-decompiled/release/pokewilds.jar ]]; then
  game_jar=../../../PokeWilds-v0.8.11-decompiled/release/pokewilds.jar
fi
if [[ -z "$game_jar" ]]; then
  mkdir -p build/cache
  game_jar=build/cache/pokewilds.jar
  if [[ ! -f "$game_jar" ]]; then
    curl -fsSL -o build/cache/official.zip \
      https://github.com/SheerSt/pokewilds/releases/download/v0.8.11/pokewilds-otherplatforms.zip
    unzip -p build/cache/official.zip pokewilds-v0.8.11-otherplatforms/pokewilds.jar > "$game_jar"
  fi
fi
if command -v sha256sum >/dev/null 2>&1; then
  actual=$(sha256sum "$game_jar" | cut -d' ' -f1)
else
  actual=$(shasum -a 256 "$game_jar" | cut -d' ' -f1)
fi
if [[ "$actual" != "$expected" ]]; then
  echo "Controls agent requires the official PokeWilds 0.8.11 JAR" >&2
  exit 1
fi
rm -rf build/classes dist
mkdir -p build/classes dist
javac --release 8 -Xlint:-options -cp "$game_jar" -d build/classes src/com/pkmngen/game/*.java
printf 'Premain-Class: com.pkmngen.game.OdinAgent\n' > build/manifest.txt
jar --create --file dist/control-patches.jar --manifest build/manifest.txt -C build/classes .
