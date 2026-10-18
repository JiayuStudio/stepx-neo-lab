# StepX Neo 研究所

由 JIAYU STUDIO 整理的 STEPX Neo 社区研究合集：保存真实设备上的兼容性实践、操作教程和验证边界，方便其他用户复现与反馈。

这是独立社区入口，与 STEPX、Google 和文中第三方软件没有官方合作或认证关系。

## 研究目录

| 项目 | 内容 | 当前验证状态 |
| --- | --- | --- |
| [免 ROOT 使用 Google Play：GBox 手机教程](https://github.com/JiayuStudio/stepx-neo-gbox-guide) | 手机下载与安装、Google 登录、应用安装、故障排查、失败组件清理 | 一台 Neo 已完成容器内登录与安装；全新系统纯手机从零流程待独立复测 |
| [Amoo 接入 Notion：图文教程与提示词](guides/amoo-notion/README.md) | 6 张流程图、页面授权、手机导入只读 CLI、9 组提示词、私人记忆模板与新鲜度验收 | 指定工具后跨新聊天读取通过；自动读取与全新设备纯手机首次配置未完整通过 |

### Amoo / Notion 工作记忆

- [图文教程](guides/amoo-notion/README.md)：从私人页面、连接授权到显式读取。
- [9 组可复制提示词](guides/amoo-notion/prompts.md)：环境核查、首次配置、日常启动、故障恢复与自动读取审计。
- [CLI 1.0.1 源码与手机附件](downloads/amoo-notion/README.md)：与实测版字节相同，不含凭据或个人配置。
- [下载离线图文手册与提示词包](https://github.com/JiayuStudio/stepx-neo-lab/releases/tag/amoo-notion-v1.0.0)。
- [实测记录和复现边界](guides/amoo-notion/verification.md)。

页面保持私密；本教程采用 Amoo 执行工具 → Node CLI → Notion API。每次新聊天手动触发是已验证入口，自动记忆触发需要单独验收。

### Google Play / GBox

- [开始阅读教程](https://github.com/JiayuStudio/stepx-neo-gbox-guide/blob/main/docs/手机安装教程.md)
- [下载离线教程](https://github.com/JiayuStudio/stepx-neo-gbox-guide/releases/latest)
- [实测设备、版本与限制](https://github.com/JiayuStudio/stepx-neo-gbox-guide/blob/main/docs/实测记录.md)
- [常见问题](https://github.com/JiayuStudio/stepx-neo-gbox-guide/blob/main/docs/常见问题.md)
- [GBox 官方安装包入口](https://www.gboxlab.com/)

此项目使用兼容容器，不代表整台手机原生集成完整 GMS。安装包从上游官方取得，研究所不公开镜像未经授权的第三方 APK。

## 如何阅读研究结果

每个项目分别说明适用机型、系统与软件版本、实际通过的步骤、没有验证的功能和复现条件。源码、安装成功、能够启动、真实功能可用是不同阶段，结果不会互相替代。

只有经过相应验证的资料才标为已通过。旧版本结果保留时间和环境，不自动适用于后来的系统更新。

## 社区反馈

操作问题优先在对应项目反馈，并写明机型、系统版本、应用版本、失败步骤和错误文字。请移除账号、设备序列号、代理订阅、验证码、密钥与私人通知，不直接上传完整原始日志。

合集结构建议可以在本仓库提出；新的研究资料经核验后再加入目录。机器可读索引见 [catalog.json](catalog.json)。

## 资料与版权

原创资料目前保留版权，欢迎分享仓库链接。第三方名称、标识、软件和引用资料属于各自权利人，遵守各自许可。公开资料不包含个人设备的原始诊断、账号或网络配置。

维护：JIAYU STUDIO。建立日期：2026-10-18。
