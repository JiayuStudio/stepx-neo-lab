# 晨星 Morningstar：基金资料查询卡

[课程导航](../README.md) · [连接器总表](../connector-index.md) · [官方详情](https://studio.stepx.com/connectors/ic_morningstar)

2026-11-17 核对；官方版本 **1.0.0**，当前能力表 **18 项**。本课提供开发步骤与验收方法，尚未代替你完成授权、调用或真机验收。

## 这一课做什么

按基金名称搜索，选择准确标识后展示概况与费率，不给买卖建议。

## 添加与准备

1. 打开官方 Step Studio，新建或进入一个测试用应用项目。
2. 打开「连接器」，搜索「晨星 Morningstar」，查看详情后选择「添加到项目」。选择刚建的测试项目。
3. 在项目中添加连接器，选择「网页授权」，由使用者登录自己的服务账号；授权范围与额度随该账号。不要把密码或令牌写入提示词、前端代码。
4. 让 Studio 读取当前连接器说明与参数，先完成一个最小动作。资料不足时先显示待配置，不用假数据冒充成功。

## 按顺序实现

选择网页授权，先搜索基金再用返回标识查概况和费率；展示数据日期、单位和来源。

对照两个基金的同口径数据，缺失值保留为空，不补造收益。

| 起步能力 | 用途 | 当前必填参数 |
| --- | --- | --- |
| `searchFundByNameOrId` | 基金搜索 | keyword |
| `getFundProfile` | 基金概况 | fundCode |
| `getFundFee` | 基金费率 | fundCode |

参数类型、可选值和返回字段，以项目内当前 schema 为准；复制名字不等于接口已经接通。

## 可复制提示词

```text
使用官方「晨星 Morningstar」连接器做「基金资料查询卡」。
目标：按基金名称搜索，选择准确标识后展示概况与费率，不给买卖建议。
先读取当前能力、inputSchema 和返回格式，不猜工具名和参数。
先展示页面及调用计划；只接入实现本目标所需的能力。
选择网页授权，先搜索基金再用返回标识查概况和费率；展示数据日期、单位和来源。
页面显示未连接、执行中、成功、无结果与失败状态；成功以真实返回为准。
修改、发送、支付、删除或批量操作前显示目标与影响，等待确认。
官方说明提供 OAuth，但元数据仍有访问令牌字段；只教网页授权路径，若要求手填 Token 则暂停。数据展示不等于投资建议。
完成后给我逐项测试方法，并明确哪些只是代码完成、哪些已实际运行。
```

## 验收与下一步

- 正常输入：记录一次调用的输入、真实返回与页面结果；数字、单位和对象相互一致。
- 未授权或拒绝权限：显示需要连接或授权，不能显示成功。
- 空结果、无网络或服务错误：保留可理解的说明，恢复条件后再由用户重试。
- 写操作：只用测试对象；完成后在原服务或手机中读回，不把按钮点击视作成功。

官方说明提供 OAuth，但元数据仍有访问令牌字段；只教网页授权路径，若要求手填 Token 则暂停。数据展示不等于投资建议。

<details>
<summary>本次官方能力名称（开发者查阅）</summary>

- `fundReviewData`
- `getCompanyInfo`
- `getFilterFund`
- `getFundFee`
- `getFundFullHoldings`
- `getFundGrowthDataByDateRange`
- `getFundHolderStructure`
- `getFundHoldingsStatistics`
- `getFundManagerFullInfo`
- `getFundPerformance`
- `getFundProfile`
- `getFundRiskAndReturn`
- `getFundSizeAndInvestorReturn`
- `getFundStrategyAnalysis`
- `searchFundByNameOrId`
- `searchFundManagerByKeyword`
- `searchKnowledgeBase`
- `xrayDiagnose`

</details>
