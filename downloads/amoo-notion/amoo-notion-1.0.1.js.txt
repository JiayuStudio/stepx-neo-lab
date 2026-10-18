#!/usr/bin/env node
'use strict';
// Derived from the user's existing notion-cli.js v1.0.0; see upstream provenance.
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const readline = require('node:readline');
const { Writable } = require('node:stream');
const { randomUUID } = require('node:crypto');

const VERSION = '1.0.1';
const API_BASE = 'https://api.notion.com/v1';
const NOTION_VERSION = process.env.NOTION_VERSION || '2022-06-28';
const CONFIG_DIR = process.env.NOTION_CLI_HOME || (process.platform === 'darwin'
  ? path.join(os.homedir(), 'Documents', 'VPS SSH KEY', 'NotionCLI')
  : path.join(os.homedir(), '.config', 'notion-cli'));
const CONFIG_FILE = path.join(CONFIG_DIR, 'config.json');
const secrets = new Set();
const out = value => process.stdout.write((typeof value === 'string' ? value : JSON.stringify(value, null, 2)) + '\n');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

function redact(value) {
  let text = String(value);
  for (const secret of secrets) if (secret) text = text.split(secret).join('[REDACTED]');
  return text.replace(/\b(?:ntn_|secret_|sk-)[A-Za-z0-9_-]{12,}/g, '[REDACTED]');
}

function loadToken() {
  for (const name of ['NOTION_TOKEN', 'NOTION_API_TOKEN']) {
    const token = process.env[name]?.trim();
    if (token) { secrets.add(token); return { token, source: name }; }
  }
  let stat;
  try { stat = fs.statSync(CONFIG_FILE); }
  catch (error) {
    if (error.code === 'ENOENT') return { token: null, source: null };
    throw new Error('无法读取凭据文件，请检查路径和权限。');
  }
  if (!stat.isFile() || stat.size > 16384) throw new Error('凭据文件格式无效。');
  if (process.platform !== 'win32' && (stat.mode & 0o077)) throw new Error('凭据文件权限过宽，请设为 0600。');
  let cfg;
  try { cfg = JSON.parse(fs.readFileSync(CONFIG_FILE, 'utf8')); }
  catch { throw new Error('凭据文件不是有效 JSON；未显示文件内容。'); }
  const token = typeof cfg?.token === 'string' ? cfg.token.trim() : null;
  if (token) { secrets.add(token); return { token, source: 'config' }; }
  return { token: null, source: null };
}

function writeConfig(token, force) {
  if (!token || /\s/.test(token) || token.length > 4096) throw new Error('请输入一个有效的单行 token。');
  secrets.add(token);
  fs.mkdirSync(CONFIG_DIR, { recursive: true, mode: 0o700 });
  if (fs.lstatSync(CONFIG_DIR).isSymbolicLink()) throw new Error('配置目录不能是符号链接。');
  let existing;
  try { existing = fs.lstatSync(CONFIG_FILE); } catch (error) { if (error.code !== 'ENOENT') throw error; }
  if (existing && (!existing.isFile() || existing.isSymbolicLink())) throw new Error('配置目标必须是普通文件。');
  if (existing && !force) throw new Error('配置已存在。明确要更换 token 时使用 init --force。');
  const temporary = path.join(CONFIG_DIR, `.config-${randomUUID()}.tmp`);
  try {
    fs.writeFileSync(temporary, JSON.stringify({ token }, null, 2) + '\n', { mode: 0o600, flag: 'wx' });
    if (force) fs.renameSync(temporary, CONFIG_FILE);
    else { fs.linkSync(temporary, CONFIG_FILE); fs.unlinkSync(temporary); }
  } finally { if (fs.existsSync(temporary)) fs.unlinkSync(temporary); }
  return CONFIG_FILE;
}

function timeoutMs() {
  const value = Number(process.env.NOTION_TIMEOUT_MS || 30000);
  if (!Number.isInteger(value) || value < 1 || value > 120000) throw new Error('NOTION_TIMEOUT_MS 必须是 1–120000 的整数。');
  return value;
}

