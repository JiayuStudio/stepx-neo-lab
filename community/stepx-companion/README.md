# StepX Companion · 社区桌面伴侣与配套 APK

[研究所首页](../../README.md) · [社区 APK 与网页游戏](../../sections/community-apks.md) · [Agent 索引](../../llms.txt)

**作者：倒影颠倒；来源：社区群（均由用户提供）。** 原仓库为 [xydzf520/stepx](https://github.com/xydzf520/stepx)。第三方社区项目，不属于 JIAYU STUDIO 自研，也不是 STEPX、Amoo 或 Google 官方客户端。首次收录日期：**2026-10-23 UTC**；本次资料更新：**2026-10-25 UTC**。

## 最新版与功能

本次按[上游 v0.2.25 发行说明](https://github.com/xydzf520/stepx/releases/tag/v0.2.25)更新，面向 STEPX Neo PZ261A / Step AOS；普通 Android 仅有部分设备管理能力。以下是**上游功能说明与自述验证**，不是研究所新实测：

- Amoo 统一对话入口，按需选择处理文件、生成图片或制作应用；核对模型、资料与保存位置后开始，作品集中预览。
- 图片、文字与 CSV 混合问答，Excel/Word/PPT 生成和原文档修改，离线小应用创作与隔离浏览器验收。
- 简洁窗口、草稿与附件保留、拖入文件、快捷键、托盘及更新下载校验与回滚。
- 存储扩容需先选择范围和电脑保存位置，确认开启；上游称校验原件后才释放手机空间。涉及个人文件，使用前自行备份。
- 配套手机端整理任务、文件、作品与设置，整合 Studio 项目同步与本地创作；GUI Agent 使用独立模型配置，提供暂停、继续和停止。

本条目保留在社区 APK 分区：桌面工作台是主要产品，Android APK 为配套应用，不另建重复项目。操作前阅读[固定版本入门文档](https://github.com/xydzf520/stepx/blob/v0.2.25/docs/GETTING_STARTED.md)，准备对应桌面平台、Neo 设备、USB/ADB 连接与所需账号；没有设备或权限时不应将界面可见当作执行成功。

## 版本、输入输出与验收

| 对象 | 本次核对到的信息 | 证据范围 |
| --- | --- | --- |
| 桌面发行版 | v0.2.25，2026-10-24 14:59:54 UTC 发布 | GitHub 发行元数据，不等于研究所收录日期或稳定性认证 |
| 桌面源码 | [Cargo.toml](https://github.com/xydzf520/stepx/blob/v0.2.25/desktop/Cargo.toml)声明 0.2.25 | 仅文本核对，未构建 |
| 桌面下载 | Windows x86_64、macOS arm64 / x86_64、Linux x86_64 完整压缩包及摘要 | 按平台下载完整包，不单独复制 EXE；研究所未运行桌面包 |
| Android 下载 | 同一发行的 StepX-companion.apk，459592 字节；GitHub 提供 SHA-256 `2bbd1c9c3a9007e4e0b46254efe32ffd3cc75c73b67e291eaa4b7e339ee07bc2` | 本轮实际下载字节与摘要一致，APK v3 签名结构校验通过；不代表签名者可信、功能或安全通过 |
| Android 源码 | [AndroidManifest.xml](https://github.com/xydzf520/stepx/blob/v0.2.25/desktop/companion-app/AndroidManifest.xml)声明 `com.stepx.companion.app`、versionName `0.1.26`、versionCode `27` | 电脑发行版与 Android 内部版本分别记录，aapt 读取公开 APK 亦为 0.1.26/27；未重新构建证明源码与二进制一致 |

学习例子：先用不含私人信息的小型 CSV 和明确问题验证只读问答，核对输出是否引用正确数据；再在副本上尝试文档生成，检查文件能打开、内容和公式正确。执行失败、取消或授权不足时停止并核对错误，不把示例当成本轮已执行结果。研究所本轮未做这些练习。

## 获取、许可与限制

[原作者最新版发行](https://github.com/xydzf520/stepx/releases/tag/v0.2.25)提供完整桌面包、手机 APK 与校验文件。[研究所 Android 原包镜像](https://github.com/JiayuStudio/stepx-neo-lab/releases/tag/community-stepx-companion-v0.2.25)同时提供对应 Android 源码、构建脚本、AGPL/版权/第三方说明及 ZXing 3.5.4 依赖源码；未修改或重新签名 APK。[对应版本源码](https://github.com/xydzf520/stepx/tree/v0.2.25)保留原作者署名。

项目源码声明 **AGPL-3.0-only**；[LICENSE](https://github.com/xydzf520/stepx/blob/v0.2.25/LICENSE)、[NOTICE](https://github.com/xydzf520/stepx/blob/v0.2.25/NOTICE)和[第三方说明](https://github.com/xydzf520/stepx/blob/v0.2.25/THIRD_PARTY_NOTICES.md)分别记录许可、版权与依赖边界，品牌资产另有权利限制。这不是对所有文件权属的独立法律审查。

上游说明：Linux x64 发行需 glibc 2.39 或更新；macOS 无 Apple 公证，Windows 无商业代码签名；制作应用和 Studio 独立会话需 Chrome、Edge 或 Chromium，Safari 不替代验收引擎。Google Play 安装不等于 Play Protect 认证。文件迁移、GUI 控制和账号权益应按实际授权使用。

上游报告 Linux + PZ261A 的混合附件、Office、图片及离线应用验证，以及四平台 CI；Windows/macOS 尚无连接真机的完整 GUI 验收。详见[跨平台边界](https://github.com/xydzf520/stepx/blob/9ef8e78d2afe2b3fe7e693697dbeb94363559a5e/docs/CROSS_PLATFORM_0_2_25.md)。这些均为上游报告，不扩大为研究所独立验收。

## 研究所验证状态与历史

**公开资料更新；安全待检查；Neo 未实测。** 未安装或执行桌面/Android 应用、未授予设备权限、未做安全审计；下载或摘要一致不等于应用安全或功能通过，不授予安全徽章。

快照：上游 main `9ef8e78d2afe2b3fe7e693697dbeb94363559a5e`；v0.2.25 标签 `0c94d2e0e208ae91bab7def228d718e28efaf1d5`。本次更新依据上游发行元数据、固定标签清单和说明，详见[本轮记录](../../maintenance/2026-10-25-review.md)。

2026-10-23 首次收录的是桌面 0.2.0 与源码声明 Android 0.1.8（9），历史来源与许可边界保留在 Git 历史和 CHANGELOG。社区署名与群来源由用户提供，未独立核验群消息，不公开群内私人内容或作者联系方式。
