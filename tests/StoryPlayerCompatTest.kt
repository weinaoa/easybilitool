/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

/** Regression fixtures model the real arity/field-type change, not Android UI. */
object StoryPlayerCompatTest {
    open class HostContext
    class OldPlayer {
        @JvmField val H = HostContext()
        var received = intArrayOf()
        fun p3(offset: Int, left: Int, top: Int, right: Int, bottom: Int) {
            received = intArrayOf(offset, left, top, right, bottom)
        }
    }
    class NewPlayer {
        @JvmField val G = HostContext()
        @JvmField val H = false
        var received = intArrayOf()
        fun a1(offset: Int, top: Int) { received = intArrayOf(offset, top) }
    }
    class WrongField {
        @JvmField val G = false
        fun a1(offset: Int, top: Int) = Unit
    }
    class WrongReturn {
        @JvmField val G = HostContext()
        fun a1(offset: Int, top: Int): Int = offset + top
    }
    class WrongArity {
        @JvmField val G = HostContext()
        fun a1(offset: Int, left: Int, top: Int) = Unit
    }

    @JvmStatic fun main(args: Array<String>) {
        var checks = 0
        fun verify(value: Boolean) { check(value); checks++ }
        val old = OldPlayer()
        val current = NewPlayer()
        for ((player, values, topIndex) in listOf(
            Triple(old, intArrayOf(17, 3, 24, 4, 5), 2),
            Triple(current, intArrayOf(17, 24), 1),
        )) {
            val binding = StoryPlayerCompat.resolve(player.javaClass, HostContext::class.java)
            verify(binding.topIndex == topIndex)
            verify(binding.context.get(player) is HostContext)
            verify(binding.shouldRemoveTop(values, 24))
            verify(!binding.shouldRemoveTop(values, 0))
            verify(!binding.shouldRemoveTop(values, 25))
            verify(!binding.shouldRemoveTop(intArrayOf(), 24))
            val adjusted = values.copyOf().apply { this[binding.topIndex] = 0 }
            binding.viewport.invoke(player, *adjusted.toTypedArray())
            val received = if (player is OldPlayer) player.received else (player as NewPlayer).received
            verify(received.contentEquals(adjusted))
            verify(received[0] == 17) // Never remove the vertical crop/offset.
            verify(values[topIndex] == 24) // Retain the native request for pending replay.
            binding.viewport.invoke(player, *values.toTypedArray())
            val replayed = if (player is OldPlayer) player.received else (player as NewPlayer).received
            verify(replayed.contentEquals(values))
        }
        for (type in listOf(WrongField::class.java, WrongReturn::class.java, WrongArity::class.java)) {
            verify(runCatching { StoryPlayerCompat.resolve(type, HostContext::class.java) }
                .exceptionOrNull() is NoSuchMethodException)
        }
        println("StoryPlayerCompatTest: $checks checks passed")
    }
}
