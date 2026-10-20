# 图库：课堂图片浏览器

[新版课程](../README.md) · [连接器总表](../connector-index.md) · [官方详情](https://studio.stepx.com/connectors/ic_atomic_446)

阶段 6。当前能力表于 2026-10-20 复核；本课练习尚未执行。历史截图保留在[旧版单课](../../archive/official-step-studio-2026-10-20-early/connectors/ic_atomic_446.md)，仅用于对照。

## 小任务

使用事先准备的无个人内容测试图片，先浏览再做编辑练习。先做一项可核对的小功能，再扩展页面。

## 从这些能力开始

- `gallery_select_media_file_card`
- `gallery_open_picture_editor`

## 操作顺序

1. 阅读官方详情，确认接口用途与当前参数；加入课堂项目。
2. 先给输入、执行按钮、结果和失败状态留出位置，可搭配移动端卡片和工具栏按钮。
3. 让 Studio 读取实际 schema，不猜参数；先做一条调用，不一次接完所有方法。
4. 在支持的环境执行，核对结果；有实际设备行为时观察手机而非只看提示文字。
5. 复测取消、拒绝权限和失败，结束后恢复本轮改动的设备状态。

## 可复制提示词

```text
在课堂项目使用官方「图库」连接器，完成「课堂图片浏览器」。
目标：使用事先准备的无个人内容测试图片，先浏览再做编辑练习。先做一项可核对的小功能，再扩展页面。
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

- `gallery_get_latest_media`
- `gallery_get_media_by_time_range`
- `gallery_open_picture_editor`
- `gallery_ai_extend_image`
- `gallery_add_picture_text`
- `gallery_add_picture_watermark`
- `gallery_add_picture_mosaic`
- `gallery_edit_picture_paint`
- `gallery_add_picture_filter`
- `gallery_reduce_picture_resolution`
- `gallery_edit_picture_sharpen`
- `gallery_edit_picture_temperature`
- `gallery_edit_picture_saturation`
- `gallery_edit_picture_brightness`
- `gallery_edit_picture_aspect_ratio`
- `gallery_edit_picture_degree`
- `gallery_edit_picture_flip`
- `gallery_open_video_editor`
- `gallery_open_video_player`
- `gallery_intelligent_search`
- `gallery_local_search`
- `gallery_create_album`
- `gallery_rename_album`
- `gallery_copy_files_to_album`
- `gallery_move_media_to_album`
- `gallery_hide_album`
- `gallery_set_album_cover`
- `gallery_open_trash_bin`
- `gallery_open_hidden_album`
- `gallery_select_media_file_card`
- `gallery_get_sticker_columns`
- `gallery_get_stickers`
- `gallery_get_local_stickers`
- `gallery_get_style_templates`
- `gallery_get_photo_info`
- `gallery_get_video_info`
- `gallery_copy_media`
- `gallery_move_to_folder`
- `gallery_restore_media_from_trash`
- `gallery_get_trash_media_list`
- `gallery_hide_media`
- `gallery_unhide_media`
- `gallery_share_media`
- `gallery_favorite_media`
- `gallery_rename_media`
- `gallery_compress_media`
- `gallery_ai_erase_image`
- `gallery_ai_style_image`

</details>
