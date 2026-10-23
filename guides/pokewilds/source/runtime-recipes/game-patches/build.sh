#!/bin/bash
# SPDX-License-Identifier: MIT
# Builds the four-fix Java agent and offline patcher.
set -euo pipefail
cd "$(dirname "$0")"

ASM_VERSION=9.7
BASE=https://repo.maven.apache.org/maven2/org/ow2/asm
expected_sha256() {
  case "$1" in
    asm) echo adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d ;;
    asm-commons) echo 389bc247958e049fc9a0408d398c92c6d370c18035120395d4cba1d9d9304b7a ;;
    asm-tree) echo 62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1 ;;
    *) echo "unknown ASM artifact: $1" >&2; return 2 ;;
  esac
}
sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d' ' -f1
  else
    shasum -a 256 "$1" | cut -d' ' -f1
  fi
}

rm -rf build/tools build/agent build/asm-relocated.jar build/manifest.txt dist
mkdir -p build/lib build/tools build/agent build/resources/META-INF/licenses dist
for artifact in asm asm-commons asm-tree; do
  jar_path="build/lib/$artifact-$ASM_VERSION.jar"
  if [ ! -f "$jar_path" ]; then
    curl -fsSL -o "$jar_path" "$BASE/$artifact/$ASM_VERSION/$artifact-$ASM_VERSION.jar"
  fi
  if [ "$(sha256 "$jar_path")" != "$(expected_sha256 "$artifact")" ]; then
    echo "SHA-256 mismatch for $jar_path" >&2
    rm -f "$jar_path"
    exit 1
  fi
done
cp ASM-LICENSE.txt build/resources/META-INF/licenses/ASM-LICENSE.txt
cp LICENSE build/resources/META-INF/licenses/BUGFIX-LICENSE.txt
cp PROVENANCE.md build/resources/META-INF/BUGFIX-PROVENANCE.md

CP="build/lib/asm-$ASM_VERSION.jar:build/lib/asm-commons-$ASM_VERSION.jar:build/lib/asm-tree-$ASM_VERSION.jar"
javac -d build/tools -cp "$CP" tools/RelocateAsm.java
java -cp "build/tools:$CP" RelocateAsm "build/lib/asm-$ASM_VERSION.jar" build/asm-relocated.jar "local/pokewilds/bugfix/asm/"
javac --release 8 -d build/agent -cp build/asm-relocated.jar src/local/pokewilds/bugfix/*.java
(cd build/agent && jar xf ../asm-relocated.jar)
printf 'Premain-Class: local.pokewilds.bugfix.BugFixAgent\nMain-Class: local.pokewilds.bugfix.PatchJar\n' > build/manifest.txt
jar --create --file dist/bugfix.jar --manifest build/manifest.txt -C build/agent . -C build/resources .
echo "Built dist/bugfix.jar"
