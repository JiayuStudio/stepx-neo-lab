# Amoo 真机实测结果（资料整理）

[资料库](README.md) · [一句话清单](quick-start.md) · [提示词手册](manual/README.md)

原报告日期：2026-10-20（北京时间）。来源为提供的 6 页实测 PDF；本次未重新操作手机，也未取得原始日志进行独立复核。下文的成功、失败、部分可用均是**原报告记载**。

## 阅读结果

结果和能力名称分开记录：`—` 表示源表没有记录 method，不能由回复内容反推为已调用。原 PDF 有窄列截断，保留能确定的结论并注明缺口；地点、联系人、设备状态及个人内容改为占位或不收录。

## 统计

源表共有 **65 条记录：56 条单项 + 9 条串联或收尾**。其中 ✅ 55、❌ 5、⚠ 4、⏭ 1；此外另列 12 类未执行任务。源表中的“成功”可能只表示打开页面、返回查询或确认分支，不等于完整业务成功。

## 按类型查阅

- [应用与浏览器](#apps)
- [通讯与查询](#queries)
- [系统设置](#settings)
- [闹钟与日程](#schedule)
- [串联任务](#compound)
- [未执行任务](#not-run)

## 单项结果

<a id="apps"></a>

## 应用与浏览器

<a id="test-01-open-ctrip"></a>

### 01-open-ctrip · ✅

> 打开携程

目标应用已打开。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-02-open-jd"></a>

### 02-open-jd · ✅

> 打开京东

目标应用已打开。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-03-open-taobao"></a>

### 03-open-taobao · ✅

> 打开淘宝

目标应用已打开。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-04-open-pdd"></a>

### 04-open-pdd · ❌

> 打开拼多多

因目标应用未安装而失败，不表示所有环境均不支持。

<details>
<summary>触发 method 与来源</summary>

```text
common_ability_get_app_list, open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-05-open-amap"></a>

### 05-open-amap · ✅

> 打开高德地图

目标应用已打开。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-06-open-qqmusic"></a>

### 06-open-qqmusic · ✅

> 打开QQ音乐

目标应用已打开；未验证 App 内点歌。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-07-open-migu"></a>

### 07-open-migu · ❌

> 打开咪咕音乐

因目标应用未安装而失败；原报告另记录 MCP 10001。

<details>
<summary>触发 method 与来源</summary>

```text
check_app_installed_by_name, open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-08-open-meituan"></a>

### 08-open-meituan · ✅

> 打开美团

目标应用已打开。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-09-open-alipay"></a>

### 09-open-alipay · ✅

> 打开支付宝

目标应用已打开。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-10-open-wechat"></a>

### 10-open-wechat · ✅

> 打开微信

目标应用已打开。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-11-open-settings"></a>

### 11-open-settings · ✅

> 打开设置

设置页已打开；不代表后续界面操作完成。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_package,settings_open_settings_homepage_activity
```

原报告第 1 页。

</details>

<a id="test-12-open-gallery"></a>

### 12-open-gallery · ✅

> 打开相册

相册已打开；未公开相册内容。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_name
```

原报告第 1 页。

</details>

<a id="test-13-check-ctrip"></a>

### 13-check-ctrip · ✅

> 看一下手机里有没有装携程

安装状态查询成功。

<details>
<summary>触发 method 与来源</summary>

```text
check_app_installed_by_name
```

原报告第 1 页。

</details>

<a id="test-14-app-count"></a>

### 14-app-count · ✅

> 我手机上装了多少个应用

应用计数查询成功；个人安装数量不收录。

<details>
<summary>触发 method 与来源</summary>

```text
common_ability_get_app_count
```

原报告第 2 页。

</details>

<a id="test-15-app-list"></a>

### 15-app-list · ✅

> 列出手机上装了哪些第三方应用,只报数量和前10个名字

应用列表查询成功；个人安装清单不收录。

<details>
<summary>触发 method 与来源</summary>

```text
common_ability_get_app_list
```

原报告第 2 页。

</details>

<a id="test-16-browser"></a>

### 16-browser · ✅

> 用浏览器打开https://www.stepfun.com

官网链接已在浏览器打开。

<details>
<summary>触发 method 与来源</summary>

```text
open_url_with_browser
```

原报告第 2 页。

</details>

<a id="queries"></a>

## 通讯、信息与出行查询

<a id="test-17-sms-keyword"></a>

### 17-sms-keyword · ✅

> 检索包含[测试关键词]的短信，最多 5 条，只回报条数和是否命中，不返回号码与正文。

按关键词检索成功；不公开短信内容及个人命中数量。原 PDF 备注尾部有截断。

<details>
<summary>触发 method 与来源</summary>

```text
sms_search
```

原报告第 2 页。

</details>

<a id="test-18-sms-date"></a>

### 18-sms-date · ✅

> 查询最近 3 天短信，最多 5 条，只回报条数和日期字段名，不返回号码与正文。

按日期和数量上限检索成功；原报告提到日期字段名为“时间”。

<details>
<summary>触发 method 与来源</summary>

```text
sms_search
```

原报告第 2 页。

</details>

<a id="test-19-contacts"></a>

### 19-contacts · ✅

> 查询测试联系人，只回报匹配数量。

首测停在风险声明；源表标为成功，但联系人查询的完成证据见 19b。

<details>
<summary>触发 method 与来源</summary>

```text
assistant_show_risk_statement
```

原报告第 2 页。

</details>

<a id="test-19b-contacts"></a>

### 19b-contacts · ✅

> 查询测试联系人，只回报匹配数量。

联系人查询能力可用；不公开联系人资料。

<details>
<summary>触发 method 与来源</summary>

```text
dialer_contacts_query
```

原报告第 2 页。

</details>

<a id="test-20-balance"></a>

### 20-balance · ❌

> 帮我查一下话费余额和剩余流量,只读查询。

只读路径未取得余额或流量；需要发送运营商查询短信，原测试未发送。

<details>
<summary>触发 method 与来源</summary>

```text
assistant_show_risk_statement,dialer_contacts_query
```

原报告第 2 页。

</details>

<a id="test-20b-balance"></a>

### 20b-balance · ❌

> 请分别查询话费余额和剩余流量,只读,把数字告诉我。

同一只读查询失败边界；未发送查询短信。

<details>
<summary>触发 method 与来源</summary>

```text
sms_search
```

原报告第 2 页。

</details>

<a id="test-21-weather-local"></a>

### 21-weather-local · ✅

> 我这里今天天气怎么样,会下雨吗?

原报告标为成功；备注中的方法名被 PDF 截断，完整方法名以“触发 method”栏为准。

<details>
<summary>触发 method 与来源</summary>

```text
weather_get_current_location_weather
```

原报告第 2 页。

</details>

<a id="test-22-weather-city"></a>

### 22-weather-city · ✅

> 杭州未来三天天气怎么样

返回指定城市天气；过滤日志未记录 method。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 2 页。

</details>

<a id="test-22b-weather-city"></a>

### 22b-weather-city · ✅

> 杭州未来三天天气怎么样?给出逐日概况。

逐日天气回复有效；表中没有记录 method。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 2 页。

</details>

<a id="test-23-location"></a>

### 23-location · ✅

> 查询当前位置，只回报定位是否成功。

定位返回成功；实际地址、坐标、精度及时间戳不收录。

<details>
<summary>触发 method 与来源</summary>

```text
get_current_location
```

原报告第 2 页。

</details>

<a id="test-24-world-time"></a>

### 24-world-time · ✅

> 纽约现在几点

城市时间查询成功；不收录当时的具体读值。

<details>
<summary>触发 method 与来源</summary>

```text
alarm_clock_get_current_time_by_city
```

原报告第 2 页。

</details>

<a id="test-25-stock"></a>

### 25-stock · ✅

> 看一下今天上证指数大概点位,只要描述不要投资建议。

返回指数点位描述，源表注明财联社检索。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 2 页。

</details>

<a id="test-26-websearch"></a>

### 26-websearch · ✅

> 上网搜一下 StepX Neo,只要前3条标题,不要打开下单。

返回资料标题列表。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 3 页。

</details>

<a id="test-27-route"></a>

### 27-route · ✅

> 从[起点]到[终点]开车大约多久？只查路线，不启动导航。

路线距离和耗时查询成功；没有启动导航。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 3 页。

</details>

<a id="test-28-nearby"></a>

### 28-nearby · ✅

> 我附近有什么咖啡店?只列名称,不要导航。

返回附近地点列表；备注尾部截断。

<details>
<summary>触发 method 与来源</summary>

```text
get_current_location
```

原报告第 3 页。

</details>

<a id="test-29-hotel"></a>

### 29-hotel · ✅

> 查询[目标区域]今晚的酒店，只要价格区间，不预订。

酒店查询返回候选；未预订。源表备注提及 query_hotel，但 method 栏为“—”。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 3 页。

</details>

<a id="test-30-flight"></a>

### 30-flight · ✅

> 查10月25日北京飞深圳的航班,列几班价格,不要订票。

返回航班列表；未订票。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 3 页。

</details>

<a id="test-31-train"></a>

### 31-train · ✅

> 查10月25日上海到杭州下午的高铁,列几班,不要订票。

返回高铁列表；未订票。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 3 页。

</details>

<a id="test-32-smzdm"></a>

### 32-smzdm · ✅

> 在什么值得买上查一下AirPods Pro 2 现在大概多少钱,只查不买。

商品比价有回复；表中没有记录 method。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 3 页。

</details>

<a id="test-33-didi-est"></a>

### 33-didi-est · ⚠

> 从当前位置去[目标地点]，只估价，不下单。

估价不完整，涉及账号授权；未实际叫车。原 PDF 备注末尾截断。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 3 页。

</details>

<a id="test-34-transit"></a>

### 34-transit · ⚠

> 打开支付宝乘车码,只展示页面,不要支付。

调起生活服务卡片；是否直达乘车码存疑。

<details>
<summary>触发 method 与来源</summary>

```text
ailauncher_load_tripartite_common_card,get_current_location,proxy_third_party_ability_alipay_life_service
```

原报告第 3 页。

</details>

<a id="settings"></a>

## 系统设置

<a id="test-35-wifi-get"></a>

### 35-wifi-get · ✅

> WiFi 现在开着吗?只查不改。

仅查询状态，没有修改；实际开关读值不收录。

<details>
<summary>触发 method 与来源</summary>

```text
ailauncher_load_tripartite_common_card,proxy_third_party_ability_alipay_life_service,settings_get_wifi_state
```

原报告第 3 页。

</details>

<a id="test-36-wifi-toggle"></a>

### 36-wifi-toggle · ✅

> 请执行可逆 PoC:先查WiFi 状态;若开着则关掉再立刻打开恢复;若关着则打开再立刻关掉恢复。每步回报。

原报告记载往返成功并恢复；不收录联网配置及初始开关值。备注末尾截断。

<details>
<summary>触发 method 与来源</summary>

```text
settings_get_data_state,settings_get_wifi_state, settings_wifi_switch
```

原报告第 3 页。

</details>

<a id="test-37-brightness"></a>

### 37-brightness · ⚠

> 请把亮度先记下来,再调到大约 40%,然后立刻还原到原来的亮度。逐步回报。

缺少亮度读值接口，拒绝盲目修改后恢复；本项不证明精确亮度设置可用。

<details>
<summary>触发 method 与来源</summary>

```text
settings_get_auto_brightness_state
```

原报告第 3 页。

</details>

<a id="test-38-dark"></a>

### 38-dark · ✅

> 如果现在不是深色模式就打开深色模式,等两秒再关回原来的模式。逐步回报。

只查询到状态，因条件不触发而没有切换；不能认定已测试深色模式开关。

<details>
<summary>触发 method 与来源</summary>

```text
settings_get_dark_mode_state
```

原报告第 3 页。

</details>

<a id="test-39-dnd"></a>

### 39-dnd · ✅

> 打开勿扰模式,确认后再立刻关闭勿扰,恢复原状。逐步回报。

原报告记录勿扰开关回执；缺少独立读状态接口，备注末尾截断。

<details>
<summary>触发 method 与来源</summary>

```text
settings_disturb_mode_switch
```

原报告第 3 页。

</details>

<a id="test-40-saver"></a>

### 40-saver · ✅

> 打开省电模式,确认后再立刻关闭省电模式恢复。逐步回报。

原报告记录开启与回读；本页备注末尾截断，完整往返结论另见手册对应条目。

<details>
<summary>触发 method 与来源</summary>

```text
settings_battery_saver_switch,settings_get_battery_saver_state
```

原报告第 4 页。

</details>

<a id="test-41-flashlight"></a>

### 41-flashlight · ✅

> 打开手电筒,等两秒后关掉手电筒。

手电筒开启后关闭。

<details>
<summary>触发 method 与来源</summary>

```text
open_flash_light
```

原报告第 4 页。

</details>

<a id="test-42-screenshot"></a>

### 42-screenshot · ✅

> 截个屏,保存即可。

截图能力已触发；本次未查看或发布实际截图。

<details>
<summary>触发 method 与来源</summary>

```text
screen_capture_screenshot
```

原报告第 4 页。

</details>

<a id="test-43-record"></a>

### 43-record · ✅

> 开始录屏,录大约3秒后停止录屏。

原报告记录录屏开始及停止；备注保存位置部分截断，不补猜。

<details>
<summary>触发 method 与来源</summary>

```text
screen_capture_record,screen_capture_stop_record
```

原报告第 4 页。

</details>

<a id="test-44-cleanup"></a>

### 44-cleanup · ⚠

> 帮我清理一下后台,释放内存。

清理能力已触发，但回执出现暂时无法响应。

<details>
<summary>触发 method 与来源</summary>

```text
security_center_acceleration_execute
```

原报告第 4 页。

</details>

<a id="test-45-sound"></a>

### 45-sound · ✅

> 打开声音设置页,只打开页面不要改音量。

声音设置页已打开；不证明音量数值可直接调整。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_package,settings_open_settings_sound_settings_activity
```

原报告第 4 页。

</details>

<a id="schedule"></a>

## 闹钟与日程

<a id="test-46-alarm-add"></a>

### 46-alarm-add · ✅

> 设置一个指定标签和测试时间的闹钟。

测试闹钟创建成功；记录 ID 与具体时间不收录，备注尾部截断。

<details>
<summary>触发 method 与来源</summary>

```text
alarm_clock_add
```

原报告第 4 页。

</details>

<a id="test-47-alarm-list"></a>

### 47-alarm-list · ✅

> 我现在开着哪些闹钟?列出标签。

已启用闹钟查询成功；具体数量和时间不收录。

<details>
<summary>触发 method 与来源</summary>

```text
alarm_clock_get_enable
```

原报告第 4 页。

</details>

<a id="test-48-alarm-del"></a>

### 48-alarm-del · ✅

> 只删除标签含「Amoo测试闹钟」的那一个闹钟,不要删除其他闹钟。

指定测试闹钟删除成功；备注末尾截断。

<details>
<summary>触发 method 与来源</summary>

```text
alarm_clock_delete, alarm_clock_get_enable
```

原报告第 4 页。

</details>

<a id="test-49-timer"></a>

### 49-timer · ✅

> 倒计时 15 秒,到时即可。

倒计时创建成功。

<details>
<summary>触发 method 与来源</summary>

```text
alarm_clock_timer_add
```

原报告第 4 页。

</details>

<a id="test-50-cal-add"></a>

### 50-cal-add · ✅

> 创建一条测试日程。

测试日程创建成功；具体日程字段不收录，备注尾部截断。

<details>
<summary>触发 method 与来源</summary>

```text
calendar_add_event
```

原报告第 4 页。

</details>

<a id="test-51-cal-del"></a>

### 51-cal-del · ✅

> 删除指定测试日程并查询确认。

指定测试日程删除并查询；备注末尾截断。

<details>
<summary>触发 method 与来源</summary>

```text
calendar_delete_event, calendar_query_event
```

原报告第 4 页。

</details>

<a id="test-52-todo-add"></a>

### 52-todo-add · ❌

> 添加一条待办:Amoo测试待办-周五前交。

返回待办能力不存在。

<details>
<summary>触发 method 与来源</summary>

```text
memo_create_todo
```

原报告第 4 页。

</details>

<a id="test-53-todo-del"></a>

### 53-todo-del · ⏭

> 删除待办「Amoo测试待办-周五前交」,只删这一条。

创建失败，没有可删除对象；本项跳过。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 4 页。

</details>

<a id="compound"></a>

## 串联任务

<a id="test-cx1-bt-calc"></a>

### cx1-bt-calc · ✅

> 请依次:1)查蓝牙状态 2)若蓝牙开着就先关掉 3)打开计算器 4)再把蓝牙恢复到原来状态。逐步回报每一步。

**拆解**：是；**确认栏**：—。

查询与切换蓝牙、打开计算器后恢复；原报告记录完成。

<details>
<summary>触发 method 与来源</summary>

```text
open_app_by_name,settings_bluetooth_switch,settings_get_bluetooth_state
```

原报告第 5 页。

</details>

<a id="test-cx2-weather-alarm"></a>

### cx2-weather-alarm · ✅

> 查一下今天本地天气概况,然后设一个标签「天气测试闹钟」明天早上7点的闹钟。闹钟建好后告诉我标签名。

**拆解**：是；**确认栏**：—。

查询天气后建立测试闹钟；源表指向下一条删除记录。

<details>
<summary>触发 method 与来源</summary>

```text
alarm_clock_add,weather_get_current_location_weather
```

原报告第 5 页。

</details>

<a id="test-cx2b-del-alarm"></a>

### cx2b-del-alarm · ✅

> 只删除标签「天气测试闹钟」的闹钟,不要删别的。

**拆解**：是；**确认栏**：—。

删除指定测试闹钟；原报告记录完成。

<details>
<summary>触发 method 与来源</summary>

```text
alarm_clock_delete,alarm_clock_get_enable
```

原报告第 5 页。

</details>

<a id="test-cx3-meeting"></a>

### cx3-meeting · ✅

> 会议模式:打开勿扰,并把亮度调低一点。逐步回报。

**拆解**：是；**确认栏**：—。

开启勿扰并相对调暗亮度；后续通过测试工具恢复，收尾再次关闭勿扰。

<details>
<summary>触发 method 与来源</summary>

```text
settings_adjust_brightness,settings_disturb_mode_switch
```

原报告第 5 页。

</details>

<a id="test-cx4-list-open"></a>

### cx4-list-open · ✅

> 列出我装了哪些第三方应用(只要前5个名字),然后打开计算器。

**拆解**：是；**确认栏**：—。

查询应用列表后打开计算器；不收录实际列表。

<details>
<summary>触发 method 与来源</summary>

```text
common_ability_get_app_list,open_app_by_name
```

原报告第 5 页。

</details>

<a id="test-cx5-sms-code"></a>

### cx5-sms-code · ✅

> 查最近三天的短信里有没有「验证码」三个字,最多5条。只报条数;号码正文 。

**拆解**：是；**确认栏**：—。

按条件检索短信；不收录内容和命中数量。

<details>
<summary>触发 method 与来源</summary>

```text
sms_search
```

原报告第 5 页。

</details>

<a id="test-cx6-route-weather"></a>

### cx6-route-weather · ✅

> 先查从我当前位置到最近地铁站的路线(不要开始导航),再查当地今天天气。两步都做。

**拆解**：是；**确认栏**：—。

查询路线与本地天气；未启动导航。

<details>
<summary>触发 method 与来源</summary>

```text
get_current_location,weather_get_current_location_weather
```

原报告第 5 页。

</details>

<a id="test-cx7-sms-observe"></a>

### cx7-sms-observe · ✅

> 帮我给[测试联系人]发短信:测试。注意:如果到了要真正发送的一步,请先停住只告诉我你准备调用哪个能力,不要真的发送。

**拆解**：观察；**确认栏**：确认。

只观察到发送前的确认分支；未实际发送短信，不能记为发送成功。

<details>
<summary>触发 method 与来源</summary>

```text
—
```

原报告第 5 页。

</details>

<a id="test-zz-restore-dnd"></a>

### zz-restore-dnd · ✅

> 请关闭勿扰模式,恢复到未开启状态。只做这一件事。

**拆解**：收尾；**确认栏**：—。

关闭测试中残留的勿扰状态，原报告记载收尾完成。

<details>
<summary>触发 method 与来源</summary>

```text
settings_disturb_mode_switch
```

原报告第 5 页。

</details>

<a id="not-run"></a>

## 未执行任务

以下 12 类为原报告单独列出的未执行范围，不包含在 65 条案例的成功数量内。

- **发短信 sms_send / sms_send_direct 真发**：不可逆外发；⏭ 观察到能力名 sms_send_direct,未执行发送
- **拨打电话**：不可逆；⏭ 未执行
- **微信发消息/视频/转账/红包/朋友圈**：不可逆或不支持；⏭ 未执行
- **支付/缴费/充值/挂号/寄快递/代缴**：花钱风险；⏭ 未执行
- **滴滴叫车 create_order**：真实计费；⏭ 未执行
- **实际开始导航**：改变出行状态；⏭ 未执行
- **订酒店/机票/火车票预订**：花钱；⏭ 未执行
- **购物下单**：花钱；⏭ 未执行
- **卸载 App**：不可逆；⏭ 未执行
- **删除全部闹钟**：不可逆；⏭ 未执行
- **飞行模式/移动数据/热点开关**：断网；⏭ 未执行
- **把照片分享给他人**：外发隐私；⏭ 未执行

## 主要边界

- 短信关键词、日期检索和联系人查询有记录；话费与流量只读查询失败。
- 多数设置写操作没有额外确认；短信读取出现风险确认。
- 待办能力在该测试环境缺失，不能推断为永久不支持。
- 多步任务有执行记录，但发送短信只停在确认前。
- 原报告记录清理测试日程、闹钟和临时工具，并处理残留勿扰状态；本次没有重新执行这些清理动作。

[来源与整理说明](sources.md) · [结构化案例](data.json)
