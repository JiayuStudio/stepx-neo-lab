# 系统设置

[手册目录](README.md) · [资料库](../README.md) · [一句话清单](../quick-start.md) · [实测结果](../test-results.md)

原稿更新：2026-10-20。证据标签表示原资料的记录，本次未重新实测。人名、联系方式和地点参数使用示例或占位符。

> 10 月 20 日更新包含 WiFi、勿扰、省电、手电筒、截录屏等结果。飞行模式、蜂窝数据和热点写入仍未测。深色模式仅有查询及条件分支记录，不能当作切换成功。

<a id="prompt-09-01"></a>

### 查蓝牙状态

> 蓝牙现在是开着还是关着

**原文证据**：●实测

**预期效果**：返回当前蓝牙状态

<details>
<summary>能力名称与来源</summary>

```text
settings_get_bluetooth_state
```

来源：原手册第 9 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-02"></a>

### 开 /关蓝牙

> 把蓝牙关掉 /把蓝牙打开

**原文证据**：●实测(关→查→开→查,已恢复)

**预期效果**：直接开关蓝牙;实测未弹额外确认

<details>
<summary>能力名称与来源</summary>

```text
settings_bluetooth_switch (参数 openState)
```

来源：原手册第 9 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-03"></a>

### 查飞行模式

> 现在是不是飞行模式?只查不改

**原文证据**：●实测(2026-10-18 前轮,与ADB 独立读取一致)

**预期效果**：返回开关状态

<details>
<summary>能力名称与来源</summary>

```text
settings_get_airplane_mode_state
```

来源：原手册第 9 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-04"></a>

### 开 /关飞行模式

> 打开飞行模式

**原文证据**：◆(明细表;Amoo 自述"能")

**预期效果**：开关飞行模式;会断网,Amoo 之后可能无法响应(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
settings_airplane_mode_switch
```

来源：原手册第 9 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-05"></a>

### 查WiFi状态

> WiFi 现在开着吗

**原文证据**：●实测(2026-10-20)

**预期效果**：返回开关状态

<details>
<summary>能力名称与来源</summary>

```text
settings_get_wifi_state
```

来源：原手册第 9 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-06"></a>

### 开 /关WiFi

> 把 WiFi 关了/ 打开

**原文证据**：●实测(2026-10-20,已还原)

**预期效果**：可逆往返成功;常无二次确认

<details>
<summary>能力名称与来源</summary>

```text
settings_wifi_switch
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-07"></a>

### 切换已保存WiFi

> 切换到"家里的WiFi"

**原文证据**：◆(明细表)

**预期效果**：在已保存网络间切换(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
settings_choose_wifi
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-08"></a>

### 蜂窝数据 /热点

> 关闭移动数据;打开个人热点

**原文证据**：◆(工具映射)

**预期效果**：开关数据或热点(未实测,以真机返回为准;可能断网)

<details>
<summary>能力名称与来源</summary>

```text
settings_data_switch / settings_hotspot_switch
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-09"></a>

### 调亮度

> 屏幕调暗一点

**原文证据**：●/⚠ 2026-10-20

**预期效果**：相对调节可用;无读值接口时会拒绝「记下再还原」盲改

<details>
<summary>能力名称与来源</summary>

```text
settings_adjust_brightness
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-10"></a>

### 自动亮度

> 打开自动亮度

**原文证据**：◆(明细表)

**预期效果**：开关自动亮度(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
settings_auto_brightness_switch
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-11"></a>

### 深色模式

> 打开深色模式

**原文证据**：●实测(2026-10-20)

**预期效果**：可查询;条件打开逻辑正确

<details>
<summary>能力名称与来源</summary>

```text
settings_dark_mode_switch / settings_get_dark_mode_state
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

**核对说明**：对照实测 38-dark：只出现状态查询，条件未触发，不能据此认定深色模式切换已经验证。

<a id="prompt-09-12"></a>

### 勿扰模式

> 打开/关闭勿扰模式

**原文证据**：●实测(2026-10-20)

**预期效果**：开关可用;常无二次确认;缺独立读状态

<details>
<summary>能力名称与来源</summary>

```text
settings_disturb_mode_switch
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-13"></a>

### 音量 /铃声设置

> 打开声音设置页

**原文证据**：●实测(2026-10-20)

**预期效果**：打开声音设置页

<details>
<summary>能力名称与来源</summary>

```text
settings_open_settings_sound_settings_activity
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-14"></a>

### 系统振动

> 关闭系统振动

**原文证据**：◆(明细表)

**预期效果**：开关系统振动(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
settings_vibration_switch
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-15"></a>

### 省电模式

> 打开/关闭省电模式

**原文证据**：●实测(2026-10-20)

**预期效果**：完整开→回读→关→回读

<details>
<summary>能力名称与来源</summary>

```text
settings_battery_saver_switch + 状态查询
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-16"></a>

### 手电筒

> 打开手电筒两秒后关掉

**原文证据**：●实测(2026-10-20)

**预期效果**：开→关成功

<details>
<summary>能力名称与来源</summary>

```text
open_flash_light
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-17"></a>

### 截屏 /长截屏

> 截个屏

**原文证据**：●实测(2026-10-20)

**预期效果**：截图已触发

<details>
<summary>能力名称与来源</summary>

```text
screen_capture_screenshot
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-18"></a>

### 录屏

> 开始录屏约3秒后停止

**原文证据**：●实测(2026-10-20)

**预期效果**：开停均可

<details>
<summary>能力名称与来源</summary>

```text
screen_capture_record / screen_capture_stop_record
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-19"></a>

### 清理加速

> 帮我清理一下后台

**原文证据**：⚠ 2026-10-20

**预期效果**：已触发;回执偶发异常提示

<details>
<summary>能力名称与来源</summary>

```text
security_center_acceleration_execute
```

来源：原手册第 10 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-20"></a>

### 关于本机 /存储

> 打开"关于本机"页面;看看存储空间还剩多少

**原文证据**：◆(明细表 / 检索命中)

**预期效果**：打开对应设置页(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
settings_open_settings_device_info_settings_activity /settings_open_settings_storage_dashboard_activity
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-21"></a>

### 打开设置主页

> 打开设置

**原文证据**：●实测(2026-10-20)

**预期效果**：打开系统设置;之后滑动点选仍做不到

<details>
<summary>能力名称与来源</summary>

```text
settings_open_settings_homepage_activity
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-09-22"></a>

### 骚扰拦截

> 打开骚扰拦截设置

**原文证据**：◆(检索命中)

**预期效果**：进入骚扰拦截设置页(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
security_manager_open_harassment_interception
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

[返回手册目录](README.md) · [查看实测结果](../test-results.md)
