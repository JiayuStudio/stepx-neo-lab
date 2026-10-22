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

## Neo Agent Touch · Beta

手机内运行的聊天式目标执行应用，视觉截图结合系统控件语义，支持蓝色贴边反馈、可拖动胶囊及接管／交还。

- [功能、内测安装与验证范围](../guides/neo-agent-touch/README.md)
- [下载 0.1.11-beta.1 APK](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.11-beta.1/Neo-Agent-Touch-0.1.11-beta.1.apk)
- [180 秒开发实录](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.11-beta.1/neo-agent-touch-development-180s.mp4) · [开发手册](../guides/neo-agent-touch/development.md)

淘宝商品探索、电影场次至座位图、WPS 新建／保存／修改／重开已有真机记录；完整购物、购票与外部 MCP 网关尚未验通。需要用户自行配置视觉模型 API。
