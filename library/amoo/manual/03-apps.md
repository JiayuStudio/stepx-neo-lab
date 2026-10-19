# 打开应用与应用管理

[手册目录](README.md) · [资料库](../README.md) · [一句话清单](../quick-start.md) · [实测结果](../test-results.md)

原稿更新：2026-10-20。证据标签表示原资料的记录，本次未重新实测。人名、联系方式和地点参数使用示例或占位符。

<a id="prompt-03-01"></a>

### 打开某个 App

> 打开计算器

**原文证据**：●实测(前台为计算器 Activity)

**预期效果**：计算器被拉到前台;只打开,不按键

<details>
<summary>能力名称与来源</summary>

```text
open_app_by_name
```

来源：原手册第 3 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-03-02"></a>

### 打开第三方App

> 打开B站 / 携程 / 京东 / 淘宝 / 美团/ 支付宝 / 微信 / 高德 / QQ音乐 /相册等

**原文证据**：●实测(2026-10-20批量)

**预期效果**：拉到前台;未装则失败(如拼多多、咪咕)

<details>
<summary>能力名称与来源</summary>

```text
open_app_by_name
```

来源：原手册第 3 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-03-03"></a>

### App 名说不准时

> 列出手机上装了哪些第三方应用,然后打开阶跃AI

**原文证据**：●实测(名称匹配失败时 Amoo 会自动改走这一步)

**预期效果**：返回已装应用名与包名,再按名称打开

<details>
<summary>能力名称与来源</summary>

```text
common_ability_get_app_list →open_app_by_name
```

来源：原手册第 3 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-03-04"></a>

### 按包名精确打开

> 按包名 tv.danmaku.bili 打开应用

**原文证据**：◆(检索命中)

**预期效果**：按包名拉起App(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
open_app_by_package
```

来源：原手册第 3 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-03-05"></a>

### 查是否安装

> 看一下手机里有没有装携程旅行

**原文证据**：●实测(2026-10-20)

**预期效果**：返回是否已安装

<details>
<summary>能力名称与来源</summary>

```text
check_app_installed_by_name
```

来源：原手册第 3 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-03-06"></a>

### 统计应用数量

> 我手机上装了多少个应用

**原文证据**：●实测（2026-10-20；原报告记录数量核对成功）

**预期效果**：返回第三方应用数量

<details>
<summary>能力名称与来源</summary>

```text
common_ability_get_app_count
```

来源：原手册第 3 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-03-07"></a>

### 查应用权限

> 看看高德地图申请了哪些权限,只读不改

**原文证据**：◆(检索命中)

**预期效果**：返回权限列表(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
common_ability_get_app_permissions
```

来源：原手册第 3 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-03-08"></a>

### 用浏览器打开网址

> 用浏览器打开https://www.stepfun.com

**原文证据**：●实测(2026-10-20单测 stepfun.com)

**预期效果**：手机原生浏览器打开该链接

<details>
<summary>能力名称与来源</summary>

```text
open_url_with_browser
```

来源：原手册第 3 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-03-09"></a>

### 应用商店搜App

> 在应用商店搜一下"咪咕音乐",不要下载

**原文证据**：◆(明细表)

**预期效果**：跳转应用商店按关键词检索,不下载(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
appstore_search_app
```

来源：原手册第 4 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-03-10"></a>

### 查看全部应用页

> 打开系统的"所有应用"页面

**原文证据**：◆(检索命中)

**预期效果**：打开设置里的应用列表页(未实测,以真机返回为准)

<details>
<summary>能力名称与来源</summary>

```text
settings_open_manage_applications
```

来源：原手册第 4 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

<a id="prompt-03-11"></a>

### 卸载应用(不可逆)

> 帮我卸载"某某应用",卸载前先跟我确认

**原文证据**：◆(明细表)

**预期效果**：按应用名卸载(未实测,以真机返回为准;不可逆,慎用)

<details>
<summary>能力名称与来源</summary>

```text
security_packageinstaller_uninstall
```

来源：原手册第 4 页。这里是文档中的能力参考，不等于本次实际调用记录。

</details>

[返回手册目录](README.md) · [查看实测结果](../test-results.md)
