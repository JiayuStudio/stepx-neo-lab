# JIAYU STUDIO 原创教程与研究

[研究所首页](../README.md) · [游戏](games.md) · [APP](apps.md) · [开发工具](tools.md)

本区收录 **JIAYU STUDIO 整理、撰写的教程和实测研究**。教程中使用 GBox、Notion 等第三方产品，不表示这些产品由研究所开发。官方发布的信息请看独立的[官方群信息](official-updates.md)分区。

## 新人入门

- [开发第一款应用：口袋计数器](../guides/first-app/README.md)：用内置模板完成创建、改字、运行、打包与安装，基础练习无需配置 AI。
- [新手路线与后续练习](start-here.md)：先完成一个独立小应用，再逐步加入保存、AI 或背屏能力。

## 背屏开发

- [从 17×23 点阵到拓麻歌子](../guides/neo-pet/development.md)：需要哪些文件、怎样调用、如何制作角色和保持锁屏动画。
- [配套 Step Studio 提示词](../guides/neo-pet/studio-prompts.md)：按步骤复制，包含失败时的排查要求。
- [游戏安装与后台设置](../guides/neo-pet/README.md)：只想使用游戏，从这里开始。

## Amoo 与知识管理

想查日常说法或新收录的 PDF 文字版，请到独立的 [Amoo 使用资料库](../library/amoo/README.md)。该组按提供资料标注来源，与本区原创内容分开。

- [Amoo 能力研究](../guides/amoo-capabilities/README.md)：通俗版与专业版，说明能力边界、调用链和复现方法；沿用原稿记录，本次整理未复测。
- [Amoo 接入 Notion](../guides/amoo-notion/README.md)：图文接入、只读 CLI、提示词和记忆页模板。
- [Notion 实测记录](../guides/amoo-notion/verification.md)：明确调用读取及新聊天复用已通过，自动读取与全新设备首次配置仍有待验项。

## 应用兼容与 Google 服务

- [GBox 手机安装教程](https://github.com/JiayuStudio/stepx-neo-gbox-guide/blob/main/docs/手机安装教程.md)
- [常见问题](https://github.com/JiayuStudio/stepx-neo-gbox-guide/blob/main/docs/常见问题.md) · [实测记录](https://github.com/JiayuStudio/stepx-neo-gbox-guide/blob/main/docs/实测记录.md)
- [离线教程](https://github.com/JiayuStudio/stepx-neo-gbox-guide/releases/latest) · [GBox 官方下载](https://www.gboxlab.com/)

已验证容器内 Google 登录及安装。它不等于系统原生 GMS；全新系统纯手机从零流程待独立复测。

## 官方平台新手课

[官方 Step Studio 新手课程](../courses/official-step-studio/README.md)：真实网页截图、需求拆解、提示词、31 个连接器分阶段练习和课堂案例。课程由研究所编写，和自研 Studio 教程分别阅读。

## AI 接口入门

[DeepSeek API Key 申请与配置](../guides/deepseek-api/README.md)：官方平台申请、通用配置、自研 Studio 操作与错误排查。

## 游戏开发实战

- [用 Step Studio 开发象棋泡泡堂](../guides/chess-bomber/README.md)：从需求、规则与UI修复，到账号、回放、音乐和真实宣传片；网页与真机验证边界分别记录。

## 手机 Agent 开发实录

- [Neo Agent Touch：从目标到可验证的手机操作](../guides/neo-agent-touch/README.md)：三分钟实录、Beta APK、架构、原生控件与视觉、触点变化、接管与检查点、WPS 结果验证，以及论文和仓库的参考范围。提供可编辑影片工程及关键代码阅读样例。

- [Neo Agent Touch 最新研究与Agent拉取](../guides/neo-agent-touch/ai.md)：原话绑定、完整候选、输入法交接、证据区分、ASR许可与暂停结论，提供固定提交与SHA校验的只读获取。

[120秒功能原理展示](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/neo-agent-touch-principles-120s.mp4) · [可编辑影片1.0.3](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/neo-agent-touch-film-editable-v1.0.3.zip)：以游戏闭环、微信发送边界、输入、接管与记忆的步骤动效讲解原理，辅以历史实录；使用新Suno v6器乐。
