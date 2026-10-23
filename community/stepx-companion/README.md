# StepX Companion · 社区桌面伴侣与配套 APK

[研究所首页](../../README.md) · [社区 APK 与网页游戏](../../sections/community-apks.md) · [Agent 索引](../../llms.txt)

**作者：倒影颠倒；来源：社区群（均由用户提供）。** 原仓库为 [xydzf520/stepx](https://github.com/xydzf520/stepx/tree/main)。这是第三方社区项目，不属于 JIAYU STUDIO 自研，也不是 STEPX、Amoo 或 Google 官方客户端。收录及公开资料核查日期：**2026-10-23 UTC**。

## 收录内容

根据上游 README，StepX Companion 是面向 STEPX Neo 的电脑端工作台，提供 Amoo/Work 交互、文件与应用管理、投屏、空间压缩与清理、背屏设置等入口；配套 Android 应用包含“重力对齐”背屏体感游戏。上述为**上游功能说明**，研究所没有安装、运行或复现。

本条目归入社区 APK 分区，因为公开发行包含配套 Android APK；桌面客户端才是主要产品。背屏游戏属于配套手机应用，不将其列为独立网页游戏。详细操作以[上游入门文档](https://github.com/xydzf520/stepx/blob/main/docs/GETTING_STARTED.md)为准。

## 版本与公开获取入口

| 对象 | 本次核对到的信息 | 证据范围 |
| --- | --- | --- |
| 桌面发行版 | [v0.2.0](https://github.com/xydzf520/stepx/releases/tag/v0.2.0)，2026-10-23 10:24:06 UTC 发布 | 上游称“首个公开预览版”；GitHub prerelease 标志为 false，不能因此称为稳定版 |
| 桌面源码 | [v0.2.0 的 Cargo.toml](https://github.com/xydzf520/stepx/blob/v0.2.0/desktop/Cargo.toml)声明 0.2.0 | 仅文本核对，未构建 |
| 桌面下载 | Linux x86_64、macOS arm64 / x86_64、Windows x86_64 四个压缩包，各附 `.sha256` | 仅核对 Release 资产名称；未下载或验签 |
| Android 下载 | 同一 Release 提供 `StepX-companion.apk`（336712 字节）及 `.sha256` | 未下载 APK，内部版本、签名和实际哈希未独立核验 |
| Android 源码 | [发布标签的 AndroidManifest.xml](https://github.com/xydzf520/stepx/blob/v0.2.0/desktop/companion-app/AndroidManifest.xml)声明包名 `com.stepx.companion.app`、versionName `0.1.8`、versionCode `9` | 源码声明不证明 Release APK 与源码一致；不能把 APK 内部版本写成 0.2.0 |

仅链接[原作者 Releases](https://github.com/xydzf520/stepx/releases/tag/v0.2.0)，不在研究所镜像源码、安装包或截图。获取文件前自行核对原作者发布说明、校验文件与所需权限；存在 `.sha256` 文件不等于研究所已完成校验。

## 许可与适配边界

上游 [README](https://github.com/xydzf520/stepx/blob/main/README.md)、[NOTICE](https://github.com/xydzf520/stepx/blob/main/NOTICE)及 Cargo 清单声明项目源码采用 **AGPL-3.0-only**；[LICENSE](https://github.com/xydzf520/stepx/blob/main/LICENSE)提供 AGPL v3 文本。第三方组件遵循各自许可，品牌资产另有权利限制，见[第三方说明](https://github.com/xydzf520/stepx/blob/main/THIRD_PARTY_NOTICES.md)。这不是对所有文件权属的独立法律审查。

上游主要适配 PZ261A / Step AOS，Amoo、背屏与系统服务依赖固件和账号授权。上游说明 macOS / Windows 尚未完成完整实体手机及图形界面验收，发行包没有 Apple 公证或 Windows 商业代码签名。Google 相关入口不等于设备认证，公开包不包含 Google APK；研究所没有验证 Google Play 安装、Token 权益、空间清理或后台能力。

## 研究所验证状态

**资料收录；安全待检查；Neo 未实测。** 本轮只读查看了仓库 README、完整文件树、公开 Release、版本清单与许可说明。没有下载、安装、执行第三方代码，没有授予手机权限，没有开展安全审计。不授予安全徽章或 Neo 认证。

可复核快照：上游 main 为 `1a6a2498638182bb527156df40fc2b27f62fc96d`，发布标签 v0.2.0 指向 `11b8e5fa614828abb2dfe3af41149872ef94ce3e`。未来版本需重新记录，不沿用本次状态为运行保证。社区群出处及作者署名来自用户说明，未独立核验群消息；不收录群内私人内容或作者联系方式。
