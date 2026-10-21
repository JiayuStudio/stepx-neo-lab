# 背屏达人安全修订 4.0.1 · 使用与在线 GIF 教程

[应用详情与安全状态](README.md) · [图文实机报告](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/community-backscreenfun-v4.0/backscreenfun-4.0-device-report.pdf)

[下载安全修订版 4.0.1](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/community-backscreenfun-v4.0.1-secure/backscreenfun-4.0.1-secure.apk)，采用独立包名，可与原版并存。下列媒体操作来自 4.0 实测，4.0.1 沿用相关实现，新版运行复测尚未完成；安全修改与验证范围见[报告](security-4.0.1.md)。原版 4.0 APK 仍暂停下载。

## 在线 URL 解析背屏动画

1. 打开“自定义背屏 → 动画”。
2. 先选择完整适配或铺满裁切，以及灰度、黑白与反色参数。
3. 粘贴可直接下载的 **HTTPS GIF 文件链接**。
4. 点击“从在线 URL 导入 GIF”，等候下载和转换完成。
5. 点击“播放已转换动画”。结束点击“停止”；交还显示时点击“恢复系统显示”。

实测成功示例：[8 帧加载环 GIF](https://www.runoob.com/try/demo_source/demo_wait.gif)。64×64 简单图形在点阵中容易辨认；复杂人物或背景压缩到 17×23 后会损失大量细节。

下载要求 HTTPS、无网址内嵌账号密码，并拒绝常见本机及私网地址；上限 16 MiB，每次连接和读取超时为 10 秒，整体期限约 60 秒。能否下载仍取决于手机网络与来源站点。转换取前 30 秒、每秒 12 帧，最多 360 帧；参数应在导入前选择。

## Tenor 页面能否直接使用

2026-10-21 补测了指定的[Tenor 篮球 GIF 页面](https://tenor.com/zh-CN/view/%E8%94%A1%E5%BE%90%E5%9D%A4%E7%B1%83%E7%90%83-cai-xukun-basketball-gif-14005881)。

| 路径 | 结果 |
| --- | --- |
| 直接输入作品页面 | 手机连接超时，未进入可判定的解码阶段；电脑确认返回 HTML |
| 输入页面内 GIF 媒体直链 | 手机连接仍超时；电脑可下载有效 GIF |
| 电脑下载后本地导入 | 成功转换为 29 帧，应用循环播放；本次补测未拍摄该 GIF 的实体背屏 |

源文件为 GIF、500×350、24 个源帧、300,246 字节。代码路径是下载输入网址后直接送入媒体解码器，未发现 Tenor 页面元数据提取器。目前应使用 GIF 文件直链，不能宣传为支持任意网页自动解析；一次连接超时也不能证明解码器不支持该文件。

## 其他玩法与排障

- **表情与文字**：选择表情或输入简短文字，核对预览后查看实体背屏。
- **点绘**：从已亮点起笔会擦除，从空白点起笔会点亮，注意当前笔刷行为。
- **本地 GIF / MP4 / PNG**：使用系统选择器，无需全盘文件权限。
- **音乐律动**：需要当次系统录屏/播放音频捕获授权；结束后确认捕获释放。
- **流沙与重力**：落沙和堆积已观察；与手机倾斜同步的响应仍待取得可判定证据。

“停止”可能保留最后图案，“恢复系统显示”用于交还控制。自动模式、姿态、相机联动和系统显示策略会影响点亮；本次熄屏通过只对应报告列明的临时条件。

安全修订版已为所有本机状态接口加入配对认证。背屏上的“批准 / 完成”是显示状态，不是执行授权。


## 电脑助手配对（4.0.1）

在 Xspace 查看本机生成的 32 位令牌，仅交给可信助手。用 USB 转发本机端口后，读取使用 GET，修改使用 POST；所有路径均需凭据。旧的外部广播已关闭。

```sh
adb forward tcp:47321 tcp:47321
curl -H "Authorization: Bearer $BSF_TOKEN" http://127.0.0.1:47321/status
curl -X POST -H "Authorization: Bearer $BSF_TOKEN" -d 'tool=codex&state=waiting' http://127.0.0.1:47321/status
curl -X POST -H "Authorization: Bearer $BSF_TOKEN" http://127.0.0.1:47321/quit
```

`BSF_TOKEN` 是当前终端中私下设置的配对令牌变量；示例不含实际令牌。更换令牌后旧值立即失效；鉴权失败返回 401，GET 修改返回 405。`/feed` 不返回账号或原始诊断。联动测试结束，移除自己的 USB 转发：`adb forward --remove tcp:47321`。
