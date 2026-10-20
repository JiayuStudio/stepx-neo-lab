# 设置：设备状态小卡

[新版课程](../README.md) · [连接器总表](../connector-index.md) · [官方详情](https://studio.stepx.com/connectors/ic_atomic_464)

阶段 3。当前能力表于 2026-10-20 复核；本课练习尚未执行。历史截图保留在[旧版单课](../../archive/official-step-studio-2026-10-20-early/connectors/ic_atomic_464.md)，仅用于对照。

## 小任务

只查询电量和深色模式状态，暂不修改设置。先做一项可核对的小功能，再扩展页面。

## 从这些能力开始

- `settings_get_current_battery_level`
- `settings_get_dark_mode_state`

## 操作顺序

1. 阅读官方详情，确认接口用途与当前参数；加入课堂项目。
2. 先给输入、执行按钮、结果和失败状态留出位置，可搭配移动端卡片和工具栏按钮。
3. 让 Studio 读取实际 schema，不猜参数；先做一条调用，不一次接完所有方法。
4. 在支持的环境执行，核对结果；有实际设备行为时观察手机而非只看提示文字。
5. 复测取消、拒绝权限和失败，结束后恢复本轮改动的设备状态。

## 可复制提示词

```text
在课堂项目使用官方「设置」连接器，完成「设备状态小卡」。
目标：只查询电量和深色模式状态，暂不修改设置。先做一项可核对的小功能，再扩展页面。
先读取上述实际能力、input_schema 和返回格式，说明权限与运行环境。
使用当前官方组件展示输入、执行中、结果与错误，失败时允许重试。
不要自行发送公开内容、支付或操作无关文件；需要确认的动作在界面中提供明确预览。
缺少设备支持时显示不支持，不用示例数据冒充成功。完成后列出仍未测试的项目。
```

## 注意与验收

已补齐旧索引漏记的双击亮屏与抬起亮屏：各有开关和状态查询。这四项在旧网页存档中已存在，不作为本次新增。首次练习仍以只读查询为主。

记录输入、预期、实际结果、环境和结论；核对输出确实来自调用，有写操作时再核对真实效果。未运行时保留“待测”。

<details>
<summary>当前官方能力名称</summary>

- `settings_open_settings_homepage_activity`
- `settings_open_settings_locale_picker_activity`
- `settings_open_settings_device_info_settings_activity`
- `settings_open_manage_applications`
- `settings_open_settings_storage_dashboard_activity`
- `settings_open_settings_sound_settings_activity`
- `settings_battery_percentage_switch`
- `settings_wifi_switch`
- `settings_bluetooth_switch`
- `settings_hotspot_switch`
- `settings_airplane_mode_switch`
- `settings_data_switch`
- `settings_vibration_switch`
- `settings_dark_mode_switch`
- `settings_disturb_mode_switch`
- `settings_adjust_sound`
- `settings_adjust_brightness`
- `settings_dialing_keypad_sound_switch`
- `settings_screen_lock_sound_switch`
- `settings_charging_notif_sound_switch`
- `settings_touch_sound_switch`
- `settings_auto_brightness_switch`
- `settings_auto_rotation_switch`
- `settings_one_handed_mode_switch`
- `settings_tapToCheck_phone_switch`
- `settings_liftToCheck_phone_switch`
- `settings_battery_saver_switch`
- `settings_choose_wifi`
- `settings_forget_wifi`
- `settings_set_screen_off_time`
- `settings_set_media_sound`
- `settings_get_battery_percentage_state`
- `settings_get_wifi_state`
- `settings_get_bluetooth_state`
- `settings_get_hotspot_state`
- `settings_get_airplane_mode_state`
- `settings_get_data_state`
- `settings_get_vibration_state`
- `settings_get_dark_mode_state`
- `settings_get_dialing_keypad_sound_state`
- `settings_get_screen_lock_sound_state`
- `settings_get_charging_notif_sound_state`
- `settings_get_touch_sound_state`
- `settings_get_auto_brightness_state`
- `settings_get_auto_rotation_state`
- `settings_get_one_handed_mode_state`
- `settings_get_tapToCheck_phone_state`
- `settings_get_liftToCheck_phone_state`
- `settings_get_battery_saver_state`
- `settings_get_app_lock_state`
- `settings_adjust_font_size`
- `settings_eye_protection_mode_switch`
- `settings_get_current_battery_level`

</details>
