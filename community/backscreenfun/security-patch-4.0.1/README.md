# 4.0.1 安全补丁源码

原作：鸿渐。安全修改：JIAYU STUDIO / StepX Neo 研究所。原始工程与签名私钥未提供；这是基于 4.0 APK 的独立修订，不是原作者签名升级。这里公开本轮新写的修复类及虚构数据测试，不发布完整第三方反编译源码，不改变原作的权利归属。

| 文件 | 作用 |
| --- | --- |
| `src/com/amoo/backscreenfun/SecureHttp.java` | 有界解析、全路由认证、输入约束、正确状态码与最小返回值 |
| `StatusServer.java` | 回环监听、线程/队列/连接频率与读取期限、停止关闭 |
| `Pairing.java` | 128 位随机令牌、私有持久保存、轮换、常量时间比较 |
| `StatusReceiver.java` | 旧广播处理函数关闭；清单另行禁用且不导出 |
| `tests/SecurityRegression.java` | 虚构状态的确定性协议测试；`offline` 不运行主机套接字部分 |
| `tests/AndroidProbe.java` | 独立 Android UID 的无令牌请求、资源限制和广播尝试；不接收配对凭据 |
| `tests/stubs/` | JVM 替身，不进入正式 APK |

## 运行离线检查

需要 Java 编译器；在本目录执行：

```sh
mkdir -p build/jvm
javac --release 8 -g:none -d build/jvm $(find src tests/stubs -name '*.java') tests/SecurityRegression.java
java -cp build/jvm com.amoo.backscreenfun.SecurityRegression offline
```

离线检查不能代替 Android、最终 APK 和实体背屏验证。`SecurityRegression` 无参数时还有真实套接字检查，依赖本机端口与建连环境；本次电脑建连延迟影响该部分，报告单独记录。

## APK 修改记录

基包 SHA-256：`3dc9333bf7ddada0fd10f6ffab255152c6017edb13856003e0bce472c373918e`。使用 Apktool 3.0.3 解码，Android 36 工具链以最低 API 26 编译上述修复类，D8 后替换对应原类及其内部类，再重建、对齐、签名。`PlaybackService` 编译替身不进入新包；保留原播放实现。

清单包名改为 `com.jiayu.backscreenfun.secure`，组件名保留原类的全限定名；versionName `4.0.1`、versionCode `40001`。`StatusReceiver` 设置 `enabled=false`、`exported=false`，移除 action 过滤器；`debuggable=false`、`allowBackup=false`。配对帮助改为带令牌的 POST 示例，界面标识安全修订；保留原作署名、素材、协议和媒体实现。

新签名证书使用通用产品与工作室名称，密钥不随包或源码分发。安装与原版并存，不覆盖原版数据。第三方需要自行重建与签名，不能用本目录证明原作者认可或获得原作源码许可。

网络实现仅参考 [NanoHTTPD 的范围说明](https://github.com/NanoHttpd/nanohttpd) 评估轻量服务器方案；最终未引入或复制该库。小型专用协议代码在本轮编写，避免把通用服务器默认配置当作鉴权与输入保护。


## 本次 Android 复测

[结构化结果](../security-results-4.0.1.json) 对应正式 APK 的同一 SHA-256。手机本地 TCP 完成 31 项接口检查、2 项界面令牌轮换检查和 7 项状态/恢复检查；探针使用不同 UID 和不同签名、无配对凭据，10 种请求均被拒绝。慢速、并发与突发请求后服务可恢复；探针已卸载。

正式 APK 不包含测试替身、探针或测试令牌。r2 仅补充测试工具与实机证据，正式 APK 字节未改。原始 r1 源码压缩包及首次静态报告作为发行历史保留，最新范围以详细报告和 r2 附件为准。
