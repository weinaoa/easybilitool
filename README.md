# 简单bili小工具（停更）

[![Release](https://img.shields.io/github/v/release/weinaoa/easybilitool)](https://github.com/weinaoa/easybilitool/releases/latest)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![libxposed](https://img.shields.io/badge/libxposed-API%20102-5865F2)
[![License](https://img.shields.io/badge/License-Mulan%20PubL%20v2-blue)](LICENSE)

## 请移步：[BiliCharm](https://github.com/weinaoa/BiliCharm)

**简体中文** · [English](README_EN.md)

> **停止更新（2026-11-18）：** 由于功能维护的复杂性，本项目不再安排新功能、缺陷修复或新客户端适配。现有源码、历史发布、验证记录和 Mulan PubL v2 许可保留。下方使用与构建说明供历史版本参考，不代表持续兼容性保证。

为哔哩哔哩 Android 客户端提供系统栏沉浸与滑动收起底栏的 LSPosed 模块。保留原生界面，让背景延伸到系统栏区域，同时为按钮、输入栏和列表末项保留安全距离。

[历史下载](https://github.com/weinaoa/easybilitool/releases) · [更新记录](docs/CHANGELOG.md) · [问题记录](https://github.com/weinaoa/easybilitool/issues) · [构建与验证记录](docs/TESTING.md)

## 功能特性

- **沉浸状态栏**：视频、Story 竖屏视频和直播背景延伸到顶部，保留系统状态图标，并为操作控件避让。
- **沉浸导航栏**：页面及弹层背景延伸到系统导航区，固定按钮、评论输入栏和列表末项保留安全距离。
- **滑动时收起底栏**：首页、动态详情和视频评论底栏随上滑逐步下移、渐隐，下滑恢复。

沉浸状态栏提供默认开启的“状态栏渐变保护”子选项，使用 AndroidX 官方组件改善图标对比度；关闭后透明，沿用客户端选择的图标明暗。

## 界面预览

<img src="docs/images/settings.png" width="320" alt="简单bili小工具功能设置">

## 使用要求

| 项目 | 要求 |
| --- | --- |
| 适配客户端 | 官方哔哩哔哩 **8.95.0** |
| Android | **8.0 及以上** |
| 框架 | Root，支持 **libxposed API 102** 的 LSPosed 框架 |
| 作用域 | 哔哩哔哩 **`tv.danmaku.bili`** |

## 安装与使用

1. 从 [Releases](https://github.com/weinaoa/easybilitool/releases) 下载并安装模块 APK。
2. 在框架中启用“简单bili小工具”，作用域勾选 **哔哩哔哩**。
3. 强行停止并重新打开哔哩哔哩，进入 **我的 → 设置 → 简单bili小工具**；也可从模块桌面入口选择“打开功能设置”。
4. 按需启用功能，点击 **保存并重启**。

首次安装时三项主功能均关闭，渐变保护仅随沉浸状态栏生效。设置在保存前属于编辑草稿；退出未保存时可继续编辑或放弃。

同包名、相同签名的 APK 可以覆盖升级。配置保存在哔哩哔哩私有目录，清除客户端数据会清除配置。模块不申请网络或存储权限。

## 常见问题

**找不到设置入口或功能没有生效？**

确认框架支持 API 102、作用域已勾选，并在启用或更新模块后强停重开客户端。修改设置后需“保存并重启”。

**出现黑边、遮挡或底栏异常？**

先单独启用本模块复现，检查客户端版本和有关开关。关闭对应功能并保存重启可撤回调整。历史问题可查阅 Issues，停更后不再安排修复或新版本适配。

## 从源码构建

技术栈：**Kotlin、Android View、现代 libxposed API**，使用 AndroidX 处理系统栏保护与边衬。

需要 **JDK 17+、Android SDK Platform 36、Build Tools 36.0.0、Python 3.10+**。通过 `ANDROID_SDK_ROOT` / `ANDROID_HOME` 或本地 `local.properties` 配置 SDK。

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
python tools/test.py
python tools/audit_apk.py
python package_source.py
```

Linux / macOS 首行使用 `bash ./gradlew :app:assembleDebug :app:lintDebug`。首次构建需要下载依赖。

APK 输出到 `app/build/outputs/apk/debug/`；打包后的 APK、源码 ZIP 和 SHA-256 校验值位于 `dist/`。详细流程见[构建与发布](docs/RELEASE.md)。

## 历史资料与自行维护

[Issues](https://github.com/weinaoa/easybilitool/issues) 与既有 Pull Request 保留为历史记录，项目不再安排维护或处理时限。需要继续开发时，可在保留来源声明并遵守许可的前提下自行 fork。

停更范围与收尾要求见 [AGENTS.md](AGENTS.md)。历史测试结果见[验证文档](docs/TESTING.md)，版本变化见[更新记录](docs/CHANGELOG.md)；这些记录不表示新系统或新客户端已经通过验证。

## 来源与许可

本项目自有代码、文档和图标采用 **Mulan PubL v2（木兰公共许可证，第2版）**，Copyright (c) 2026 weinaoa。完整文本见 [LICENSE](LICENSE)，第三方依赖保留各自许可。

感谢 BBZQ、哔哩漫游及 Android 官方文档提供的机制与接口参考。具体范围见[源码来源](docs/SOURCES.md)、[第三方声明](THIRD_PARTY_NOTICES.md)及[图标来源](docs/ARTWORK.md)。
