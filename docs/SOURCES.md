# 源码来源清单

整理日期：2026-10-04。对象：简单bili小工具 1.0.2，包名 `com.weinaoa.easybilitool`。

**主许可证待定。** 本清单用于决定后续许可，不能把“参考”“移植”“改写”和“原创”一概视为同一种授权。没有因为修改包名、移除 SPDX 或缩减功能就认定可以重新授权。

## 25 个 Java 文件

简单版从自用 BiliTool 中提取并裁剪三个原生界面功能，同时重建简单设置与配置。以下是本次保留文件及现存记录能支持的来源范围。

| 文件 | 来源与修改范围 |
| --- | --- |
| `ImmersiveStatusBarHook.java` | 原模块透明状态栏逻辑裁剪；原 0.8.3 记录为参考 BBZQ APK 的透明状态栏机制，独立适配哔哩哔哩 8.95 的 inset 与图标覆盖；保留视频顶部菜单间距和直播安全区处理 |
| `HomeBottomBarHook.java`、`MainTabBarScroll.java` | 原模块首页原生底栏收起与滚动状态逻辑；去掉玻璃和其他底栏功能分支 |
| `DetailBottomBarHook.java`、`DetailBarScroll.java` | 原模块动态详情、视频评论的原生底栏收起逻辑；去掉悬浮底栏配置分支 |
| `DetailBarLocator.java` | 从原模块详情底栏定位器中提取原生固定底栏定位；没有保留悬浮控件、材质和动画实现 |
| `NavigationBarHook.java`、`MainNavigationInsets.java`、`DetailNavigationInsets.java`、`PageNavigationInsets.java`、`SettingsNavigationInsets.java` | 原模块系统导航栏与安全区适配裁剪；移除玻璃、悬浮及强制开启相关分支；使用 Android WindowInsets API |
| `BiliSettingsHook.java`、`SettingsRoute.java`、`HostActions.java` | 原模块宿主设置入口、路由与重启适配；原记录致谢 BBZQ / 哔哩漫游的设置入口分析，重启代码也保留过哔哩漫游参考注释；简单版修改入口名、动作与路由标识 |
| `MethodHook.java`、`HookRuntime.java`、`Reflector.java` | 原模块现代 libxposed API 封装及宿主反射辅助代码；不是内置的 libxposed API 实现 |
| `Config.java`、`SettingsDraft.java`、`HomeBarSync.java`、`HostSettings.java`、`EasyBiliToolModule.java` | 原模块基础结构提取或简单版重写；配置仅三个布尔值，宿主配置上下文统一，模块入口只安装这三个功能 |
| `InAppSettings.java`、`SettingsActivity.java`、`About.java` | 简单版 Java/View 设置页、桌面介绍页和共享关于页；BBZQ 仅作为设置界面参考，关于页列出五个来源 |

文件位置均为 `app/src/main/java/com/weinaoa/easybilitool/`。四个 JVM 测试文件在 `tests/`，验证配置草稿、首页及详情滚动状态、现代 hook 辅助行为。

## 原记录与目前证据的边界

原自用项目的下列记录为本次来源判断提供线索，摘录保留在这里，原项目和其历史测试文件不作为简单版构建依赖：

- `qa/VALIDATION-v050.md`：只读分析 BBZQ 与哔哩漫游 APK 的宿主 Preference 结构；未复制、重新分发其实现或 APK。
- `qa/VALIDATION-v081.md`：参考 BBZQ SettingsActivity 的操作栏与 WindowInsets；独立实现相似布局，没有复制代码、资源或整个 Activity。
- `qa/VALIDATION-v083.md`：核对 BBZQ 的透明状态栏开关，参考机制，未复制代码。
- 原 `InAppSettings.java` 关于页及 `HostActions.java` 注释：设置入口与重启流程致谢哔哩漫游。

这些是开发过程中的来源记录，当前没有发现简单版直接包含上游 Kotlin 源文件、上游 APK 或上游设置资源。它们不等同于完整版权审计；尤其是较早的宿主路由、重启与公共辅助代码，仍需要作者确认是否存在其他未记录的复制或改写来源。

原项目整体声明为 GPL-3.0-or-later，部分被移除的功能还有其他许可。一些被提取文件原来带 GPL-3.0-or-later 文件头。当前清单没有把这些标记当作无关信息：若相应代码完全属于作者且无其他权利人限制，可由权利人决定许可；若含受该许可约束的第三方代码，则必须保留相应授权义务。此项应在主协议确定前确认。

## 未随简单版保留的来源

原模块的 Pakku 弹幕合并、Bilibili Evolved 弹幕空降与勋章功能、PiliPlus 全屏手势、HyperModifier / LightMemo 玻璃与悬浮材质代码、着色器、拼音数据及统计功能均未进入简单版源码或 APK。

没有据此把原项目全部第三方 NOTICE 或许可证复制进简单版；后续若重新加入其中代码，需重新核对来源与许可。

## 资源与依赖

- 图标来自原模块已有的本地图标资源。已核对原 `artwork/README.md`：原图和留白调整由内置 image_gen 生成，没有使用第三方图标作为输入。简单版 PNG 与原设计 PNG 的 SHA-256 相同，具体记录见 [ARTWORK.md](ARTWORK.md)。没有复制 BBZQ 图标；最终资源许可仍需与主协议一并确定。
- Manifest、字符串、主题与 Xposed 入口元数据在简单版目录内；文案改为简单版名称和三个功能。
- libxposed API JAR、Gradle wrapper 及构建生成/引入的运行库见 [第三方声明](../THIRD_PARTY_NOTICES.md)。这些是第三方许可范围，不受主协议待定状态替代。

## BBZQ 的木兰协议

[BBZQ README](https://github.com/HSSkyBoy/BBZQ/blob/master/README.md) 标注的是 **Mulan PubL v2（木兰公共许可证）**，不是 **Mulan PSL v2（木兰宽松许可证）**。

[许可证维护方向 OSI 提交的说明与正文](https://lists.opensource.org/pipermail/license-review_lists.opensource.org/2025-February/005655.html) 区分了两者：PubL v2 对分发衍生作品设置源码提供要求；通过下载地址提供时，地址有效期至少三年。PSL v2 为宽松许可，参见 [Mulan PSL v2 正文](https://opensource.org/license/MulanPSL-2.0)。不能仅因 BBZQ 使用 PubL，就直接把简单版也标为 PubL。

原项目保存的 `licenses/BBZQ-LICENSE.txt` 为简化中文文本，日期和内容与维护方公开的 2021 年 5 月双语正文不一致。将来若选择 PubL v2，应核对并采用发布方完整正文，不能直接沿用这份旧副本。当前没有新增本项目的木兰或 GPL 主许可证。

## 确定主协议前的待确认项

1. 作者确认上表提取代码的版权归属，补充较早路由、重启、公共辅助代码中任何未记录的第三方来源。
2. 根据已记录的图标生成来源，确定该资源的公开分发许可范围。
3. 在以上结论上选择主许可证，再统一补充 `LICENSE`、README 许可说明和需要的源文件标记。第三方声明继续独立保留。
