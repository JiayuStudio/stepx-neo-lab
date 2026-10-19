# 闹钟、日程与提醒

[手册目录](README.md) · [资料库](../README.md) · [一句话清单](../quick-start.md) · [实测结果](../test-results.md)

原稿更新：2026-10-20。证据标签表示原资料的记录，本次未重新实测。人名、联系方式和地点参数使用示例或占位符。

> 原稿“全部未实测”的总述已被后续逐项记录更新：闹钟、倒计时和日程有成功记录，待办能力在测试环境中不存在。提醒参数仍需在日历或时钟中核对。

<a id="prompt-10-01"></a>

### 新建闹钟

> 设一个标签「Amoo测试闹钟」的闹钟

**原文证据**：●实测(2026-10-20)

**预期效果**：创建成功并返回 ID

<details>
<summary>能力名称与来源</summary>

```text
alarm_clock_add
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-02"></a>

### 倒计时

> 倒计时 15 秒

**原文证据**：●实测(2026-10-20)

**预期效果**：创建成功

<details>
<summary>能力名称与来源</summary>

```text
alarm_clock_timer_add
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-03"></a>

### 查闹钟

> 我现在开着哪些闹钟

**原文证据**：●实测(2026-10-20)

**预期效果**：返回已启用闹钟

<details>
<summary>能力名称与来源</summary>

```text
alarm_clock_get_enable
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-04"></a>

### 稍后提醒

> 闹钟响了,再睡 10 分钟

**原文证据**：◆(明细表)

**预期效果**：按配置时长推迟正在响铃的闹钟(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
alarm_clock_snooze
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-05"></a>

### 删除某个闹钟

> 删掉指定测试闹钟

**原文证据**：●实测(2026-10-20)

**预期效果**：按时间删除单条成功

<details>
<summary>能力名称与来源</summary>

```text
alarm_clock_delete
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-06"></a>

### 删除全部闹钟(不可逆)

> 删除全部闹钟(慎用)

**原文证据**：◆(明细表)

**预期效果**：永久清除全部闹钟,不可撤销(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
alarm_clock_delete_all
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-07"></a>

### 新建日程

> 添加测试日程并稍后删除

**原文证据**：●实测(2026-10-20)

**预期效果**：创建成功

<details>
<summary>能力名称与来源</summary>

```text
calendar_add_event
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-08"></a>

### 查日程

> 查询某天日程

**原文证据**：●实测(2026-10-20)

**预期效果**：可查询(删后回读 0 条)

<details>
<summary>能力名称与来源</summary>

```text
calendar_query_event
```

来源：原手册第 11 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-09"></a>

### 生日 / 纪念日 / 倒数日

> 添加一个重要日：[日期]是[测试联系人]生日，每年提醒

**原文证据**：◆(明细表)

**预期效果**：新增生日、纪念日、倒数日(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
calendar_add_special_event
```

来源：原手册第 12 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-10"></a>

### 打开新建日程页

> 打开日历的新建日程页面

**原文证据**：◆(明细表)

**预期效果**：只跳转页面,不代填内容(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
calendar_open_new_edit_event_activity
```

来源：原手册第 12 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-11"></a>

### 待办

> 添加一条待办

**原文证据**：❌ 2026-10-20

**预期效果**：能力不存在

<details>
<summary>能力名称与来源</summary>

```text
memo_create_todo / phone_note_todo_add
```

来源：原手册第 12 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-12"></a>

### 记笔记

> 记一条笔记:车停在B2 层 C 区

**原文证据**：◆(工具映射)

**预期效果**：添加笔记(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
phone_note_add
```

来源：原手册第 12 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-13"></a>

### 其他城市时间

> 纽约现在几点

**原文证据**：●实测(2026-10-20)

**预期效果**：返回当地时间

<details>
<summary>能力名称与来源</summary>

```text
alarm_clock_get_current_time_by_city
```

来源：原手册第 12 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-10-14"></a>

### 定时 / 周期任务

> 每天早上 8 点告诉我今天的天气

**原文证据**：○

**预期效果**：能力清单称支持"定时执行 /触发执行"(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
Amoo 定时任务机制(未查到具体method)
```

来源：原手册第 12 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

[返回手册目录](README.md) · [查看实测结果](../test-results.md)
