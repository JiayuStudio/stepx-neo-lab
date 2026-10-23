# Neo Agent Touch 0.1.16-beta.1

Codex · 来源0.1.15-beta.1。独立修订目录，原交付不覆盖。

三平台共用一个商品目标的整句比价指令由手机本地编译为名单顺序。完整型号、规格、原文预算和否定条件保留；各阶段仅查价，不加购、不下单、不付款。独立应用目标仍分别绑定原文，合法应用重访保持。规划器校正提示只用于内部重规划，用户看到固定简短结果。

菜单与关于页新增一键检查更新。固定公开更新清单为主源，GitHub应用发布为备用；支持Beta语义版本并排除影片和源码附件。用户选择更新后下载APK，校验字节数、SHA-256、包名、精确版本、递增versionCode及同一签名，再交给系统安装确认；安装来源许可按Android系统处理，执行任务时不会打开安装器。没有后台检查或静默安装。

release继续使用搜狗等当前系统输入法，不内置API Key、不打包旧ASR模型、麦克风权限或调试活动。

## 构建与测试

需要JDK、Android SDK 36/build-tools 36.0.0和R8/D8 JAR。设置ANDROID_SDK_ROOT、JAVA_HOME、R8_JAR、独立NEO_AGENT_SIGNING_DIR，运行 `bash build.sh release`。产物在dist。独立签名文件不属于源码包，升级已安装版本须使用同一签名。运行 `bash tests/run-unit.sh` 检查本地纯规则。

更新实现及参考来源见 [tests/update-integration.md](tests/update-integration.md)。当前目录的测试和安装记录会在完成后登记；构建、原生构造检查、更新检查和实际购物闭环分别核验，不互相代替。
