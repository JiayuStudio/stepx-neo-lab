# Neo Agent Touch 0.1.20 Beta

Neo Agent Touch 是运行在手机内的聊天式应用助手。通过主聊天或可拖动侧边对话提出目标，Agent结合应用画面和系统控件执行搜索、播放及文档任务；蓝色屏幕边缘光效提示控制状态，胶囊支持拖动、接管和交还。

## 功能与使用

支持QQ音乐关键词搜索与播放、高德地点查询、微信搜一搜、企业微信工作台、淘宝/京东商品查价、网盘搜索，以及WPS新文档生成、保存和重开。B站指定第一条视频选择、大麦动态详情的结束判断仍需优化；美团遇到人机验证时交由用户处理。付款、消息发送、完整电影购票和已有文档通用修改不在当前已验范围。

安装同签名新版可直接覆盖，无需先卸载。配置自己的HTTPS视觉模型接口与Key，选择允许控制的应用并开启系统无障碍服务，然后在主聊天或侧边对话中描述目标。使用搜狗等当前输入法时，核对文字后发送；专用输入法自动切换默认关闭。

[Android安装包](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.20-beta.1/Neo-Agent-Touch-0.1.20-beta.1.apk) · [APK校验值](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.20-beta.1/SHA256SUMS.txt)。

## 源码阅读

本源码版本为 `0.1.20-beta.1`，Android versionCode为21。代码导航和构建方法见 [AGENT_GUIDE.md](AGENT_GUIDE.md)，功能与版本数据见 [tests/acceptance-v20.json](tests/acceptance-v20.json)。

固定源码标签为 `neo-agent-touch-source-v0.1.20-beta.1`。Agent读取入口为该标签下的 `guides/neo-agent-touch/ai.md`，结构化说明为 `guides/neo-agent-touch/knowledge.json`；通过标签解析完整提交后读取，固定版本不可用时报告不可用，不切换APK标签。
