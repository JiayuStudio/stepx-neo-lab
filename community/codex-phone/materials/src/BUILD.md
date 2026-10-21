# 重建 UI helper（可选）

分享包附带 `phone/phone-ui-helper.jar` 及本次生成它的 `PhoneUiHelper.java` 源码。JAR 是 Android DEX 容器，不是桌面 Java 运行入口。

预构建 JAR 大小为 7119 字节，SHA-256：

```text
2949ae2fd39763de67dc6505e9250bf0928e9555ea76e7c192dfbf78ae0809e4
```

需要重建时，准备 JDK、Android SDK Platform 36 的 `android.jar`，以及 Google Maven 的 R8/D8 `8.13.17`，放在本目录：

```text
https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.13.17/r8-8.13.17.jar
```

在本目录执行：

```powershell
New-Item -ItemType Directory -Force -Path classes | Out-Null
javac -encoding UTF-8 --release 8 -Xlint:-options -classpath android.jar -d classes PhoneUiHelper.java
java -cp r8-8.13.17.jar com.android.tools.r8.D8 --min-api 26 --lib android.jar --output phone-ui-helper.jar classes/PhoneUiHelper.class
Get-FileHash -Algorithm SHA256 phone-ui-helper.jar
```

`--min-api 26` 是 D8 编译参数，不是已经验证所有 Android 8 及以上设备的声明。helper 使用 Android 框架接口及反射，本次实际运行环境是 Android 16、shell UID 2000。

部署用 `adb push` 传二进制。若需要升级正在使用的安装，先确认没有手机控制任务在执行，保留原文件再替换；仅重新构建或升级 helper 不需要重新配对客户端。
