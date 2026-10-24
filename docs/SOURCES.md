# 源码来源清单

初始整理日期：2026-10-04；最近补充：2026-10-24。当前对象：简单bili小工具 1.1.12 适配分支候选，包名 `com.weinaoa.easybilitool`。当前有 51 个模块 Kotlin 文件、12 组 JVM 检查；下方 1.1.0 清单保留为来源历史，后续新增实现按版本另列。

**主许可证：Mulan PubL v2。** 维护者于 2026-10-04 明确选择以该协议公开简单版的自有实现、文档和图标，Copyright (c) 2026 weinaoa，完整文本见 [LICENSE](../LICENSE)。第三方依赖保留各自许可。本清单记录“参考”“移植”“改写”和“原创”的不同来源范围；重新授权依据版权人的选择，不依据包名或文件头是否改变。

## 1.1.0 初始清单：29 个 Kotlin 文件

简单版从自用 BiliTool 中提取并裁剪三个原生界面功能，同时重建简单设置与配置。1.1.0 将原有 26 个 Java 文件迁移至 Kotlin，并新增窗口可见状态、安全区基线及数值兼容辅助文件。以下是本次保留文件及现存记录能支持的来源范围。

| 文件 | 来源与修改范围 |
| --- | --- |
| `ImmersiveStatusBarHook.kt` | 原模块透明状态栏逻辑裁剪；原 0.8.3 记录为参考 BBZQ APK 的透明状态栏机制，独立适配哔哩哔哩 8.95 的 inset 与图标覆盖；保留视频顶部菜单间距和直播安全区处理 |
| `HomeBottomBarHook.kt`、`MainTabBarScroll.kt` | 原模块首页原生底栏收起与滚动状态逻辑；去掉玻璃和其他底栏功能分支 |
| `DetailBottomBarHook.kt`、`DetailBarScroll.kt` | 原模块动态详情、视频评论的原生底栏收起逻辑；去掉悬浮底栏配置分支 |
| `DetailScrollViewport.kt` | 本项目自有的原生列表范围修复；从既有详情安全区处理机制抽取约束与末项间距，独立实现可恢复的原生底栏收起适配，不包含悬浮材质或控件 |
| `DetailBarLocator.kt` | 从原模块详情底栏定位器中提取原生固定底栏定位；没有保留悬浮控件、材质和动画实现 |
| `NavigationBarHook.kt`、`MainNavigationInsets.kt`、`DetailNavigationInsets.kt`、`PageNavigationInsets.kt`、`SettingsNavigationInsets.kt` | 原模块系统导航栏与安全区适配裁剪；移除玻璃、悬浮及强制开启相关分支；使用 Android WindowInsets API |
| `BiliSettingsHook.kt`、`SettingsRoute.kt`、`HostActions.kt` | 原模块宿主设置入口、路由与重启适配；原记录致谢 BBZQ / 哔哩漫游的设置入口分析，重启代码也保留过哔哩漫游参考注释；简单版修改入口名、动作与路由标识 |
| `MethodHook.kt`、`HookRuntime.kt`、`Reflector.kt` | 原模块现代 libxposed API 封装及宿主反射辅助代码；不是内置的 libxposed API 实现 |
| `Config.kt`、`SettingsDraft.kt`、`HomeBarSync.kt`、`HostSettings.kt`、`EasyBiliToolModule.kt` | 原模块基础结构提取或简单版重写；配置仅三个布尔值，宿主配置上下文统一，模块入口只安装这三个功能 |
| `InAppSettings.kt`、`SettingsActivity.kt`、`About.kt` | 简单版 Kotlin/View 设置页、桌面介绍页和共享关于页；BBZQ 仅作为设置界面参考，关于页列出五个来源 |
| `NavigationVisibility.kt`、`InsetBaseline.kt`、`JvmNumbers.kt` | 本项目自有的窗口生命周期状态、安全区基线及 Kotlin 迁移数值兼容辅助实现 |

文件位置均为 `app/src/main/kotlin/com/weinaoa/easybilitool/`。五个 Kotlin JVM 测试文件在 `tests/`，验证配置草稿、首页及详情滚动状态、现代 hook 辅助行为。

## 原记录与目前证据的边界

原自用项目的下列记录为本次来源判断提供线索，摘录保留在这里，原项目和其历史测试文件不作为简单版构建依赖：

- `qa/VALIDATION-v050.md`：只读分析 BBZQ 与哔哩漫游 APK 的宿主 Preference 结构；未复制、重新分发其实现或 APK。
- `qa/VALIDATION-v081.md`：参考 BBZQ SettingsActivity 的操作栏与 WindowInsets；独立实现相似布局，没有复制代码、资源或整个 Activity。
- `qa/VALIDATION-v083.md`：核对 BBZQ 的透明状态栏开关，参考机制，未复制代码。
- 迁移前的 `InAppSettings.java` 关于页及 `HostActions.java` 注释：设置入口与重启流程致谢哔哩漫游。

