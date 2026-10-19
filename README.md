# StepX Neo 研究所

JIAYU STUDIO 整理的 STEPX Neo 使用教程与工具合集。

## 研究目录

| 项目 | 内容 | 实测结果 |
| --- | --- | --- |
| [Amoo 能力研究](guides/amoo-capabilities/README.md) | 通俗版与专业版：能力边界、调用链、接口与复现步骤 | 原稿记录打开应用可用，手机 GUI 未触发；本次发布未复测 |
| [Google Play / GBox 手机教程](https://github.com/JiayuStudio/stepx-neo-gbox-guide) | 免 ROOT 安装、Google 登录、应用管理与故障处理 | 容器内登录和安装通过 |
| [Amoo 接入 Notion](guides/amoo-notion/README.md) | 6 张流程图、只读 CLI、9 组提示词与记忆页模板 | 明确调用后读取成功，支持跨新聊天复用 |
| [StepX Neo Studio](guides/neo-studio/README.md) | Neo客户端、端侧AI、本地开发、绘图API、云端接力和Token分账 | 绘图生成、自动引用、离线预览和独立APK图片显示已实测；云端接力完整流程待验 |
| [拓麻歌子 · Neo 背屏电子宠物](guides/neo-pet/README.md) | Mori 养成、小游戏、换装与正反面同步的点阵宠物 | 0.3.4 双角色、孵化与耳机；用户确认真机背屏及锁屏动画成功；长期耗电待测 |

### 拓麻歌子 · Neo 背屏电子宠物

- [玩法、安装与后台设置](guides/neo-pet/README.md)
- [公开源码](https://github.com/JiayuStudio/stepx-neo-pet)
- [60 秒宣传片](https://github.com/JiayuStudio/stepx-neo-pet/releases/download/v0.3.4/tamagochi-promo-60s.mp4)
- [下载 APK 0.3.4](https://github.com/JiayuStudio/stepx-neo-pet/releases/tag/v0.3.4)

### StepX Neo Studio

- [功能、安装与应用内更新](guides/neo-studio/README.md)
- [公开源码](https://github.com/JiayuStudio/stepx-neo-studio)
- [正式APK 1.2.3](https://github.com/JiayuStudio/stepx-neo-studio/releases/tag/v1.2.3)

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

维护：JIAYU STUDIO · 2026-10-19。原创资料保留版权，欢迎分享仓库链接；第三方软件遵循各自许可。
