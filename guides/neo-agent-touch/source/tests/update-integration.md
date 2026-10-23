# 一键更新接线与验证范围

本轮新增原生更新器，无第三方运行库依赖。

## MainActivity 接线

- 字段 `AppUpdater appUpdater;`
- 创建 WebView 前：`appUpdater=new AppUpdater(this,new AppUpdater.Listener(){public void changed(JSONObject state){event("update",state);}public boolean installationAllowed(){ControlService s=ControlService.instance;return resumed&&!isFinishing()&&!planning&&(s==null||!s.busy());}});`
- `sendState()` 增加 `.put("update",appUpdater.state())`。
- Bridge：`checkUpdate` → `appUpdater.check()`；`installUpdate` → `appUpdater.downloadAndInstall()`；`updateRelease` → `appUpdater.openRelease()`。
- `onResume()` 在 `resumed=true` 后调用 `appUpdater.onResume()`。
- `onDestroy()` 调用 `appUpdater.close()`。
- 安装入口可在 MainActivity 再拦当前任务，更新器下载前、下载后及安装权限返回后均独立检查 `installationAllowed()`。

## 更新清单

固定主源：`https://raw.githubusercontent.com/JiayuStudio/stepx-neo-lab/main/guides/neo-agent-touch/update.json`。

字段：`schema=1`、`packageName`、`versionName`、`versionCode`、`tag`、`assetName`、`downloadUrl`、`sha256`、`size`、`notes`。所有 APK 字段必填，notes 可空。包名固定 `studio.jiayu.neoagent`，tag 固定 `neo-agent-touch-v` 加语义版本；下载 URL 必须位于同一仓库、tag、精确 APK 文件名的 GitHub release 中。

有效清单直接返回官方通道结果。主源缺失、异常或格式不符时，查询公开 releases 列表，最多 10 页、每页 100 条；包含 prerelease，按 SemVer 选较新应用 APK，忽略宣传片、源码与 debug APK。API 分页未完成、限流或存在较新发布但没有可校验 APK 时，明确返回失败，不显示已是最新版。

下载严格校验发布字节数和 SHA-256；归档包名、版本、递增 versionCode 和已安装签名全部匹配后，才开放只读单文件 URI 给系统安装器。没有后台检查、静默安装或执行手机业务。安装前如出现新任务则保留已验证文件，等用户结束任务后再点击。

## 参考与复用

- [HeyPouya/AndroidAppUpdater](https://github.com/HeyPouya/AndroidAppUpdater)，Apache-2.0：参考“检查 → 提示 → 用户确认更新”的结构；其 Kotlin/Compose/商店适配对现有纯 Java 工程没有必要，因此未复用代码、未引入库。
- [GitHub Releases REST](https://docs.github.com/en/rest/releases/releases)：匿名公开发布列表、分页与 prerelease 数据。
- [Android PackageManager](https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls())：当前安装身份、APK 归档签名、安装来源许可。
- [Android ContentProvider](https://developer.android.com/reference/android/content/ContentProvider)：非导出、临时只读 URI 授权。

## 验证边界

纯规则测试覆盖 beta 数字排序、正式版排序、非法版本、宣传片/debug 过滤、仿冒 URL/越界路径、SHA 歧义、大小上限、包名/版本/签名不匹配。原生代码需经 Android 36 编译；UI 需在实际安装包确认。未以下载器测试替代实体机安装许可、安装成功和升级后数据保留的验收。
