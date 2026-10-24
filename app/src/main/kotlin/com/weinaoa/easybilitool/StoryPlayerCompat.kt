/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Exact contracts verified against the official 8.95.0 and 9.16.1 DEX files. */
internal object StoryPlayerCompat {
    data class Binding(val viewport: Method, val context: Field, val topIndex: Int) {
        fun shouldRemoveTop(values: IntArray, statusHeight: Int): Boolean =
            values.size == viewport.parameterCount && statusHeight > 0 && values[topIndex] == statusHeight
    }

    private data class Contract(val method: String, val context: String, val arity: Int, val topIndex: Int)
    private val contracts = listOf(
        Contract("a1", "G", 2, 1), // 9.16.1: offsetY, top; left/right/bottom are zero internally.
        Contract("p3", "H", 5, 2), // 8.95.0: offsetY, left, top, right, bottom.
    )

    fun resolve(player: Class<*>, contextType: Class<*>): Binding {
        for (contract in contracts) {
            val method = try {
                player.getDeclaredMethod(contract.method, *Array(contract.arity) { Int::class.javaPrimitiveType!! })
            } catch (_: NoSuchMethodException) { continue }
            val field = try {
                player.getDeclaredField(contract.context)
            } catch (_: NoSuchFieldException) { continue }
            // In 9.16.1 H still exists but is a Boolean, so a name-only lookup is unsafe.
            if (method.returnType != Void.TYPE || Modifier.isStatic(method.modifiers) ||
                Modifier.isStatic(field.modifiers) || !contextType.isAssignableFrom(field.type)) continue
            method.isAccessible = true
            field.isAccessible = true
            return Binding(method, field, contract.topIndex)
        }
        throw NoSuchMethodException("Unsupported StoryPlayer viewport/context contract")
    }
}
