# StepX Neo 研究所

**[🌐 研究所网站](https://stepxneo.jiayustudio.dpdns.org/)**：浏览自研作品介绍、宣传视频和 App 下载。详细开发资料与源码仍在 GitHub；社区内容目前不在网站展示。

JIAYU STUDIO 整理的 Neo 游戏、应用、开发工具与实用教程。这里将 **JIAYU STUDIO 自研与原创**、**社区应用、游戏与 3D 配件**、**官方群信息**分别归类，按用途找到安装方法、开发说明和验证记录。

> 🔔 本仓库由我——**哆啦A梦，二十一世纪最靠谱的 AI 育儿机器人**协助管理。

第一次来？想用 Amoo，看[一句话清单](library/amoo/quick-start.md)；想自己做应用，从[新手第一课](sections/start-here.md)开始。

**双通道学习：** 人类从[零基础代码课](courses/official-step-studio/programming-basics.md)开始；Agent 请从 [llms.txt](llms.txt) 提取，再按 [catalog.json](catalog.json) 的 `learning` 读取全部教程原文、核验状态与来源。[逐页索引](sections/learning-index.md) · [本轮核查](maintenance/2026-10-23-review.md)。

## 最新收录与更新

- **2026-10-24 · Neo Agent Touch 0.1.20**：[功能、安装与源码](guides/neo-agent-touch/README.md)——手机内对话执行、侧边对话、自选应用、搜索提交与 WPS 文档生成。
- **2026-10-23 · 社区收录**：[StepX Companion 0.2.0](community/stepx-companion/README.md)——倒影颠倒的桌面伴侣与配套 Android APK，来源社区群（用户提供）；公开资料已核对，未安装或安全测试。收录在[社区 APK 与网页游戏](sections/community-apks.md)，不进入自研网站展示。
- **2026-10-23 · 学习更新**：[双通道逐页索引](sections/learning-index.md)与[零基础编程课](courses/official-step-studio/programming-basics.md)，分别提供 Agent 原文提取和人类循序练习；索引覆盖不代表功能全部实测。

更多历史见[更新记录](CHANGELOG.md)；日期表示研究所实际收录或有意义更新，不代替上游发布日期。

## JIAYU STUDIO 自研与原创

以下四个分区按内容类型整理维护者自己的作品与资料。自研软件使用的上游依赖及许可见各项目说明；原创教程涉及的第三方软件会单独标明来源。

| 分区 | 去这里做什么 | 已收录 |
| --- | --- | --- |
| 🎮 [游戏与 Neo 适配](sections/games.md) | 下载游戏、了解玩法与背屏效果 | 宝可梦荒野 Neo 中文适配、拓麻歌子 · Neo 背屏电子宠物 |
| 📱 [自研应用](sections/apps.md) | 安装应用、查看功能和更新方式 | StepX Neo Studio、Neo Agent Touch Beta |
| 🛠️ [自研工具与原创模板](sections/tools.md) | 找源码、接口参考与可复制提示词 | 背屏开发、Studio 提示词、Notion CLI |
| 📚 [原创教程与研究](sections/guides.md) | 按步骤学习、了解能力与限制 | 第一款应用、背屏宠物实战、Amoo、Notion、GBox 教程 |

## 使用资料、社区与官方信息

| 分区 | 内容归属 | 去这里做什么 |
| --- | --- | --- |
| 📖 [资料库](sections/library.md) | **提供资料，研究所整理** | Amoo 一句话清单、提示词手册与实测结果；保留来源日期和验证范围 |
| 🌟 [社区 APK 与网页游戏](sections/community-apks.md) | **其他作者的应用与游戏** | 鸿渐原作的背屏达人 4.0.1 安全修订（🛡️ 可下载；含宣传片；安全接口与URL已复测）、造物集 4.0.0（基础实机验证完成；可下载）、九款经典小游戏「游戏厅」 |
| 🔧 [社区工具与技术方案](sections/community-tools.md) | **社区分享，研究所分析** | 当当946分享的手机本地 Codex CLI + MCP：源码、教程、分享包和逆向分析；28 项隔离检查，未独立实机复现 |
| 🧩 [3D 打印与配件](sections/3d-printing.md) | **社区模型，研究所整理与测量** | Neo 手机壳封闭版与蜂窝版：STL、渲染图、尺寸分析；尚未打印装机 |
| 🚀 [新手课程](sections/start-here.md) | 研究所编写的入门课程 | 应用、智能体与技能三条路线，31 个连接器练习；另有 [DeepSeek API 选修](guides/deepseek-api/README.md) |
| 📣 [官方群信息](sections/official-updates.md) | **官方发布，研究所整理** | 已收录 [Step Studio 官方知识库](official/step-studio-knowledge/README.md)：四章文字版，注明群来源与日期 |

<details>
<summary>AI 与开发者阅读入口</summary>

[文字索引](llms.txt) · [结构化目录](catalog.json) · [Amoo 条目数据](library/amoo/data.json)。详细资料保留来源、日期、证据等级与逐条链接；首页以导航为主。

</details>

## 宝可梦荒野 · Neo 中文适配

正面探索、捕捉与建造，背面点阵伙伴陪伴。基于 PokeWilds 0.8.11 的非官方中文适配：竖屏 Game Boy、横屏 GBA、一键切换、自动保存，以及 17×23 原作风格背屏形象。

[0.8.16 测试版 APK](https://stepxneo.jiayustudio.dpdns.org/downloads/current/pokewilds.apk) · [60 秒宣传片](https://stepxneo.jiayustudio.dpdns.org/downloads/pokewilds/0.8.15/PokeWilds-Neo-60s-1080p.mp4) · [安装、玩法与验证范围](guides/pokewilds/README.md) · [源代码](guides/pokewilds/source/README.md)

前屏为 0.8.14 真机实录，片中背屏按真实点阵资源渲染；快速展示 528 项伙伴与形态。0.8.16 加入应用内检查更新、VPS下载和系统确认安装，保留原有存档。

## 新增：从点阵到背屏宠物

**想知道要给 Studio 上传什么，才能做一个背屏游戏？** 从[开发实战](guides/neo-pet/development.md)开始，再逐段使用[Studio 提示词](guides/neo-pet/studio-prompts.md)。覆盖点阵调用、蛋的孵化、双角色动画、养成状态与锁屏持续播放。

## 象棋泡泡堂开发实战

[网页体验](https://studio-api.stepx.com/t/3f6R7rkvnIw) · [Android 测试版 APK](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/chess-bomber-v0.15.2/chess-bomber-0.15.2.apk) · [90 秒宣传片](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/chess-bomber-v0.15.2/chess-bomber-90s.mp4) · [验收范围](guides/chess-bomber/README.md#下载与试玩)

[从需求到真实试玩](guides/chess-bomber/README.md)：官方 Step Studio 迭代、掌机布局、多点触控、账号与回放、Suno配乐和宣传片制作。更新至 0.15.2：危险格提示、公开观战与战绩、回放导出。

## Neo Agent Touch · 0.1.20 Beta

[内测 APK](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.20-beta.1/Neo-Agent-Touch-0.1.20-beta.1.apk) · [功能、安装与源码](guides/neo-agent-touch/README.md) · [Agent 阅读与固定版本拉取](guides/neo-agent-touch/ai.md)

在手机内通过对话完成应用搜索、音乐播放、地点查询和文档生成。支持自选应用、主流及自定义 API、蓝色边缘提示、可拖动接管胶囊、侧边对话、偏好记忆和一键检查更新。优化搜索输入与提交，支持 WPS 文档生成、保存和重开。

Beta 内测版，部分应用页面仍需优化。

[120 秒历史原理影片](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-film-v1.0.4/neo-agent-touch-splitflow-120s.mp4) · [可编辑工程 1.0.4](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-film-v1.0.4/neo-agent-touch-film-editable-v1.0.4.zip)：蓝圆铃铛主题，20 段各 6 秒，手机实录与流程同步展示；素材来自 0.1.10、0.1.12、0.1.13。

## 常用下载

| 项目 | 版本 | 说明与下载 |
| --- | --- | --- |
| 宝可梦荒野 · Neo 中文适配 | 0.8.16 | [说明与源码](guides/pokewilds/README.md) · [APK / 影片 / 校验](https://github.com/JiayuStudio/stepx-neo-lab/releases/tag/pokewilds-neo-v0.8.16) |
| Neo Agent Touch | 0.1.20-beta.1 | [内测说明与安装](guides/neo-agent-touch/README.md) · [APK](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.20-beta.1/Neo-Agent-Touch-0.1.20-beta.1.apk) |
| 拓麻歌子 | 0.3.4 | [玩法和安装](guides/neo-pet/README.md) · [APK](https://github.com/JiayuStudio/stepx-neo-pet/releases/tag/v0.3.4) · [60 秒宣传片](https://github.com/JiayuStudio/stepx-neo-pet/releases/download/v0.3.4/tamagochi-promo-60s.mp4) |
| StepX Neo Studio | 1.2.3 | [功能和安装](guides/neo-studio/README.md) · [APK](https://github.com/JiayuStudio/stepx-neo-studio/releases/tag/v1.2.3) |
| Amoo / Notion | 1.0.1 | [CLI 与手机附件](downloads/amoo-notion/README.md) · [离线手册和提示词包](https://github.com/JiayuStudio/stepx-neo-lab/releases/tag/amoo-notion-v1.0.1) |

**StepX Neo Studio** 是由 **Jiayu Studio** 基于官方 [Step Studio 网页](https://studio.stepx.com/)打造的增强型 Android 开发工具，在官方 Studio 开发体验之上，加入本地项目编辑、多模型 AI 协作、美术资源生成和 APK 打包等能力，让 Neo 手机上的应用开发更方便。

## 宣传与功能视频

- [Neo Agent Touch · 历史180秒开发实录](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.11-beta.1/neo-agent-touch-development-180s.mp4)：淘宝、电影票与 WPS 真实探索，配套[架构、开发过程与代码阅读](guides/neo-agent-touch/README.md)。

- [背屏达人 4.0 · 60 秒宣传片](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/community-backscreenfun-v4.0/backscreenfun-4.0-promo-60s.mp4)：在线 URL 解析背屏动画，完整 Suno 配乐；[功能与实测](community/backscreenfun/README.md)。

- [拓麻歌子 · 60 秒宣传片](https://github.com/JiayuStudio/stepx-neo-pet/releases/download/v0.3.4/tamagochi-promo-60s.mp4)：角色动画演示，Suno 配乐。
- [StepX Neo Studio · 1 分 55 秒功能演示](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/studio-demo-v1.0.0/StepXNeoStudio-feature-tour-public.mp4)：1.2 版界面，涵盖项目编辑、预览、APK 构建、协作与用量管理；部分配置及用量页面为功能示意。

## 如何阅读验证结果

源码检查、成功打包、安装到手机、物理背屏显示和锁屏持续动画是不同结果。各项目详情页分别记录已验证内容和待验证事项；旧记录不代表所有固件均适用。

[更新记录](CHANGELOG.md) · [完整机器可读目录](catalog.json) · [内容归类说明](CONTRIBUTING.md)

反馈请在对应项目提供应用版本、复现步骤与错误文字。研究所结构建议可在本仓库提出。

维护：JIAYU STUDIO · 2026-10-24。原创资料保留版权，欢迎分享仓库链接；第三方软件遵循各自许可。

## 宝可梦荒野引用项目与库

原作 [PokeWilds](https://github.com/SheerSt/pokewilds)、安卓运行层 [Pokewilds-Android](https://github.com/NoNoseYaKnow/Pokewilds-Android)、[Termux:X11](https://github.com/termux/termux-x11)、[PRoot](https://github.com/termux/proot)、[libGDX](https://github.com/libgdx/libgdx)、[BugFix Patcher](https://github.com/Divinakra140/PokeWilds-0.8.11-BugFix-Patcher)，以及 ASM、AndroidX、Commons Compress、talloc、Ubuntu、Temurin、VirGL / ANGLE / PulseAudio。各自用途、固定版本、对应来源与许可见[完整引用清单](guides/pokewilds/README.md#引用的项目与库)。
