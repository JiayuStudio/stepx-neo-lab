# Neo 适配源码与构建

本目录公开 Android 界面、触摸输入、存读档桥接、背屏服务、点阵生成工具、中文补丁及运行环境配方。原游戏为 PokeWilds 0.8.11，Neo 适配版本为 0.8.16。

## 重新构建 Android 层

需要 Android SDK 36、JDK 17 或更新版本，Gradle Wrapper 会使用 9.7.1；本轮实际用 JDK 25 构建。设置 `JAVA_HOME`、`ANDROID_HOME` 为自己的安装目录。

```sh
python3 bootstrap.py --download
# 也可以使用已下载且校验一致的公开包：
# python3 bootstrap.py --apk /path/to/PokeWilds-Neo-0.8.16.apk
cd android
./gradlew :android-app:assembleDebug
```

输出为 `android-app/build/outputs/apk/debug/android-app-debug.apk`。重新签名使用自己的密钥；本目录不提供发布私钥。签名不同不能直接覆盖已有安装，先在游戏设置中导出存档。

`bootstrap.py` 核验整个 APK 的固定 SHA-256 后，提取原生库、游戏及 Linux 运行环境作为输入。这是 **重建 Android 适配层** 的方法，不能据此称整个 Linux / Java / 原游戏均已从源码重编。不要运行根级 `clean` 删除 `native/build/jniLibs`；仅清理应用模块可用 `:android-app:clean`。

## 应用内更新

游戏设置或图标长按选择“检查游戏更新”。使用网站同一份版本目录，下载后核对包大小、SHA-256、签名、包名和版本；安装由Android确认，游戏运行中先保存退出。开发实现与协议见[说明](docs/application-updates.md)。

## 阅读与修改

| 目录 | 内容 |
| --- | --- |
| `android/android-app/src/main/java/` | Game Boy / GBA 布局、屏幕切换、中文弹窗、读档、背屏设置及服务 |
| `android/lorie/` | 与固定 Termux:X11 版本配套的 Java 和本地集成源码 |
| `tools/java/` | `NeoBridge`、`ZhDialogs`、ASM 游戏挂钩补丁 |
| `tools/make_pets.py` | 原作精灵转 17×23 灰度点阵；用环境变量 `POKEWILDS_ZH_JAR` 指定中文资源 JAR |
| `localization/` | 中文词条、显示补丁及已生成的字形资源；原跨平台中文工程的相关来源 |
| `runtime-recipes/` | 固定运行环境清单、原生补丁、PRoot 构建、游戏修复与控制补丁 |

点阵目标帧率在应用中配置；原始形象大多只有一到两个姿态，连续动作由程序驱动，不表示每个角色都有数十帧独立手绘动画。

## 运行环境的对应来源

直接复用的独立安卓运行环境为 [Pokewilds-Android 的固定提交](https://github.com/NoNoseYaKnow/Pokewilds-Android/tree/50a7eb0c1857c3e2b592bf447147678672f28ab1)，二进制来自其 [v1.0.0](https://github.com/NoNoseYaKnow/Pokewilds-Android/releases/tag/v1.0.0)。本目录保留当时配方、补丁和输入哈希。

- [Termux:X11 对应提交及完整子模块](https://github.com/termux/termux-x11/tree/bd1cfadd74f98b548662a48b6e1d564aa434c86e)：按 `runtime-recipes/prepare.sh` 初始化固定提交及子模块、应用补丁；各子模块的固定提交与许可见 [原生来源索引](android/android-app/src/main/assets/notices/native/README.txt)。空的子模块目录需要按配方获取源码。
- [PRoot 对应源码](https://github.com/termux/proot/tree/7266fb3e8516535682f5a9c8f3a7e70f6506eddb)、[talloc 2.4.2 源码](https://download.samba.org/pub/talloc/talloc-2.4.2.tar.gz)、[libandroid-shmem 0.7 源码](https://github.com/termux/libandroid-shmem/tree/v0.7)：本地修改与校验值在 `runtime-recipes/native/`，包括运行目录与 Android 头文件补丁。
- Ubuntu、JRE 及 133 个 Ubuntu 软件包的输入在 [runtime/manifest.json](runtime-recipes/runtime/manifest.json)；29 个 Termux 包的精确版本和哈希在 [host-packages.lock.json](runtime-recipes/packaging/host-packages.lock.json)。包内保留系统软件包的版权文件、JRE `legal/` 及 NOTICE。
- 修改过的 VirGL 路径转换和原生编译步骤见 `runtime-recipes/packaging/`；各软件的许可分别适用，不将所有包归为同一许可证。

此处提供固定来源和修改配方；本轮没有重新编译全部第三方运行环境，也没有完成独立机器上的全链路重建验收。中文补丁源码和已生成资源可供继续开发；从未修改原版重新生成中文核心的完整构建尚待单独复核。

## 许可范围

Neo 适配不改变原游戏、宝可梦形象、音乐或其他第三方素材的权利归属。PokeWilds 上游快照没有统一的游戏再分发许可证，本仓库不对其素材另行授予许可。Termux:X11 使用 GPLv3；各子模块、运行库和修复器保留自己的声明，详见包内 `notices/` 和 [项目末尾的来源清单](../README.md#引用的项目与库)。
