# Codex 手机控制分享包：实现还原与安全分析

来源署名：**当当946 · 社区群分享**。分析整理：JIAYU STUDIO / Codex。日期：2026-10-21。

[条目主页与下载](README.md) · [材料清单](manifest.json) · [隔离检查结果](checks.json)

## 结论与证据边界

这是一套基于 Android shell 的手机本地代理方案：Codex CLI 负责模型循环，自定义 stdio MCP 执行固定手机动作，Java helper 经 `app_process` 调用 UiAutomation；可选 HTTP 任务桥将小跃的普通执行工具接到这个代理。没有发现该包实现厂商原生 GUI Agent 解锁、root 提权或默认模型替换的代码。

**🛡️ 未完全通过。** 本次在给定源码与附带 DEX 的审查范围内，未发现固定外传收集端点或隐藏任意命令 HTTP 路由；这不等于无数据外传或完整安全保证。正常工作本来就会把 UI/截图发送给配置的模型服务。CLI 本体、jq、服务商、Android 系统和小跃端的完整运行时不在本包内，未作供应链全覆盖审计。

原包有源码，故不把源码阅读包装成全部由二进制逆向获得。对唯一 JAR 使用 JADX 1.5.6 反编译，核对全部一个应用类的核心方法；确认固定命令入口、唯一目标选择、密码框拒绝、输入回读、截图缩放及断开连接逻辑。没有通过指定 JDK/R8 工具链做可重现构建，因此不声称源码与二进制逐指令一致。

## 材料校验

收到材料共 16 个文件、未压缩合计 83,356 字节。原清单列出 15 个文件，全部校验一致；新 `manifest.json` 覆盖清单本身在内的 16 文件。

| 文件 | 大小 | SHA-256 |
| --- | --- | --- |
| `phone/phone-ui-helper.jar` | 7,119 字节 | `2949ae2fd39763de67dc6505e9250bf0928e9555ea76e7c192dfbf78ae0809e4` |

JAR 只有 `classes.dex`（13,164 字节），并非桌面 JVM class JAR。DEX 字符串与压缩容器元数据另在本地检查。公开包重新生成中性 ZIP 条目时间与权限元数据；源码和 JAR 字节未改。

## 逐层还原

| 层 | 关键代码 | 实际行为 |
| --- | --- | --- |
| CLI 启动器 | [phone/codex](materials/phone/codex) | 隔离运行目录、配置目录和临时目录；设置代理、Android CA；最终 exec 用户另行安装的 CLI |
| DNS/TLS 适配 | [network.sh](materials/phone/network.sh)、[provider-connect.sh](materials/phone/provider-connect.sh) | loopback CONNECT 隧道只转发模板域名 443，由 Android nc 解析 DNS；TLS 仍由客户端与目标完成，不以关闭证书校验解决兼容 |
| MCP 协议 | [phone-mcp.sh](materials/phone/phone-mcp.sh) | 逐行 JSON-RPC，支持 initialize/ping/tools/list/tools/call；九工具白名单，无通用 shell 工具 |
| 原生 UI | [PhoneUiHelper.java](materials/src/PhoneUiHelper.java) | 反射连接 UiAutomation，保留既有辅助服务；UI 树、唯一选择器、Unicode 设置文本及图像处理 |
| 委托桥 | [bridge.sh](materials/optional/bridge.sh) | loopback HTTP、Bearer 令牌、五分钟首次配对、任务编号去重、单任务互斥、固定 CLI 启动 |
| 客户端 | [client.py](materials/optional/client.py) | 禁用环境代理、读取本地令牌、提交一次，短轮询原编号；超时给出结果不明而非自动重投 |
| 进度/错误 | [task-progress.jq](materials/optional/task-progress.jq)、[task-error.jq](materials/optional/task-error.jq) | 有限工具计数与末尾两条助手消息；规范化错误，避免直接转发原始服务商错误 |
| CLI 配置 | [config.toml](materials/templates/config.toml) | Responses 接口占位、关闭自动批准询问、workspace-write、九工具启用、六个交互工具允许执行 |

启动器使用两个 loopback 端口：19092 是网络兼容隧道，19094 是可选任务桥。它们不同于不监听端口的 stdio MCP。代码的绑定地址限制不等于按 App 身份鉴权；其他进程能否访问还取决于实际 Android 环境。

## 手机工具的实现与约束

