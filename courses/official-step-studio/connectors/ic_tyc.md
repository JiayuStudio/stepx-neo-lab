# 天眼查：企业基础资料卡

[课程导航](../README.md) · [连接器总表](../connector-index.md) · [官方详情](https://studio.stepx.com/connectors/ic_tyc)

2026-11-17 核对；官方版本 **1.0.0**，当前能力表 **17 项**。本课提供开发步骤与验收方法，尚未代替你完成授权、调用或真机验收。

## 这一课做什么

按公司名称找到准确主体，查询其基础工商资料并标明来源时间。

## 添加与准备

1. 打开官方 Step Studio，新建或进入一个测试用应用项目。
2. 打开「连接器」，搜索「天眼查」，查看详情后选择「添加到项目」。选择刚建的测试项目。
3. 在项目中添加连接器，选择「网页授权」，由使用者登录自己的服务账号；授权范围与额度随该账号。不要把密码或令牌写入提示词、前端代码。
4. 让 Studio 读取当前连接器说明与参数，先完成一个最小动作。资料不足时先显示待配置，不用假数据冒充成功。

## 按顺序实现

先检索并让用户选定主体，再读取该主体允许的内部工具名；选择一个基础资料工具调用。

加入同名主体选择和权益不足提示；空记录如实说明，不生成虚构工商信息。

| 起步能力 | 用途 | 当前必填参数 |
| --- | --- | --- |
| `search_companies` | 检索公司（主体锚定） | query |
| `get_company_capabilities` | 发现公司可用能力 | company_id |
| `call_tool` | 调用内部业务工具 | tool_name, arguments |

参数类型、可选值和返回字段，以项目内当前 schema 为准；复制名字不等于接口已经接通。

## 可复制提示词

```text
使用官方「天眼查」连接器做「企业基础资料卡」。
目标：按公司名称找到准确主体，查询其基础工商资料并标明来源时间。
先读取当前能力、inputSchema 和返回格式，不猜工具名和参数。
先展示页面及调用计划；只接入实现本目标所需的能力。
先检索并让用户选定主体，再读取该主体允许的内部工具名；选择一个基础资料工具调用。
页面显示未连接、执行中、成功、无结果与失败状态；成功以真实返回为准。
修改、发送、支付、删除或批量操作前显示目标与影响，等待确认。
通用 call_tool 还需限制内部工具白名单；不开放 call_tools_batch。权益不足显示原始限制，不自动升级套餐。
完成后给我逐项测试方法，并明确哪些只是代码完成、哪些已实际运行。
```

## 验收与下一步

- 正常输入：记录一次调用的输入、真实返回与页面结果；数字、单位和对象相互一致。
- 未授权或拒绝权限：显示需要连接或授权，不能显示成功。
- 空结果、无网络或服务错误：保留可理解的说明，恢复条件后再由用户重试。
- 写操作：只用测试对象；完成后在原服务或手机中读回，不把按钮点击视作成功。

通用 call_tool 还需限制内部工具白名单；不开放 call_tools_batch。权益不足显示原始限制，不自动升级套餐。

<details>
<summary>本次官方能力名称（开发者查阅）</summary>

- `call_tool`
- `call_tools_batch`
- `get_company_basic_profile`
- `get_company_capabilities`
- `get_company_group_profile`
- `get_company_people`
- `get_group_info`
- `get_person_profile`
- `get_person_risk_profile`
- `search_bids`
- `search_companies`
- `search_companies_by_industry_region`
- `search_companies_by_ranking`
- `search_companies_by_tag`
- `search_listed_companies`
- `search_patents`
- `search_trademarks`

</details>
