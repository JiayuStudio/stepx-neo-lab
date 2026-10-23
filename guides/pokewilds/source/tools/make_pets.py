from PIL import Image,ImageDraw
from pathlib import Path
import zipfile,re,io,json,collections,os
from collections import deque
root=Path(__file__).resolve().parent.parent;old=root/'localization';out=root/'android/android-app/src/main/assets';(root/'evidence').mkdir(exist_ok=True);source=zipfile.ZipFile(Path(os.environ['POKEWILDS_ZH_JAR']))
def img(n):return Image.open(io.BytesIO(source.read(n))).convert('RGBA')
names={}
for line in (old/'extras/localization/pokemondisplayname_zh_CN.properties').read_text().splitlines():
 if '=' in line and not line.startswith('#'):k,v=line.split('=',1);names[k]=v
fronts={n.split('/')[-2]:n for n in source.namelist() if n.endswith('/front.png') and n.startswith('pokemon/')}
order=re.findall(r'db "([^"]+)"',source.read('pokemon/pokemon_names.asm').decode());order=[s.lower().replace('@','') for s in order];idx={n:i for i,n in enumerate(order)}
adjust=source.read('pokemon/pokemon_overworld_adjustments.asm').decode();idx.update({n.lower():int(i) for n,i in re.findall(r'(\w+):\s+db i, (\d+)',adjust)})
dex={n.lower():i for i,n in enumerate(source.read('pokemon/pokemon_to_index.txt').decode('cp1252').splitlines())}
atlas=img('pokemon/overworlds_sheet.png');crystal=img('pokemon/crystal-overworld-sprites1.png');pets={};missing=[];counts=collections.Counter()
for name in sorted(set(fronts)|set(order)):
 if '?' in name:continue # engine placeholder, not a species
 frames=[];method=''
 if name in idx:
  k=idx[name]*6;x=k%156*16;y=k//156*16
  if y+16<=atlas.height:frames=[atlas.crop((x+64,y,x+80,y+16)),atlas.crop((x+80,y,x+96,y+16))];method='original-overworld'
 if not frames and name in dex and dex[name]<251:
  k=dex[name];x=1+(k%15)*34;y=31+(k//15)*25
  frames=[crystal.crop((x,y,x+16,y+16)),crystal.crop((x+17,y,x+33,y+16))];method='original-crystal'
 if not frames and name in fronts:
  im=img(fronts[name]);frames=[im.crop((0,0,im.width,im.width))];method='battle-portrait'
 if not frames:missing.append(name);continue
 arrays=[]
 for im in frames:
  # Preserve silhouettes and grayscale identity; alpha masks transparent backgrounds.
  # Remove only border-connected background. Internal bright face/eye pixels survive.
  bg=im.getpixel((0,0));clear=set();queue=deque()
  for x in range(im.width):queue.extend([(x,0),(x,im.height-1)])
  for y in range(im.height):queue.extend([(0,y),(im.width-1,y)])
  while queue:
   x,y=queue.popleft()
   if (x,y) in clear:continue
   color=im.getpixel((x,y))
   if color[3]>=30 and not (bg[3]>200 and color==bg):continue
   clear.add((x,y))
   for nx,ny in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)]:
    if 0<=nx<im.width and 0<=ny<im.height and (nx,ny) not in clear:queue.append((nx,ny))
  mask=Image.new('L',im.size);pix=[]
  for y in range(im.height):
   for x in range(im.width):
    r,g,b,a=im.getpixel((x,y));lum=r*.299+g*.587+b*.114
    if (x,y) in clear or a<30:pix.append(0);continue
    boundary=any(nx<0 or nx>=im.width or ny<0 or ny>=im.height or (nx,ny) in clear for nx,ny in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)])
    # Dark pupils remain unlit; dark outer contours retain a dim silhouette.
    value=(80 if boundary else 0) if lum<48 else 115+140*lum/255
    pix.append(round(value*a/255))
  mask.putdata([min(255,v) for v in pix]);box=mask.getbbox()
  if box:mask=mask.crop(box)
  mask.thumbnail((15,17),Image.Resampling.NEAREST)
  frame=Image.new('L',(17,23));frame.paste(mask,((17-mask.width)//2,21-mask.height));arrays.append(list(frame.getdata()))
 if not any(arrays[0]):missing.append(name);continue
 pets[name]={'name':names.get(name,name),'source':method,'frames':arrays};counts[method]+=1
(out/'neo-pets.json').write_text(json.dumps(pets,ensure_ascii=False,separators=(',',':')))
(out/'neo-catalog.json').write_text(json.dumps([{'id':n,'name':p['name']+' · '+n} for n,p in pets.items()],ensure_ascii=False))
(root/'evidence/pet-coverage.json').write_text(json.dumps({'count':len(pets),'sources':dict(counts),'missing':missing},ensure_ascii=False,indent=2))
# Contact sheet is an inspection artifact, not proof of physical display.
sel=list(pets);sheet=Image.new('RGB',(850,((len(sel)+9)//10)*132),'#101a24');d=ImageDraw.Draw(sheet)
for i,n in enumerate(sel):
 im=Image.new('L',(17,23));im.putdata(pets[n]['frames'][0]);sheet.paste(im.resize((68,92),Image.Resampling.NEAREST),(i%10*85,i//10*132));d.text((i%10*85,i//10*132+94),n[:12],fill='white')
sheet.save(root/'evidence/pet-atlas.png');print(len(pets),counts,'missing',missing)