- `phone_ui`：读取当前活动窗口，最多递归深度 60、收集 2000 节点、输出 300 项；返回文本、描述、hint、包名、ID 和图片坐标。密码节点的 text 被替换，但没有覆盖所有自定义敏感字段或截图。
- `phone_screenshot`：`screencap` 到私有临时目录，helper 缩放后 base64 返回；短边最多 920、长边最多 2048，不放大。正常退出清理临时截图，不代表 CLI 事件记录中也不会存下图像。
- `phone_tap` / `phone_swipe`：校验整数和边界；默认图片坐标换算为原屏，允许明确指定原屏坐标；滑动时长 50–3000 ms。
- `phone_key`：有限导航键枚举，包含 ENTER。输入函数不发送，不代表完整工具集合永远无法提交表单。
- `phone_launch`：仅允许包名或组件形状，包名通过系统 resolve-activity 寻找真实入口，避免把未验证自由字符串作为命令。
- `phone_click`：精确文本/ID，可加包名；拒绝多目标，最多向上寻找五层父节点再点击。被点击父节点的最终业务含义仍需 UI 回读确认。
- `phone_input`：4000 字符上限，拒绝 NUL，唯一可编辑且聚焦目标，拒绝密码框，replace/append 后十次短回读；没有 shell 字符串拼接输入、剪贴板或逐键拼字后备。

参数通过 jq 校验、带引号的进程参数或 JSON 临时文件传递；在已审查路径中未见把输入文本 eval 为 shell 命令的操作。这一结论限于该 MCP/helper，不延伸至模型可使用的其他 CLI 工具。

## 桥接的状态机

首次部署 → 本地打开五分钟配对窗口 → 客户端提交 64 位十六进制随机令牌 → 服务端保存令牌并关窗 → 带 Bearer 提交任务 → 查询相同任务编号。

任务为 queued → running → completed/failed。`request_id` 限字母数字开头及 64 字符；同编号相同规范 JSON 返回 duplicate，不同内容返回 409。创建 active 目录取得互斥，worker 退出时按编号释放。普通任务期限 600 秒，状态诊断的 CLI 版本检查期限 20 秒。状态为 completed 的条件只是退出成功且有最终文本，不代表用户业务目标已验收。

HTTP 拒绝不符 Host、重复 Content-Length/Authorization、Transfer-Encoding、不完整正文、拼接 JSON 和超长任务。`/health`、`/assets/client.py` 可匿名读取；任务接口需要令牌；`/v1/pair` 依赖配对窗口，不要求旧令牌。没有发现通过路由拼接自由文件路径的下载功能。

## 本次独立测试

共 **28 项通过**：包括 19 项桥接请求/权限状态检查、5 份 shell 语法检查、1 份 Python 语法检查、2 项进度聚合/虚构 Key 脱敏检查和 1 项超时错误规范化检查。

桥接测试使用宿主机临时目录、替换绝对路径及 shell 路径，worker 启动被 mock。覆盖无令牌健康检查、任务拒绝、Host、关窗配对、无效令牌、首次配对、拒绝再次配对、错误令牌、路径穿越、额外字段、拼接 JSON、传输编码、接收任务、重复编号、编号冲突、并发拒绝、状态读取、令牌文件权限和首个调用者令牌持久化。未监听网络端口，未执行真实手机动作，未启动 CLI/模型。

这些测试证明的是解析与状态分支在替换环境中的表现，不证明 Android nc 并发模型、SELinux、UiAutomation 权限、CLI 沙箱、真正的中文输入或断电恢复。宿主机语法检查也不能替代 Android shell 全兼容验证。

## 数据流与持久化

| 数据 | 去向或保存位置 | 审查结论 |
| --- | --- | --- |
| 模型 API Key | 手机 CLI 登录状态文件 | 不在分享包；shell 身份可读，提示词禁止读取不构成隔离 |
| 配对令牌 | 服务端 token 与客户端私有 JSON | 随机生成、文件收紧权限；未使用模型 API Key 作桥接令牌 |
| UI 文本与截图 | MCP → CLI → 用户配置的模型服务 | 属正常功能数据流，必须视实际页面与服务商决定是否允许 |
| 请求/提示词/事件/输出 | bridge/tasks 下的任务文件 | 无自动保留期限；CLI 原始事件可能含 UI、截图、模型文本 |
| MCP 审计 | mcp-events.jsonl | 输入 text 删除，仅记字符数；但 target_text/resource_id 等选择器仍可能泄露业务信息 |
| 查询进度与最终回答 | 认证客户端 | 进度消息只做有限模式脱敏；最终回答原文按长度截取，并非全面隐私过滤 |