function allowedRequest(method, endpoint) {
  const id = '[0-9a-f-]{36}';
  if (method === 'GET') return endpoint === '/users/me' || new RegExp(`^/(?:pages/${id}/markdown|databases/${id}|blocks/${id}/children(?:\\?page_size=\\d+(?:&start_cursor=[^&]+)?)?)$`).test(endpoint);
  return method === 'POST' && (endpoint === '/search' || new RegExp(`^/(?:databases|data_sources)/${id}/query$`).test(endpoint));
}

async function api(method, endpoint, body, version = NOTION_VERSION) {
  if (!allowedRequest(method, endpoint)) throw new Error('此 CLI 仅允许指定的 Notion 读取、搜索和查询端点。');
  if (process.env.NOTION_API_BASE && process.env.NOTION_API_BASE.replace(/\/$/, '') !== API_BASE) throw new Error('凭据仅发送到 https://api.notion.com；不接受自定义 API 地址。');
  if (!/^\d{4}-\d{2}-\d{2}$/.test(version)) throw new Error('NOTION_VERSION 必须为 YYYY-MM-DD。');
  const { token } = loadToken();
  if (!token) throw new Error('未找到 Notion 凭据。请配置 NOTION_TOKEN、NOTION_API_TOKEN，或使用 init --stdin。');
  const timeout = timeoutMs();
  for (let attempt = 0; attempt <= 2; attempt++) {
    let res, data;
    try {
      res = await fetch(API_BASE + endpoint, {
        method, redirect: 'error', signal: AbortSignal.timeout(timeout),
        headers: { Authorization: `Bearer ${token}`, 'Notion-Version': version, 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body)
      });
      const text = await res.text();
      try { data = text ? JSON.parse(text) : {}; }
      catch { throw new Error(`Notion 返回了非 JSON 响应（HTTP ${res.status}）。`); }
    } catch (error) {
      if (error.name === 'TimeoutError' || error.name === 'AbortError') throw new Error(`请求超时（${timeout} ms），未写入 Notion。`);
      throw new Error(redact(error.message || '网络请求失败。'));
    }
    if (res.ok) return data;
    const retry = [429, 500, 502, 503, 504, 529].includes(res.status) && attempt < 2;
    const header = res.headers.get('retry-after');
    const parsed = header === null ? NaN : Number(header);
    const delay = Number.isFinite(parsed) ? parsed * 1000 : (header && Number.isFinite(Date.parse(header)) ? Date.parse(header) - Date.now() : 500 * 2 ** attempt);
    if (retry && delay <= 30000) { await sleep(Math.max(0, delay)); continue; }
    const error = new Error(redact(`Notion API ${res.status} ${data.code || ''} ${data.message || ''}`.trim()));
    error.status = res.status;
    error.code = data.code;
    throw error;
  }
}

function normalizeId(input) {
  let value = String(input || '').trim();
  if (/^https?:\/\//i.test(value)) {
    try { value = decodeURIComponent(new URL(value).pathname).split('/').filter(Boolean).at(-1) || ''; }
    catch { return null; }
    const match = value.match(/(?:^|-)([0-9a-f]{32}|[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})$/i);
    if (!match) return null;
    value = match[1];
  }
  if (!/^(?:[0-9a-f]{32}|[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12})$/i.test(value)) return null;
  const s = value.replace(/-/g, '').toLowerCase();
  return `${s.slice(0, 8)}-${s.slice(8, 12)}-${s.slice(12, 16)}-${s.slice(16, 20)}-${s.slice(20)}`;
}
function requireId(input) { const id = normalizeId(input); if (!id) throw new Error('请提供有效的 Notion 对象 ID 或页面链接。'); return id; }
function plainText(richText) { return typeof richText === 'string' ? richText : (Array.isArray(richText) ? richText.map(t => t.plain_text || t.text?.content || '').join('') : ''); }
function pageTitle(page) { for (const p of Object.values(page.properties || {})) if (p?.type === 'title') return plainText(p.title); return ''; }
function objectTitle(obj) { return obj.object === 'page' ? pageTitle(obj) : plainText(obj.title); }
function fmtTime(iso) { return iso ? String(iso).replace('T', ' ').replace(/\.\d+Z$/, ' UTC') : ''; }

