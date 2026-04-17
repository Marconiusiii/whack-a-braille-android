package com.marconius.whackabraille.core

data class BrailleItem(
    val id: String,
    val displayLabel: String,
    val announceText: String,
    val dots: List<Int>,
    val dotMask: Int,
    val perkinsKeys: List<String>,
    val perkinsSequenceDots: List<List<Int>>,
    val perkinsSequenceMasks: List<Int>,
    val expectedPerkinsCellCount: Int,
    val standardKey: String?,
    val acceptedTextInputs: List<String>,
    val textInputTokenSequences: List<List<String>>,
    val modeTags: Set<String>,
    val nato: String?,
)
