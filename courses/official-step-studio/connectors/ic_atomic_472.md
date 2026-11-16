# 通用原子能力：应用启动助手

[课程导航](../README.md) · [连接器总表](../connector-index.md) · [官方详情](https://studio.stepx.com/connectors/ic_atomic_472)

2026-11-17 核对；官方版本 **1.0.1**，当前能力表 **36 项**。本课提供开发步骤与验收方法，尚未代替你完成授权、调用或真机验收。

## 这一课做什么

输入应用显示名称，先检查是否安装，再打开匹配应用。

## 添加与准备

1. 打开官方 Step Studio，新建或进入一个测试用应用项目。
2. 打开「连接器」，搜索「通用原子能力」，查看详情后选择「添加到项目」。选择刚建的测试项目。
3. 在兼容的 Neo 设备上按提示授予本课所需权限；不需要自行申请 API Key。
4. 让 Studio 读取当前连接器说明与参数，先完成一个最小动作。资料不足时先显示待配置，不用假数据冒充成功。

## 按顺序实现

先实现上述目标中的一项动作。为输入、执行中、结果和失败分别留出位置；依据连接器真实参数生成调用，在支持的 Neo 手机上观察结果。

完成第一次调用后，再加入第二项操作；查询应显示真实返回，修改应在手机或对应应用里复核实际效果。

| 起步能力 | 用途 | 当前必填参数 |
| --- | --- | --- |
| `check_app_installed_by_name` | 按名称检查应用安装状态 | appName |
| `open_app_by_name` | 按名称打开应用 | appName |

参数类型、可选值和返回字段，以项目内当前 schema 为准；复制名字不等于接口已经接通。

## 可复制提示词

```text
使用官方「通用原子能力」连接器做「应用启动助手」。
目标：输入应用显示名称，先检查是否安装，再打开匹配应用。
先读取当前能力、inputSchema 和返回格式，不猜工具名和参数。
先展示页面及调用计划；只接入实现本目标所需的能力。
先实现上述目标中的一项动作。为输入、执行中、结果和失败分别留出位置；依据连接器真实参数生成调用，在支持的 Neo 手机上观察结果。
页面显示未连接、执行中、成功、无结果与失败状态；成功以真实返回为准。
修改、发送、支付、删除或批量操作前显示目标与影响，等待确认。
手机专属能力需要兼容的 STEPX Neo 与对应权限；网页预览不代表设备操作成功。修改设置后恢复原状态，发送信息、通话、录屏和删除文件需明确目标与确认。
完成后给我逐项测试方法，并明确哪些只是代码完成、哪些已实际运行。
```

## 验收与下一步

- 正常输入：记录一次调用的输入、真实返回与页面结果；数字、单位和对象相互一致。
- 未授权或拒绝权限：显示需要连接或授权，不能显示成功。
- 空结果、无网络或服务错误：保留可理解的说明，恢复条件后再由用户重试。
- 写操作：只用测试对象；完成后在原服务或手机中读回，不把按钮点击视作成功。

手机专属能力需要兼容的 STEPX Neo 与对应权限；网页预览不代表设备操作成功。修改设置后恢复原状态，发送信息、通话、录屏和删除文件需明确目标与确认。

<details>
<summary>本次官方能力名称（开发者查阅）</summary>

- `open_app_by_package`
- `open_app_by_name`
- `show_widget_by_package_class`
- `show_notify_by_info`
- `batch_file_upload`
- `batch_file_upload_return_all`
- `batch_file_upload_all_progress`
- `common_ability_clipboard_text`
- `common_ability_get_screen_size`
- `check_app_installed_by_name`
- `open_url_with_browser`
- `common_ability_get_app_list`
- `common_ability_get_app_count`
- `set_back_screen_enabled`
- `get_back_screen_enabled`
- `speak_text`
- `start_vibration`
- `stop_vibration`
- `stop_speak_text`
- `get_back_screen_auto_brightness`
- `set_back_screen_auto_brightness`
- `get_back_screen_brightness`
- `set_back_screen_brightness`
- `get_back_screen_camera_link`
- `set_back_screen_camera_link`
- `get_back_screen_auto_rotate`
- `set_back_screen_auto_rotate`
- `get_back_screen_display_mode`
- `set_back_screen_display_mode`
- `get_back_screen_basic_type`
- `set_back_screen_basic_type`
- `set_back_screen_frame`
- `clear_back_screen_frame`
- `wake_up_back_screen`
- `sleep_back_screen`
- `get_back_screen_display_info`

</details>
