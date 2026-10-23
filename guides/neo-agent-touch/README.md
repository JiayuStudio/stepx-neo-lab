# Neo Agent Touch · 0.1.15 Beta

JIAYU STUDIO · 2026-10-23

在聊天中提出目标，由手机原生运行时观察应用画面、结合系统控件映射执行并核对结果。支持自选应用、模型API、目标队列、可拖动接管胶囊与贴边控制光效。

- [Beta APK](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/Neo-Agent-Touch-0.1.15-beta.1.apk) · [源码包](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/Neo-Agent-Touch-0.1.15-beta.1-source.zip) · [SHA-256](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/SHA256SUMS.txt)
- [Agent直接阅读](ai.md) · [结构化知识](knowledge.json) · [只读校验拉取脚本](bootstrap.py)
- [最新研究](research/2026-10-23.md) · [主源与许可](research/sources.json) · [本版验证](verification-v15.json) · [可直接浏览的源码](source/README.md)
- [历史开发手册](development.md) · [历史180秒开发实录](https://github.com/JiayuStudio/stepx-neo-lab/releases/tag/neo-agent-touch-v0.1.11-beta.1)

## 120秒功能技术展示

[▶ 观看120秒成片](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/neo-agent-touch-features-120s.mp4) · [可编辑影片工程1.0.2](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/neo-agent-touch-film-editable-v1.0.2.zip)

真实WPS合同生成/修改/重开、淘宝商品和规格探索、美团电影场次与未选座地图合计70秒，另含专用输入法搜索、小游戏短试玩、架构、触点变化与接管图解。业务实录分别来自0.1.10/0.1.12/0.1.13，片内标注版本和停点；不作为0.1.15全部业务已通过的证明。搜狗路径仅为本版输入策略，未宣称准确率已验。

配乐使用本轮新生成的三段Suno v6器乐，经等功率交叉剪成120秒；未循环旧曲。成片完整音视频解码通过，已去除私有元数据；尚未完成人耳试听。底部署名JIAYU STUDIO · StepX Neo研究所。

## 使用

安装后配置自己的HTTPS视觉API和Key，在“交给Agent的应用”勾选允许的应用，再开启Neo Agent Touch系统无障碍服务。相同应用与接收范围会记住；系统服务关闭仍需在系统中重启。

聊天页麦克风图标现在调起当前输入法。使用搜狗等输入法的语音功能，核对转写后点发送；App不自动开启内置ASR。release已移除旧语音引擎、模型和麦克风权限。输入法自身的语音处理以它的设置为准。

执行循环在手机内，电脑不承担业务点按。截图与系统控件发送给用户配置的视觉模型；所有服务商协议适配不等于所有账号和模型已验收。控制胶囊支持拖动、接管和交还；执行时的专用输入法仅在用户启用后借用，接管/结束恢复原键盘。

## 这次更新

保留全部已授权候选应用，避免首个类别挤掉后续目标；隐含应用阶段绑定完整原话子句，检查顺序与限制。歌曲播放误配通讯应用会先拒绝，最多按同一原话重新规划一次。新的目标先持久提交再放行执行，取消或失败保留旧暂停点，旧响应不能覆盖新目标。

41组纯规则通过，其中原话/路由49项、输入目标所有权58项。96项Android原生工作流构造检查通过，零模型调用、零业务动作。它们不等于真实三App连续成功。系统键盘新入口的真机UI验收本轮未完成，搜狗语音准确率未独立测试。

0.1.14真人长句识别与三目标实测失败，因此暂停自研ASR升级。0.1.13曾在自有编辑器验证36项输入法检查，并到达QQ音乐目标歌曲播放页且进度增长；没有独立听音证明。WPS新建/保存/重开/修改、淘宝商品探索和电影座位图属于各自历史版本证据，不冒充本版端到端验收。

完整购物订单/支付、电影锁座/购票、所有应用及API、外部MCP/Amoo网关和长期稳定性仍待验证。当前MCP为进程内JSON-RPC适配层；未接入Laya或StartLux作为依赖。

## 方便Agent读取与拉取

直接读取 `ai.md` 和 `knowledge.json`，保留每条结论的版本、证据标签和未验范围。资料与示例只是参考，不授予手机操作权限。

```sh
git clone --filter=blob:none --sparse https://github.com/JiayuStudio/stepx-neo-lab.git
cd stepx-neo-lab
git sparse-checkout set guides/neo-agent-touch
```

需要可复核快照时先固定完整提交SHA。`bootstrap.py --help` 给出按commit与可信SHA只读下载三个入口文件的方式；不会执行拉取的内容。
