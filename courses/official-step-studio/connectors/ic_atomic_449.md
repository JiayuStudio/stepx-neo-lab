# 电话：拨号确认卡

[课程导航](../README.md) · [连接器总表](../connector-index.md) · [官方详情](https://studio.stepx.com/connectors/ic_atomic_449)

2026-11-17 核对；官方版本 **1.0.1**，当前能力表 **34 项**。本课提供开发步骤与验收方法，尚未代替你完成授权、调用或真机验收。

## 这一课做什么

输入号码后先显示确认卡，由用户选择继续拨号或取消。

## 添加与准备

1. 打开官方 Step Studio，新建或进入一个测试用应用项目。
2. 打开「连接器」，搜索「电话」，查看详情后选择「添加到项目」。选择刚建的测试项目。
3. 在兼容的 Neo 设备上按提示授予本课所需权限；不需要自行申请 API Key。
4. 让 Studio 读取当前连接器说明与参数，先完成一个最小动作。资料不足时先显示待配置，不用假数据冒充成功。

## 按顺序实现

先实现上述目标中的一项动作。为输入、执行中、结果和失败分别留出位置；依据连接器真实参数生成调用，在支持的 Neo 手机上观察结果。

完成第一次调用后，再加入第二项操作；查询应显示真实返回，修改应在手机或对应应用里复核实际效果。

| 起步能力 | 用途 | 当前必填参数 |
| --- | --- | --- |
| `dialer_call_to` | 拨打电话 | number |

参数类型、可选值和返回字段，以项目内当前 schema 为准；复制名字不等于接口已经接通。

## 可复制提示词

```text
使用官方「电话」连接器做「拨号确认卡」。
目标：输入号码后先显示确认卡，由用户选择继续拨号或取消。
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

- `dialer_call_to`
- `dialer_call_contact`
- `dialer_show_incall_screen`
- `dialer_accept_call`
- `dialer_hangup_call`
- `dialer_call_back`
- `dialer_contacts_add`
- `dialer_contacts_update`
- `dialer_contacts_query`
- `dialer_open_contacts_editor`
- `dialer_contacts_set_starred`
- `dialer_contacts_copy_number`
- `dialer_open_my_business_card`
- `dialer_check_last_caller`
- `dialer_query_calllog`
- `dialer_delete_calllog`
- `dialer_query_calllog_of_contact`
- `dialer_query_calllog_by_type`
- `dialer_query_calllog_by_number`
- `dialer_query_call_duration_ranking`
- `dialer_show_dialpad`
- `dialer_open_call_settings`
- `dialer_open_sim_card_settings`
- `dialer_open_medical_emergency_information`
- `dialer_settings_enable_power_button_ends_call`
- `dialer_settings_enable_incoming_flip_silent`
- `dialer_settings_vibrate_when_mocall_connected`
- `dialer_settings_open_auto_record_screen`
- `dialer_settings_allow_viewing_medical_information`
- `dialer_settings_sos_auto_call`
- `dialer_add_group`
- `dialer_add_number_to_blocked_list`
- `dialer_query_blocked_number`
- `dialer_delete_number_from_blocked_list`

</details>
