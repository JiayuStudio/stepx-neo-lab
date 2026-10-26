"""Embed the canonical player into the standalone HTML; file previews need no module server."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
page=ROOT/'examples/index.html'
html=page.read_text(encoding='utf-8')
core=re.sub(r'^export ', '', (ROOT/'examples/matrix-player.mjs').read_text(encoding='utf-8'),flags=re.M)
begin='// BEGIN BUNDLED PLAYER\n';end='// END BUNDLED PLAYER\n'
if begin in html:
 left,tail=html.split(begin,1);_,right=tail.split(end,1)
 html=left+begin+core+'\n'+end+right
else:
 old="<script type=\"module\">\nimport {MatrixPlayer,makeBuddy,connectMcp} from './matrix-player.mjs';\n"
 assert old in html,'Expected original player script'
 html=html.replace(old,'<script>\n(()=>{\n'+begin+core+'\n'+end,1)
 html=html.replace('</script></html>','})();\n</script></html>')
page.write_text(html,encoding='utf-8')
print('Standalone example synced from the canonical player')
