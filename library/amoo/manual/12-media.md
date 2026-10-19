# 音乐、媒体与相册

[手册目录](README.md) · [资料库](../README.md) · [一句话清单](../quick-start.md) · [实测结果](../test-results.md)

原稿更新：2026-10-20。证据标签表示原资料的记录，本次未重新实测。人名、联系方式和地点参数使用示例或占位符。

<a id="prompt-12-01"></a>

### 按心情/场景放歌

> 放几首适合跑步的歌

**原文证据**：◆(检索命中 +明细表)

**预期效果**：按歌名、歌手、心情、场景推荐并可播放(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
music_recommend (咪咕音乐)
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-12-02"></a>

### 点名播放

> 用咪咕音乐播放周杰伦的《晴天》

**原文证据**：◆(明细表)

**预期效果**：同上(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
music_recommend
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-12-03"></a>

### 播放音乐组件

> 播放音乐

**原文证据**：◆(固件字符串 +工具映射)

**预期效果**：播放音乐组件(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
common_ability_play_music
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-12-04"></a>

### 用其他音乐App

> 打开 QQ 音乐

**原文证据**：●(能力已实测;该目标 App未单测)

**预期效果**：只能打开;QQ 音乐、网易云类App 在对照表中多为"未接入",App 内点歌做不到

<details>
<summary>能力名称与来源</summary>

```text
open_app_by_name
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-12-05"></a>

### 停止朗读

> 别念了 / 停止播报

**原文证据**：◆(明细表)

**预期效果**：结束当前朗读(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
stop_speak_text
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-12-06"></a>

### 语义搜照片

> 找一下去年夏天在海边拍的照片

**原文证据**：◆(明细表)

**预期效果**：自然语言语义搜图与视频(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
gallery_intelligent_search
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-12-07"></a>

### 条件搜照片

> 找上个月在杭州拍的、有文字的照片

**原文证据**：◆(明细表)

**预期效果**：按时间、地点、人物、相册、OCR组合检索(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
gallery_local_search
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-12-08"></a>

### 播放视频

> 用相册播放刚才找到的那个视频

**原文证据**：◆(明细表)

**预期效果**：用图库播放器播放(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
gallery_open_video_player
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-12-09"></a>

### 分享照片

> 把这张照片分享出去

**原文证据**：◆(明细表)

**预期效果**：调起系统分享面板,选目标需你操作(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
gallery_share_media
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-12-10"></a>

### 录音

> 打开快记开始录音

**原文证据**：◆(明细表)

**预期效果**：进入录音功能,缺权限先引导授权(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
lite_recorder_open_recording
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-12-11"></a>

### 截屏文件

> 列出我最近的截图文件

**原文证据**：◆(检索命中 +明细表)

**预期效果**：获取截屏文件清单(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
filemanager_get_screenshot_file_list
```

来源：原手册第 13 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

[返回手册目录](README.md) · [查看实测结果](../test-results.md)
