# 控制服务状态修复

2026-10-23 · 0.1.17-beta.1

系统保存的无障碍服务选择、Android实际连接回调、应用内保存的可控制应用范围是三个状态。启用待连接时不能声称未开启，也不能以打开设置页成功作为权限已授予的证明。

本版使用本应用的精确ComponentName核对，读取系统启用记录和公开AccessibilityManager列表；连接状态来自当前ControlService实例。结果仅暴露本服务布尔与五态，不记录其他服务名单。设置入口使用公开ACTION_ACCESSIBILITY_SETTINGS及可忽略的定位提示，失败后退回公开设置页；不会写系统开关。进入设置先暂停原目标，返回只核对状态，不自动开始任务。

聊天页状态更新保留输入框节点、文字、选择区和焦点，避免状态轮询打断当前系统输入法的组合输入。本机WebView夹具使用合成文字与五态切换，不能作为真人语音或搜狗识别准确率证明。

## 参考与采用范围

- [Android AccessibilityManager](https://developer.android.com/reference/android/view/accessibility/AccessibilityManager)：采用getEnabledAccessibilityServiceList与状态监听的公开API。
- [Android AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)：以onServiceConnected实际绑定回调判断可控制状态，解绑撤销旧执行所有权。
- [Android Settings](https://developer.android.com/reference/android/provider/Settings#ACTION_ACCESSIBILITY_SETTINGS)：使用公开设置入口，厂商页面与返回数据不能被推断为授权结果。
- [AOSP Android 16 Settings定义](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android16-release/core/java/android/provider/Settings.java)：用于核对隐藏详情入口与公开API边界；不调用已被系统拒绝的受限详情入口。

本轮为原实现的窄修复，未引入第三方运行库或复制第三方代码。真机结果单独登记；不把规则检查、合成输入或安装通过当作多应用业务闭环完成。
