#!/usr/bin/env python3
"""Original vector instruction diagrams; no private screenshots or external art."""
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
    parts.extend([text(40,fy,footer,25,'#147b70',600),text(40,fy+43,'原创操作示意 · 非实机截图 · 不含私人数据',21,'#61756f'),'</g></svg>'])
    (OUT/name).write_text('\n'.join(parts)+'\n')

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    specs=[
      ('01-overview.svg','01','从提问到真实读取','页面仍是私密的，工具按授权范围读取。',[
          ('Amoo 收到明确任务',['先读指定页面，再处理当前任务']),
          ('执行工作区里的 Node CLI',['复用已有配置，不在聊天展示密钥']),
          ('Notion API 验证权限',['内部连接 + 具体页面访问范围']),
          ('返回正文与读取回执',['版本 / 校验词 / 完整性'])], '已验证显式读取；自动触发仍未通过。'),
      ('02-memory-page.svg','02','一页记忆，四个清楚区域','这是结构示例，请在自己的页面填写。',[
          ('校验区',['版本、更新时间、随机校验词','答案留在页面，不写进读取提示']),
          ('长期偏好',['语言、回答风格、工作方式']),
          ('当前项目',['简短目标、已确认事实、复核日期']),
          ('临时测试',['单独记录，避免变成真实用户事实'])], '保持简短；不放密钥或客户原始资料。'),
      ('03-page-access.svg','03','授权一页，不是公开一页','在 Notion 选择你自己的内部连接。',[
          ('确认连接和工作区',['仅启用所需的内容读取能力']),
          ('打开 Content access',['Edit access → 选择记忆页面']),
          ('检查授权覆盖范围',['父页授权可能覆盖它的子页面']),
          ('保存后重新打开核对',['页面仍在授权列表才算完成'])], '页面入口：••• → 连接 → 添加连接。'),
      ('04-script-transfer.svg','04','把脚本交给 Amoo 执行','手机下载目录与 Amoo 工作区不是一回事。',[
          ('下载可信源码附件',['.js 与 .js.txt 字节完全相同']),
          ('Amoo 附件入口选择文件',['传入 .js.txt，不用截图识别代码']),
          ('在执行工作区原样保存',['保存为 .js，不覆盖旧工具和配置']),
          ('比较 SHA-256，再检查版本',['Node ≥ 22；CLI 输出 1.0.1'])], '附件收到 ≠ 已经能够执行。'),
      ('05-fresh-read.svg','05','用新值证明这一次读过','校验词是测试标记，不是密码。',[
          ('你在 Notion 修改内容',['换版本、随机词和一条非敏感偏好']),
          ('新开 Amoo 聊天',['只发页面链接和读取要求','不给版本号、随机词的正确答案']),
          ('查看真实调用与回执',['工具、时间、版本、校验词、完整性']),
          ('与你的页面逐项比对',['新值一致才有本轮读取证据'])], '旧聊天里记得，不等于本轮重新读取。'),
      ('06-daily-loop.svg','06','每天只做四件事','把简短启动提示存到自己的快捷短语。',[
          ('新会话先发启动提示',['明确指定页面与现有读取工具']),
          ('核对后继续任务',['只应用相关偏好，当前要求优先']),
          ('需要更新时生成草稿',['只读 CLI 不直接修改 Notion']),
          ('你保存修改，再独立复测',['递增版本、更换校验词、重新读取'])], '手动触发可复现；自动规则单独验收。')]
    for args in specs:figure(*args)
    print(f'Created {len(specs)} original SVG diagrams.')

if __name__=='__main__':main()
