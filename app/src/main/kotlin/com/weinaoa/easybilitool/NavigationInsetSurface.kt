/*
Copyright (c) 2026 weinaoa
EasyBiliTool is licensed under Mulan PubL v2.
You can use this software according to the terms and conditions of the Mulan PubL v2.
You may obtain a copy of Mulan PubL v2 at:
    http://license.coscl.org.cn/MulanPubL-2.0
THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
See the Mulan PubL v2 for more details.
*/

package com.weinaoa.easybilitool

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.RenderNode
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.TypedValue
import android.view.ViewGroup
import android.view.ViewTreeObserver

/** Paint native surface color through navigation padding, including Compose-painted backgrounds. */
internal class NavigationInsetSurface(private val target: ViewGroup, private val source: ViewGroup = target) {
    private val footer = ColorDrawable(Color.TRANSPARENT)
    private var attached = false
    private var sampledWidth = 0
    private var sampledHeight = 0
    private var layoutObserver: ViewTreeObserver? = null
    private val layoutChanges = ViewTreeObserver.OnGlobalLayoutListener {
        // Keyboard/emoji content can change while the Dialog's outer bounds stay fixed.
        sampledWidth = 0; sampledHeight = 0
    }

    fun apply(inset: Int) {
        sampleSurface()
        footer.setBounds(0, (target.height - inset).coerceAtLeast(0), target.width, target.height)
        if (!attached) {
            target.overlay.add(footer)
            layoutObserver = source.viewTreeObserver.also { it.addOnGlobalLayoutListener(layoutChanges) }
            attached = true
        }
    }

    fun surfaceColor(): Int? = footer.color.takeIf { Color.alpha(it) == 255 }

    // This runs in the LSPosed host, where hidden API reflection is available.
    // Renderer access is optional: unsupported implementations fall back to the native theme color.
    @SuppressLint("BlockedPrivateApi")
    private fun sampleSurface() {
        val contentHeight = source.height - source.paddingBottom
        if (source.width <= 0 || contentHeight <= 0 || source.isLayoutRequested ||
            (sampledWidth == source.width && sampledHeight == contentHeight)) return
        var bitmap: Bitmap? = null
        var hardware: Bitmap? = null
        var node: RenderNode? = null
        try {
            val width = (source.width / 2).coerceAtLeast(1)
            val height = (source.height / 2).coerceAtLeast(1)
            if (Build.VERSION.SDK_INT >= 29) {
                node = RenderNode("EasyBiliTool sheet surface color")
                node.setPosition(0, 0, width, height)
                val canvas = node.beginRecording(width, height)
                try { canvas.scale(.5F, .5F); source.draw(canvas) }
                finally { node.endRecording() }
                val createBitmap = HardwareRenderer::class.java.getDeclaredMethod(
                    "createHardwareBitmap", RenderNode::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                createBitmap.isAccessible = true
                hardware = createBitmap.invoke(null, node, width, height) as? Bitmap
                bitmap = hardware?.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap!!)
                canvas.scale(.5F, .5F); source.draw(canvas)
            }
            // The center can be a selected emoji pack tab with its own dark fill.
            // Sample the empty outer margin immediately above the navigation padding.
            val color = bitmap?.getPixel(8.coerceAtMost(width - 1), ((contentHeight - 4) / 2).coerceIn(0, height - 1))
            if (color != null && Color.alpha(color) == 255) {
                footer.color = color
                sampledWidth = source.width; sampledHeight = contentHeight
            }
        } catch (unavailable: RuntimeException) {
            // Some older renderers cannot draw a Compose surface to a bitmap.
            sampledWidth = source.width; sampledHeight = contentHeight
        } catch (unavailable: ReflectiveOperationException) {
            sampledWidth = source.width; sampledHeight = contentHeight
        } finally {
            bitmap?.recycle(); hardware?.recycle()
            if (Build.VERSION.SDK_INT >= 29) node?.discardDisplayList()
        }
        if (Color.alpha(footer.color) != 255) {
            val value = TypedValue()
            if (source.context.theme.resolveAttribute(android.R.attr.colorBackground, value, true) &&
                value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) footer.color = value.data
        }
    }

    fun restore() {
        if (attached) {
            target.overlay.remove(footer)
            // A replaced/detached source may now expose a different observer.
            val observer = layoutObserver?.takeIf { it.isAlive } ?: source.viewTreeObserver
            if (observer.isAlive) observer.removeOnGlobalLayoutListener(layoutChanges)
            layoutObserver = null
            attached = false
        }
        sampledWidth = 0; sampledHeight = 0
    }
}
