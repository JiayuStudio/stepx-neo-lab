#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
JDK="${JAVA_HOME:?Set JAVA_HOME}"
KEYDIR="${NEO_AGENT_SIGNING_DIR:?Set private signing directory}"
BT="$SDK/build-tools/36.0.0"
R8="${R8_JAR:?Set R8_JAR}"
MODE="${1:-release}"
case "$MODE" in release|debug) ;; *) echo "Build mode must be release or debug" >&2; exit 2;; esac
APP_VERSION=$(python3 -c 'import xml.etree.ElementTree as E;print(E.parse("AndroidManifest.xml").getroot().get("{http://schemas.android.com/apk/res/android}versionName"))')
rm -rf out/classes out/dex out/gen out/voice-libs
mkdir -p out/classes out/dex out/gen dist
VOICE_JARS=()
VOICE_CP=""
if [ "$MODE" = debug ]; then
 python3 vendor/voice/prepare.py
 VOICE_JARS=(out/voice-libs/*.jar)
 VOICE_CP=$(IFS=:; echo "${VOICE_JARS[*]}")
fi
python3 - "$MODE" <<'PYASSETS'
from pathlib import Path
import shutil,sys
root=Path('out/app-assets');shutil.rmtree(root,ignore_errors=True)
shutil.copytree('assets',root,ignore=shutil.ignore_patterns('voice') if sys.argv[1]=='release' else None)
PYASSETS
if [ ! -d "$KEYDIR" ]; then mkdir -p "$KEYDIR"; chmod 700 "$KEYDIR"; fi
if [ ! -f "$KEYDIR/keystore.p12" ]; then
 umask 077
 /usr/bin/openssl rand -hex 24 > "$KEYDIR/password"
 "$JDK/bin/keytool" -genkeypair -keystore "$KEYDIR/keystore.p12" -storepass:file "$KEYDIR/password" -keypass:file "$KEYDIR/password" -alias neoagent -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Neo Agent' >/dev/null 2>&1
fi
cp AndroidManifest.xml out/AndroidManifest.xml
if [ "$MODE" = release ];then python3 -c 'from pathlib import Path;p=Path("out/AndroidManifest.xml");p.write_text(p.read_text().replace("<uses-permission android:name=\"android.permission.RECORD_AUDIO\"/>",""))';fi
if [ "$MODE" = debug ];then python3 -c 'from pathlib import Path;p=Path("out/AndroidManifest.xml");p.write_text(p.read_text().replace("<application ","<application android:debuggable=\"true\" ").replace("</application>","<activity android:name=\".FlowQaActivity\" android:exported=\"true\" android:permission=\"android.permission.DUMP\"/><activity android:name=\".QaActivity\" android:exported=\"true\" android:permission=\"android.permission.DUMP\"/><activity android:name=\".UiQaActivity\" android:exported=\"true\" android:permission=\"android.permission.DUMP\" android:configChanges=\"orientation|screenSize|keyboardHidden\"/><receiver android:name=\".InspectReceiver\" android:exported=\"true\" android:permission=\"android.permission.DUMP\"/></application>"))';fi
"$BT/aapt2" compile --dir res -o out/compiled.zip
"$BT/aapt2" link -o out/resources.apk -I "$SDK/platforms/android-36/android.jar" --manifest out/AndroidManifest.xml --java out/gen -A out/app-assets out/compiled.zip
SOURCES=(src/studio/jiayu/neoagent/*.java)
if [ "$MODE" = debug ];then SOURCES+=(tests/debug-src/studio/jiayu/neoagent/*.java);else
 FILTERED_SOURCES=()
 for SOURCE in "${SOURCES[@]}";do if [ "$SOURCE" != src/studio/jiayu/neoagent/OfflineVoice.java ];then FILTERED_SOURCES+=("$SOURCE");fi;done
 SOURCES=("${FILTERED_SOURCES[@]}" release-src/studio/jiayu/neoagent/OfflineVoice.java)
fi
"$JDK/bin/javac" -encoding UTF-8 -source 8 -target 8 -bootclasspath "$SDK/platforms/android-36/android.jar:$BT/core-lambda-stubs.jar" -classpath "$VOICE_CP" -d out/classes "${SOURCES[@]}"
if [ "$MODE" = debug ];then
 find out/classes -name '*.class' -print0 | xargs -0 "$JDK/bin/java" -cp "$R8" com.android.tools.r8.D8 --min-api 30 --lib "$SDK/platforms/android-36/android.jar" --output out/dex "${VOICE_JARS[@]}"
else
 find out/classes -name '*.class' -print0 | xargs -0 "$JDK/bin/java" -cp "$R8" com.android.tools.r8.D8 --min-api 30 --lib "$SDK/platforms/android-36/android.jar" --output out/dex
fi
python3 - "$MODE" <<'PY'
from pathlib import Path
import zipfile,shutil,sys
shutil.copyfile('out/resources.apk','out/unaligned.apk')
with zipfile.ZipFile('out/unaligned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for p in Path('out/dex').glob('*.dex'):z.write(p,p.name)
 if sys.argv[1]=='debug':
  for p in Path('out/voice-libs/lib').glob('*/*.so'):z.write(p,'lib/'+p.parent.name+'/'+p.name)
PY
"$BT/zipalign" -f 4 out/unaligned.apk out/aligned.apk
JAVA_HOME="$JDK" "$BT/apksigner" sign --ks "$KEYDIR/keystore.p12" --ks-key-alias neoagent --ks-pass "file:$KEYDIR/password" --out "dist/Neo-Agent-Touch-${APP_VERSION}-$MODE.apk" out/aligned.apk
JAVA_HOME="$JDK" "$BT/apksigner" verify "dist/Neo-Agent-Touch-${APP_VERSION}-$MODE.apk"
(cd dist && shasum -a 256 "Neo-Agent-Touch-${APP_VERSION}-$MODE.apk" > "Neo-Agent-Touch-${APP_VERSION}-$MODE.sha256")
