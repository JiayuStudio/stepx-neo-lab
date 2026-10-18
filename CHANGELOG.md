# 更新记录

## 2026-10-18 · Amoo / Notion 图文教程 v1.0.0

- 新增一份完整接入教程、6 张原创流程示意图、9 组中文提示词、私人记忆页面模板和实测边界。
- 经维护者授权，公开已经在 Amoo 中实测的只读 CLI 1.0.1 单文件源码，以及字节相同的手机 `.js.txt` 附件。没有修改脚本，没有带入个人配置、账号、页面内容或原始截图。
- 提供内嵌配图的离线 HTML、提示词/模板 ZIP 和 SHA-256 清单。
- 区分显式读取成功、未通过的自动读取，以及尚未完整独立复测的全新设备纯手机首次配置。

## 2026-10-18 · 研究所入口建立

- 建立 StepX Neo 研究所 GitHub 合集。
- 收录 Google Play / GBox 手机教程，保持独立社区身份和验证边界。

## 维护

图文手册使用 Pandoc 构建：`python3 tools/draw_amoo_notion_figures.py`，然后 `python3 tools/build_amoo_notion_guide.py`。这些工具只供维护者生成文档，手机读者无需安装。

构建结果在被 Git 忽略的 `release-assets/`；验证与原始排版截图在被 Git 忽略的 `publication-audit/`。不要把这些目录整体加入 Git；发布附件应逐项白名单上传。