function parseFlags(argv) {
  const flags = {}, rest = [];
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--') { rest.push(...argv.slice(i + 1)); break; }
    if (a === '--json' || a === '--stdin' || a === '--force') flags[a.slice(2)] = true;
    else if (a === '--help' || a === '-h') flags.help = true;
    else if (a === '--limit') {
      const value = argv[++i];
      if (!/^\d+$/.test(value || '') || Number(value) < 1 || Number(value) > 10000) throw new Error('--limit 必须是 1–10000 的整数。');
      flags.limit = Number(value);
    } else if (a === '--type') {
      flags.type = argv[++i];
      if (!['page', 'database', 'data_source'].includes(flags.type)) throw new Error('--type 必须为 page、database 或 data_source。');
    } else if (a.startsWith('-')) throw new Error(`未知选项：${a}`);
    else rest.push(a);
  }
  return { flags, rest };
}

async function paginated(method, endpoint, body, limit, version) {
  const results = [], seen = new Set();
  let cursor, response, pages = 0;
  do {
    if (++pages > 1000) throw new Error('分页超过 1000 次，已停止；请缩小查询范围。');
    const pageSize = Math.min(100, limit - results.length);
    const params = { ...body, page_size: pageSize, ...(cursor ? { start_cursor: cursor } : {}) };
    const target = method === 'GET' ? `${endpoint}?${new URLSearchParams(params)}` : endpoint;
    response = await api(method, target, method === 'GET' ? undefined : params, version);
    results.push(...(response.results || response.blocks || []));
    if (!response.has_more) break;
    cursor = response.next_cursor;
    if (!cursor || seen.has(cursor)) throw new Error('Notion 返回无效的分页游标；已停止，避免重复读取。');
    seen.add(cursor);
  } while (results.length < limit);
  return { ...response, results: results.slice(0, limit) };
}
function noteMore(res) { if (res.has_more) process.stderr.write('提示：还有更多结果，可增大 --limit（最多 10000）；本次并非完整列表。\n'); }

