# 图标来源

简单版沿用自用 BiliTool 的图标，没有重新下载或引入第三方项目的图标。

原项目 `artwork/README.md` 记录：初始画稿使用内置 image_gen 生成，为粉色渐变背景、白色玻璃视频胶囊、播放三角形和两条流线；随后使用同一工具编辑中央图案的大小与留白，以适配 Android 启动器遮罩。提示词要求不含官方标识、文字或水印。最终导出为不透明 512 × 512 PNG。

当前原始画稿即 `app/src/main/res/drawable-nodpi/ic_launcher_artwork.png`。它与原项目 `artwork/app-icon-512.png` 逐字节相同，SHA-256 为：

```text
5cd6bdb326ecbdf462c445416989c3717ba0f95c977e410bda15d350149cfe57
```

自适应图标、背景和单色图标 XML 同样在 `app/src/main/res/` 内。不需要原项目的图标导出脚本或密度图片才能构建简单版。

根据维护者 2026-10-04 的授权决定，本图标资源随本项目以 Mulan PubL v2 发布，Copyright (c) 2026 weinaoa，见 [LICENSE](../LICENSE)。
