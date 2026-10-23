// Executes the published snippets, with a small fake DOM (not a browser test).
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
const text = fs.readFileSync(new URL('../courses/official-step-studio/programming-basics.md', import.meta.url), 'utf8');
const html = text.match(/```html\n([\s\S]*?)```/)[1];
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
function counter(code) {
  let click;
  const result = { textContent: '当前次数：0' };
  const button = { addEventListener(event, callback) { assert.equal(event, 'click'); click = callback; } };
  vm.runInNewContext(code, { document: { querySelector(selector) {
    assert.ok(['#add', '#result'].includes(selector));
    return selector === '#add' ? button : result;
  } } });
  for (let i = 0; i < 3; i++) click();
  return result.textContent;
}
assert.equal(counter(script), '当前次数：3');
assert.equal(counter(script.replace('count + 1', 'count + 2')), '当前次数：6');
const json = JSON.parse(text.match(/```json\n([\s\S]*?)```/)[1]);
assert.equal(json.count, 3);
const snippets = [...text.matchAll(/```js\n([\s\S]*?)```/g)].map(m => m[1]);
const output = [];
vm.runInNewContext(snippets[0], { console: { log: x => output.push(x) } });
assert.deepEqual(output, [4, '{"title":"计数练习","count":3}']);
assert.equal(JSON.parse('{"count":"3"}').count + 1, '31');
assert.throws(() => JSON.parse('{"count":3,}'), SyntaxError);
const mock = [];
vm.runInNewContext(snippets[1], { console: { log: x => mock.push(x) } });
await new Promise(resolve => setImmediate(resolve));
assert.deepEqual(mock, ['次数：3', '读取失败：示例不存在']);
console.log('PASS: counter, +2 exercise, JSON/type/error and async success/failure; fake DOM only');