async function cmdSelf(flags) {
  const me = await api('GET', '/users/me');
  if (flags.json) return out(me);
  out(`Integration : ${me.name || '(未命名)'}`);
  if (me.bot?.workspace_name) out(`Workspace   : ${me.bot.workspace_name}`);
  out(`User ID     : ${me.id}`); out(`Type        : ${me.type}`);
}
async function cmdSearch(flags, rest, find) {
  const body = find ? { query: rest.join(' ') } : {};
  const modern = NOTION_VERSION >= '2025-09-03';
  let type = flags.type;
  if (type === 'database' && modern) type = 'data_source';
  if (type === 'data_source' && !modern) throw new Error('--type data_source 需要 NOTION_VERSION=2025-09-03 或更新版本。');
  if (type) body.filter = { property: 'object', value: type };
  const res = await paginated('POST', '/search', body, flags.limit || (find ? 20 : 100));
  if (flags.json) return out(res);
  noteMore(res);
  if (!res.results.length) return out(find ? '没有标题匹配的对象；Notion 搜索不覆盖正文。' : '没有可见对象，请在 Notion 页面 Connections 中授权。');
  out(`共 ${res.results.length} 个对象\n`);
  for (const item of res.results) {
    out(`[${item.object === 'page' ? '页面' : '数据库/数据源'}] ${objectTitle(item) || '(无标题)'}`);
    out(`        ${item.id}  更新于 ${fmtTime(item.last_edited_time)}`);
  }
}
async function cmdRead(flags, rest) {
  const res = await api('GET', `/pages/${requireId(rest[0])}/markdown`);
  if (flags.json) return out(res);
  if (res.truncated) process.stderr.write('提示：Notion 标记正文已截断，本次不是完整页面。\n');
  out(res.markdown || '(空页面)');
}
async function cmdTree(flags, rest) {
  const res = await paginated('GET', `/blocks/${requireId(rest[0])}/children`, {}, flags.limit || 100);
  if (flags.json) return out(res);
  noteMore(res);
  if (!res.results.length) return out('(没有子块)');
  out(`共 ${res.results.length} 个直接子块\n`);
  for (const block of res.results) {
    const type = block.type || 'unknown', data = block[type] || {};
    out(`  [${type}] ${plainText(data.title || data.rich_text || data.text) || '(无标题)'}`);
    if (block.id) out(`          -> id: ${block.id}${block.has_children ? '（含子块）' : ''}`);
  }
}
async function queryModernDatabase(id, limit) {
  const version = NOTION_VERSION >= '2025-09-03' ? NOTION_VERSION : '2025-09-03';
  let db;
  try { db = await api('GET', `/databases/${id}`, undefined, version); }
  catch (error) {
    if (error.status !== 404) throw error;
    return paginated('POST', `/data_sources/${id}/query`, {}, limit, version);
  }
  if (db.data_sources?.length !== 1) throw new Error('该数据库没有唯一数据源。请提供数据源 ID 并加 --type data_source。');
  return paginated('POST', `/data_sources/${requireId(db.data_sources[0].id)}/query`, {}, limit, version);
}
async function cmdDb(flags, rest) {
  const id = requireId(rest[0]), limit = flags.limit || 20;
  let res;
  if (flags.type === 'data_source') res = await paginated('POST', `/data_sources/${id}/query`, {}, limit, NOTION_VERSION >= '2025-09-03' ? NOTION_VERSION : '2025-09-03');
  else if (NOTION_VERSION >= '2025-09-03') res = await queryModernDatabase(id, limit);
  else {
    try { res = await paginated('POST', `/databases/${id}/query`, {}, limit); }
    catch (error) {
      if (![400, 404].includes(error.status)) throw error;
      res = await queryModernDatabase(id, limit);
    }
  }
  if (flags.json) return out(res);
  noteMore(res);
  if (!res.results.length) return out('(没有记录)');
  out(`共 ${res.results.length} 条记录\n`);
  for (const row of res.results) {
    out(`- ${pageTitle(row) || '(无标题)'}`);
    const extras = [];
    for (const [key, p] of Object.entries(row.properties || {})) {
      if (!p || p.type === 'title') continue;
      let value = '';
      if (p.type === 'rich_text') value = plainText(p.rich_text);
      else if (p.type === 'select') value = p.select?.name || '';
      else if (p.type === 'multi_select') value = (p.multi_select || []).map(x => x.name).join(',');
      else if (p.type === 'date' && p.date) value = p.date.start + (p.date.end ? ` → ${p.date.end}` : '');
      else if (p.type === 'number') value = p.number === null ? '' : String(p.number);
      else if (p.type === 'status') value = p.status?.name || '';
      if (value) extras.push(`${key}=${value}`);
    }
    if (extras.length) out(`    ${extras.join('  ')}`);
  }
}
async function cmdInit(flags) {
  let token;
  if (flags.stdin) {
    let input = '';
    for await (const chunk of process.stdin) {
      input += chunk.toString('utf8');
      if (Buffer.byteLength(input) > 8192) throw new Error('输入过长，未写入配置。');
    }
    token = input.trim();
  } else {
    if (!process.stdin.isTTY) throw new Error('非交互环境，请用 init --stdin，从私有凭据文件重定向输入。');
    // Suppress terminal echo so a pasted token never appears in the transcript.
    const muted = new Writable({ write(chunk, encoding, callback) { callback(); } });
    const rl = readline.createInterface({ input: process.stdin, output: muted, terminal: true });
    process.stderr.write('输入 Notion token（隐藏输入）：');
    try { token = await new Promise(resolve => rl.question('', resolve)); }
    finally { rl.close(); process.stderr.write('\n'); }
    token = token.trim();
  }
  const file = writeConfig(token, flags.force);
  out(flags.json ? { configured: true, config_file: file } : `已写入 ${file}（POSIX 权限 0600）`);
}
function cmdConfig(flags) {
  const { token, source } = loadToken();
  const info = { config_file: CONFIG_FILE, credential_source: source, configured: Boolean(token), notion_version: NOTION_VERSION };
  if (flags.json) return out(info);
  out(`配置文件 : ${CONFIG_FILE}\n凭据来源 : ${source || '(未找到)'}\n凭据状态 : ${token ? '已配置（不显示内容）' : '未配置'}\nAPI 版本 : ${NOTION_VERSION}`);
}

