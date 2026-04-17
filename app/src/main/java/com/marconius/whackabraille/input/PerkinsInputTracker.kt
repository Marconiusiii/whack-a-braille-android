package com.marconius.whackabraille.input

import android.view.KeyEvent

class PerkinsInputTracker {

    private val keyToDot = mapOf(
        KeyEvent.KEYCODE_F to 1,
        KeyEvent.KEYCODE_D to 2,
        KeyEvent.KEYCODE_S to 3,
        KeyEvent.KEYCODE_J to 4,
        KeyEvent.KEYCODE_K to 5,
        KeyEvent.KEYCODE_L to 6,
    )

    private val pressedKeys = linkedSetOf<Int>()
    private val chordDots = linkedSetOf<Int>()

    fun onKeyDown(keyCode: Int): Boolean {
        val dot = keyToDot[keyCode] ?: return false
        if (pressedKeys.add(keyCode)) {
            chordDots.add(dot)
        }
        return true
    }

    fun onKeyUp(keyCode: Int): Int? {
        if (keyToDot[keyCode] == null) return null
        val hadDots = chordDots.isNotEmpty()
        pressedKeys.remove(keyCode)

        if (!hadDots || pressedKeys.isNotEmpty()) {
            return null
        }

        var mask = 0
        for (dot in chordDots) {
            mask = mask or (1 shl (dot - 1))
        }

        chordDots.clear()
        return if (mask == 0) null else mask
    }

    fun reset() {
        pressedKeys.clear()
        chordDots.clear()
    }
}
