# 拓麻歌子 · Neo 背屏电子宠物

让 Neo 的 17×23 背屏住进一只可照料、成长和换装的小伙伴。正面是基于 Mori 的养成世界，背面是星跃或方跃；支持从蛋孵化及耳机造型。

当前版本 **0.3.4**，2026-10-20。

[公开源码与完整说明](https://github.com/JiayuStudio/stepx-neo-pet) · [下载 APK](https://github.com/JiayuStudio/stepx-neo-pet/releases/tag/v0.3.4) · [开源来源](https://github.com/JiayuStudio/stepx-neo-pet/blob/main/NOTICE.md) · [返回研究所](../../README.md)

[观看／下载60秒宣传片](https://github.com/JiayuStudio/stepx-neo-pet/releases/download/v0.3.4/tamagochi-promo-60s.mp4)（动画演示，非真机实拍；Suno配乐）

## 自己开发

[背屏点阵开发实战](development.md) · [可复制的 Step Studio 提示词](studio-prompts.md) · [游戏分区](../../sections/games.md)

## 玩法

- 喂食、陪玩、洗澡、休息、拥抱、探索，照顾五项需求。
- 三种成长阶段、三种性格、回忆剧情、日常事件、饰品与房间摆件。
- 接星星、记忆信号两款小游戏，四种主题，存档导出与恢复。
- 背屏根据同一份进度呈现表情、成长、性格和穿戴，共 1014 组角色及孵化动画，另有默认帧。

## 安装和开始陪伴

1. 下载 `diandian-native-0.3.4.apk`，按 Android 提示安装。已有同签名原生版可覆盖升级，建议先在“存档”导出备份。
2. 打开“拓麻歌子”，允许通知，点“开启背屏”。一次只运行一个背屏播放器。
3. 进入“后台设置”，允许后台运行并关闭本应用的电池优化。
4. 把背屏朝上或竖直放置，保持无遮挡，再锁屏观察是否持续动作。
5. 点“停止陪伴”会清帧并恢复之前的系统背屏模式。

游戏离线运行，不需要账号或 API Key。背屏依赖兼容的 STEPX Neo 接口；其他手机不支持。卸载前请导出存档。

## 锁屏动画与限制

原生前台服务独立于正面 WebView 运行。部分 Neo 固件仍会冻结后台进程，需要单独为本应用设置冻结例外；APK 无权自动完成此操作。具体 ADB 命令与撤销方法见[后台说明](https://github.com/JiayuStudio/stepx-neo-pet#锁屏后不动时)。

0.3.4 已完成同签名覆盖安装，用户确认真机背屏动画和锁屏持续运行成功。本轮未记录独立计时的锁屏时长；5600 项点阵检查通过，手机预览安全区及导航布局已实际检查。

长期耗电、脱离充电的深度待机、重启恢复和全部固件组合尚未验证。系统省电、温度、遮挡、强行停止及固件策略变化均可能影响实际显示，不承诺永久后台运行。

## 来源与许可

实际复用 MIT 开源 [Tamagochi / Mori](https://github.com/Tech-aficionado/Tamagochi---Open-Source-Game) 的养成、3D 场景、小游戏与界面，适配中文、原生存档和背屏。背屏会话与构建工具基于 [backscreen-toolbox](https://github.com/stepx-advance/backscreen-toolbox)，脚本引擎为未修改的 Rhino。完整声明见项目 NOTICE；未使用商业拓麻歌子角色或 ROM。
