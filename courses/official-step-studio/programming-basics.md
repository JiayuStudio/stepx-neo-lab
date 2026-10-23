# 从零理解应用、智能体、技能与代码

[新手路线](../../sections/start-here.md) · [Agent 提取入口](../../llms.txt) · [逐页元数据](../../catalog.json)

这是一节通用 Web 编程基础课，不依赖账号、模型或付费服务。示例不是 Step Studio SDK，也不证明平台支持某个接口。平台入口、发布流程和能力权限请对照[官方资料与当前核查](../../maintenance/2026-10-23-review.md)。

## 先知道自己要做什么

**应用**把输入、规则、状态与输出组合成用户能操作的产品。计数器中，点击是输入，数字加一是规则，当前数字是状态，屏幕文字是输出。AI 可以帮助写应用；应用本身不一定需要 AI。

**智能体（Agent）**通常接收目标，依据上下文决定下一步，必要时调用工具，读取结果再继续。模型说“已完成”不等于工具真的成功；必须检查返回值及最终产物。一个会议纪要助手可以只整理用户提供的文字，不能凭空声称听过录音或已给参会者发信。

**技能（Skill）**是可复用的处理说明或能力封装，例如把一段作品介绍变为标题、摘要和标签。不同平台对技能文件、执行环境、工具与发布的定义不同，不能把一个平台的格式直接当作另一个平台的标准。技能与 Agent 的区别在于前者描述可复用任务，后者可能在执行过程中选择任务与工具。

练习：把“作品展示助手”拆成三张卡。应用卡写“浏览作品”；Agent 卡写“根据访客问题选择作品”；技能卡写“把作品描述整理成摘要”。先做应用卡，验收条件写成“点击一次后能看到变化”。

## HTML：内容是什么

HTML 用标签描述结构。例如 `<button>加一</button>` 创建按钮，开始标签与结束标签围住内容。`id="add"` 是这个元素的标识，方便程序定位。HTML 不负责“点击后加一”的运算。

先新建 UTF-8 文件 `counter.html`，粘贴以下完整内容，用浏览器打开。请先读代码中的注释，再进行下一节的修改。

```html
<!doctype html>
<html lang="zh-CN">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>我的第一个计数器</title>
<style>
  /* CSS：选择 button 元素，设置外观；不改变计数规则。 */
  body { font-family: sans-serif; padding: 24px; }
  button { padding: 12px 20px; background: #173d78; color: white; }
</style>
<h1>练习计数器</h1>
<p id="result" aria-live="polite">当前次数：0</p>
<button id="add" type="button">加一</button>
<script>
  // let 声明会变化的变量。0 是数字，不是字符串 "0"。
  let count = 0;
  // const 保存元素引用；这些引用不需要重新赋值。
  const button = document.querySelector('#add');
  const result = document.querySelector('#result');
  // 回调函数会在点击时运行，不是在注册时立即运行。
  button.addEventListener('click', () => {
    count = count + 1;
    result.textContent = `当前次数：${count}`;
  });
</script>
</html>
```

## CSS：外观怎样变化

CSS 规则由“选择器”和花括号内的属性组成。`button` 选择按钮，`padding` 控制内边距，`background` 控制背景色。把 `24px` 改成 `40px`，观察页面边距；再改变按钮颜色。预期只有外观变化，点击计数仍每次加一。若计数也变了，检查是否误改了脚本。

## JavaScript：事件、变量和函数

JavaScript 执行行为。`count = count + 1` 先读旧值再赋新值；`textContent` 将结果作为文字显示。这里不用 `innerHTML`，因为显示用户文本不需要把它解释成 HTML。

练习一：把加一改为加二，连续点击三次，预期显示 6。练习二：增加“重置”按钮，绑定另一个点击函数，将 `count` 设为 0，并同步更新结果文字。只改变量却不更新界面时，屏幕仍可能显示旧值，这就是状态与展示不同步。

