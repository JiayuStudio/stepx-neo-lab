# Amoo Notion CLI 1.0.1

与 [图文教程](../../guides/amoo-notion/README.md) 配套的只读工具，使用 Node.js 22+ 运行，支持页面读取、标题搜索和数据库查询。

| 下载 | 用途 |
| --- | --- |
| [手机附件 .js.txt](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/amoo-notion-v1.0.1/amoo-notion-1.0.1.js.txt) | 文件选择器不支持 .js 时使用；让 Amoo 按原字节保存为 .js |
| [单文件 JavaScript](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/amoo-notion-v1.0.1/amoo-notion-1.0.1.js) | Node.js 22+ 直接执行 |
| [查看源码](amoo-notion-1.0.1.js) | 安装前阅读代码 |
| [SHA256SUMS.txt](SHA256SUMS.txt) | 两份源码的字节校验 |
| [provenance.json](provenance.json) | 来源与分发范围 |

两份源文件均为 **18,823 字节**，SHA-256：

```text
bc1fa570e86bd2b23b72d6f3d2f979f8b6934d0a3fba873d3e9e9ec1003717b3
```

源码与 Amoo 实测版本一致，没有第三方运行依赖。

## 运行与配置

```sh
node ./amoo-notion-1.0.1.js --help
node ./amoo-notion-1.0.1.js --version
node ./amoo-notion-1.0.1.js config --json
```

已有配置时直接复用；首次配置可在交互终端执行 `node ./amoo-notion-1.0.1.js init`，按提示录入 token。完整步骤见图文教程第 5 节。

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

默认 API 版本为 `NOTION_VERSION=2022-06-28`，沿用实测配置。`init` 保存本地配置，其余命令用于查询 Notion。

单次请求默认超时 30 秒，临时错误最多重试两次。分页结果出现 `has_more` 时，表示还有后续内容。

JIAYU STUDIO 原创工具，保留版权，尚未另行指定开源许可证。
