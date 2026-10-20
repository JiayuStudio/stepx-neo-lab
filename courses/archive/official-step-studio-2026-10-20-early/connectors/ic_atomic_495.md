# 网络设置：网络设置导航

> **已归档：2026-10-20 早期版。** 保留当时的内容与验证记录，接口和界面可能已变化。[阅读新版课程](../../../official-step-studio/README.md)。

[课程首页](../README.md) · [连接器总表](../connector-index.md) · [添加连接器图解](../02-connectors.md)

阶段 3 · 系统状态与设置入口。本课不要求自行申请第三方 API 凭证；手机系统权限仍按实际功能提示处理。

## 这一课做什么

分别提供 Wi-Fi、蓝牙、VPN 设置入口。先做一项可核对的小功能，再扩展页面。

## 在官方页面找到它

打开[官方「网络设置」详情](https://studio.stepx.com/connectors/ic_atomic_495)，先看概览和能力说明，再点右上角 **添加到项目**，选择课堂项目；没有项目时选择 **创建项目**。

![官方网络设置详情页，右上角为添加到项目](../images/ic_atomic_495.png)

图中是 2026-10-20 官方网页的教学区域截图。能力名称在下方的折叠区可查；参数结构应以项目实际拿到的最新说明为准。

## 动手顺序

1. 用自己的话写清楚输入是什么、按哪个按钮、结果显示在哪里。
2. 将「网络设置」加入课堂项目；若出现带连接器卡片的消息草稿，保留卡片，补充下面的需求后发送。
3. 先检查界面状态与错误提示，再到支持该能力的环境执行。
4. 按本页验收条件记录结果；有设备行为时，观察手机上的实际效果。

## 本课先用哪些能力

- `connectivitysettings_open_settings_wifi_settings_activity`
- `connectivitysettings_open_settings_bluetooth_settings_activity`
- `connectivitysettings_open_settings_vpn_settings_activity`

先围绕这些方法完成上面的最小作品。加入项目后，让 Studio 读取方法的参数、必填字段和返回结构，再把页面输入逐项对应过去；不要直接把自然语言当成参数 JSON。

## 可复制提示词

```text
在当前官方 Step Studio 课堂项目中增加「网络设置导航」，使用官方「网络设置」连接器。
目标：分别提供 Wi-Fi、蓝牙、VPN 设置入口。
先读取已添加连接器的实际能力与参数说明，只使用官方存在的接口；缺少信息时指出缺口，不自行猜测方法或字段。
界面只保留本功能需要的输入、执行按钮、状态和结果。区分未执行、执行中、成功、失败；执行中防止重复点击，失败时保留输入并允许重试。
验收：点击按钮后应进入正确的系统页，返回应用后按钮恢复。
范围：打开页面不是完成连接，不自动修改网络。
如果当前预览环境不支持该能力，显示明确的环境提示，不用模拟值冒充设备返回结果。
完成后列出改动位置、检查步骤和仍需在手机验证的项目。
```

## 怎么判断完成

点击按钮后应进入正确的系统页，返回应用后按钮恢复。

填写一行练习记录：测试环境、输入、预期、实际结果、是否通过。能看到界面不等于调用成功；调用回执也不一定代表最终设备行为已经完成。

## 常见卡点与下一步

打开页面不是完成连接，不自动修改网络。

遇到失败，把「输入、点击动作、实际提示、预期结果」交给 Studio 定向修改；不要直接要求重做整个应用。完成后返回[课程路线](../README.md)选择同阶段下一课。

<details>
<summary>本页官方列出的能力名称（检索用）</summary>

- `connectivitysettings_open_settings_wifi_settings_activity`
- `connectivitysettings_open_settings_advanced_wifi_settings_activity`
- `connectivitysettings_open_settings_wifi_info_settings_activity`
- `connectivitysettings_open_settings_wifi_p_2_p_settings_activity`
- `connectivitysettings_open_settings_saved_access_points_wifi_settings`
- `connectivitysettings_open_settings_wifi_display_settings_activity`
- `connectivitysettings_open_settings_tether_settings_activity`
- `connectivitysettings_open_settings_bluetooth_settings_activity`
- `connectivitysettings_open_settings_vpn_settings_activity`
- `connectivitysettings_open_settings_mobile_network_settings_activity`

这些名称来自官方详情页，并非本课程逐项执行通过的清单。

</details>

本页验证范围：已核对官方详情与截图；练习提示词为研究所编写，未逐课在 Neo 上执行。
