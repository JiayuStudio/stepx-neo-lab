# JIAYU STUDIO 自研应用

[研究所首页](../README.md) · [游戏](games.md) · [开发工具](tools.md) · [教程与研究](guides.md)

本区只整理 **JIAYU STUDIO 自己开发的应用**。其他作者的仓库与 APK 单独收录在[社区推荐](community-apks.md)。

## StepX Neo Studio

第三方 Android 开发客户端，提供端侧 AI、本地项目编辑、预览、受支持项目的 APK 构建、绘图 API 与云端项目协作。

- [功能、安装与更新指南](../guides/neo-studio/README.md)
- [下载 1.2.3 APK](https://github.com/JiayuStudio/stepx-neo-studio/releases/tag/v1.2.3)
- [观看／下载功能演示（约 1 分 55 秒）](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/studio-demo-v1.0.0/StepXNeoStudio-feature-tour-public.mp4)：1.2 版界面，部分配置与用量画面为功能示意。
- [公开源码](https://github.com/JiayuStudio/stepx-neo-studio)

**验证情况：** 绘图生成、自动引用、离线预览及独立 APK 图片显示已实测；完整实时云端接力流程仍待验证。具体能力与各版本记录见指南。

此客户端与 [Step Studio 网页](https://studio.stepx.com/)不同；支持的项目模板和构建环境也不同，不能据此推断任意 Gradle 或 npm 项目都可以离线打包。

需要 Google Play？请看[教程分区中的 GBox](guides.md)，它是第三方容器方案，不是本研究所开发的 APP。

## Neo Agent Touch · 0.1.20 Beta

手机内运行的聊天式目标执行应用，视觉截图结合系统控件语义，支持侧边对话、蓝色贴边反馈、可拖动胶囊及接管／交还。

- [功能、安装与本轮实测](../guides/neo-agent-touch/README.md) · [下载0.1.20-beta.1 APK](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.20-beta.1/Neo-Agent-Touch-0.1.20-beta.1.apk)
- [Agent阅读与固定版本源码](../guides/neo-agent-touch/ai.md) · [机器验收](../guides/neo-agent-touch/verification-v20.json)

56套规则检查通过，11个授权应用分批实测，8个场景按限定范围通过：音乐搜索Live播放、地点详情、当前工作台、微信搜一搜、京东/淘宝查价、WPS新文档保存重开全文读回、网盘搜索。B站严格第一条选择未通过；美团停在人机验证；网盘与大麦稳定性未宣称根治。拼多多不在本轮范围，未下单、付款或发消息。最后正式包安装摘要一致，不等于所有应用在最后二进制上重跑。

使用当前系统输入法，专用输入法自动切换独立且默认关闭；没有真人语音准确率结论。完整场景边界与逐项构建摘要见机器验收。

[120秒历史原理影片](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-film-v1.0.4/neo-agent-touch-splitflow-120s.mp4) · [历史180秒开发实录](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.11-beta.1/neo-agent-touch-development-180s.mp4) · [历史开发手册](../guides/neo-agent-touch/development.md)。历史影片不作为0.1.20实录。

