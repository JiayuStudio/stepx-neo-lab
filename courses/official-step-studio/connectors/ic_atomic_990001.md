# 背面点阵屏：背屏状态面板

[新版课程](../README.md) · [连接器总表](../connector-index.md) · [官方详情](https://studio.stepx.com/connectors/ic_atomic_990001)

阶段 7。当前能力表于 2026-10-20 复核；本课练习尚未执行。历史截图保留在[旧版单课](../../archive/official-step-studio-2026-10-20-early/connectors/ic_atomic_990001.md)，仅用于对照。

## 小任务

先读显示信息与亮度；进阶绘制一个简单图案并提供清空。先做一项可核对的小功能，再扩展页面。

## 从这些能力开始

- `get_back_screen_display_info`
- `get_back_screen_brightness`
- `set_back_screen_frame`
- `clear_back_screen_frame`

## 操作顺序

1. 阅读官方详情，确认接口用途与当前参数；加入课堂项目。
2. 先给输入、执行按钮、结果和失败状态留出位置，可搭配移动端卡片和工具栏按钮。
3. 让 Studio 读取实际 schema，不猜参数；先做一条调用，不一次接完所有方法。
4. 在支持的环境执行，核对结果；有实际设备行为时观察手机而非只看提示文字。
5. 复测取消、拒绝权限和失败，结束后恢复本轮改动的设备状态。

## 可复制提示词

```text
在课堂项目使用官方「背面点阵屏」连接器，完成「背屏状态面板」。
目标：先读显示信息与亮度；进阶绘制一个简单图案并提供清空。先做一项可核对的小功能，再扩展页面。
先读取上述实际能力、input_schema 和返回格式，说明权限与运行环境。
使用当前官方组件展示输入、执行中、结果与错误，失败时允许重试。
不要自行发送公开内容、支付或操作无关文件；需要确认的动作在界面中提供明确预览。
缺少设备支持时显示不支持，不用示例数据冒充成功。完成后列出仍未测试的项目。
```

## 注意与验收

当前为 17 项，取消倒计时不再列出。概览说版本 10012 仅支持 frame，能力表仍写可传 text，二者冲突；优先核对所选接口 schema，本课仅用 391 个 0–255 整数，按 17 列 × 23 行绘制，不宣称 text 可用。

记录输入、预期、实际结果、环境和结论；核对输出确实来自调用，有写操作时再核对真实效果。未运行时保留“待测”。

<details>
<summary>当前官方能力名称</summary>

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
