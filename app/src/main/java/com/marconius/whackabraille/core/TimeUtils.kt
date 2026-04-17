package com.marconius.whackabraille.core

object TimeUtils {

    fun nowMs(): Int = System.currentTimeMillis().toInt()

    fun lerp(start: Int, end: Int, t: Double): Int {
        return (start + ((end - start) * t)).toInt()
    }
}
