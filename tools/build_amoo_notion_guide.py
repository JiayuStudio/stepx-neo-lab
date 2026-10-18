#!/usr/bin/env python3
"""Build an offline illustrated handbook plus reproducible public attachments."""
from pathlib import Path
import hashlib
import html
import json
import re
import shutil
import subprocess
import zipfile

ROOT=Path(__file__).resolve().parents[1]
GUIDE=ROOT/'guides/amoo-notion'
OUT=ROOT/'release-assets'
AUDIT=ROOT/'publication-audit/amoo-notion-v1.0.0'
PUBLIC='https://github.com/JiayuStudio/stepx-neo-lab/blob/main/'

def without_title(text):
    return text.split('\n',1)[1].lstrip()

def main():
    OUT.mkdir(exist_ok=True);AUDIT.mkdir(parents=True,exist_ok=True)
    main=without_title((GUIDE/'README.md').read_text())
    main='<a id="guide-main"></a>\n\n'+main
    prompts=without_title((GUIDE/'prompts.md').read_text())
    template=without_title((GUIDE/'templates/memory-page.md').read_text())
    evidence=without_title((GUIDE/'verification.md').read_text())
    cli=without_title((ROOT/'downloads/amoo-notion/README.md').read_text())
    combined=(main+'\n\n# 提示词合集 {#prompt-library}\n\n'+prompts+
              '\n\n# 可复制的记忆页面模板 {#memory-template}\n\n'+
              '复制下面全文到你自己的 Notion 页面，替换示例字段。\n\n```text\n# Amoo 工作记忆\n\n'+template+'\n```\n'+
              '\n\n# 实测记录与复现边界 {#verified-status}\n\n'+evidence+
              '\n\n# CLI 下载与说明 {#cli-downloads}\n\n'+cli)
    link_map={'../../README.md':'https://github.com/JiayuStudio/stepx-neo-lab',
              'prompts.md':'#prompt-library','templates/memory-page.md':'#memory-template',
              'verification.md':'#verified-status','../../downloads/amoo-notion/README.md':'#cli-downloads',
              '../../guides/amoo-notion/README.md':'#guide-main','README.md':'#guide-main',
              'amoo-notion-1.0.1.js':PUBLIC+'downloads/amoo-notion/amoo-notion-1.0.1.js',
              'SHA256SUMS.txt':PUBLIC+'downloads/amoo-notion/SHA256SUMS.txt',
              'provenance.json':PUBLIC+'downloads/amoo-notion/provenance.json'}
    def rewrite(m):
        target=m.group(1)
        if target.startswith('prompts.md#'):return ']('+target.removeprefix('prompts.md')+')'
        if target.startswith('prompts/'):return ']('+PUBLIC+'guides/amoo-notion/'+target+')'
        return ']('+link_map.get(target,target)+')'
    combined=re.sub(r'\]\(([^)]+)\)',rewrite,combined)
    source=AUDIT/'combined.md';source.write_text(combined)
    output=OUT/'Amoo-Notion-Guide-v1.0.0.html'
    subprocess.run(['pandoc','--standalone','--toc','--toc-depth=2','--embed-resources',
                    '--metadata=lang:zh-CN','--metadata=toc-title:目录',
                    '--metadata=title:Amoo 接入 Notion',
                    '--metadata=subtitle:图文教程 · 9 组提示词 · CLI 1.0.1 · 2026-10-18',
                    '--css='+str(ROOT/'assets/amoo-notion-guide.css'),
                    '--resource-path='+str(GUIDE),str(source),'-o',str(output)],check=True)
    content=output.read_text()
    start=content.index('<nav id="TOC"');end=content.index('</nav>',start)+len('</nav>')
    nav=content[start:end]
    content=content[:start]+'<details class="guide-toc"><summary>打开目录：设置 / 提示词 / 模板 / 实测</summary>'+nav+'</details>'+content[end:]
    content=content.replace('</body>', '''<script>
document.querySelectorAll('pre').forEach((pre, index) => {
  const code = pre.querySelector('code'); if (!code) return;
  const button=document.createElement('button'); button.type='button'; button.className='copy';
  button.textContent='复制全文'; button.setAttribute('aria-label','复制代码或提示词 '+(index+1));
  button.addEventListener('click',async()=>{
    try { await navigator.clipboard.writeText(code.textContent); button.textContent='已复制'; }
    catch (_) { const selection=window.getSelection(); const range=document.createRange(); range.selectNodeContents(code); selection.removeAllRanges(); selection.addRange(range); button.textContent='已选中，请手动复制'; }
  }); pre.appendChild(button);
});
</script>
</body>''')
    output.write_text(content)
    for name in ['amoo-notion-1.0.1.js','amoo-notion-1.0.1.js.txt']:
        shutil.copyfile(ROOT/'downloads/amoo-notion'/name,OUT/name)
    bundle=OUT/'Amoo-Notion-Prompts-and-Template-v1.0.0.zip'
    with zipfile.ZipFile(bundle,'w',zipfile.ZIP_DEFLATED) as z:
        for p in [GUIDE/'prompts.md',*sorted((GUIDE/'prompts').glob('*.txt')),GUIDE/'templates/memory-page.md']:
            info=zipfile.ZipInfo(str(p.relative_to(GUIDE)),date_time=(2026,10,18,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o644<<16
            data=p.read_bytes()
            if p.name=='prompts.md':
                data=p.read_text().replace('](README.md)',']('+PUBLIC+'guides/amoo-notion/README.md)').encode()
            z.writestr(info,data)
        info=zipfile.ZipInfo('README.txt',date_time=(2026,10,18,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o644<<16
        z.writestr(info,('Amoo Notion 提示词与页面模板 v1.0.0\n\n'
                        'prompts/：9 组可直接复制的纯文本提示。只替换括号字段，不填写 token。\n'
                        'templates/memory-page.md：复制到自己的私人 Notion 页面。\n'
                        'prompts.md：带说明和目录的合集。\n'
                        '图文教程：'+PUBLIC+'guides/amoo-notion/README.md\n').encode())
    assets=[output,OUT/'amoo-notion-1.0.1.js',OUT/'amoo-notion-1.0.1.js.txt',bundle]
    checksum=OUT/'SHA256SUMS-amoo-notion-v1.0.0.txt'
    checksum.write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in assets))
    manifest=[{'name':p.name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in [*assets,checksum]]
    (AUDIT/'release-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(manifest,ensure_ascii=False))

if __name__=='__main__':main()