原包没有用户现场任务日志或真实令牌，不代表今后使用过程中生成的数据也适合分享。

## 发现、影响与修改建议

| 优先级 | 发现及代码依据 | 影响与建议 |
| --- | --- | --- |
| 高 | `bridge.sh` 的 `/v1/pair` 在窗口内接收首个合规 token；没有独立的一次性确认码或显式客户端确认 | 若不可信本地进程可达该端口并赶在正常客户端前提交，可抢占首次配对，取得任务提交能力。隔离测试确认首个令牌被接受；未在 Android 跨 App 复现攻击。建议服务端生成单次随机挑战，经用户可信渠道交付，并在服务端确认客户端 |
| 高 | worker 仅通过提示词禁止读凭据/改桥接，模板 `approval_policy=never`，运行 workspace-write | 提示词不强制隔离。CLI 的其他 shell/文件工具是否可越过工作区，取决于实际沙箱；原教程也记载 Android bwrap 兼容限制。建议独立 UID/凭据代理、强制工具范围及真实沙箱拒绝测试；不能仅把模式名称当安全证据 |
| 中 | 任务目录保留 request、prompt、codex-events、stderr、final，且无自动清理 | UI、截图和回答可能长期落盘；建议明确保留期限、配额、用户可核验清理入口和日志字段白名单，重启后不要盲删正在执行的任务 |
| 中 | `task-progress.jq` 仅过滤部分 Key/Bearer/base64；status 直接返回最终回答 | 联系方式、账号文字、非匹配形式的秘密仍可返回；建议最小进度数据结构、可配置敏感字段处理和最终回答策略，不能宣称通用脱敏 |
| 中 | `phone_status` 读模型和 shell identity，UI/screenshot 返回当前内容 | 接口会把设备与页面上下文交给模型；建议按任务关闭不必要字段，提供可预览数据和明确的只读工具配置 |
| 低 | `tools/list` 以原屏尺寸声明 tap/swipe 最大值，但默认执行按图片尺寸校验 | 超过图片范围但符合 schema 的坐标会被拒绝；建议 schema 与默认空间一致，或分开声明图片/屏幕参数 |
| 低 | UI 几何值复用，phone_ui 自身不刷新 geometry；状态/截图/点击分支刷新时机不同 | 旋转或分辨率改变后可能出现旧 UI 坐标与新操作坐标错配；建议逐帧绑定几何和旋转标识，变更即拒绝旧坐标 |
| 低 | 依赖 nc 启动但无完整守护/恢复；任务活跃状态与大量文件可跨故障留下 | 不承诺开机自启、重启恢复或长期离线存活；建议重启检测、worker 存活核对、磁盘配额和可核验恢复流程 |

优先级表达改进顺序，不等同于已在真实手机上成功利用。此版本按研究资料收录，保持“未完全通过”；没有授予安全通过徽章。

## 与原视频、作者说明的对应

原片呈现聊天发起、应用导航、搜索、目标个人页与按钮变化，说明操作链路具有直观演示价值；不能从视频证明动作一定由本包完成、是否调用某个模型、没有中途人工操作或后端已永久保存。视频抽帧只用于本地核对，原片和现场截图未进入公开附件。

作者说明中的 Android 16 / ARM64 / 无 root / CLI 0.163.0、中文输入及小跃新聊天复用，均保留为来源陈述。未执行新机从零安装、实际模型调用、跨 App 输入、网络监听可达性、端口竞争、重启/断线、长时间守护、耗电和沙箱隔离验收。

## 参考与许可

[OpenAI 官方 MCP 文档](https://developers.openai.com/codex/mcp/)用于核对 CLI 连接 MCP 的一般概念；它不构成本包 Android 适配的官方认证。分析的主要证据为本目录原始材料、JAR 的独立反编译核对以及本次隔离检查。

原分享未附 LICENSE 或原始仓库。按来源署名展示，不把该材料改署为研究所原创，不自动套用研究所或 OpenAI 的许可。本次未分发 CLI/jq 上游二进制，原教程中的外部版本、下载地址和校验值未在本轮重新下载验证。
