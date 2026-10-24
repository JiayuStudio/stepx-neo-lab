# Agent Touch · 0.1.20 Beta

![应用图标](assets/app-icon.png)

展示名：Agent Touch。原项目名称、包名和既有签名升级链保持兼容。


Neo Agent Touch 是运行在手机内的聊天式应用助手。通过主聊天或可拖动侧边对话提出目标，Agent结合应用画面和系统控件执行搜索、播放及文档任务；蓝色屏幕边缘光效提示控制状态，胶囊支持拖动、接管和交还。

- [下载Android安装包](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.20-beta.1/Neo-Agent-Touch-0.1.20-beta.1.apk) · [APK校验值](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.20-beta.1/SHA256SUMS.txt)
- [下载源码包](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-source-v0.1.20-beta.1/Neo-Agent-Touch-0.1.20-beta.1-source.zip) · [源码校验值](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-source-v0.1.20-beta.1/SHA256SUMS.txt)
- [Agent读取入口](ai.md) · [结构化功能说明](knowledge.json) · [源码与使用](source/README.md)

## 可以做什么

- 用一句话启动允许的应用，连续处理多个目标。
- 通过当前画面寻找控件，执行点击、滚动、输入与搜索提交。
- 从侧边随时展开对话，拖动控制胶囊避开内容，随时接管或交还。
- 使用当前系统输入法输入或转写指令，确认文字后执行。
- 在菜单和关于页检查新版本，经系统确认后安装更新。

支持QQ音乐关键词搜索与播放、高德地点查询、微信搜一搜、企业微信工作台、淘宝/京东商品查价、网盘搜索，以及WPS新文档生成、保存和重开。B站指定第一条视频选择、大麦动态详情的结束判断仍需优化；美团遇到人机验证时交由用户处理。付款、消息发送、完整电影购票和已有文档通用修改不在当前已验范围。

## 安装与使用

安装 `0.1.20-beta.1`，同签名更新可直接覆盖，无需先卸载。配置自己的HTTPS视觉模型接口和Key，选择允许控制的应用，并开启系统无障碍服务。截图与系统控件由当前配置的模型处理。

在聊天框或侧边对话里描述目标，例如“用QQ音乐搜索伍佰的突然的自我并播放”或“在WPS新建一份说明文档，介绍手机Agent的用途并保存”。使用搜狗等系统输入法时，先核对转写文字，再发送。专用输入法的自动切换为独立选项，默认关闭。

## Agent读取与固定版本拉取

先读 [ai.md](ai.md) 与 [knowledge.json](knowledge.json)，源码导航见 [source/AGENT_GUIDE.md](source/AGENT_GUIDE.md)。源码版本固定为 `neo-agent-touch-source-v0.1.20-beta.1`。

```sh
git clone --filter=blob:none --no-checkout https://github.com/JiayuStudio/stepx-neo-lab.git
cd stepx-neo-lab
git sparse-checkout init --cone
git sparse-checkout set guides/neo-agent-touch
git fetch origin tag neo-agent-touch-source-v0.1.20-beta.1
git checkout --detach neo-agent-touch-source-v0.1.20-beta.1
git rev-parse HEAD
```

已有克隆使用同样的fetch与checkout获取固定版本。只读下载可用 [bootstrap.py](bootstrap.py) 与 [三个入口摘要](agent-entry-sha256.json)；找不到固定版本时应报告不可用，不切换到其他版本。

## 原理与示例

[研究与实现](research/2026-10-24.md) · [版本记录](verification-v20.json) · [功能流程图](implementation-flows.md) · [来源与许可](research/sources.json)。

[120秒历史原理影片](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-film-v1.0.4/neo-agent-touch-splitflow-120s.mp4)采用手机画面与流程图同步展示；素材来自0.1.10/0.1.12/0.1.13，不是0.1.20新实录。[可编辑工程](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-film-v1.0.4/neo-agent-touch-film-editable-v1.0.4.zip)。
