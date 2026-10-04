# 简单bili小工具 1.0.0 验证记录

日期：2026-10-04。包名 `com.weinaoa.easybilitool`，versionCode 1，versionName 1.0.0。

本文保留首次拆分时的本地验证记录。后续实机发现设置持久化问题，已在 1.0.1 修复并完成实机回归；最终结果见 [VALIDATION-v101.md](VALIDATION-v101.md)。`apk-audit.json` 与发布产物已更新为 1.0.1。

## 分离范围

从 Bili 小工具 0.14.5 提取沉浸状态栏、原生底栏随滑动收起、沉浸导航栏及必要的设置和 libxposed 适配代码，建立独立的 `easybilitool` 项目。

- 沉浸状态栏保留视频详情、播放列表与直播间适配；视频菜单保留固定 80px 附加间距，直播头栏按真实状态栏与刘海安全区避让。
- 底栏收起保留首页 AppBar 联动、关注/动态/会员购/我的实际滚动，以及动态正文、动态评论和视频评论适配。独立提取 `DetailBarLocator`；保留评论页实际页签判断及切页后清零。
- 导航栏保留主界面、原生设置页、视频/动态详情、直播间和通用竖屏页面的底部安全区处理，移除悬浮模式强制沉浸分支。
- 设置只包含三个布尔开关，默认全部关闭，采用草稿、保存并重启及未保存退出提示。配置文件、设置入口 key、启动 action/extra 均使用新模块标识。
- 桌面介绍、模块描述、哔哩哔哩设置入口、设置页及 README 使用简单版名称与说明。沿用现有图标。
- 简单版没有项目 LICENSE、NOTICE、许可页面或源码 SPDX 标记。没有复制上游参考仓库或历史验证资料。

## 本地验证

- JVM 回归共 **69 项**：配置/草稿/首页联动 24、主界面收起 13、详情收起 17、现代 Hook 与反射 15。覆盖三个开关全部八种组合、编辑与已保存状态隔离、放弃修改、无效设置、滚动方向/惯性/超时、切页及 Hook 回调异常回退。
- `assembleDebug` 与 `lintDebug` 成功，**0 errors、18 warnings**。提示来自既有内部 API/资源访问、目标 SDK 和数据提取规则；没有编译错误。构建日志见 `build-v100.log`。
- `aapt dump badging` 确认应用名称“简单bili小工具”、包名 `com.weinaoa.easybilitool`、版本 1.0.0、minSdk 26、targetSdk 35。
- APK ZIP、DEX 字符串与类定义核对通过；24 个 Java 源文件对应所需模块代码，没有弹幕合并、空降、全屏手势、勋章、统计、悬浮/玻璃特效或旧包名残留。
- APK 中 Xposed 入口为 `com.weinaoa.easybilitool.EasyBiliToolModule`，作用域只有 `tv.danmaku.bili`；没有配置 provider 或额外权限。libxposed API JAR 仅用于编译，没有打入 APK。详见 `apk-audit.json`。
- `apksigner verify --print-certs` 通过，采用本机 Android 调试签名。
- 源码包仅包含独立项目的源码、资源、编译依赖、Gradle wrapper、测试与本次文档；排除 local.properties、构建缓存、父项目、研究资料和历史功能代码。附 SHA-256 校验文件。

## 验证限制

本次没有安装模块、操作手机或进行实机界面测试。此前完整版的设备验证用于选择提取基线，不能视为简单版 APK 的实测结果。设置入口、三个开关、保存重启和页面布局仍需在 LSPosed 与哔哩哔哩 8.95.0 上验证。