const HELP = `amoo-notion v${VERSION} — Notion 只读 CLI

  self                         查看连接身份
  ls [--type page|database|data_source] [--limit N]
                               列出已授权对象（默认上限 100）
  find <标题关键词> [--limit N] 按标题搜索（默认 20，不搜正文）
  read <id|链接>               读取 Markdown（服务端截断时会提示）
  tree <id|链接> [--limit N]    列出直接子块（默认 100，不递归）
  db <id|链接> [--limit N] [--type database|data_source]
                               查询记录（默认 20）
  init [--stdin] [--force]      写入本地凭据（不写 Notion）
  config                       检查凭据来源，不显示 token

通用选项：--json、--help；--limit 为 1–10000。
--version 显示版本。搜索关键词以 - 开头时使用 -- 分隔。
凭据：NOTION_TOKEN > NOTION_API_TOKEN > 本地 config.json。
NOTION_CLI_HOME 可指定配置目录；不会访问系统钥匙串。
NOTION_VERSION 默认 2022-06-28，保留既有 Amoo 兼容性。
`;

async function main(argv = process.argv.slice(2)) {
  const json = argv.includes('--json');
  try {
    const cmd = argv[0];
    if (!cmd || ['help', '-h', '--help'].includes(cmd)) return out(HELP);
    if (['-v', '--version'].includes(cmd)) return out(VERSION);
    const { flags, rest } = parseFlags(argv.slice(1));
    if (flags.help) return out(HELP);
    const commands = ['self', 'ls', 'find', 'read', 'tree', 'db', 'init', 'config'];
    if (!commands.includes(cmd)) throw new Error(`未知命令：${cmd}。运行 amoo-notion help 查看用法。`);
    if (['read', 'tree', 'db'].includes(cmd) ? rest.length !== 1 : (cmd === 'find' ? rest.length === 0 : rest.length !== 0)) throw new Error('命令参数数量不正确，请查看 --help。');
    if ((flags.stdin || flags.force) && cmd !== 'init') throw new Error('--stdin 和 --force 仅用于 init。');
    if (flags.limit && !['ls', 'find', 'tree', 'db'].includes(cmd)) throw new Error('此命令不支持 --limit。');
    if (flags.type && (!['ls', 'db'].includes(cmd) || (cmd === 'db' && flags.type === 'page'))) throw new Error('此命令不支持该 --type。');
    switch (cmd) {
      case 'self': return await cmdSelf(flags);
      case 'ls': return await cmdSearch(flags, rest, false);
      case 'find': return await cmdSearch(flags, rest, true);
      case 'read': return await cmdRead(flags, rest);
      case 'tree': return await cmdTree(flags, rest);
      case 'db': return await cmdDb(flags, rest);
      case 'init': return await cmdInit(flags);
      case 'config': return cmdConfig(flags);
    }
  } catch (error) {
    const messages = { 401: '认证失败（401）：token 无效或已撤销。', 403: '权限不足（403）：请检查连接能力与页面授权。', 404: '未找到（404）：对象不存在或没有授权。', 429: '仍被限流（429）：请稍后重试。' };
    const message = redact(messages[error.status] || error.message);
    process.stderr.write((json ? JSON.stringify({ error: message, status: error.status || null }) : `错误：${message}`) + '\n');
    process.exitCode = 1;
  }
}
module.exports = { main, normalizeId, allowedRequest };

if (require.main === module) main();
