# 简单bili小工具

为官方哔哩哔哩 Android 客户端提供状态栏沉浸、导航栏沉浸和滑动收起底栏的 LSPosed 模块。保留原生界面，让背景延伸到系统栏区域，同时为按钮、输入栏和列表末项保留安全距离。

[下载稳定版](https://github.com/weinaoa/easybilitool/releases/latest) · [更新记录](docs/CHANGELOG.md) · [验证范围](docs/TESTING.md) · [反馈问题](https://github.com/weinaoa/easybilitool/issues) · [许可证](LICENSE)

## 功能

| 功能 | 使用效果 |
| --- | --- |
| 沉浸状态栏 | 视频、Story 竖屏视频和直播背景延伸到顶部，保留系统状态图标，并为操作控件避让 |
| 沉浸导航栏 | 页面及弹层背景延伸到系统导航区，固定按钮、评论输入栏和列表末项保留安全距离 |
| 滑动时收起底栏 | 首页、动态详情和视频评论底栏随上滑逐步下移、渐隐，下滑恢复；处理切页、重进和键盘往返时的恢复 |

“状态栏渐变保护”是沉浸状态栏的子选项，默认开启，使用 AndroidX 官方组件改善图标对比度。关闭后保持透明，沿用客户端选择的图标明暗，不逐帧分析视频颜色。

本项目仅维护上述三项主功能及必要设置。

## 版本与兼容性

| 项目 | 当前范围 |
| --- | --- |
| 稳定版 | **1.1.6**，适配目标为官方客户端 **8.95.0**，从 Releases 获取 |
| 当前适配分支 | **1.1.12**，`compat/bili-9161`；增加官方客户端 **9.16.1 / 9161100** 适配，尚未合入 `main` 或发布 Release |
| 系统与框架 | Android **8.0+**、Root、支持 **libxposed API 102** 的 LSPosed 框架 |
| 作用域 | 官方客户端 `tv.danmaku.bili`；其他包名、共存修改版和客户端灰度版本需另行验证 |

最新候选已在 9.16.1 上独立实测沉浸开关、滑动收起、动态与视频评论安全区及键盘关闭动画。**最新键盘补丁尚未在 8.95.0 重新实测**；此前旧版回归对应较早 APK，不能视为当前包通过。其他设备、浅色皮肤和三键导航尚未完成本轮验证。详见[验证范围](docs/TESTING.md)。

横屏全屏、画中画、分屏等场景以客户端原有行为为主。新版视频页的键盘动画使用单独适配，具体边界见[键盘动画记录](qa/IME-FLICKER-v1112.md)。

## 安装与使用

1. 从 [GitHub Releases](https://github.com/weinaoa/easybilitool/releases) 下载 APK。尝试适配分支时，请自行构建该分支，确认验证范围。
2. 在框架中启用“简单bili小工具”，作用域只勾选 **哔哩哔哩 / `tv.danmaku.bili`**。
3. 强行停止并重新打开哔哩哔哩，进入 **我的 → 设置 → 简单bili小工具**；也可从模块桌面入口选择“打开功能设置”。
4. 按需打开功能，点击 **保存并重启**。首次安装时三项主功能均关闭；渐变保护默认开启，但仅随沉浸状态栏生效。

设置在保存前属于编辑草稿，退出未保存时可以继续编辑或放弃。升级时使用相同签名的 APK；签名不一致时无法直接覆盖安装。

测试兼容性时请只启用本模块，避免多个模块同时修改同一窗口或底栏。模块不申请网络或存储权限；功能配置保存在哔哩哔哩私有目录，清除哔哩哔哩数据会清除配置。

## 常见问题

**找不到设置入口或开关没有生效**

确认框架支持 API 102、作用域正确，并在启用或更新模块后强停重开客户端。确认使用的是官方包名；仅修改开关而未“保存并重启”不会保存草稿。

**升级客户端后出现黑边、遮挡或底栏异常**

客户端内部页面结构可能改变。先检查当前模块版本的[验证范围](docs/TESTING.md)，单独启用本模块复现，再分别检查导航栏沉浸开启、关闭时的结果。关闭有关功能并保存重启，可撤回该功能的调整。

**关闭键盘后输入栏能看到，是否就表示安全区正常**

还需要检查按钮与手势条的距离、关闭动画中是否跳动，以及收起后的列表实际范围。当前候选的相关检查见[键盘动画记录](qa/IME-FLICKER-v1112.md)。

## 问题反馈与贡献

请在 [Issues](https://github.com/weinaoa/easybilitool/issues) 提供模块版本、客户端版本与 versionCode、Android 版本、框架版本、导航方式、有关开关及最短复现步骤。适配分支可在设置中使用“复制运行诊断”；提交前请检查内容，遮盖账号、评论正文、通知及其他个人信息。

欢迎提交范围明确的修复。保留旧客户端适配路径，注明代码来源和授权；涉及底栏、系统栏、键盘或安全区的改动，需要记录动态正文、动态评论、视频评论的滑动、惯性、切页、重进和键盘往返结果，并分别检查导航栏沉浸开、关。开发要求见 [AGENTS.md](AGENTS.md)。

## 从源码构建

需要 **JDK 17+、Android SDK Platform 36、Build Tools 36.0.0、Python 3.10+**。通过 `ANDROID_SDK_ROOT` / `ANDROID_HOME` 或未提交的 `local.properties` 设置 SDK 路径。首次构建需下载 Gradle 和 Android 构建依赖，Kotlin 编译由 AGP 管理。

在仓库根目录执行：

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
python tools/test.py
python tools/audit_apk.py
python package_source.py
```

Linux / macOS 将首行替换为 `bash ./gradlew :app:assembleDebug :app:lintDebug`，其余命令相同。Windows 也可通过 `test.ps1` 执行 JVM 检查。

构建 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。打包后 `dist/` 生成当前版本 APK、源码 ZIP 与 `SHA256SUMS.txt`；默认使用本机 Android 调试签名。`libxposed-api-102.0.0.jar` 仅参与编译，不打入 APK。

| 目录 | 内容 |
| --- | --- |
| `app/` | Kotlin 模块源码、资源、入口元数据和编译接口 |
| `tests/`、`tools/` | JVM 检查、APK 审计与宿主静态接口核对工具 |
| `docs/`、`qa/` | 使用与发布文档、来源清单及经过审查的验证记录 |
| `licenses/` | 第三方许可证副本 |
| `dist/` | 本地生成产物，不由 Git 跟踪 |

构建成功、静态检查通过和实机验证是不同结论。发布流程及文件边界见[发布说明](docs/RELEASE.md)。

## 来源与许可

本项目自有代码、文档和图标以 **Mulan PubL v2（木兰公共许可证，第2版）** 发布，Copyright (c) 2026 weinaoa，完整文本见 [LICENSE](LICENSE)。第三方依赖保留各自许可，APK 内附许可证副本。

感谢 BBZQ、哔哩漫游及 Android 官方文档提供的机制与接口参考。具体使用范围见[源码来源](docs/SOURCES.md)、[第三方声明](THIRD_PARTY_NOTICES.md)及[图标来源](docs/ARTWORK.md)。发布 APK 时同时提供对应源码、版本标签和校验值。
