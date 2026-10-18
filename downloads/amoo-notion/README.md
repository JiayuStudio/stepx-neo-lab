# Amoo Notion CLI 1.0.1

与 [图文教程](../../guides/amoo-notion/README.md) 配套的个人研究工具。维护者已于 2026-10-18 授权作为教程附件公开，保留版权，未声明开源许可证。不是 Notion 或 STEPX 官方产品，没有发布 npm 或技能市场。

| 下载 | 用途 |
| --- | --- |
| [手机附件 .js.txt](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/amoo-notion-v1.0.0/amoo-notion-1.0.1.js.txt) | 文件选择器不支持 .js 时使用；让 Amoo 按原字节保存为 .js |
| [单文件 JavaScript](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/amoo-notion-v1.0.0/amoo-notion-1.0.1.js) | Node.js 22+ 直接执行 |
| [查看源码](amoo-notion-1.0.1.js) | 安装前阅读代码 |
| [SHA256SUMS.txt](SHA256SUMS.txt) | 两份源码的字节校验 |
| [provenance.json](provenance.json) | 来源与分发范围 |

两份源文件均为 **18,823 字节**，SHA-256：

```text
bc1fa570e86bd2b23b72d6f3d2f979f8b6934d0a3fba873d3e9e9ec1003717b3
```

这与先前在 Amoo 上实测通过的单文件版本一致，本次没有修改程序。脚本没有第三方运行依赖，不携带 token、配置文件、Notion 页面内容、账号或工作区标识。

## 运行与配置

```sh
node ./amoo-notion-1.0.1.js --help
node ./amoo-notion-1.0.1.js --version
node ./amoo-notion-1.0.1.js config --json
```

现有配置优先复用。只有在你本人可安全操作的私密终端中首次初始化时，才使用 `init` 交互输入；输入不回显，不把 token 写进命令行或聊天。具体边界见图文教程第 5 节。

凭据来源顺序：`NOTION_TOKEN` → `NOTION_API_TOKEN` → 本地配置文件。Amoo / Linux 的默认配置目录为 `~/.config/notion-cli/`；macOS 默认使用 `~/Documents/VPS SSH KEY/NotionCLI/`；可通过 `NOTION_CLI_HOME` 自定义。路径属于脚本实际运行的环境。配置权限需要 `0600`，已有配置默认不会被 `init` 覆盖。

## 命令范围

| 命令 | 作用 |
| --- | --- |
| `config --json` | 脱敏报告配置是否就绪、来源与 API 版本 |
| `self --json` | 查询当前连接身份 |
| `read "页面链接" --json` | 读取页面 Markdown；核对 truncated 和 unknown_block_ids |
| `find "关键词" --limit 20` | 按标题搜索，不搜索正文 |
| `ls --limit 100` | 列出可访问对象；不意味着获取全部对象 |
| `tree "页面ID" --limit 100` | 列直接子块，不自动递归所有子页 |
| `db "数据库或数据源ID" --limit 20 --json` | 查询记录；多数据源时须明确目标 |
| `init` | 初始化本地凭据，不修改 Notion 内容 |

只有身份、搜索、页面读取、子块与数据库查询端点；查询用的 POST 不代表页面写入。凭据只发送到固定 `https://api.notion.com/v1`，不跟随重定向。配置默认兼容既有 `NOTION_VERSION=2022-06-28`，不是宣称该值是 Notion 最新 API 版本；未来服务变更需要单独复测，不要无依据改参数。

单次请求默认超时 30 秒，临时错误最多重试两次；分页有数量上限，仍应查看 has_more 等完整性信息。CLI 限制不能替代 Notion 服务端的最小授权，也不能赋予 Amoo 本来不存在的 Node / Shell 能力。
