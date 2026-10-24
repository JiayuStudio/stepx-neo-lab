# 应用内更新 0.8.16

2026-10-24 · Codex · 来源 0.8.15。

## 行为

从荒野菜单“检查更新”、管理页“检查游戏更新”或应用图标长按快捷方式进入。仅用户打开时检查，没有定时后台检查或强制升级。复用研究所现有 `/apk-catalog.json` 和安装包同步服务；只读 `items.pokewilds`，按 Android versionCode 比较，拒绝降级。支持失败重试、下载进度、取消、横竖屏切换、浏览更新说明。

下载仅允许本站 HTTPS 的 Poke 托管文件目录，路径必须带匹配的 SHA-256；限制大小、重定向、超时和元数据长度。下载后核对精确字节数、SHA-256、APK 密码学签名、包名、版本号及当前安装签名。安装前再次复核。仅向系统安装器临时开放更新缓存目录，最终安装由 Android 确认。游戏运行或游戏文件安装未结束时阻止安装，提示先保存退出。文件不写入游戏或存档目录。取消或失败的分片不作为可安装包使用。

公版维持相同签名以便覆盖更新，并关闭 Android 调试标志。历史版本没有更新入口，首次需手动安装0.8.16，后续可从游戏检查更新。VPS同步器核验候选后晋升并清理所属旧托管APK，GitHub历史发行及源码、影片不在清理范围。

## 参考与实际复用

查询过 GitHub 的 javiersantos/AppUpdater 和 elliot619/AppUpdater；前者支持自定义更新源、后者演示JSON与安装器，但它们的旧支持库/版本比较/校验策略不直接满足此项目的 Android36 与大包验证。本轮未复制其代码，也未引入第三方更新框架。

实际新增运行库：Android apksig 9.3.1（AOSP，Apache-2.0），用于验证整个APK签名；许可与版权保存在包内notices。界面、下载和版本策略为本次适配代码。AndroidX FileProvider沿用已有AndroidX依赖。

- https://github.com/javiersantos/AppUpdater
- https://github.com/elliot619/AppUpdater
- https://android.googlesource.com/platform/tools/apksig/
- https://developer.android.com/reference/androidx/core/content/FileProvider
- https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls()

## 验收

版本与地址策略、错误摘要、截断/超长传输、取消及缓存损坏有单元测试。真实下载、系统确认、安装后再检查和存档保留以本轮设备证据为准，尚未完成项目不得从构建推断通过。
