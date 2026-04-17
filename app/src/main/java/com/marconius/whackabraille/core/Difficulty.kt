package com.marconius.whackabraille.core

enum class Difficulty(val label: String, val isTimed: Boolean) {
    TRAINING(label = "Training", isTimed = false),
    BEGINNER(label = "Beginner", isTimed = true),
    NORMAL(label = "Normal", isTimed = true),
    SUPREME(label = "Supreme Mole Whacker", isTimed = true),
}