刷新页面会回到 0，因为变量在内存中。它没有保存到云端、文件或手机数据库；不要把“还看得到文字”当成持久保存。后续学习存储前，先写清保存位置、删除方式和权限。

## JSON：用数据描述内容

JSON 是数据格式，不是可执行程序。对象使用键值对，数组保存有顺序的一组值。键名与字符串必须用双引号，不能写注释或末尾多余逗号。

```json
{"title":"计数练习","count":3,"tags":["入门","无网络"],"saved":false}
```

在浏览器开发者工具的 Console 中输入：

```js
const text = '{"title":"计数练习","count":3}';
const item = JSON.parse(text);
console.log(item.count + 1); // 预期 4
console.log(JSON.stringify(item)); // 将对象转换回 JSON 文本
```

尝试把 JSON 中 `3` 改为 `"3"`，再运行加法：会得到字符串拼接的 `31`。这说明输入不仅要看内容，还要检查类型。尝试多加一个逗号，观察解析错误，然后恢复。错误消息是定位线索，不是让你忽略问题的理由。

## API：程序之间的约定

API 约定“怎么请求、给哪些参数、返回什么、失败如何表达”。HTTP API 常包含 URL、方法（例如 GET 读取）、请求头、状态码和响应正文。收到 HTTP 响应不等于业务成功；还要检查状态码和字段。浏览器跨域限制、登录权限、额度和网络中断也会影响结果。

下面是**本地模拟接口**，不访问真实平台。它帮助理解异步等待和输入验证，不是 Step Studio 的调用方式。粘贴到 Console 运行：

```js
async function mockReadCounter(id) {
  if (id !== 'demo') throw new Error('示例不存在');
  return { count: 3 }; // 模拟已经解析的响应对象
}
async function showCounter(id) {
  try {
    const data = await mockReadCounter(id);
    if (typeof data.count !== 'number') throw new Error('count 必须是数字');
    console.log(`次数：${data.count}`);
  } catch (error) {
    console.log(`读取失败：${error.message}`);
  }
}
showCounter('demo'); // 预期 次数：3
showCounter('missing'); // 预期 读取失败：示例不存在
```

`async` 函数返回 Promise，`await` 等待结果；`try/catch` 处理失败。连接真实服务时，先阅读官方参数与认证说明，验证返回结构，再决定是否重试。不要把秘密密钥写入公开 HTML、截图或仓库；平台是否提供服务端凭据管理必须查对应官方说明。

## 自己验收，而不是相信一句“成功”

| 操作 | 预期 | 记录 |
| --- | --- | --- |
| 打开 HTML，点击三次 | 0 → 1 → 2 → 3 | 浏览器、日期、实际结果 |
| 刷新 | 回到 0 | 解释为什么没有保存 |
| 改为加二，点击三次 | 6 | 改动位置与结果 |
| 解析数字与字符串 JSON | 4 与 31 | 解释类型区别 |
| 模拟接口成功与失败 | 两种明确提示 | 保留失败测试结果 |

本页的预期结果用于自测，不代表读者已通过验收。仓库自动检查仅验证原始示例的脚本逻辑与模拟 DOM 交互，不替代浏览器视觉、移动端或 Step Studio 实测。

接下来读 [00 需求](00-start.md) → [01 应用](01-create.md) → [03 排错](03-iterate.md)；对 Agent 感兴趣再读 [09 智能体](09-agent.md)，希望复用固定规则则读 [10 技能](10-skill.md)。

技术规范参考：[HTML](https://html.spec.whatwg.org/multipage/)、[CSS](https://www.w3.org/TR/CSS/)、[ECMAScript](https://tc39.es/ecma262/)、[JSON RFC 8259](https://www.rfc-editor.org/rfc/rfc8259)、[HTTP RFC 9110](https://www.rfc-editor.org/rfc/rfc9110)。这些通用标准不证明特定平台能力。
