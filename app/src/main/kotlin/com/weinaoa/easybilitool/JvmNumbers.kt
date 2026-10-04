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

/** Preserve Java numeric promotion at mixed-type math call sites. */
internal object JvmNumbers {
    fun min(a: Int, b: Int): Int = kotlin.math.min(a.toInt(), b.toInt())
    fun max(a: Int, b: Int): Int = kotlin.math.max(a.toInt(), b.toInt())
    fun min(a: Int, b: Long): Long = kotlin.math.min(a.toLong(), b.toLong())
    fun max(a: Int, b: Long): Long = kotlin.math.max(a.toLong(), b.toLong())
    fun min(a: Int, b: Float): Float = kotlin.math.min(a.toFloat(), b.toFloat())
    fun max(a: Int, b: Float): Float = kotlin.math.max(a.toFloat(), b.toFloat())
    fun min(a: Int, b: Double): Double = kotlin.math.min(a.toDouble(), b.toDouble())
    fun max(a: Int, b: Double): Double = kotlin.math.max(a.toDouble(), b.toDouble())
    fun min(a: Long, b: Int): Long = kotlin.math.min(a.toLong(), b.toLong())
    fun max(a: Long, b: Int): Long = kotlin.math.max(a.toLong(), b.toLong())
    fun min(a: Long, b: Long): Long = kotlin.math.min(a.toLong(), b.toLong())
    fun max(a: Long, b: Long): Long = kotlin.math.max(a.toLong(), b.toLong())
    fun min(a: Long, b: Float): Float = kotlin.math.min(a.toFloat(), b.toFloat())
    fun max(a: Long, b: Float): Float = kotlin.math.max(a.toFloat(), b.toFloat())
    fun min(a: Long, b: Double): Double = kotlin.math.min(a.toDouble(), b.toDouble())
    fun max(a: Long, b: Double): Double = kotlin.math.max(a.toDouble(), b.toDouble())
    fun min(a: Float, b: Int): Float = kotlin.math.min(a.toFloat(), b.toFloat())
    fun max(a: Float, b: Int): Float = kotlin.math.max(a.toFloat(), b.toFloat())
    fun min(a: Float, b: Long): Float = kotlin.math.min(a.toFloat(), b.toFloat())
    fun max(a: Float, b: Long): Float = kotlin.math.max(a.toFloat(), b.toFloat())
    fun min(a: Float, b: Float): Float = kotlin.math.min(a.toFloat(), b.toFloat())
    fun max(a: Float, b: Float): Float = kotlin.math.max(a.toFloat(), b.toFloat())
    fun min(a: Float, b: Double): Double = kotlin.math.min(a.toDouble(), b.toDouble())
    fun max(a: Float, b: Double): Double = kotlin.math.max(a.toDouble(), b.toDouble())
    fun min(a: Double, b: Int): Double = kotlin.math.min(a.toDouble(), b.toDouble())
    fun max(a: Double, b: Int): Double = kotlin.math.max(a.toDouble(), b.toDouble())
    fun min(a: Double, b: Long): Double = kotlin.math.min(a.toDouble(), b.toDouble())
    fun max(a: Double, b: Long): Double = kotlin.math.max(a.toDouble(), b.toDouble())
    fun min(a: Double, b: Float): Double = kotlin.math.min(a.toDouble(), b.toDouble())
    fun max(a: Double, b: Float): Double = kotlin.math.max(a.toDouble(), b.toDouble())
    fun min(a: Double, b: Double): Double = kotlin.math.min(a.toDouble(), b.toDouble())
    fun max(a: Double, b: Double): Double = kotlin.math.max(a.toDouble(), b.toDouble())
    fun hypot(a: Number, b: Number): Double = Math.hypot(a.toDouble(), b.toDouble())
    fun pow(a: Number, b: Number): Double = Math.pow(a.toDouble(), b.toDouble())
}

internal operator fun Number.plus(text: String): String = toString() + text
internal operator fun Boolean.plus(text: String): String = toString() + text
