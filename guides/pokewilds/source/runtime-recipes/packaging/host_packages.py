#!/usr/bin/env python3
"""Assemble the pinned Android graphics/audio closure; do not run package scripts.

Termux packages are used as compatibility-prototype inputs, not assumed relocatable.
The APK supplies library/module paths explicitly; emulator probes are required.
"""
import argparse, hashlib, json, pathlib, re, shutil, subprocess, urllib.request
ROOT=pathlib.Path(__file__).resolve().parents[1]
BASE='https://packages.termux.dev/apt/termux-main/'

def lock(index, output):
    packages={}
    for block in pathlib.Path(index).read_text().split('\n\n'):
        fields={}
        for line in block.splitlines():
            if ': ' in line and not line.startswith(' '):
                k,v=line.split(': ',1); fields[k]=v
        if 'Package' in fields: packages[fields['Package']]=fields
    selected={}
    def add(name):
        if name in selected: return
        p=packages[name]; selected[name]={k:p[k] for k in ['Package','Version','Filename','SHA256']}
        for d in p.get('Depends','').split(','):
            if d.strip(): add(re.split(r'[ (]',d.strip().split('|')[0].strip())[0])
    for name in ['virglrenderer-android','pulseaudio']: add(name)
    pathlib.Path(output).write_text(json.dumps({'repository':BASE,'packages':[selected[k] for k in sorted(selected)]},indent=2)+'\n')

def build(lockfile):
    data=json.loads(pathlib.Path(lockfile).read_text())
    cache=ROOT/'.cache/host-packages'; cache.mkdir(parents=True,exist_ok=True)
    stage=ROOT/'native/build/host'
    # This is a generated tree. Recreate it so links and removed package files
    # cannot leak from an earlier build. build-proot.sh adds its closure next.
    shutil.rmtree(stage,ignore_errors=True); stage.mkdir(parents=True)
    jni=ROOT/'native/build/jniLibs/arm64-v8a'; jni.mkdir(parents=True,exist_ok=True)
    unpack=cache/'unpack'; shutil.rmtree(unpack,ignore_errors=True); unpack.mkdir()
    for p in data['packages']:
        f=cache/pathlib.Path(p['Filename']).name
        if not f.exists(): urllib.request.urlretrieve(data['repository']+p['Filename'],f)
        if hashlib.sha256(f.read_bytes()).hexdigest()!=p['SHA256']: raise ValueError('Checksum mismatch: '+str(f))
        part=cache/'part'; shutil.rmtree(part,ignore_errors=True); part.mkdir()
        blob=f.read_bytes()
        if not blob.startswith(b'!<arch>\n'): raise ValueError('Not a Debian ar archive')
        offset=8; tar=None
        while offset+60<=len(blob):
            header=blob[offset:offset+60]; size=int(header[48:58]); name=header[:16].decode().strip().rstrip('/')
            body=blob[offset+60:offset+60+size]
            if name.startswith('data.tar.'):
                tar=part/name; tar.write_bytes(body); break
            offset+=60+size+(size%2)
        if tar is None: raise ValueError('Missing Debian data archive')
        subprocess.run(['tar','-xf',str(tar.resolve()),'-C',str(unpack)],check=True)
    prefix=unpack/'data/data/com.termux/files/usr'
    for name in ['lib','etc','share','opt']:
        source=prefix/name
        if source.exists(): shutil.copytree(source,stage/name,symlinks=True,dirs_exist_ok=True)
    # Android's graphics libraries require the platform binder symbol versions.
    # Termux's compatibility shim shadows that library through LD_LIBRARY_PATH
    # and makes system EGL fail to load on Android 13.
    (stage/'lib/libbinder_ndk.so').unlink(missing_ok=True)
    for original,renamed in [('virgl_test_server_android','libvirgl_test_server_android.so'),('pulseaudio','libpulseaudio.so')]:
        shutil.copy2(prefix/'bin'/original,jni/renamed)
    # The pinned VirGL executable contains absolute ANGLE directory literals.
    # Make those relative to the host payload working directory, preserving ELF
    # offsets. Fail closed if an upstream update changes the expected literals.
    virgl = jni/'libvirgl_test_server_android.so'
    blob = virgl.read_bytes()
    for backend in ['gl', 'vulkan', 'vulkan-null']:
        old = ('/data/data/com.termux/files/usr/opt/angle-android/' + backend).encode() + b'\0'
        new = ('./opt/angle-android/' + backend).encode() + b'\0'
        if blob.count(old) != 1: raise ValueError('Unexpected ANGLE path layout: ' + backend)
        blob = blob.replace(old, new.ljust(len(old), b'\0'))
    virgl.write_bytes(blob)
    # Build inputs retain package copyright/source notices from share/doc.
    shutil.copy2(lockfile,stage/'packages.lock.json')
    print('Built host payload:',stage)
    print('Packages:',len(data['packages']))

if __name__=='__main__':
    p=argparse.ArgumentParser(); sub=p.add_subparsers(dest='command',required=True)
    l=sub.add_parser('lock');l.add_argument('index');l.add_argument('output')
    b=sub.add_parser('build');b.add_argument('lockfile')
    a=p.parse_args()
    if a.command=='lock': lock(a.index,a.output)
    else: build(a.lockfile)
