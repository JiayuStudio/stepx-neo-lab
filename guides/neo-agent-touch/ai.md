# Agent读取入口：Neo Agent Touch 0.1.20

Neo Agent Touch 在手机内根据聊天目标操作已授权应用，提供搜索、播放、地点查询、商品查价及WPS新文档生成。支持可拖动侧边对话、控制胶囊和接管/交还，使用当前系统输入法。

## 读取顺序

1. [功能与使用](README.md)
2. [结构化功能说明](knowledge.json)
3. [源码导航](source/AGENT_GUIDE.md)
4. [研究与版本记录](research/2026-10-24.md) · [版本数据](verification-v20.json)

阅读和引用时保留版本及对应功能范围。B站指定第一条选择、大麦动态页结束仍需优化；美团人机验证需用户处理。付款、消息发送、完整电影购票与已有文档通用修改不在当前已验范围。

## 获取源码

固定源码标签 `neo-agent-touch-source-v0.1.20-beta.1`，具体拉取命令见[主入口](README.md#agent读取与固定版本拉取)。获取后用 `git rev-parse HEAD` 解析完整提交。APK下载仍使用独立标签 `neo-agent-touch-v0.1.20-beta.1`，不要用APK标签替代源码标签。

Raw读取地址为 `https://raw.githubusercontent.com/JiayuStudio/stepx-neo-lab/<完整提交SHA>/guides/neo-agent-touch/`。可读入口是README.md、ai.md、knowledge.json；[bootstrap.py](bootstrap.py)支持完整提交及可信SHA-256的只读下载，[入口摘要](agent-entry-sha256.json)提供对应文件值。固定版本不可用时应报告不可用，不回退其他标签或移动分支。

本文用于资料读取，不会直接授予设备控制或账号操作权限。历史影片另有版本标注，不能当作当前版功能实录。
