# 轻流：只读待办与记录看板

[课程导航](../README.md) · [连接器总表](../connector-index.md) · [官方详情](https://studio.stepx.com/connectors/ic_qingflow)

2026-11-17 核对；官方版本 **1.0.0**，当前能力表 **106 项**。本课提供开发步骤与验收方法，尚未代替你完成授权、调用或真机验收。

## 这一课做什么

列出自己的测试工作区、应用和记录，做一个可筛选的只读页面。

## 添加与准备

1. 打开官方 Step Studio，新建或进入一个测试用应用项目。
2. 打开「连接器」，搜索「轻流」，查看详情后选择「添加到项目」。选择刚建的测试项目。
3. 在项目中添加连接器，选择「网页授权」，由使用者登录自己的服务账号；授权范围与额度随该账号。不要把密码或令牌写入提示词、前端代码。
4. 让 Studio 读取当前连接器说明与参数，先完成一个最小动作。资料不足时先显示待配置，不用假数据冒充成功。

## 按顺序实现

先选工作区，再读取应用和字段；根据真实字段生成筛选表单，仅查询一页记录。

查询通过后再做单条测试录入；写入前展示目标应用、字段与值，写完重新查询。

| 起步能力 | 用途 | 当前必填参数 |
| --- | --- | --- |
| `workspace_list` | 工作区列表 | 无必填字段（仍核对可选项） |
| `app_list` | 应用列表 | 无必填字段（仍核对可选项） |
| `app_get` | 应用详情 | 无必填字段（仍核对可选项） |
| `data_query` | 查询数据 | 无必填字段（仍核对可选项） |

参数类型、可选值和返回字段，以项目内当前 schema 为准；复制名字不等于接口已经接通。

## 可复制提示词

```text
使用官方「轻流」连接器做「只读待办与记录看板」。
目标：列出自己的测试工作区、应用和记录，做一个可筛选的只读页面。
先读取当前能力、inputSchema 和返回格式，不猜工具名和参数。
先展示页面及调用计划；只接入实现本目标所需的能力。
先选工作区，再读取应用和字段；根据真实字段生成筛选表单，仅查询一页记录。
页面显示未连接、执行中、成功、无结果与失败状态；成功以真实返回为准。
修改、发送、支付、删除或批量操作前显示目标与影响，等待确认。
区分应用结构与业务记录；初学版不要开放 builder_*、批量删除或注销能力。
完成后给我逐项测试方法，并明确哪些只是代码完成、哪些已实际运行。
```

## 验收与下一步

- 正常输入：记录一次调用的输入、真实返回与页面结果；数字、单位和对象相互一致。
- 未授权或拒绝权限：显示需要连接或授权，不能显示成功。
- 空结果、无网络或服务错误：保留可理解的说明，恢复条件后再由用户重试。
- 写操作：只用测试对象；完成后在原服务或手机中读回，不把按钮点击视作成功。

区分应用结构与业务记录；初学版不要开放 builder_*、批量删除或注销能力。

<details>
<summary>本次官方能力名称（开发者查阅）</summary>

- `auth_use_credential`
- `auth_whoami`
- `get_login_link`
- `auth_logout`
- `record_export_start`
- `record_export_status_get`
- `record_export_get`
- `record_export_direct`
- `workspace_list`
- `workspace_get`
- `workspace_select`
- `app_list`
- `app_get`
- `data_query`
- `data_get`
- `data_insert`
- `data_update`
- `data_delete`
- `portal_list`
- `portal_get`
- `view_get`
- `chart_get`
- `file_get_upload_info`
- `file_upload_local`
- `record_import_schema_get`
- `record_import_template_get`
- `record_import_verify`
- `record_import_repair_local`
- `record_import_start`
- `record_import_status_get`
- `feedback_submit`
- `record_insert_schema_get`
- `record_member_candidates`
- `record_department_candidates`
- `record_list`
- `record_access`
- `record_get`
- `record_logs_get`
- `record_browse_schema_get`
- `record_update_schema_get`
- `record_insert`
- `record_update`
- `record_delete`
- `record_code_block_schema_get`
- `record_code_block_run`
- `task_list`
- `task_get`
- `task_action_execute`
- `task_associated_report_detail_get`
- `task_workflow_log_get`
- `directory_search`
- `builder_auth_use_credential`
- `builder_auth_whoami`
- `builder_auth_logout`
- `builder_get_login_link`
- `builder_workspace_list`
- `builder_workspace_get`
- `builder_file_upload_local`
- `builder_feedback_submit`
- `builder_builder_tool_contract`
- `builder_app_form_schema`
- `builder_app_form_get`
- `builder_app_form_apply`
- `builder_app_views_schema`
- `builder_app_views_get`
- `builder_app_views_validate`
- `builder_app_delete`
- `builder_workspace_icon_catalog_get`
- `builder_package_list`
- `builder_package_get`
- `builder_package_apply`
- `builder_solution_install`
- `builder_member_search`
- `builder_role_search`
- `builder_role_create`
- `builder_app_release_edit_lock_if_mine`
- `builder_app_resolve`
- `builder_button_style_catalog_get`
- `builder_app_custom_buttons_apply`
- `builder_app_associated_resources_apply`
- `builder_app_get`
- `builder_app_get_fields`
- `builder_app_repair_code_blocks`
- `builder_app_get_layout`
- `builder_app_get_views`
- `builder_app_get_flow`
- `builder_app_get_charts`
- `builder_app_get_buttons`
- `builder_app_get_associated_resources`
- `builder_portal_list`
- `builder_portal_schema`
- `builder_portal_validate`
- `builder_portal_get`
- `builder_view_get`
- `builder_chart_get`
- `builder_app_flow_get`
- `builder_app_flow_get_schema`
- `builder_app_flow_validate`
- `builder_app_flow_list_versions`
- `builder_app_flow_restore_version`
- `builder_app_flow_apply`
- `builder_app_views_apply`
- `builder_app_charts_apply`
- `builder_portal_apply`
- `builder_portal_delete`
- `builder_app_publish_verify`

</details>
