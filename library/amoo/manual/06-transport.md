# 打车、导航与出行

[手册目录](README.md) · [资料库](../README.md) · [一句话清单](../quick-start.md) · [实测结果](../test-results.md)

原稿更新：2026-10-20。证据标签表示原资料的记录，本次未重新实测。人名、联系方式和地点参数使用示例或占位符。

> 原稿中“全部未实测”的旧总述与更新条目不一致。10 月 20 日记录包含定位、路线和附近查询；滴滴估价不完整，真实叫车与导航未执行。以下逐项保留原证据标签。

<a id="prompt-06-01"></a>

### 我在哪

> 我现在在哪?告诉我详细地址

**原文证据**：●实测(2026-10-20)

**预期效果**：返回经纬度与文字地址

<details>
<summary>能力名称与来源</summary>

```text
get_current_location
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-02"></a>

### 滴滴估价

> 从当前位置去某地,用滴滴估个价,不要下单

**原文证据**：⚠ 2026-10-20 部分

**预期效果**：本轮估价未完整(授权提示);未叫车

<details>
<summary>能力名称与来源</summary>

```text
didi_taxi_estimate
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-03"></a>

### 滴滴叫车

> 就叫滴滴快车,下单前跟我确认一次

**原文证据**：◆(明细表)

**预期效果**：按估价结果下单并匹配司机;会真实叫车计费(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
didi_taxi_create_order
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-04"></a>

### 查滴滴订单

> 我的滴滴司机到哪了?还有多久到

**原文证据**：◆(明细表)

**预期效果**：返回司机、接驾距离、预计到达(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
didi_taxi_query_order
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-05"></a>

### 曹操出行估价

> 用曹操出行估一下从公司到浦东机场的价格

**原文证据**：◆(检索命中 + 明细表)

**预期效果**：返回车型与预估价(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
trip_estimate_v2
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-06"></a>

### 曹操下单 / 查单

> 用曹操叫一辆车;查一下我曹操订单的车牌和司机

**原文证据**：◆(明细表)

**预期效果**：下单(真实计费)/ 查进行中订单(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
trip_create_order_v2 / trip_query_order_v2
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-07"></a>

### 曹操取消 / 联系司机

> 取消刚才的曹操订单;给司机留言"我在南门"

**原文证据**：◆(明细表)

**预期效果**：取消需二次确认;留言;获取虚拟号(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
trip_cancel_order_v2 / trip_send_driver_message /trip_call_driver
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-08"></a>

### 曹操历史订单

> 查一下我这个月的曹操打车记录

**原文证据**：◆(明细表)

**预期效果**：跨度不超过 30 天(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
trip_query_history_orders
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-09"></a>

### 路线对比

> 从 A 到 B 开车要多久(不要导航)

**原文证据**：●实测(2026-10-20)

**预期效果**：返回公里与预计时间

<details>
<summary>能力名称与来源</summary>

```text
amap_route_card / 路线测算
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-10"></a>

### 公交地铁路线

> 从人民广场坐公共交通去虹桥机场怎么走

**原文证据**：◆(工具映射)

**预期效果**：公共交通规划(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
maps_direction_transit_integrated
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-11"></a>

### 附近搜索

> 我附近有什么咖啡店

**原文证据**：●实测(2026-10-20)

**预期效果**：返回店名列表

<details>
<summary>能力名称与来源</summary>

```text
amap_overview_card / maps_around_search
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-12"></a>

### 地点详情

> 外滩源的地址、营业时间和评分是多少

**原文证据**：◆(明细表)

**预期效果**：返回坐标、地址、评分、营业时间(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
maps_search_detail
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-13"></a>

### 两地距离

> 从静安寺到陆家嘴开车多远、要多久

**原文证据**：◆(明细表)

**预期效果**：驾车/步行/直线距离与耗时(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
maps_distance
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-14"></a>

### 多点行程图

> 把外滩、豫园、新天地、武康路排成一条路线做成地图

**原文证据**：◆(明细表)

**预期效果**：生成多点标图链接,单线最多 16 点(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
maps_schema_personal_map
```

来源：原手册第 6 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-15"></a>

### 唤起导航

> 导航去上海虹桥站

**原文证据**：◆(工具映射)

**预期效果**：唤起地图导航;进入地图后的操作需你自己(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
maps_schema_navi
```

来源：原手册第 7 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-06-16"></a>

### 乘车码

> 打开支付宝乘车码/ 打开微信乘车码

**原文证据**：◆(工具映射)

**预期效果**：打开对应乘车码页面(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
assistant_quick_action_alipay_transit_code /assistant_quick_action_wechat_transit_code
```

来源：原手册第 7 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

**核对说明**：对照实测 34-transit：只确认调起生活服务卡片，是否直达乘车码仍存疑。

[返回手册目录](README.md) · [查看实测结果](../test-results.md)