这些是开发过程中的来源记录，当前没有发现简单版直接包含上游 Kotlin 源文件、上游 APK 或上游设置资源。早期宿主路由、重启与公共辅助代码的判断依据是上述独立实现记录，不把字符串或文件名检查视为完整版权审计。后续若发现遗漏的第三方改写来源，应补充记录并保留相应授权义务。

原项目整体声明为 GPL-3.0-or-later，部分被移除的功能还有其他许可。一些被提取的本项目自有文件原来带 GPL-3.0-or-later 文件头；维护者现将简单版中的这些自有实现以 Mulan PubL v2 许可。该选择不会改变原项目或第三方材料的许可，也不授权重新许可任何其他权利人的代码。

## 未随简单版保留的来源

原模块的 Pakku 弹幕合并、Bilibili Evolved 弹幕空降与勋章功能、PiliPlus 全屏手势、HyperModifier / LightMemo 玻璃与悬浮材质代码、着色器、拼音数据及统计功能均未进入简单版源码或 APK。

没有据此把原项目全部第三方 NOTICE 或许可证复制进简单版；后续若重新加入其中代码，需重新核对来源与许可。

## 资源与依赖

- 图标来自原模块已有的本地图标资源。已核对原 `artwork/README.md`：原图和留白调整由内置 image_gen 生成，没有使用第三方图标作为输入。简单版 PNG 与原设计 PNG 的 SHA-256 相同，具体记录见 [ARTWORK.md](ARTWORK.md)。该图标随自有实现以 Mulan PubL v2 许可。
- Manifest、字符串、主题与 Xposed 入口元数据在简单版目录内；文案改为简单版名称和三个功能。
- libxposed API JAR、Gradle wrapper 及构建生成/引入的运行库见 [第三方声明](../THIRD_PARTY_NOTICES.md)。它们保持各自的第三方许可。

## BBZQ 的木兰协议

