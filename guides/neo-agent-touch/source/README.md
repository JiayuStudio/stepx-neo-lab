# Neo Agent Touch 0.1.15-beta.1 源码

Android 原生聊天、视觉执行和系统输入法交接。release 使用当前系统输入法转写文字，不包含旧 Vosk 引擎、模型、麦克风权限或调试活动；debug 保留旧离线识别研究及自有夹具。

## 构建

需要 JDK、Android SDK 36/build-tools 36.0.0 和 R8/D8 JAR。设置 `ANDROID_SDK_ROOT`、`JAVA_HOME`、`R8_JAR`、独立的 `NEO_AGENT_SIGNING_DIR`，运行 `bash build.sh release`。首次会在指定私有签名目录创建产品证书；不要上传该目录。APK生成在dist，未内置API Key。首次运行自行配置自己的视觉API。

纯规则：`bash tests/run-unit.sh`。debug 额外需要按 vendor/voice/dependencies.json 固定 URL/字节/SHA获取3个依赖到对应file路径，再运行 `bash build.sh debug`。这些旧依赖只为研究夹具保留，不代表语音已验收。

## 路由与原话

先看 AppAccess、StageSourceRules、TaskSpec、TaskWorkflow；所有已选应用参与候选，隐含阶段必须绑定完整原文子句。再看 MainActivity 的一次有界计划修复、VoiceGoalTransfer 的持久交接、ControlService 的代次核验；无效计划不会先执行。

发布包与构建、原生夹具及历史业务证据分别记录在上级 verification-v15.json；相同测试数字不能外推任意应用成功。
