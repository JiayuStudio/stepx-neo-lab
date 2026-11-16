# 华宇元典法律数据：法规来源检索卡

[课程导航](../README.md) · [连接器总表](../connector-index.md) · [官方详情](https://studio.stepx.com/connectors/ic_yuandian)

2026-11-17 核对；官方版本 **1.0.0**，当前能力表 **48 项**。本课提供开发步骤与验收方法，尚未代替你完成授权、调用或真机验收。

## 这一课做什么

查询一项法规并展示标题、出处与正文链接，不生成法律结论。

## 添加与准备

1. 打开官方 Step Studio，新建或进入一个测试用应用项目。
2. 打开「连接器」，搜索「华宇元典法律数据」，查看详情后选择「添加到项目」。选择刚建的测试项目。
3. 在项目中添加连接器，选择「网页授权」，由使用者登录自己的服务账号；授权范围与额度随该账号。不要把密码或令牌写入提示词、前端代码。
4. 让 Studio 读取当前连接器说明与参数，先完成一个最小动作。资料不足时先显示待配置，不用假数据冒充成功。

## 按顺序实现

选择网页授权方式；只从当前能力表选法规检索方法，读取必填参数，再查询并核对返回的法规名称。

区分法规、案例和公告；为结果标注来源与检索时间。

| 起步能力 | 用途 | 当前必填参数 |
| --- | --- | --- |
| `yuandian_search_listed_company_announcements` | 公告检索 | 无必填字段（仍核对可选项） |
| `yuandian_search_securities_regulatory_cases` | 监管案例检索 | 无必填字段（仍核对可选项） |
| `yuandian_search_securities_regulations` | 证券法规检索 | 无必填字段（仍核对可选项） |

参数类型、可选值和返回字段，以项目内当前 schema 为准；复制名字不等于接口已经接通。

## 可复制提示词

```text
使用官方「华宇元典法律数据」连接器做「法规来源检索卡」。
目标：查询一项法规并展示标题、出处与正文链接，不生成法律结论。
先读取当前能力、inputSchema 和返回格式，不猜工具名和参数。
先展示页面及调用计划；只接入实现本目标所需的能力。
选择网页授权方式；只从当前能力表选法规检索方法，读取必填参数，再查询并核对返回的法规名称。
页面显示未连接、执行中、成功、无结果与失败状态；成功以真实返回为准。
修改、发送、支付、删除或批量操作前显示目标与影响，等待确认。
官方说明提供 OAuth，但元数据仍有访问令牌字段；本课程只走网页授权。如果界面只能手动粘贴 Token，就暂停该步骤，不自行申请凭证。
完成后给我逐项测试方法，并明确哪些只是代码完成、哪些已实际运行。
```

## 验收与下一步

- 正常输入：记录一次调用的输入、真实返回与页面结果；数字、单位和对象相互一致。
- 未授权或拒绝权限：显示需要连接或授权，不能显示成功。
- 空结果、无网络或服务错误：保留可理解的说明，恢复条件后再由用户重试。
- 写操作：只用测试对象；完成后在原服务或手机中读回，不把按钮点击视作成功。

官方说明提供 OAuth，但元数据仍有访问令牌字段；本课程只走网页授权。如果界面只能手动粘贴 Token，就暂停该步骤，不自行申请凭证。

<details>
<summary>本次官方能力名称（开发者查阅）</summary>

- `yuandian_search_listed_company_announcements`
- `yuandian_search_securities_regulatory_cases`
- `yuandian_search_securities_regulations`
- `yuandian_search_securities_articles`
- `yuandian_semantic_search_securities_articles`
- `yuandian_hall_detect`
- `yuandian_get_company_annual_report`
- `yuandian_get_company_statistics_overview`
- `yuandian_search_companies`
- `yuandian_get_company_basic_profile`
- `yuandian_list_company_external_investments`
- `yuandian_list_company_trademarks`
- `yuandian_list_company_patents`
- `yuandian_list_company_software_copyrights`
- `yuandian_list_company_work_copyrights`
- `yuandian_list_company_website_filings`
- `yuandian_list_company_registration_changes`
- `yuandian_get_company_litigation_statistics`
- `yuandian_list_company_litigation_document_summaries`
- `yuandian_list_company_court_hearing_notices`
- `yuandian_list_company_court_announcements`
- `yuandian_list_company_judgment_defaulter_records`
- `yuandian_list_company_judicial_enforcement_records`
- `yuandian_list_company_equity_freezes`
- `yuandian_list_company_administrative_penalties`
- `yuandian_list_company_equity_pledges`
- `yuandian_list_company_external_guarantees`
- `yuandian_list_company_abnormal_operations`
- `yuandian_list_company_tax_arrears`
- `yuandian_list_company_serious_violations`
- `yuandian_semantic_search_legal_articles`
- `yuandian_semantic_search_cases`
- `yuandian_get_company_profile`
- `yuandian_search_company_profiles_by_name`
- `yuandian_get_legal_article_detail`
- `yuandian_get_law_or_regulation_detail`
- `yuandian_search_legal_articles`
- `yuandian_search_laws_and_regulations`
- `yuandian_get_case_detail`
- `yuandian_search_authoritative_cases`
- `yuandian_search_judicial_cases`
- `yuandian_stream_contract_review`
- `yuandian_get_contract_review_guide`
- `yuandian_prepare_cli`
- `yuandian_authorize_cli`
- `yuandian_get_user_balance`
- `yuandian_get_free_policy`
- `yuandian_report_issue`

</details>
