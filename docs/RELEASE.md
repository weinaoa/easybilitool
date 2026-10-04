# 公开发布前准备

目标仓库：[weinaoa/easybilitool](https://github.com/weinaoa/easybilitool)。目前只整理本地项目，没有推送或创建 Release。

## 项目边界

`easybilitool` 本身是完整 Android 工程根目录：构建配置、wrapper、源码、图标、API 编译 JAR、测试、检查/打包工具和文档都在这里。它不引用父目录的 BiliTool 工程，也不需要把自用工程公开。

为了分别维护，后续适合放成并列目录，例如：

```text
projects/
  bilitool/       # 自用工程与它自己的仓库
  easybilitool/   # 开源工程与它自己的仓库
```

并列目录是管理上的建议，不是构建要求。当前仍位于自用项目下的 `easybilitool/`，没有移动原项目。公开仓库应以本目录为根，不能以其父目录为根；若父目录今后建立 Git 仓库，应将子目录排除，避免两个项目互相追踪。

临时实机检查应用、LSPosed 配置备份、设备原始截图/XML、旧 APK 和历史分析材料在外部本地归档，不进入公开源码包。项目内 `qa/` 保留测试结论和筛选后的界面截图，原始构建日志仅本地保留。

## 独立构建与检查

需要 JDK 17+、Android SDK Platform 36、Build Tools 36.0.0、Python 3.10+。SDK 用环境变量 `ANDROID_SDK_ROOT` / `ANDROID_HOME` 或本地 `local.properties` 配置。Python 检查工具通过 PATH 调用 `java` 和 `javac`。

Windows：

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
python tools/test.py
python tools/audit_apk.py
python package_source.py
```

Linux / macOS 使用 `bash ./gradlew :app:assembleDebug :app:lintDebug`，其余命令相同。首次 Gradle 构建需要联网取得构建依赖。源码 ZIP 包含 wrapper 和 API JAR，不包含 SDK、Gradle 缓存或本机路径配置。

源码打包采取明确文件清单：包含代码、资源、测试、工具、文档、第三方许可和筛选后的 QA 文件；排除私有工程、归档、构建缓存、原始日志、`local.properties`、密钥与生成的 APK。

当前 APK 采用本机 Android 调试签名，APK 验证和实机测试已完成。正式维护更新时应确定长期签名策略；私钥、密码不得提交仓库。`.gitignore` 已排除常见 JKS / keystore 文件，仍需在发布前审查最终文件清单。

## 尚待最终决定

- 主许可证：按 [来源清单](SOURCES.md) 完成权利确认后再定；目前没有项目主 `LICENSE`。第三方 Apache-2.0 副本不代表整个项目采用 Apache-2.0。
- 公开提交：核对现有远端内容与最终待提交文件，确认主协议后再准备提交和推送。
- Release：确定签名与版本策略后，再发布 APK、对应源码和校验值。当前 `dist/` 为本地产物，不被 Git 跟踪。
