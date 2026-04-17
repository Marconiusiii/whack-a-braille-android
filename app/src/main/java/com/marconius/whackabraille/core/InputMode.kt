package com.marconius.whackabraille.core

enum class InputMode(val label: String, val usesBufferedTextEntry: Boolean) {
    STANDARD(label = "Standard External Keyboard", usesBufferedTextEntry = false),
    PERKINS(label = "Perkins Home Row", usesBufferedTextEntry = false),
    BRAILLE_TEXT(label = "Braille Text Entry", usesBufferedTextEntry = true),
    BRAILLE_DISPLAY(label = "Braille Display Input", usesBufferedTextEntry = true),
}
