# 更新记录

## 2026-10-18 · Amoo / Notion 图文教程 v1.0.1

- 精简接入步骤与 9 组提示词，更新记忆页面模板和 6 张流程图。
- 同步更新离线手册与提示词包。
- CLI 保持 1.0.1，命令和配置方式不变。

## 2026-10-18 · Amoo / Notion 图文教程 v1.0.0

- 新增接入教程、6 张流程示意图、9 组提示词、记忆模板和实测记录。
- 提供 CLI 1.0.1 单文件源码及手机 `.js.txt` 附件。
- 提供离线 HTML、提示词 ZIP 和 SHA-256 清单。

## 2026-10-18 · 研究所入口建立

- 建立 StepX Neo 研究所，收录 Google Play / GBox 手机教程。

## 构建文档

需要 Python 3 与 Pandoc。在仓库目录依次运行：

```sh
python3 tools/draw_amoo_notion_figures.py
python3 tools/build_amoo_notion_guide.py
```

生成的离线手册和下载包位于 `release-assets/`。

## 2026-10-18 · StepX Neo Studio 1.1.3

收录公开源码、正式APK、应用内更新教程、Neo开发与多模型协作能力及实测边界；上游参考和实际依赖均链接至来源声明。

## 2026-10-18 · StepX Neo Studio 1.1.4

新增长按项目菜单、左滑删除、最近删除恢复及彻底删除说明；保留本地与云端删除范围、用量账本不变等边界。
