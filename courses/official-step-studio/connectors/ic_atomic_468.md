# 文件管理：课堂文件整理器

[新版课程](../README.md) · [连接器总表](../connector-index.md) · [官方详情](https://studio.stepx.com/connectors/ic_atomic_468)

阶段 6。当前能力表于 2026-10-20 复核；本课练习尚未执行。历史截图保留在[旧版单课](../../archive/official-step-studio-2026-10-20-early/connectors/ic_atomic_468.md)，仅用于对照。

## 小任务

只在用户选定的课堂目录查找测试文件并展示名称与大小。先做一项可核对的小功能，再扩展页面。

## 从这些能力开始

- `filemanager_search_file`
- `filemanager_get_file_size`

## 操作顺序

1. 阅读官方详情，确认接口用途与当前参数；加入课堂项目。
2. 先给输入、执行按钮、结果和失败状态留出位置，可搭配移动端卡片和工具栏按钮。
3. 让 Studio 读取实际 schema，不猜参数；先做一条调用，不一次接完所有方法。
4. 在支持的环境执行，核对结果；有实际设备行为时观察手机而非只看提示文字。
5. 复测取消、拒绝权限和失败，结束后恢复本轮改动的设备状态。

## 可复制提示词

```text
在课堂项目使用官方「文件管理」连接器，完成「课堂文件整理器」。
目标：只在用户选定的课堂目录查找测试文件并展示名称与大小。先做一项可核对的小功能，再扩展页面。
先读取上述实际能力、input_schema 和返回格式，说明权限与运行环境。
使用当前官方组件展示输入、执行中、结果与错误，失败时允许重试。
不要自行发送公开内容、支付或操作无关文件；需要确认的动作在界面中提供明确预览。
缺少设备支持时显示不支持，不用示例数据冒充成功。完成后列出仍未测试的项目。
```

## 注意与验收

先在目标环境核验参数和权限；当前能力表存在不代表本次已执行。

记录输入、预期、实际结果、环境和结论；核对输出确实来自调用，有写操作时再核对真实效果。未运行时保留“待测”。

<details>
<summary>当前官方能力名称</summary>

- `filemanager_get_file_path`
- `filemanager_get_file_size`
- `filemanager_get_file_create_time`
- `filemanager_get_file_update_time`
- `filemanager_get_file_detail`
- `filemanager_create_folder`
- `filemanager_create_file`
- `filemanager_search_file`
- `filemanager_get_apk_file_list`
- `filemanager_get_compress_file_list`
- `filemanager_get_document_file_list`
- `filemanager_get_voice_file_list`
- `filemanager_get_video_file_list`
- `filemanager_get_download_file_list`
- `filemanager_get_weixin_file_list`
- `filemanager_get_browser_file_list`
- `filemanager_get_bluetooth_file_list`
- `filemanager_get_ringtone_file_list`
- `filemanager_get_screenshot_file_list`
- `filemanager_get_screen_recording_file_list`
- `filemanager_move_file`
- `filemanager_get_garbage_file_list`
- `filemanager_edit_folder_name`
- `filemanager_edit_file_name`

</details>
