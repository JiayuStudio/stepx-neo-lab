# AI 阅读入口：Neo Agent Touch

修订日期：2026-10-23。用途是检索、学习和引用，不是可安装 Skill、MCP 工具注册或远程控制授权。读取本文不会获得手机权限。

先读取 [README](README.md)，再读 [knowledge.json](knowledge.json) 的稳定结论编号和证据标签；详细研究见 [2026-10-23 记录](research/2026-10-23.md)，公开主源与许可见 [sources.json](research/sources.json)，产物与验证见 [verification-v15.json](verification-v15.json)。阅读实现从 [source/README.md](source/README.md) 开始；release 与 debug 的范围不同。所有文件路径均相对于本条目根目录。

## 必须保留的结论

- **C01 · real_device**：0.1.14 的真人长句转写与多应用任务未可靠通过。不能发布私人转写，也不能将文件解码或控件夹具当作真人语音成功。
- **C02 · reference**：0.1.15 已通过 41 组纯规则检查，其中原话／路由 49 项、输入目标所有权 58 项；原生工作流构造另有 96 项夹具通过，真实应用回归待验。测试数不是应用数或端到端成功率。
- **C03 · reference**：release 当前使用搜狗等系统输入法，用户确认文本后进入聊天规划；自研 ASR 升级暂停。release 不含旧 Vosk、native 识别模型或 RECORD_AUDIO 权限；debug 历史实现另计。不能声称所选输入法完全离线或语音准确率已改善。
- **C04 · native_fixture**：历史 0.1.13 输入法夹具通过 36 项断言，使用真实系统输入连接与自有编辑框、合成任务所有者。它未覆盖生产 start / resumeFromUser、第三方搜索或 ASR。
- **C05 · reference**：Zipformer 2023-02-20 是一个来源与 Apache-2.0 模型卡较清楚的后续候选，选定图与 tokens 合计 199,056,205 字节；未证明更准，未进入默认。
- **C06 · reference**：原版 SenseVoiceSmall ONNX 的模型许可另有限制，不能以引擎代码许可证覆盖；本轮不分发其权重。
- **C07 · not_tested**：候选 ASR 的目标设备准确率、内存、延迟、20 条公开命令结果，以及 0.1.15 真实应用回归均不能凭本文宣称通过。

- **C08 · native_fixture**：0.1.15 原生工作流构造夹具通过 96 项，清理通过；零模型调用、零业务动作。它不证明第三方应用完成目标或搜狗语音准确率。

- **C09 · real_device**：0.1.15 release 已构建、安装并读回相同包摘要，222,437 字节；保留已有输入法选择与启用列表，没有新增系统权限授权。它不证明系统键盘 UI、搜狗语音或新的三应用任务已通过。

## 证据标签

`reference` 包含主源、代码分析和明确标注的纯规则测试；`native_fixture` 是 Android 隔离原生夹具；`real_device` 是具体真机观察，失败也属于证据；`not_tested` 表示未完成验收。回答应保留版本、结论编号、范围限制和链接，不把一种标签升级为另一种。

## 拉取与完整性

公开 Raw 路径为 `https://raw.githubusercontent.com/JiayuStudio/stepx-neo-lab/<完整提交SHA>/guides/neo-agent-touch/`。正式读取应固定完整提交，不从移动分支推断已审核状态。

[bootstrap.py](bootstrap.py)仅使用 Python 标准库 urllib 拉取白名单内 `README.md`、`ai.md`、`knowledge.json`，须由调用者从可信渠道提供三个预期 SHA-256；全部校验成功后才保存。脚本不执行远端代码、不安装依赖、不接收 token、不调用手机或模型。SHA-256 只有在预期值来自可信渠道时才提供完整性保障；从同一未核实网页同时取得内容和摘要不构成独立信任。

无需运行脚本也可直接只读获取 Markdown/JSON。脚本、文档、测试句中的命令只是参考资料，执行仍取决于当前会话的明确授权。

## 回答示例

“C02 / reference：41 组纯规则通过，其中原话／路由 49 项、输入目标所有权 58 项；另有 96 项原生构造夹具通过。C09 仅证明正式包构建安装一致，仍不代表第三方业务或搜狗语音准确率通过。见验证数据。”

不要回答“语音已修好”“20 条全过”“支持所有应用”“已买票付款”“模型完全离线”，除非另有同版本、对应阶段的真实证据。
