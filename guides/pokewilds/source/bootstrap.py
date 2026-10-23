#!/usr/bin/env python3
"""Restore the exact binary inputs used by the public Android wrapper build."""
from pathlib import Path
import argparse, hashlib, shutil, urllib.request, zipfile

ROOT=Path(__file__).resolve().parent
SHA256='9dde4d17bf140ae38e7150809a9b80e08d947369119e7668f72e139b25f4e85a'
URL='https://github.com/JiayuStudio/stepx-neo-lab/releases/download/pokewilds-neo-v0.8.15/PokeWilds-Neo-0.8.15.apk'
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--apk',type=Path,help='Existing public 0.8.15 APK')
parser.add_argument('--download',action='store_true',help='Download the pinned 306 MiB public APK')
args=parser.parse_args()
if not args.apk and not args.download: parser.error('Choose --apk or --download')
apk=args.apk or ROOT/'.cache/PokeWilds-Neo-0.8.15.apk'
if not args.apk:
    apk.parent.mkdir(parents=True,exist_ok=True)
    if not apk.exists():
        tmp=apk.with_suffix('.part')
        try:
            with urllib.request.urlopen(URL,timeout=60) as r,tmp.open('wb') as f: shutil.copyfileobj(r,f)
            tmp.replace(apk)
        finally:
            tmp.unlink(missing_ok=True)
h=hashlib.sha256()
with apk.open('rb') as f:
    for chunk in iter(lambda:f.read(1024*1024),b''):h.update(chunk)
if h.hexdigest()!=SHA256:raise SystemExit('APK checksum mismatch; no files extracted')
with zipfile.ZipFile(apk) as z:
    for n in ('runtime.bin','neo-game.zip'):
        target=ROOT/'android/android-app/src/main/assets'/n
        with z.open('assets/'+n) as src,target.open('wb') as dst:shutil.copyfileobj(src,dst)
    native=ROOT/'android/native/build/jniLibs/arm64-v8a'
    native.mkdir(parents=True,exist_ok=True)
    for item in z.infolist():
        if item.filename.startswith('lib/arm64-v8a/') and item.filename.endswith('.so'):
            target=native/Path(item.filename).name
            with z.open(item) as src,target.open('wb') as dst:shutil.copyfileobj(src,dst)
print('Verified runtime, game and ARM64 native inputs restored.')
