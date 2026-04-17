package com.marconius.whackabraille.core

data class Attempt(
    val moleId: Int,
    val type: InputMode,
    val dotMask: Int? = null,
    val key: String? = null,
    val text: String? = null,
)
