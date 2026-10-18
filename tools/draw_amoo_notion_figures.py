#!/usr/bin/env python3
"""Build original vector diagrams for the illustrated guide."""
from pathlib import Path
from html import escape

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'guides/amoo-notion/figures'

def text(x, y, content, size=28, fill='#344850', weight=400):
    return f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" fill="{fill}">{escape(content)}</text>'

def figure(name, number, title, subtitle, cards, footer):
    height = 240 + len(cards) * 166 + 105
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 720 {height}" role="img" aria-labelledby="title desc">',
             f'<title id="title">{escape(title)}</title><desc id="desc">{escape(subtitle+"。"+"；".join(c[0]+"："+"，".join(c[1]) for c in cards)+"。操作示意，非实机截图。")}</desc>',
             '<defs><linearGradient id="bg" x2="0" y2="1"><stop stop-color="#f3f8f7"/><stop offset="1" stop-color="#eaf1ef"/></linearGradient></defs>',
             f'<rect width="720" height="{height}" rx="28" fill="url(#bg)"/>',
             '<g font-family="-apple-system,BlinkMacSystemFont,PingFang SC,Microsoft YaHei,sans-serif">',
             '<rect x="40" y="36" width="8" height="34" rx="4" fill="#147b70"/>',
             text(62,61,'STEPX NEO 研究所  /  图 '+number,21,'#147b70',650),
             text(40,121,title,38,'#173831',700),text(40,169,subtitle,25)]
    for i,(heading,lines) in enumerate(cards):
        y=210+i*166
        parts.extend([f'<rect x="40" y="{y}" width="640" height="138" rx="20" fill="#fff" stroke="#cbded8"/>',
                      f'<circle cx="79" cy="{y+40}" r="22" fill="#147b70"/>',
                      text(71,y+49,str(i+1),27,'#ffffff',700),text(119,y+48,heading,31,'#173831',650)])
        for j,line in enumerate(lines):parts.append(text(119,y+88+j*32,line,26))
        if i<len(cards)-1:parts.extend([f'<path d="M360 {y+143}v15m-6-6 6 6 6-6" fill="none" stroke="#147b70" stroke-width="3"/>'])
    fy=210+len(cards)*166+10
    parts.extend([text(40,fy,footer,25,'#147b70',600),text(40,fy+43,'操作示意 · Amoo 接入 Notion',21,'#61756f'),'</g></svg>'])
    (OUT/name).write_text('\n'.join(parts)+'\n')

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    specs=[
      ('01-overview.svg','01','Amoo 读取 Notion 的流程','指定页面，读取正文，继续处理任务。',[
          ('Amoo 收到明确任务',['先读指定页面，再处理当前任务']),
          ('执行工作区里的 Node CLI',['使用已保存的连接配置']),
          ('Notion API 验证权限',['内部连接 + 具体页面访问范围']),
          ('返回正文与读取回执',['版本 / 校验词 / 完整性'])], '日常使用：每个新聊天发送启动提示。'),
      ('02-memory-page.svg','02','一页记忆，四个清楚区域','这是结构示例，请在自己的页面填写。',[
          ('校验区',['版本、更新时间、校验词','读取后与页面内容比对']),
          ('长期偏好',['语言、回答风格、工作方式']),
          ('当前项目',['简短目标、已确认事实、复核日期']),
          ('临时测试',['记录测试内容、结果与后续处理'])], '定期更新当前项目与页面版本。'),
      ('03-page-access.svg','03','为连接添加 Notion 页面','选择内部连接，添加需要读取的页面。',[
          ('确认连接和工作区',['启用 Read content']),
          ('打开 Content access',['Edit access → 选择记忆页面']),
          ('核对页面列表',['确认目标页面与所属工作区']),
          ('保存后重新打开核对',['确认目标页面已经添加'])], '页面入口：••• → 连接 → 添加连接。'),
      ('04-script-transfer.svg','04','把脚本交给 Amoo 执行','通过附件把下载文件传入执行工作区。',[
          ('下载脚本附件',['.js 与 .js.txt 字节完全相同']),
          ('Amoo 附件入口选择文件',['文件选择器中选取 .js.txt']),
          ('在执行工作区原样保存',['保存为 amoo-notion-1.0.1.js']),
          ('比较 SHA-256，再检查版本',['Node ≥ 22；CLI 输出 1.0.1'])], '版本输出 1.0.1 后继续配置连接。'),
      ('05-fresh-read.svg','05','检查是否读到最新内容','修改页面后，在新聊天读取并比对。',[
          ('你在 Notion 修改内容',['更新版本、校验词和一项偏好']),
          ('新开 Amoo 聊天',['只发页面链接和读取要求','由工具返回页面中的校验信息']),
          ('查看真实调用与回执',['工具、时间、版本、校验词、完整性']),
          ('与你的页面逐项比对',['核对新版本与修改后的偏好'])], '读取结果应与刚保存的页面一致。'),
      ('06-daily-loop.svg','06','每天只做四件事','把简短启动提示存到自己的快捷短语。',[
          ('新会话先发启动提示',['明确指定页面与现有读取工具']),
          ('核对后继续任务',['结合相关偏好与资料处理任务']),
          ('需要更新时生成草稿',['只读 CLI 不直接修改 Notion']),
          ('你保存修改，再独立复测',['递增版本、更换校验词、重新读取'])], '每次更新后，重新读取确认结果。')]
    for args in specs:figure(*args)
    print(f'Created {len(specs)} original SVG diagrams.')

if __name__=='__main__':main()
