# 拓麻歌子 · Neo 背屏电子宠物

让 Neo 的17×23背屏住进一只可照料、成长和换装的小伙伴。当前版本 **0.3.8**，2026-10-24。

![拓麻歌子应用图标](assets/app-icon.png)

[公开源码与完整说明](https://github.com/JiayuStudio/stepx-neo-pet) · [下载0.3.8 APK](https://github.com/JiayuStudio/stepx-neo-pet/releases/tag/v0.3.8) · [更新记录](https://github.com/JiayuStudio/stepx-neo-pet/blob/main/CHANGELOG.md) · [来源与许可](https://github.com/JiayuStudio/stepx-neo-pet/blob/main/NOTICE.md)

## 可以做什么

- 八位原创点阵小伙伴：星跃、方跃、芽灵、绒兔、焰狐、泡獭、眠鸮、角龙。
- 每只拥有15个动画场景、每场景8帧，共120段基础动画，包含走动、吃饭、玩耍、洗澡、睡觉、抚摸、开心，以及饥饿、不开心、生病、死亡和孵化过程。
- 喂食、陪玩、洗澡、休息、拥抱与探索；五项需求影响状态，保留成长、性格、亲密度、回忆和饰品。
- 正面像素预览与原生背屏共用养成进度；包含接星星、记忆信号小游戏和本地备份恢复。
- 暖纸色背景、彩色块、马克笔描边与硬阴影，配套粉色电子宠物机和绿色像素萌宠图标。

## 0.3.8 使用与更新

黄色导航位于标题栏下方，滚动时吸附顶部；“检查更新”位于页面最后的存档区之后。可检查GitHub正式版并打开安装下载入口。

下载安装包后按Android提示安装。同签名版本可以覆盖更新，升级或卸载前建议导出存档。开启背屏需兼容的STEPX Neo接口；进入后台设置后按系统实际提示配置，再检查锁屏显示。停止陪伴会清帧并恢复之前的系统背屏模式。游戏本地运行，不需要账号或API Key。

本版Web/Android构建、342px与390px浏览器布局、五个导航锚点避让和更新检测通过。0.3.7可发现0.3.8，0.3.8显示当前最新。**0.3.8安装、真实背屏与锁屏仍待实机复验**；历史0.3.4曾获用户真机确认，不能代替新版验收。不同固件、省电、温度、遮挡和后台策略均可能影响显示；长期耗电、深度待机和重启恢复待验证。

## 演示与开发

[历史0.3.4版60秒宣传片](https://github.com/JiayuStudio/stepx-neo-pet/releases/download/v0.3.4/tamagochi-promo-60s.mp4)为旧版角色动画演示，不代表本版八宠物界面或真机实拍。

[背屏点阵开发实战](development.md)与[Step Studio提示词](studio-prompts.md)保留0.3.4固定版本案例；当前实现以0.3.8源码为准。原生后台方案使用Android APK；浏览器预览不能驱动真实背屏。

## 来源与许可

实际复用MIT开源 [Tamagochi / Mori](https://github.com/Tech-aficionado/Tamagochi---Open-Source-Game) 的养成、场景、小游戏和界面，适配中文、本地存档与Neo背屏。背屏会话与构建工具参考 [backscreen-toolbox](https://github.com/stepx-advance/backscreen-toolbox)，脚本引擎为未修改的Rhino。完整声明见项目NOTICE；未使用商业拓麻歌子角色或ROM。
