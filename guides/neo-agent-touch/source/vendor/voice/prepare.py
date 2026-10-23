#!/usr/bin/env python3
"""Verify pinned offline-ASR files and extract only build inputs, without network access."""
from pathlib import Path
from zipfile import ZipFile
import hashlib,json,struct

root=Path(__file__).resolve().parents[2]
manifest=json.loads((root/'vendor/voice/dependencies.json').read_text())
target=root/'out/voice-libs'
target.mkdir(parents=True,exist_ok=True)
native=[]
for dependency in manifest['dependencies']:
    source=root/dependency['file']
    if source.stat().st_size!=dependency['bytes'] or hashlib.sha256(source.read_bytes()).hexdigest()!=dependency['sha256']:
        raise SystemExit('Offline voice dependency integrity mismatch: '+source.name)
    if source.suffix!='.aar':
        continue
    with ZipFile(source) as archive:
        (target/(source.stem+'.jar')).write_bytes(archive.read('classes.jar'))
        for abi in manifest['architectures']:
            entries=[item for item in archive.namelist() if item.startswith('jni/'+abi+'/') and item.endswith('.so')]
            if not entries:
                raise SystemExit('Missing offline voice ABI: '+abi)
            for entry in entries:
                data=archive.read(entry)
                if data[:4]!=b'\x7fELF' or data[4]!=2 or data[5]!=1:
                    raise SystemExit('Unexpected offline voice ELF format')
                offset=struct.unpack_from('<Q',data,32)[0]
                size,count=struct.unpack_from('<HH',data,54)
                alignments=[struct.unpack_from('<Q',data,offset+i*size+48)[0] for i in range(count) if struct.unpack_from('<I',data,offset+i*size)[0]==1]
                if not alignments or min(alignments)<16384:
                    raise SystemExit('Offline voice library lacks 16KB page alignment')
                path=target/'lib'/abi/Path(entry).name
                path.parent.mkdir(parents=True,exist_ok=True)
                path.write_bytes(data)
                native.append({'file':'lib/'+abi+'/'+path.name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'load_segment_alignment':min(alignments)})
(target/'native-report.json').write_text(json.dumps({'architectures':manifest['architectures'],'native':native},indent=2)+'\n')
print('Offline voice: pinned files verified; 2 Java libraries and '+str(len(native))+' aligned native libraries prepared')
