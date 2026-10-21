# 背屏达人 4.0

[社区应用](../../sections/community-apks.md) · [研究所首页](../../README.md) · [使用教程](usage.md)

把 GIF、文字、表情和手绘图案变成 Neo 背屏上的像素动画。**作者：鸿渐 · 来源：社区群 · 社区作品，非 JIAYU STUDIO 自研应用。** 本页由 3.0 更新为 4.0，保留[历史记录](history-3.0.md)。

## 核心功能：在线 URL 解析背屏动画

复制可直接下载的 **HTTPS GIF 链接**，粘贴到“动画”页，点击“从在线 URL 导入 GIF”，完成转换后播放。应用把动画转换为 17×23 点阵，支持灰度、黑白、反色与完整适配/铺满裁切。

**粘贴链接 → 解析动画 → 背屏播放。** 这里指 GIF 文件直链，尚不支持把任意作品网页当作 GIF 直接解析。转换取前 30 秒、每秒 12 帧，最多 360 帧。

[![背屏达人 4.0：在线 URL 解析背屏动画](images/promo-4.0.jpg)](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/community-backscreenfun-v4.0/backscreenfun-4.0-promo-60s.mp4)

**[播放 / 下载 60 秒宣传片](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/community-backscreenfun-v4.0/backscreenfun-4.0-promo-60s.mp4)** · [版本资料](https://github.com/JiayuStudio/stepx-neo-lab/releases/tag/community-backscreenfun-v4.0)

宣传片用 12 秒介绍在线 URL 核心流程，沿用应用的深色、青绿与橙黄风格，配完整 Suno 音乐。画面由应用界面实录和绘制点阵组成，不含外置摄像头画面；点阵演绎不作为硬件性能证据。

## 本次实测

**[下载图文实机测试报告 PDF](https://github.com/JiayuStudio/stepx-neo-lab/releases/download/community-backscreenfun-v4.0/backscreenfun-4.0-device-report.pdf)**：9 页，含实体背屏照片、媒体导入、音乐律动、熄屏条件、帧率及后续补充。

| 功能 | 2026-10-21 结果 |
| --- | --- |
| 表情、中文逐字、手绘、宠物代表动作 | 代表图案有实体背屏照片 |
| 在线 GIF 直链 | 8 帧加载环导入及物理背屏变化通过；另有链接下载超时 |
| Tenor 篮球 GIF | 页面与媒体直链在手机连接阶段均超时；电脑下载后本地转换为 29 帧，应用循环播放；本次补测未拍摄该 GIF 的实体背屏 |
| 本地 GIF / MP4 / PNG | 导入通过；35 秒、60 FPS 视频转换为 360 帧 |
| 音乐律动 | 拒绝、允许、声音响应与停止释放捕获已检查，实体背屏可见频谱变化 |
| 熄屏动画 | 临时“常亮”、关闭相机联动且接电条件下持续约 64.7 秒；不外推到默认设置或深度待机 |
| 应用更新循环 | 数字雨采样约 16.16 次/秒；不是物理面板扫描刷新率 |
| 待验 | 倾斜重力响应、全部生肖、真实插拔充电、B 站轮询、长时耗电与多设备兼容 |

## 安全状态与获取

**❗ 安全风险，4.0 APK 暂停公开下载。** 新版为部分写入操作加入配对令牌，但状态和服务诊断读取仍未鉴权，可能返回用户配置的账号字段或状态消息；该返回路径由代码审查和虚构数据隔离测试确认。未经授权的状态重置也已在实机复现。[查看 4.0 详细安全报告](security-4.0.md)。

本页保留功能资料、测试报告与宣传片，不提供 4.0 安装包下载，也不授予“已通过哆啦A梦安全验证✅”。功能验证与安全结论分别记录。原始 APK 未修改、重签或重新构建；源码和源码许可尚未提供。

<details>
<summary>版本与安装包校验信息</summary>

- 显示版本：4.0；versionCode：4；包名：`com.amoo.backscreenfun`。
- 文件大小：370,823 字节。
- SHA-256：`3dc9333bf7ddada0fd10f6ffab255152c6017edb13856003e0bce472c373918e`。
- APK v2/v3 签名完整性通过；产品通用证书名称不单独证明作者身份。
- 最低 Android 8.0；背屏功能需要 Neo 对应系统接口。
- 版本以包内清单为准，原附件名中的“1.1”不是包内版本。

</details>

## 历史

[3.0 收录记录](history-3.0.md) · [3.0 安全报告](security-3.0.md)。历史记录仅对应当时的文件和验证范围，不代表 4.0 结论，也不作为绕过新版安全限制的建议。
