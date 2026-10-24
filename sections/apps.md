# JIAYU STUDIO 自研应用

[研究所首页](../README.md) · [游戏](games.md) · [开发工具](tools.md) · [教程与研究](guides.md)

本区只整理 **JIAYU STUDIO 自己开发的应用**。其他作者的仓库与 APK 单独收录在[社区推荐](community-apks.md)。

## Step工作台（StepX Neo Studio）

第三方 Android 开发客户端，提供端侧 AI、本地项目编辑、预览、受支持项目的 APK 构建、绘图 API 与云端项目协作。

- [功能、安装与更新指南](../guides/neo-studio/README.md)
- [下载 1.2.3 APK](https://github.com/JiayuStudio/stepx-neo-studio/releases/tag/v1.2.3)
- [观看／下载功能演示（约 1 分 55 秒）](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/studio-demo-v1.0.0/StepXNeoStudio-feature-tour-public.mp4)：1.2 版界面，部分配置与用量画面为功能示意。
- [公开源码](https://github.com/JiayuStudio/stepx-neo-studio)

**验证情况：** 绘图生成、自动引用、离线预览及独立 APK 图片显示已实测；完整实时云端接力流程仍待验证。具体能力与各版本记录见指南。

此客户端与 [Step Studio 网页](https://studio.stepx.com/)不同；支持的项目模板和构建环境也不同，不能据此推断任意 Gradle 或 npm 项目都可以离线打包。

需要 Google Play？请看[教程分区中的 GBox](guides.md)，它是第三方容器方案，不是本研究所开发的 APP。

## Agent Touch · 0.1.20 Beta

手机内运行的聊天式应用助手，结合应用画面与系统控件完成搜索、播放、地点查询、商品查价和 WPS 新文档生成。支持自选应用与模型接口、侧边对话、蓝色贴边提示、可拖动胶囊、接管与交还、偏好记忆及一键检查更新。

- [功能与安装](../guides/neo-agent-touch/README.md) · [下载 0.1.20-beta.1 APK](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.20-beta.1/Neo-Agent-Touch-0.1.20-beta.1.apk)
- [Agent 阅读与固定版本源码](../guides/neo-agent-touch/ai.md) · [结构化功能说明](../guides/neo-agent-touch/knowledge.json)

使用当前系统输入法，核对文字后发送；专用输入法自动切换默认关闭。B站指定第一条视频与大麦动态页仍需优化，美团遇人机验证时由用户处理。付款、消息发送、完整电影购票和已有文档通用修改不在当前已验范围。

[120 秒历史原理影片](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-film-v1.0.4/neo-agent-touch-splitflow-120s.mp4) · [历史 180 秒开发实录](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.11-beta.1/neo-agent-touch-development-180s.mp4) · [原理与来源](../guides/neo-agent-touch/development.md)。120 秒影片素材来自 0.1.10、0.1.12、0.1.13。

