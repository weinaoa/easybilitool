# 构建与发布

> **停止更新（2026-11-18）：** 本项目不再安排功能开发、修复、客户端适配或后续版本发布。本文保留为历史构建复现和明确授权的收尾发布参考，不构成持续维护计划。

仓库：[weinaoa/easybilitool](https://github.com/weinaoa/easybilitool)。自有实现、文档与图标采用 Mulan PubL v2，来源范围见 [SOURCES.md](SOURCES.md)，第三方依赖见[第三方声明](../THIRD_PARTY_NOTICES.md)。

## 项目边界

本仓库是独立 Android 工程，构建、测试和源码打包不需要仓库以外的项目源码。功能范围仅为状态栏沉浸、导航栏沉浸、滑动收起底栏及必要设置。

公开文件只包含本项目所需且来源、授权明确的源码、资源、构建输入、工具和经过审查的验证资料。宿主 APK、反编译材料、设备原始截图／XML／录像、账号资料、框架备份、本机配置和签名私钥不进入仓库或源码包。`qa/` 保留可公开的测试结论、数值摘要及经审查的图片。

## 构建与检查

需要 JDK 17+、Android SDK Platform 36、Build Tools 36.0.0 和 Python 3.10+。通过 `ANDROID_SDK_ROOT` / `ANDROID_HOME` 或本地 `local.properties` 配置 SDK，Java 需位于 PATH。首次构建需要联网下载依赖。

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
python tools/test.py
python tools/audit_apk.py
python package_source.py
```

Linux / macOS 首行使用 `bash ./gradlew :app:assembleDebug :app:lintDebug`。测试通过 Gradle wrapper 执行，Kotlin 编译与运行库由 AGP 管理。

`package_source.py` 按当前构建版本生成 `dist/easy-bili-tool-<版本>.apk`、对应源码 ZIP 和 `SHA256SUMS.txt`。打包采用明确文件范围，排除 SDK、缓存、原始日志、`local.properties`、密钥及生成的 APK；仍须检查最终 ZIP 文件清单。`dist/` 不由 Git 跟踪。

默认 APK 采用本机 Android 调试签名。同包名 APK 的覆盖升级需要相同签名；复现历史构建或明确授权收尾发布时，保留既有签名，私钥仅在本地保管，不提交密码或证书私钥。

## 验证与发布流程

仅在维护者明确要求收尾发布或恢复维护时执行以下流程；文档停更说明无需重新构建或操作手机。

1. 确认版本、分支、变更和来源声明一致，分别记录构建、JVM 检查、Lint 与 APK 审计结果。
2. 使用对应 APK 完成改动涉及的实机检查。底栏和安全区改动需覆盖正文、评论、滑动、惯性、切页、重进与键盘往返，导航栏沉浸开、关分别验证。检查背景、列表实际边界和末项距离。
3. 核对 APK 哈希与验证记录，明确未验证的客户端、设备和配置。测试旧包的结果不能代替新包结果；当前候选边界见 [TESTING.md](TESTING.md)。
4. 检查 Git 远端、公开可见性、提交文件和源码 ZIP，排除个人资料、无关代码及未明确授权的材料。
5. 稳定版本发布时提供版本标签、APK、对应源码包和 SHA-256 校验值，关于页引用同一版本的源码及完整许可证。

合入 `main` 不等于发布 Release。当前源码及新旧客户端的实际验证范围以 [TESTING.md](TESTING.md) 为准。
