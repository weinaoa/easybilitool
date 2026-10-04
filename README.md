# 简单bili小工具

包名：`com.weinaoa.easybilitool`。版本：1.0.2。

计划发布仓库：[weinaoa/easybilitool](https://github.com/weinaoa/easybilitool)。当前处于公开发布前整理阶段，**主许可证待定**。

代码来源见 [docs/SOURCES.md](docs/SOURCES.md)，第三方材料见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)，独立仓库与发布事项见 [docs/RELEASE.md](docs/RELEASE.md)。第三方 Apache-2.0 许可不代表本项目已选择主协议。

本轮来源整理与独立构建验证见 [qa/PREPARATION.md](qa/PREPARATION.md)。

独立的 Android LSPosed 模块，只提供三个开关：

- **沉浸状态栏**：竖屏视频与直播背景延伸到顶部，保留状态栏并避让控件。视频顶部菜单沿用 80px 附加间距，直播控件使用系统状态栏与刘海安全区。
- **滑动时收起底栏**：主界面、动态详情和视频评论的原生底栏上滑收起，下滑恢复。切换页面、打开键盘或退出后恢复。
- **沉浸导航栏**：竖屏页面背景延伸到系统导航栏后方，底部按钮和列表末项保留安全距离。

## 使用

1. 安装 `dist/easy-bili-tool-1.0.2.apk`。
2. 在支持 libxposed API 102 的 LSPosed 框架中启用“简单bili小工具”，作用域选择 **哔哩哔哩 / tv.danmaku.bili**。
3. 重新启动哔哩哔哩，进入 **我的 → 设置 → 简单bili小工具**，或从模块桌面入口点击“打开功能设置”。
4. 修改开关后点击 **保存并重启**。三个开关首次安装均关闭；退出未保存时可继续编辑或放弃修改。

如果已经启用完整版 Bili 小工具，请先禁用完整版，避免两份模块同时调整同一界面。简单版使用独立的包名、设置入口、启动动作和配置文件，不读取完整版配置。

需要 Android 8.0+、Root 与 libxposed API 102 框架；适配目标为官方哔哩哔哩 8.95.0。横屏全屏、画中画、分屏及键盘显示时，相关布局交回客户端处理。

配置仅保存在哔哩哔哩的私有目录，清除哔哩哔哩数据会清除三个开关。模块不申请网络或存储权限。

## 构建

需要 JDK 17+、Android SDK Platform 36、Build Tools 36.0.0 和 Python 3.10+。设置 `ANDROID_SDK_ROOT` / `ANDROID_HOME`，或在本地 `local.properties` 配置 `sdk.dir`。Python 工具通过 PATH 调用 Java。

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
python tools/test.py
python tools/audit_apk.py
python package_source.py
```

Linux / macOS 将首行替换为 `bash ./gradlew :app:assembleDebug :app:lintDebug`。Windows 也可用 `test.ps1` 调用同一组测试。

源码与构建完全独立，不依赖父目录的完整版项目。编译依赖 `app/libs/libxposed-api-102.0.0.jar` 仅参与编译，不打入 APK。APK 使用本机 Android 调试签名。打包工具按当前版本生成 APK、源码 ZIP 与 SHA-256 校验文件，不包含本机配置、密钥、原始日志或完整版工程。

本次实机验证范围见 [qa/VALIDATION-v101.md](qa/VALIDATION-v101.md)。

1.0.2 在功能设置与桌面介绍页增加“关于”，列出参考项目和文档，并提供可点击链接：

- [BBZQ](https://github.com/HSSkyBoy/BBZQ)：透明播放器状态栏与设置界面参考。
- [哔哩漫游](https://github.com/yujincheng08/BiliRoaming)：宿主设置入口与重启流程参考。
- [Android WindowInsets](https://developer.android.com/reference/android/view/WindowInsets)：状态栏、导航栏、刘海与键盘安全区。
- [Material Components AppBarLayout](https://developer.android.com/reference/com/google/android/material/appbar/AppBarLayout)：首页底栏随原生顶栏收起比例联动。
- [AndroidX RecyclerView](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.OnScrollListener)：主界面与详情列表的纵向滚动接口。

本次“关于”界面验证见 [qa/VALIDATION-v102.md](qa/VALIDATION-v102.md)。
