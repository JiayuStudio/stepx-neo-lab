# StepX Neo 研究所

JIAYU STUDIO 整理的 STEPX Neo 使用教程与工具合集。

## 研究目录

| 项目 | 内容 | 实测结果 |
| --- | --- | --- |
| [Google Play / GBox 手机教程](https://github.com/JiayuStudio/stepx-neo-gbox-guide) | 免 ROOT 安装、Google 登录、应用管理与故障处理 | 容器内登录和安装通过 |
| [Amoo 接入 Notion](guides/amoo-notion/README.md) | 6 张流程图、只读 CLI、9 组提示词与记忆页模板 | 明确调用后读取成功，支持跨新聊天复用 |
| [StepX Neo Studio](guides/neo-studio/README.md) | Neo客户端、端侧Gemma、本地开发工具、官方连接器目录、多模型协作和Token分账 | 端侧模型已生成代码；本机工具编译/打包与连接器查询已验证，账户余额仍未知 |

### StepX Neo Studio

- [功能、安装与应用内更新](guides/neo-studio/README.md)
- [公开源码](https://github.com/JiayuStudio/stepx-neo-studio)
- [正式APK 1.2](https://github.com/JiayuStudio/stepx-neo-studio/releases/tag/v1.2.0)

### Amoo / Notion

- [图文教程](guides/amoo-notion/README.md)
- [可复制提示词](guides/amoo-notion/prompts.md)
- [记忆页面模板](guides/amoo-notion/templates/memory-page.md)
- [CLI 1.0.1 源码与手机附件](downloads/amoo-notion/README.md)
- [下载离线手册与提示词包](https://github.com/JiayuStudio/stepx-neo-lab/releases/tag/amoo-notion-v1.0.1)
- [实测记录](guides/amoo-notion/verification.md)

通过 Amoo 的 Node CLI 读取 Notion 页面，每个新聊天发送一次启动提示。全新设备的首次配置流程和自动读取仍待补充验证。

### Google Play / GBox

- [手机安装教程](https://github.com/JiayuStudio/stepx-neo-gbox-guide/blob/main/docs/手机安装教程.md)
- [下载离线教程](https://github.com/JiayuStudio/stepx-neo-gbox-guide/releases/latest)
- [常见问题](https://github.com/JiayuStudio/stepx-neo-gbox-guide/blob/main/docs/常见问题.md)
- [实测记录](https://github.com/JiayuStudio/stepx-neo-gbox-guide/blob/main/docs/实测记录.md)
- [GBox 官方下载](https://www.gboxlab.com/)

Google 账号和应用运行在 GBox 容器内。实测登录与安装已通过，全新系统的纯手机从零流程待独立复测。

## 反馈

在对应项目提供机型、系统版本、应用版本、操作步骤与错误文字，便于复现。合集结构建议可在本仓库提出，项目索引见 [catalog.json](catalog.json)。

维护：JIAYU STUDIO · 2026-10-18。原创资料保留版权，欢迎分享仓库链接；第三方软件遵循各自许可。
