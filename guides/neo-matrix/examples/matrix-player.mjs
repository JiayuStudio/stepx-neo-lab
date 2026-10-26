/* Minimal foreground player. No display mode, brightness or background changes. */
export const WIDTH = 17, HEIGHT = 23, CELLS = WIDTH * HEIGHT;

export function validateFrame(frame) {
  if (!Array.isArray(frame) || frame.length !== CELLS ||
      frame.some(v => !Number.isInteger(v) || v < 0 || v > 255))
    throw new Error('一帧必须是 391 个 0–255 整数');
  return frame;
}

// Accept the SDK envelope or MCP text-JSON envelope, but never ignore explicit errors.
export function checkReply(reply) {
  if (reply == null) throw new Error('设备没有返回结果');
  const seen = new Set();
  function visit(value, depth = 0) {
    if (depth > 8 || value == null) return;
    if (typeof value === 'string') {
      let parsed;
      try { parsed = JSON.parse(value); } catch { return; }
      return visit(parsed, depth + 1);
    }
    if (typeof value !== 'object' || seen.has(value)) return;
    seen.add(value);
    if (value.success === false || value.isError === true || value.error)
      throw new Error('背屏接口报告失败，请检查连接器、设备与授权');
    for (const key of ['result', 'data', 'structuredContent', 'content', 'text'])
      if (Object.hasOwn(value, key)) visit(value[key], depth + 1);
    if (Array.isArray(value)) for (const item of value) visit(item, depth + 1);
  }
  visit(reply);
  return reply;
}

export async function connectMcp(mcp) {
  if (!mcp || typeof mcp.connect !== 'function' || typeof mcp.callTool !== 'function')
    throw new Error('未找到官方 MCP 宿主；请在 Neo 的 Step Studio 应用中运行');
  const reply = await mcp.connect();
  if (reply != null) checkReply(reply);
  return mcp;
}

export function makeBuddy(t = 0) {
  // Original smiling robot, deliberately separate from the game's character assets.
  const out = Array(CELLS).fill(0), dy = Math.round(Math.sin(t * 3) * .8);
  const dot = (x, y, v = 230) => {
    y += dy;
    if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) out[y * WIDTH + x] = v;
  };
  for (let y = 6; y <= 16; y++) for (let x = 3; x <= 13; x++) {
    if ((y === 6 || y === 16) && (x === 3 || x === 13)) continue;
    dot(x, y, 200);
  }
  dot(8, 3); dot(8, 4); dot(8, 5);
  const blink = t % 4.8 < .16;
  for (const x of [6, 10]) {
    dot(x, 9, 0); dot(x, 10, 0);
    dot(x, 11, blink ? 0 : 245);
  }
  dot(7, 13, 0); dot(8, 14, 0); dot(9, 13, 0);
  for (const x of [2, 14]) for (let y = 10; y <= 13; y++) dot(x, y, 160);
  for (const x of [5, 11]) { dot(x, 17); dot(x, 18); }
  return out;
}

export class MatrixPlayer {
  constructor({mcp = null, preview = () => {}, status = () => {}} = {}) {
    this.mcp = mcp; this.preview = preview; this.status = status;
    this.token = 0; this.job = Promise.resolve(); this.actions = Promise.resolve();
    this.dirty = false; this.timer = null; this.wake = null;
  }
  cancelTimer() {
    clearTimeout(this.timer); this.timer = null;
    if (this.wake) { const wake = this.wake; this.wake = null; wake(); }
  }
  queue(fn) {
    const action = this.actions.catch(() => {}).then(fn);
    this.actions = action;
    return action;
  }
  async drainAndClear() {
    await this.job; // Never clear before a pending frame has returned.
    if (this.dirty) {
      checkReply(await this.mcp.callTool('clear_back_screen_frame', {}));
      this.dirty = false;
    }
  }
  start({device = false, fps = 4, frameAt = makeBuddy} = {}) {
    if (!Number.isFinite(fps) || fps < 1 || fps > 10)
      return Promise.reject(new Error('此调试播放器接受 1–10 次/秒，默认 4；不是硬件上限'));
    const token = ++this.token;
    this.cancelTimer();
    return this.queue(async () => {
      await this.drainAndClear();
      if (token !== this.token) return;
      if (device) await connectMcp(this.mcp);
      if (token !== this.token) return;
      const started = performance.now();
      this.status(device ? '设备通道播放中（实际显示请观察背面）' : '模拟预览播放中');
      this.job = (async () => {
        try {
          while (token === this.token) {
            const frame = validateFrame(frameAt((performance.now() - started) / 1000));
            if (device) {
              this.dirty = true; // Failure can still have reached the device; cleanup remains required.
              checkReply(await this.mcp.callTool('set_back_screen_frame', {frame}));
            }
            if (token !== this.token) break;
            this.preview(frame);
            await new Promise(resolve => {
              this.wake = resolve;
              this.timer = setTimeout(() => { this.wake = null; resolve(); }, 1000 / fps);
            });
          }
        } catch (error) {
          if (token === this.token) this.token++;
          this.status('播放失败：' + error.message + '；请停止清除后重试');
          // Do not pretend the hardware was cleared. stop() remains available.
        }
      })();
    });
  }
  stop() {
    ++this.token; this.cancelTimer();
    return this.queue(async () => {
      await this.drainAndClear();
      this.preview(Array(CELLS).fill(0));
      this.status('已停止；自定义帧已清除（模拟通道只清预览）');
    });
  }
}
