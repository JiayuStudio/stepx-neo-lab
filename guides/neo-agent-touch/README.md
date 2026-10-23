# Neo Agent Touch · 0.1.16 Beta

JIAYU STUDIO · 2026-10-23

在聊天中提出目标，由手机原生运行时观察应用画面、结合系统控件映射执行并核对结果。支持自选应用、模型API、目标队列、可拖动接管胶囊与贴边控制光效。

- [Beta APK](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.16-beta.1/Neo-Agent-Touch-0.1.16-beta.1.apk) · [源码包](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.16-beta.1/Neo-Agent-Touch-0.1.16-beta.1-source.zip) · [SHA-256](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.16-beta.1/SHA256SUMS.txt)
- [Agent直接阅读](ai.md) · [结构化知识](knowledge.json) · [只读校验拉取脚本](bootstrap.py)
- [最新研究](research/2026-10-23.md) · [主源与许可](research/sources.json) · [本版验证](verification-v16.json) · [可直接浏览的源码](source/README.md)
- [历史开发手册](development.md) · [历史180秒开发实录](https://github.com/JiayuStudio/stepx-neo-lab/releases/tag/neo-agent-touch-v0.1.11-beta.1)

## 120秒同步分屏原理展示

[▶ 观看120秒成片](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-film-v1.0.4/neo-agent-touch-splitflow-120s.mp4) · [可编辑影片工程1.0.4](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-film-v1.0.4/neo-agent-touch-film-editable-v1.0.4.zip)

新版1.0.4采用蓝白圆面板、黄色铃铛与红色点缀。全片20段，每段6秒，共120秒；左侧持续展示完整手机实录与同帧焦点放大，右侧2–3个流程节点按画面推进高亮。内容加入WPS写改读回、淘宝搜索/详情/规格、电影日期/场次/座位图、音乐输入/播放器、小游戏、接管与记忆。

[分镜与剪辑说明](film-v1.0.4.md) · [结构化时间线](film-v1.0.4-timeline.json) · [影片验证](verification-video-v1.0.4.json)

淘宝未付款，是否形成待付款订单未独立核对。小游戏采用通用视觉循环，没有专用求解器；微信部分为通用机制和本机发送边界示意，自动发送尚未实现，也没有实发验收。业务实录来自0.1.10/0.1.12/0.1.13，不能当作0.1.16全部业务已通过。详见[功能实现流程图](implementation-flows.md)。

[原理版1.0.3存档](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/neo-agent-touch-principles-120s.mp4) · [1.0.3工程](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/neo-agent-touch-film-editable-v1.0.3.zip)。

另保留[功能实录版1.0.2](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/neo-agent-touch-features-120s.mp4)与[对应可编辑工程](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/neo-agent-touch-v0.1.15-beta.1/neo-agent-touch-film-editable-v1.0.2.zip)。

配乐沿用本轮生产中新生成的三段Suno v6器乐混音，经等功率交叉剪成120秒；本次风格修订未另生成歌曲。成片完整音视频解码通过，已去除私有元数据；尚未完成人耳试听。底部署名JIAYU STUDIO · StepX Neo研究所。

## 使用

安装后配置自己的HTTPS视觉API和Key，在“交给Agent的应用”勾选允许的应用，再开启Neo Agent Touch系统无障碍服务。相同应用与接收范围会记住；系统服务关闭仍需在系统中重启。

聊天页麦克风图标现在调起当前输入法。使用搜狗等输入法的语音功能，核对转写后点发送；App不自动开启内置ASR。release已移除旧语音引擎、模型和麦克风权限。输入法自身的语音处理以它的设置为准。

执行循环在手机内，电脑不承担业务点按。截图与系统控件发送给用户配置的视觉模型；所有服务商协议适配不等于所有账号和模型已验收。控制胶囊支持拖动、接管和交还；执行时的专用输入法仅在用户启用后借用，接管/结束恢复原键盘。

## 这次更新

“去京东淘宝拼多多看看怎样购买海信g7pro最便宜”这类共享商品目标由手机本地编译为名单顺序，三步保留同一个精确型号与全部原文限制，只查价、不加购、不下单、不付款。独立应用目标仍各自绑定完整原文，合法重访保持；内部模型校正提示不再直接显示给用户。

菜单与关于页新增「检查更新」，支持Beta版本。固定官方更新清单为主源，GitHub应用发布为备用；下载后核验字节数、SHA-256、包名、精确版本、递增versionCode及同一签名，再打开Android系统安装确认。执行任务期间不会打开安装器，没有后台自动检查或静默安装。详细实现、参考和维护方式见[本次修复与更新](updates-v16.md)。

44套纯规则通过，含共享原文152项、固定提示239项、更新规则50项；另有149项真机Android原生构造与JSON恢复检查通过，覆盖京东/淘宝，不调用模型、不操作购物应用，授权未改。三平台原生夹具因拼多多缺少已有授权和启动入口未运行，真实查价闭环待验。正式包安装和更新通道结果见[本版验证](verification-v16.json)，不以夹具成功代替实际购物。

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
