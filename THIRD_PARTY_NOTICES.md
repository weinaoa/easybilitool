# 第三方来源与许可

简单bili小工具的主许可证尚未确定。本文记录已有第三方材料和参考来源，不代替主许可证，也不授予本项目代码的再分发权限。

## 随源码或构建产物使用的依赖

| 材料 | 使用范围 | 许可与出处 |
| --- | --- | --- |
| libxposed API 102.0.0 | `app/libs/libxposed-api-102.0.0.jar`，仅编译，不打入 APK | Apache-2.0；[官方发布目录](https://repo.maven.apache.org/maven2/io/github/libxposed/api/102.0.0/)、[上游项目](https://github.com/libxposed/api) |
| Gradle wrapper / Gradle 9.5.0 | `gradle/wrapper` 与启动脚本，构建工具 | Apache-2.0；[Gradle LICENSE](https://github.com/gradle/gradle/blob/master/LICENSE) |
| Kotlin 标准库及 JetBrains 注解 | Android Gradle Plugin 自动引入的运行库；当前 APK 含相关类 | Apache-2.0；[Kotlin LICENSE](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt)、[JetBrains annotations](https://github.com/JetBrains/java-annotations) |
| R8 / D8 | Android 构建工具及其生成的兼容代码 | Apache-2.0；[R8 LICENSE](https://r8.googlesource.com/r8/+/refs/heads/main/LICENSE) |

完整 Apache-2.0 文本见 [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt)。上述材料保留各自许可；未来本项目的主许可证不会替代这些许可。

API JAR 从官方 AAR 的 `classes.jar` 提取。现存文件核验值：

```text
libxposed-api-102.0.0.jar SHA-256
a515dd7a53cd7a47c05e101dff77d61acb3091a97b20a885b9ea3494412db985

来源 api-102.0.0.aar SHA-256（原项目下载核验记录）
423484a6e1807e7a423c4b88fcd8176d104318259d91791877fed88fe91479d0

gradle-wrapper.jar SHA-256
16caeaf66d57a0d1d2087fef6a97efa62de8da69afa5b908f40db35afc4342da
```

## 功能与界面参考

- [BBZQ](https://github.com/HSSkyBoy/BBZQ)：透明播放器状态栏机制与设置界面参考。上游 README 标注 Mulan PubL v2。原项目验证记录说明相关部分独立实现，未复制其代码、资源或设置 Activity；这份记录与最终版权判断的区别见 [docs/SOURCES.md](docs/SOURCES.md)。
- [哔哩漫游](https://github.com/yujincheng08/BiliRoaming)：宿主设置入口与重启流程参考。原项目记录为只读分析 APK 后独立适配，没有随本项目分发其 APK。
- [Android WindowInsets](https://developer.android.com/reference/android/view/WindowInsets)：状态栏、导航栏、刘海与键盘安全区接口。
- [Material Components AppBarLayout](https://developer.android.com/reference/com/google/android/material/appbar/AppBarLayout)：读取宿主原生顶栏的收起比例。
- [AndroidX RecyclerView.OnScrollListener](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.OnScrollListener)：主界面与详情列表纵向滚动接口。

后三项为 API 文档参考。本模块通过宿主既有控件适配，没有额外捆绑 Material Components 或 AndroidX RecyclerView 库。五个来源也保留在模块“关于”中。
