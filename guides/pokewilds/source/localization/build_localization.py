"""Build display data and high resolution glyphs without changing gameplay identifiers."""
from pathlib import Path
import json,re,csv,math
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
LOC=Path(__file__).resolve().parent/'data'
OUT=Path(__file__).resolve().parent/'extras/localization'
OUT.mkdir(parents=True,exist_ok=True)

def props(path):
    d={}
    for line in path.read_text(encoding='utf-8-sig').splitlines():
        if not line.strip() or line.startswith(('#','!')):continue
        k,v=re.split(r'(?<!\\)=',line,maxsplit=1)
        d[k.replace('\\ ', ' ')]=v.replace('\\n','\n')
    return d

exact={};terms={};patterns=[]
def add(original,translated):
    for a,b in re.findall(r'\{(\d+)\}\{(\d+)\}',original):
        original=original.replace('{'+a+'}{'+b+'}','{'+a+'}')
        translated=translated.replace('{'+a+'}{'+b+'}','{'+a+'}')
    for variant in {original,original.replace('«',"'s").replace('»',"'t"),original.replace('«',"'").replace('»','ì')}:
        if re.search(r'\{\d',variant):patterns.append([variant,translated])
        else:exact[variant]=translated

for p in (LOC/'base').glob('*.properties'):
    target=LOC/'zh_CN'/f'{p.stem}_zh_CN.properties'
    if not target.exists():raise RuntimeError(f'Missing {target}')
    originals=props(p);translated=props(target)
    for key,value in originals.items():
        if key not in translated:raise RuntimeError(f'Missing key {p.name}:{key}')
        add(value,translated[key])
    if p.stem in ('pokemondisplayname','attack','item','type','fieldmove','menuitem'):
        for k,v in translated.items():terms[k]=v
        for k,v in originals.items():
            terms[v]=translated[k]
            if p.stem=='item':
                terms[v+'s']=translated[k]
                if v.endswith('y'):terms[v[:-1]+'ies']=translated[k]

for file in ('hardcoded_zh_CN.json','hardcoded_items_zh_CN.json'):
    if (LOC/file).exists():
        for k,v in json.loads((LOC/file).read_text()).items():
            if isinstance(v,str):add(k,v)
for file in ('dynamic_patterns.json','hardcoded_patterns_zh_CN.json'):
    if (LOC/file).exists():
        pairs=json.loads((LOC/file).read_text())
        if isinstance(pairs,dict):pairs=pairs.items()
        for k,v in pairs:add(k,v)
for file in ('runtime_aliases.json','hardcoded_dynamic_zh_CN.json'):
    if (LOC/file).exists():terms.update(json.loads((LOC/file).read_text()))
for label,translation in {'PSYCHIC':'精神强念','GRASS':'青草','STATUS':'状态','REPEL':'除虫喷雾'}.items():
    exact[label]=translation;terms[label.lower()]=translation

patterns.extend([['Enemy {0}','对手的{0}'],['Wild {0}','野生的{0}'],['{0} x{1}','{0} x{1}']])
data={'exact':exact,'terms':terms,'patterns':sorted(patterns,key=lambda p:len(re.sub(r'\{[^}]+\}','',p[0])),reverse=True)}
(OUT/'display.json').write_text(json.dumps(data,ensure_ascii=False,indent=2))
chars=sorted({c for text in list(exact.values())+list(terms.values())+[p[1] for p in patterns] for c in text if ord(c)>127 and c not in '\n\r'})
if (LOC/'overlay_labels_zh_CN.json').exists():
    overlay=json.dumps(json.loads((LOC/'overlay_labels_zh_CN.json').read_text()),ensure_ascii=False)
    chars=sorted(set(chars)|{c for c in overlay if ord(c)>127})
font=ImageFont.truetype('/System/Library/Fonts/STHeiti Medium.ttc',15)
atlas=Image.new('RGBA',(64*16,math.ceil(len(chars)/64)*16),(0,0,0,0))
draw=ImageDraw.Draw(atlas)
for i,c in enumerate(chars):
    bounds=font.getbbox(c)
    x=(i%64)*16+(16-(bounds[2]-bounds[0]))//2-bounds[0]
    y=(i//64)*16+(16-(bounds[3]-bounds[1]))//2-bounds[1]
    draw.text((x,y),c,font=font,fill=(0,0,0,255),stroke_width=0)
atlas.save(OUT/'cjk-black.png')
white=Image.new('RGBA',atlas.size,(255,255,255,0));white.putalpha(atlas.getchannel('A'));white.save(OUT/'cjk-white.png')
(OUT/'glyphs.txt').write_text(''.join(chars))
report={'exact':len(exact),'terms':len(terms),'patterns':len(patterns),'glyphs':len(chars),'atlas':atlas.size}
(LOC/'display-build-report.json').write_text(json.dumps(report,indent=2))
print(report)
