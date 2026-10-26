# StepX Companion 0.2.50 · 社区桌面伴侣

[研究所首页](../../README.md) · [社区推荐](../../sections/community-apks.md)

作者：**倒影颠倒**；来源：社区群（用户提供）。这是第三方社区项目，不属于研究所自研或 STEPX 官方客户端。[项目源码](https://github.com/xydzf520/stepx)。更新核对：2026-10-27。

[▶ 视频介绍 · 4 分 03 秒](https://stepxneo.jiayustudio.dpdns.org/assets/stepx-companion-0.2.50-intro.mp4) · [原作者发行说明与下载](https://github.com/xydzf520/stepx/releases/tag/v0.2.50)

## 从 0.2.25 到 0.2.50

- **读取更快**：作者同机测试中，Amoo 会话读取中位耗时由 730 ms 降至 345 ms（约 53%）；电话基础信息由 402 ms 降至 275 ms（约 31%）。测试环境为 Linux + PZ261A、相同 USB 连接、每项每版本 9 次；衡量预热后的读取，不代表 AI 生成或整款应用提速。研究所未复现。[性能报告](https://github.com/xydzf520/stepx/blob/v0.2.50/docs/PERFORMANCE_0_2_50.md)
- **对话更连贯**：流式回复、收起执行过程、简洁小窗；切换会话和展开窗口保留草稿与附件。
- **成果继续修改**：文档、表格、图片和小应用可继续修改、预览，保存到电脑或发送至手机。
- **照片与原件更好找**：独立相册和分类筛选，与存储扩容联动查看迁移到电脑的原件。
- **日常事务接着处理**：新增日历、闹钟入口，改进短信、通知、电话与应用管理。
- **连接更省心**：修复扫码入口，完善同局域网自动重连和适配固件重启后的服务恢复；重启后首次解锁仍需本人完成。
- **特色功能持续完善**：背屏、体感游戏、空间压缩、投屏控制、文件互传，以及 Google Play 安装与诊断。

以上为作者发布说明，不等同于研究所实机验收。

## 下载与初次使用

桌面版 **0.2.50**，配套 Android 版按作者说明为 **0.1.32**，两者独立编号。

| 平台 | 原作者下载 |
| --- | --- |
| Windows x86_64 | [ZIP](https://github.com/xydzf520/stepx/releases/download/v0.2.50/StepX-Companion-0.2.50-windows-x86_64.zip) |
| macOS Apple 芯片 | [ZIP](https://github.com/xydzf520/stepx/releases/download/v0.2.50/StepX-Companion-0.2.50-macos-arm64.zip) |
| macOS Intel | [ZIP](https://github.com/xydzf520/stepx/releases/download/v0.2.50/StepX-Companion-0.2.50-macos-x86_64.zip) |
| Linux x86_64 | [tar.gz](https://github.com/xydzf520/stepx/releases/download/v0.2.50/StepX-Companion-0.2.50-linux-x86_64.tar.gz) |
| 手机配套应用 | [StepX-companion.apk](https://github.com/xydzf520/stepx/releases/download/v0.2.50/StepX-companion.apk) |

1. 按电脑平台下载，核对发行页对应 SHA-256 文件，并完整解压；保留 tools、runtime、scripts 等随包目录。
2. 运行桌面入口，按界面引导连接 STEPX Neo；运行工具已随包提供，无需额外安装 ADB、Python、Java 或 Android Studio。Windows USB 连接可能仍需驱动。
3. 按引导安装配套手机应用、配对并授权所需功能；先从 Amoo 对话、小文件互传开始。已有定制合包时先读上游升级说明。
4. 在 Amoo 中描述目标，预览成果后继续修改，按需保存电脑或发送手机。首次迁移照片、清理或空间压缩前备份重要文件。
5. 无线重连需要同一局域网允许设备互访；固件重启恢复不能代替首次人工解锁。

[上游入门说明](https://github.com/xydzf520/stepx/blob/v0.2.50/docs/GETTING_STARTED.md) · [更新详情](https://github.com/xydzf520/stepx/blob/v0.2.50/docs/UPDATE_0_2_50.md)

## 适配、许可与验证边界

主要面向 STEPX Neo / PZ261A，增强权限及重启恢复以作者已适配固件为准。macOS 未公证，Windows 无商业代码签名；构建成功不等于所有系统、实体设备都已验收。Google Play 安装与诊断不等于 Google 设备认证。模型、账户权限和可能的费用以实际配置为准。

项目声明 AGPL-3.0-only，第三方依赖和品牌资产另有约定：[许可](https://github.com/xydzf520/stepx/blob/v0.2.50/LICENSE) · [第三方说明](https://github.com/xydzf520/stepx/blob/v0.2.50/THIRD_PARTY_NOTICES.md)。本次链接上游安装包，不修改或重新签名。

**本轮仅核对发布资料、资产列表和视频；未安装新版、未做完整源码安全审计或 Neo 实机回归，不授予安全通过徽章。** 视频为用户提供的社区介绍，性能和功能描述归原作者。此前版本的校验或运行记录不能作为新版安全结论。