[BBZQ README](https://github.com/HSSkyBoy/BBZQ/blob/master/README.md) 标注的是 **Mulan PubL v2（木兰公共许可证）**，不是 **Mulan PSL v2（木兰宽松许可证）**。

[许可证维护方向 OSI 提交的说明与正文](https://lists.opensource.org/pipermail/license-review_lists.opensource.org/2025-February/005655.html) 区分了两者：PubL v2 对分发衍生作品设置源码提供要求；通过下载地址提供时，地址有效期至少三年。PSL v2 为宽松许可，参见 [Mulan PSL v2 正文](https://opensource.org/license/MulanPSL-2.0)。不能仅因 BBZQ 使用 PubL，就直接把简单版也标为 PubL。

原项目保存的 `licenses/BBZQ-LICENSE.txt` 为简化中文文本，日期和内容与维护方公开的 2021 年 5 月双语正文不一致。本次采用维护方提交给 OSI 的 [完整双语正文附件](https://lists.opensource.org/pipermail/license-review_lists.opensource.org/attachments/20250212/730a803e/attachment-0001.txt)，不沿用旧副本。许可证文本包含适用说明中的示例占位符；软件版权声明已填写在源文件头和 README 中，不修改许可证正文。

## 维护许可记录

1. 保留本清单及原有来源记录，发现新来源时及时补充，不能用主协议覆盖第三方限制。
2. 根目录及 APK 附完整 `LICENSE`，自有源文件附 Mulan PubL v2 声明，第三方声明独立保留。
3. 每个 APK 发布时同时提供对应源码、版本标签和校验值；关于页链接到该版本的发布页和完整许可证。

1.1.0 将本仓库的模块源码与测试完整迁移到 Kotlin，保留原来源和许可证。`NavigationVisibility.kt` 与窗口转场修复只处理导航栏显示时机；`JvmNumbers.kt` 保留 Java 数值提升与文本拼接的既有语义。

## 1.1.3 导航窗口修复

2026-10-11：按维护者要求将自有的共用导航沉浸修复同步到简单版。`NavigationBarHook.kt` 增加底部 Dialog 的窗口生命周期和系统安全区处理；`PageNavigationInsets.kt` 延伸转发编辑页的原生背景。新增 `BottomSheetInsetState.kt`、`BottomSheetNavigationInsets.kt`、`NavigationInsetSurface.kt`，分别处理可恢复的安全区基线、弹层控件避让及宿主背景色填充。这些是自有导航实现，沿用本仓库 Mulan PubL v2 授权，不包含悬浮底栏、材质、独立按钮或其它完整版功能。

背景取色使用宿主 RenderNode 绘制；硬件位图接口属于非 SDK 反射，调用限定在 LSPosed 宿主内，并在不可用时回退到宿主主题背景色。未新增第三方依赖。新增测试仅验证安全区重复布局、系统 inset 变化和宿主覆盖后的恢复规则。

## 1.1.4 直播导航适配

2026-10-18：修复简单版自有 `DetailNavigationInsets.kt` 对直播输入框背景的错误继承。新增 `LiveWebInsets.kt`，仅处理官方客户端“全部直播”关注列表网址的原生容器、viewport 和网页滚动安全区，使用 Android WindowInsets / WebView 布局机制；适配入口由本地宿主 APK 结构核对。实现与文档沿用本仓库 Mulan PubL v2，不引入媒体通知、播放器手势、勋章、悬浮底栏或其它完整版功能，没有新增第三方依赖。

参考 [Android WebView 窗口边衬说明](https://developer.android.com/develop/ui/views/layout/webapps/understand-window-insets) 与 [Android 边到边布局指导](https://developer.android.com/codelabs/edge-to-edge)。窗口延伸和网页内容避让分别处理，不使用一块原生补色背景代替 WebView 延伸。退出、切换网址或键盘显示时撤回自有调整，并继续传递系统边衬更新。

## 1.1.5 竖屏与最常访问

2026-10-22：按维护者要求补齐简单版三个功能范围内的共用沉浸修复。`ImmersiveStatusBarHook.kt` 增加 Story Activity，保留宿主状态栏图标明暗，移除额外渐变。新增自有 `StoryStatusInsets.kt`，针对本地核对的 8.95 原生播放器 viewport 参数，只撤去一份状态栏顶部留白，并在配置或窗口晚于播放器就绪时重放待处理请求。新增自有 `QuickConsumeInsets.kt`，在最常访问各页原生列表内计算底部安全距离，撤回通用根容器填色；退出和关闭时恢复。上述简单版自有实现沿用 Mulan PubL v2，不新增第三方依赖，不包含完整版勋章、手势、悬浮材质或其它功能。

参考 [Android 边到边布局指导](https://developer.android.com/codelabs/edge-to-edge) 与[系统栏图标说明](https://developer.android.com/develop/ui/views/layout/edge-to-edge#system-bar-icons)。播放器字段与方法仅针对已核对的宿主版本；找不到时记录并跳过该适配。

## 1.1.6 官方状态栏保护

2026-10-22：`StatusBarProtection.kt` 只负责官方 AndroidX Core 1.17.0 `ProtectionLayout` / `GradientProtection` 的挂载、颜色参数和撤回，不自行计算视频亮度或重写渐变算法。保留宿主图标明暗选择，按该选择提供相应的保护背景。状态栏边衬在宿主内容层消费，DecorView 保留真实边衬供官方系统栏监视器处理；不覆盖系统手势导航区。官方组件及运行依赖保留 Apache-2.0，详见 [第三方声明](../THIRD_PARTY_NOTICES.md)。原有三项功能不变，增加默认开启的状态栏渐变保护子选项，可独立保存关闭。


## 1.1.12 候选兼容适配

维护者提供的 1.1.11 源码包基于本仓库 1.1.6 并保留 Mulan PubL v2。仅择取简单版适用的源码、四组检查及审计工具，未将包内实测声明当作验证结果。后续补齐的 Compose 首页滚动、Opus 动态底栏、精确评论列表绑定与回复详情首次触摸绑定属于本轮在简单版中的实现，沿用 Mulan PubL v2。宿主结构仅用于本地接口核对，未加入宿主 APK 或反编译源码。文件范围和验证限制见 [候选适配记录](../qa/VALIDATION-v1112.md)。


2026-10-24：新增自有 `ComposeBarMotion.kt`，通过宿主公开的 Compose layout/alpha 接口桥接连续尺寸与透明度变化；保留原生 `HomeBarSync` 透明度关系、完整子内容测量及已有滚动状态模型。应用到本项目首页、动态和评论，沿用 Mulan PubL v2，未引入第三方源文件或依赖。

2026-10-24：旧版原生回复页顶部避让由本项目自有代码修正，通过真实原生输入栏识别，未引入新依赖或第三方实现。

2026-10-24：导航区复测补充本项目自有适配：精确绑定新版分享弹窗的面板；动态在 Compose 控件内部保留导航安全距离，避免缩短整页；键盘和视口变化独立触发安全区重算，并在底栏收起后保留手势安全距离。实现沿用 Mulan PubL v2，宿主接口只在本地核对，未纳入宿主源文件或设备原始资料。

2026-10-24：普通视频键盘关闭闪动修复继续使用本项目自有实现：保持已核对的新视频窗口、原生根容器和 Compose 修饰符稳定，通过独立零尺寸 View 观察系统 IME 动画进度和结束，保留宿主回调与键盘边衬，按每帧未被键盘覆盖的导航距离连续调整自有避让。沿用 Mulan PubL v2，无新增依赖；参考 [Android 官方键盘动画说明](https://developer.android.com/develop/ui/views/layout/sw-keyboard)。
