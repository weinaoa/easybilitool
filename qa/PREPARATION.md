# 公开发布前整理验证

日期：2026-10-04。简单bili小工具 1.0.2，包名 `com.weinaoa.easybilitool`。

本轮仅整理来源、第三方许可、开发工具和公开文件边界，没有修改模块 Java 源码或资源。项目主许可证待定，没有推送 GitHub。

## 完成内容

- 25 个 Java 文件按来源分组记录；原项目 GPL 文件头、BBZQ / 哔哩漫游参考过程与仍需确认的版权范围一并记录，见 [SOURCES.md](../docs/SOURCES.md)。
- 图标生成来源和原始文件校验值已记录，见 [ARTWORK.md](../docs/ARTWORK.md)。当前画稿本身位于简单版资源目录，不依赖原项目 artwork 文件。
- libxposed API、Gradle wrapper、构建引入的 Kotlin / JetBrains 注解及 R8 支持代码的许可范围已记录，并附 Apache-2.0 全文。
- 测试、APK 审计工具收进 `tools/`，SDK 配置支持本地文件或环境变量，不再写死本机路径。Windows / Linux / macOS 命令已记录；本轮实跑平台仍是 Windows。
- Git 忽略规则排除构建目录、本机配置、生成产物、原始日志和常见密钥文件。源码打包清单纳入文档、工具与第三方许可，排除自用完整模块和设备归档。

## 独立验证

将源码 ZIP 解压到新的空目录，未提供 `local.properties`，通过 SDK 环境变量和现有 Gradle 9.5.0 缓存构建。首次 wrapper 下载发行包较慢，本次实际完整构建使用同版本已缓存的 Gradle 可执行程序。

- `assembleDebug` 与 `lintDebug`：成功，0 errors、18 warnings。
- `tools/test.py`：69 项通过（24 + 13 + 17 + 15）。
- `tools/audit_apk.py`：通过，包括包名、版本、名称、唯一宿主作用域、入口、功能边界、编译 API 未打入、无额外权限/provider，以及 v2 签名验证。
- 独立重建 APK 的 ZIP 条目集合和每个条目的解压内容与已实测 1.0.2 APK 完全相同；ZIP 排列/压缩布局不同，整体 APK SHA-256 不相同。因此未宣称逐字节可重复构建，也没有将验证目录生成的 APK 替换为设备已测产物。
- 发布准备包继续使用原本已实测 APK，SHA-256 为 `eeb2459d2811e89d6aea31852069cb7582e01e7ffee04fda4dc68aee218977de`。

最终源码包检查：主 `LICENSE` 不存在，第三方许可与来源文档齐全；仅 25 个模块 Java 文件，没有本机 SDK 配置、缓存、原始日志、私钥、旧模块代码、设备配置备份或临时实机工具。最终产物摘要以 `dist/SHA256SUMS.txt` 为准。

协议和发布待办见 [RELEASE.md](../docs/RELEASE.md)。本次未更改设备状态，功能实测结果仍见 [VALIDATION-v101.md](VALIDATION-v101.md)，关于页实测见 [VALIDATION-v102.md](VALIDATION-v102.md)。
