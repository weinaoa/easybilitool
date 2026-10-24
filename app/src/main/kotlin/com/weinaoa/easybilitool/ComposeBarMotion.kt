/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import java.lang.reflect.Proxy
import kotlin.math.roundToInt

/** Measure the bar at its full size, then reveal its remaining height at the bottom.
 * The child is never squeezed; moving the outer top down matches native translation.
 * Only host Compose interfaces cross the class-loader boundary.
 */
internal class ComposeBarMotion(private val loader: ClassLoader) {
    private fun type(name: String) = Class.forName(name, false, loader)
    private val modifier = type("androidx.compose.ui.Modifier")
    private val empty = modifier.getField("Companion").get(null)
    private val then = modifier.getMethod("then", modifier)
    private val function1 = type("kotlin.jvm.functions.Function1")
    private val function3 = type("kotlin.jvm.functions.Function3")
    private val unit = type("kotlin.Unit").getField("INSTANCE").get(null)
    private val layoutModifier = type("androidx.compose.ui.layout.LayoutModifierKt").getMethod("layout", modifier, function3)
    private val clip = type("androidx.compose.ui.draw.ClipKt").getMethod("clipToBounds", modifier)
    private val alpha = type("androidx.compose.ui.draw.AlphaKt").getMethod("alpha", modifier, Float::class.javaPrimitiveType)
    private val constraints = type("androidx.compose.ui.unit.Constraints").getMethod("unbox-impl")
    private val measure = type("androidx.compose.ui.layout.Measurable").getMethod("measure-BRTryo0", Long::class.javaPrimitiveType)
    private val placeable = type("androidx.compose.ui.layout.Placeable")
    private val width = placeable.getMethod("getWidth")
    private val height = placeable.getMethod("getHeight")
    private val layout = type("androidx.compose.ui.layout.MeasureScope").getMethod("layout", Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType, Map::class.java, function1)
    private val place = type("androidx.compose.ui.layout.Placeable\$PlacementScope").getMethod("place", placeable,
        Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Float::class.javaPrimitiveType)

    private fun function(kind: Class<*>, body: (Array<out Any?>) -> Any?): Any =
        Proxy.newProxyInstance(kind.classLoader, arrayOf(kind)) { proxy, method, args ->
            when (method.name) {
                "invoke" -> body(args ?: emptyArray())
                "equals" -> proxy === args?.get(0)
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "BarMotionLayout"
                else -> error("Unexpected Compose function method: ${method.name}")
            }
        }

    fun apply(base: Any?, fraction: Float, keepBottom: Int = 0, measured: (Int) -> Unit = {}): Any? {
        val progress = fraction.coerceIn(0f, 1f)
        val measureBlock = function(function3) { args ->
            val child = measure.invoke(args[1], constraints.invoke(args[2]) as Long)
            val fullHeight = height.invoke(child) as Int
            measured(fullHeight)
            val safe = keepBottom.coerceIn(0, fullHeight)
            val visible = safe + ((fullHeight - safe) * (1f - progress)).roundToInt()
            // Retain only the system gesture area in layout; the child still
            // travels by fraction * fullHeight, matching the original bar.
            val childOffset = (safe * progress).roundToInt()
            val placement = function(function1) { placementArgs ->
                place.invoke(placementArgs[0], child, 0, childOffset, 0f)
                unit
            }
            layout.invoke(args[0], width.invoke(child), visible, emptyMap<Any, Any>(), placement)
        }
        val outer = layoutModifier.invoke(null, alpha.invoke(null, clip.invoke(null, empty), HomeBarSync.opacity(progress)), measureBlock)
        return then.invoke(outer, base)
    }
    companion object {
        fun navigationOverlap(activity: Activity, root: View): Int {
            val decor = activity.window.decorView
            val insets = decor.rootWindowInsets ?: return 0
            val nav = if (Build.VERSION.SDK_INT >= 30)
                insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom
                else insets.stableInsetBottom
            val position = IntArray(2); root.getLocationInWindow(position)
            return (position[1] + root.height - (decor.height - nav)).coerceIn(0, nav)
        }
    }

}
