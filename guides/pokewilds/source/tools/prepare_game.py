#!/usr/bin/env python3
"""Apply Neo hooks to an unmodified Chinese 0.8.11 intermediate JAR."""
from pathlib import Path
import argparse,copy,hashlib,json,os,shutil,subprocess,tempfile,zipfile

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--chinese-jar',type=Path,required=True,help='Chinese intermediate, before NeoBridge hooks')
args=parser.parse_args()
java_home=Path(os.environ['JAVA_HOME']) if os.environ.get('JAVA_HOME') else None
def java(name):
    value=str(java_home/'bin'/name) if java_home else shutil.which(name)
    if not value:raise SystemExit('Set JAVA_HOME to a JDK 17 or newer')
    return value
build=ROOT/'build';build.mkdir(exist_ok=True)
classes=build/'classes';classes.mkdir(exist_ok=True)
cp=os.pathsep.join(map(str,[ROOT/'tools/asm.jar',ROOT/'tools/asm-tree.jar',args.chinese_jar.resolve()]))
sources=[ROOT/'tools/java/com/pkmngen/game/NeoBridge.java',ROOT/'tools/java/com/pkmngen/game/ZhDialogs.java',ROOT/'tools/java/PatchNeo.java']
subprocess.run([java('javac'),'--release','17','-cp',cp,'-d',str(classes),*map(str,sources)],check=True)
patched=build/'pokewilds-neo-unfiltered.jar'
subprocess.run([java('java'),'-cp',str(classes)+os.pathsep+cp,'PatchNeo',str(args.chinese_jar.resolve()),str(patched),str(classes/'com/pkmngen/game/NeoBridge.class')],check=True)
jar=build/'pokewilds.jar'
with zipfile.ZipFile(patched) as source,zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as out:
    for item in source.infolist():
        if item.filename.endswith('.dylib') or item.filename in {'rebel.xml','localization/reference8-source.json'}:continue
        out.writestr(copy.copy(item),source.read(item))
assets=ROOT/'android/android-app/src/main/assets'
archive=assets/'neo-game.zip'
if not archive.exists():raise SystemExit('Run bootstrap.py first to restore the release README and settings inputs')
with tempfile.TemporaryDirectory(dir=build) as d:
    tmp=Path(d)/'neo-game.zip'
    with zipfile.ZipFile(archive) as src,zipfile.ZipFile(tmp,'w',zipfile.ZIP_DEFLATED) as out:
        for item in src.infolist():
            out.writestr(copy.copy(item),jar.read_bytes() if item.filename.endswith('/pokewilds.jar') else src.read(item))
    shutil.copyfile(tmp,archive)
s=json.loads((assets/'game-source.json').read_text())
s['sha256']=hashlib.sha256(archive.read_bytes()).hexdigest();s['jarSha256']=hashlib.sha256(jar.read_bytes()).hexdigest()
(assets/'game-source.json').write_text(json.dumps(s,indent=2)+'\n')
print('Neo game hooks and embedded source checksums updated.')
