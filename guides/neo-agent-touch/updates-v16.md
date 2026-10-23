# 0.1.16：共享比价目标与一键检查更新

旧规则要求每个应用阶段拥有各自的完整原文子句。连续的“京东淘宝拼多多”后面共用一个查价目标时，旧解析器漏掉应用名单，三个source起点相同，第二步便触发顺序拒绝；这是表达契约缺口。

现在只为“完整并列应用名单＋唯一共同查价商品”的整句建立本地共享范围。SharedAppSourceRules核实并列名、精确型号和原文限制，SharedComparisonPlan按本机解析的名单编译；模型省略、颠倒或改写目标不能改变这个范围。TaskSpec再次从原文重派生目标；恢复时重查完整名单与外层搜索目标。共享只读标记不从模型或保存的JSON获取，TaskRequest和PurchaseRuntime都关闭交易例外。

独立的“淘宝搜手机→WPS写合同→淘宝再看商品”仍是三个有各自来源的阶段，不共享授权。二选一、否定平台、引用教程、不同商品、真实购买或加购请求不会套用这个共享查价范围。没有授权或可启动应用时整批不先执行，不能静默漏掉平台。

## 更新入口

菜单「检查更新」或关于页「一键检查更新」读取固定官方通道。有效清单直接给出通道版本；主源不可用时再查询公开GitHub releases，包含Beta，排除影片和源码发布。网络失败或限流不会显示“已是最新版”。检查不需要GitHub Token，不传设备标识。

用户选择更新后，AppUpdater下载并校验大小、SHA-256、包名、精确versionName、递增versionCode和当前签名，AppUpdateStore只保存一个验证包，UpdateApkProvider仅向系统安装器临时提供该文件的只读URI。安装来源许可和安装确认由Android处理；任务运行、规划或页面不在前台时不会抢占安装。生命周期恢复再次核验。

公开清单位置为本条目下update.json。发布维护者可使用源码中的tools/generate-update-feed.py从已签名APK读取包名、版本、大小和摘要。先发布APK并核对公开下载，再更新main的清单；以后版本必须增加versionCode并使用同一签名。Agent可以读取清单判断版本，阅读资料本身不授予设备操作权限。

构建接线与参考见[source/tests/update-integration.md](source/tests/update-integration.md)。[AndroidAppUpdater](https://github.com/HeyPouya/AndroidAppUpdater)（Apache-2.0）仅参考检查/提示/确认流程，未复制代码或加入库；网络数据参考[GitHub Releases API](https://docs.github.com/en/rest/releases/releases)，安装与签名使用Android原生PackageManager和ContentProvider。

## 验证范围

44套纯规则通过，149项京东/淘宝原生构造、原生JSON恢复及异常队列拒绝通过；零模型调用、零第三方业务动作，授权不变。拼多多缺少现有授权和启动入口，三平台夹具未运行。真实多平台报价、系统APK升级确认和下一版本升级后的数据保留仍需分别实测。最新机器可读结果见[verification-v16.json](verification-v16.json)。
