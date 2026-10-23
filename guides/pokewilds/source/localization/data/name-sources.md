# 名称汉化依据

执行客户端：Codex。核验日期：2026-10-05（Asia/Shanghai）。

`build_name_mappings.py` 可离线重建本目录 `zh_CN` 内的宝可梦名称、招式名、道具名三个 UTF-8 资源文件。脚本保留原键，同时增加实际物种资源使用的形态键和别名；汉化只改变显示名称，不改变游戏对战和进化规则。

## 当前名称来源

- 中国宝可梦官方图鉴：[图鉴](https://pokedex.pokemon.cn/play/pokedex)；其页面使用的公开数据地址为 [官方图鉴 API](https://pokedex.pokemon.cn/play/pokedex/api/v1)。快照 `evidence/official-pokedex-cn.json` 保存全部 1,251 个物种及形态的名称和属性，省略图片链接等无关内容。物种名以官方图鉴为准。
- [PokeAPI/pokeapi](https://github.com/PokeAPI/pokeapi) 固定提交 `bc92d3b6029ef1abe9e7ad424c400b338f3c11fe`（2026-09-30），使用全国图鉴编号、英文标识与 `zh-hans` 名称建立映射。CSV、源地址、Git blob ID、SHA-256 均存于 `evidence/pokeapi`。简体语言 ID 为 `12`，英文为 `9`。许可证副本为 `evidence/pokeapi/LICENSE.txt`（BSD-3-Clause）。
- 上游资源：[SheerSt/pokewilds](https://github.com/SheerSt/pokewilds)，本次版本目录 `source` 中保留。上游 i18n 的基础物种表有 900 个键、招式有 349 个键、道具有 113 个键。GitHub 未显示项目许可证，不将公开资源误称为已获任意再发布许可。

## 校正与专用词

- PokeAPI 中 `minun` 的简体名混入了繁体字 `負`，依据官方图鉴修正为“负电拍拍”；`porygon2` 采用官方图鉴的“多边兽2型”。
- 旧招式标识 `hi_jump_kick` 映射现行 `high-jump-kick`，显示“飞膝踢”；`faint_attack` 映射 `feint-attack`，显示“出奇一击”。
- 上游 `shield_fossil` 即用于盾甲龙家族的化石，映射标准 `armor-fossil`，显示“盾甲化石”。
- `blank_disc` 的上游显示名是 `worn disc`，据其实际文本译为“破旧光盘”。没有按旧内部键译成“空白光盘”。
- 原创野外材料、寝具、睡袋等没有对应官方宝可梦道具名，明确使用脚本中的 `CUSTOM_ITEMS` 自译词表；不声称这些词是官方汉化。名称在道具描述与其他界面中保持一致。
- 区域形态、洛托姆电器形态、代欧奇希斯形态、武道熊师流派、蓑衣形态采用当前官方图鉴名称。颜色和未知图腾字母作为区分形态的标记保留。
- `gmrmime` 是另一个魔墙人偶区域贴图目录，图鉴文本描述冰面踢踏舞，映射为“魔墙人偶（伽勒尔的样子）”。该目录原统计资料仍为旧占位信息，汉化不据此更改其数值。
- `straigar` 仅含贴图/动画/色板，没有 `base_stats.asm`、`wilds_data.asm` 或可核实全国图鉴编号，作为未启用美术目录排除，不猜造“官方中文名”。

## 资源覆盖校核

重建时检查重复键、空值、全部基础键覆盖和中文字符存在。结果记录于 `name-mapping-report.json`：宝可梦 1,003 条（900 个基础键与 103 个资源别名/形态键），招式 349/349，道具 113/113；实际物种资源目录覆盖 989 个，另一个未启用美术目录如上所述排除。

这份校核证明的是名称资源完整性。画面字形、窗口布局、实际存档及游戏操作仍须由安装后的运行验证确认。
