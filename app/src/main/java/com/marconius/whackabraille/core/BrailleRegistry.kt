package com.marconius.whackabraille.core

object BrailleRegistry {

    data class ModeOption(val id: String, val label: String)

    private const val BRAILLE_PATTERN_BASE = 0x2800

    private fun dotsToMask(dots: List<Int>): Int {
        var mask = 0
        for (dot in dots) {
            mask = mask or (1 shl (dot - 1))
        }
        return mask
    }

    private fun dotsToPerkinsKeys(dots: List<Int>): List<String> {
        val map = mapOf(
            1 to "f",
            2 to "d",
            3 to "s",
            4 to "j",
            5 to "k",
            6 to "l",
        )
        return dots.mapNotNull(map::get)
    }

    private fun normalizeSequence(rawSequence: List<List<Int>>?, fallbackDots: List<Int>): List<List<Int>> {
        val source = if (!rawSequence.isNullOrEmpty()) rawSequence else listOf(fallbackDots)
        return source.map { it.sorted() }
    }

    private fun normalizeTokens(tokens: List<String>): List<String> =
        tokens.map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    private fun dedupeInputs(inputs: List<String>): List<String> =
        normalizeTokens(inputs).distinct()

    private fun dedupeTokenSequences(sequences: List<List<String>>): List<List<String>> =
        sequences.map(::normalizeTokens).filter { it.isNotEmpty() }.distinct()

    private fun makeItem(
        id: String,
        announce: String,
        dots: List<Int>,
        modes: List<String>,
        standardKey: String? = null,
        acceptedInputs: List<String> = emptyList(),
        textTokenSequences: List<List<String>>? = null,
        perkinsSequence: List<List<Int>>? = null,
        nato: String? = null,
    ): BrailleItem {
        val acceptedTextInputs = buildList {
            add(id)
            if (standardKey != null) add(standardKey)
            addAll(acceptedInputs)
        }
        val normalizedAcceptedInputs = dedupeInputs(acceptedTextInputs)
        val normalizedPerkinsSequence = normalizeSequence(perkinsSequence, dots)
        val normalizedTokenSequences = dedupeTokenSequences(
            textTokenSequences ?: normalizedAcceptedInputs.map { listOf(it) }
        )

        return BrailleItem(
            id = id,
            displayLabel = id,
            announceText = announce,
            dots = dots,
            dotMask = dotsToMask(dots),
            perkinsKeys = dotsToPerkinsKeys(dots),
            perkinsSequenceDots = normalizedPerkinsSequence,
            perkinsSequenceMasks = normalizedPerkinsSequence.map(::dotsToMask),
            expectedPerkinsCellCount = normalizedPerkinsSequence.size,
            standardKey = standardKey,
            acceptedTextInputs = normalizedAcceptedInputs,
            textInputTokenSequences = normalizedTokenSequences,
            modeTags = modes.toSet(),
            nato = nato,
        )
    }

    val modeOptions: List<ModeOption> = listOf(
        ModeOption("grade1Letters", "Grade 1 Letters"),
        ModeOption("grade1Numbers", "Grade 1 Numbers"),
        ModeOption("grade1LettersNumbers", "Grade 1 Letters and Numbers"),
        ModeOption("grade2Symbols", "Grade 2 Contractions"),
        ModeOption("grade2Words", "Grade 2 Word Signs"),
        ModeOption("grade1MoleInvasion", "Grade 1 Mole Invasion"),
        ModeOption("grade2MoleInvasion", "Grade 2 Mole Invasion"),
    )

    val grade1Letters: List<BrailleItem> = listOf(
        makeItem("a", "a", listOf(1), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "a", nato = "Alpha"),
        makeItem("b", "b", listOf(1, 2), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "b", nato = "Bravo"),
        makeItem("c", "c", listOf(1, 4), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "c", nato = "Charlie"),
        makeItem("d", "d", listOf(1, 4, 5), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "d", nato = "Delta"),
        makeItem("e", "e", listOf(1, 5), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "e", nato = "Echo"),
        makeItem("f", "f", listOf(1, 2, 4), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "f", nato = "Foxtrot"),
        makeItem("g", "g", listOf(1, 2, 4, 5), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "g", nato = "Golf"),
        makeItem("h", "h", listOf(1, 2, 5), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "h", nato = "Hotel"),
        makeItem("i", "i", listOf(2, 4), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "i", nato = "India"),
        makeItem("j", "j", listOf(2, 4, 5), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "j", nato = "Juliet"),
        makeItem("k", "k", listOf(1, 3), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "k", nato = "Kilo"),
        makeItem("l", "l", listOf(1, 2, 3), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "l", nato = "Lima"),
        makeItem("m", "m", listOf(1, 3, 4), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "m", nato = "Mike"),
        makeItem("n", "n", listOf(1, 3, 4, 5), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "n", nato = "November"),
        makeItem("o", "o", listOf(1, 3, 5), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "o", nato = "Oscar"),
        makeItem("p", "p", listOf(1, 2, 3, 4), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "p", nato = "Papa"),
        makeItem("q", "q", listOf(1, 2, 3, 4, 5), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "q", nato = "Quebec"),
        makeItem("r", "r", listOf(1, 2, 3, 5), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "r", nato = "Romeo"),
        makeItem("s", "s", listOf(2, 3, 4), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "s", nato = "Sierra"),
        makeItem("t", "t", listOf(2, 3, 4, 5), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "t", nato = "Tango"),
        makeItem("u", "u", listOf(1, 3, 6), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "u", nato = "Uniform"),
        makeItem("v", "v", listOf(1, 2, 3, 6), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "v", nato = "Victor"),
        makeItem("w", "w", listOf(2, 4, 5, 6), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "w", nato = "Whiskey"),
        makeItem("x", "x", listOf(1, 3, 4, 6), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "x", nato = "X-ray"),
        makeItem("y", "y", listOf(1, 3, 4, 5, 6), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "y", nato = "Yankee"),
        makeItem("z", "z", listOf(1, 3, 5, 6), listOf("grade1Letters", "grade1LettersNumbers"), standardKey = "z", nato = "Zulu"),
    )

    val grade1Numbers: List<BrailleItem> = listOf(
        makeItem("1", "1", listOf(1), listOf("grade1Numbers", "grade1LettersNumbers"), standardKey = "1", acceptedInputs = listOf("#a"), textTokenSequences = listOf(listOf("1"), listOf("#", "a")), perkinsSequence = listOf(listOf(3, 4, 5, 6), listOf(1))),
        makeItem("2", "2", listOf(1, 2), listOf("grade1Numbers", "grade1LettersNumbers"), standardKey = "2", acceptedInputs = listOf("#b"), textTokenSequences = listOf(listOf("2"), listOf("#", "b")), perkinsSequence = listOf(listOf(3, 4, 5, 6), listOf(1, 2))),
        makeItem("3", "3", listOf(1, 4), listOf("grade1Numbers", "grade1LettersNumbers"), standardKey = "3", acceptedInputs = listOf("#c"), textTokenSequences = listOf(listOf("3"), listOf("#", "c")), perkinsSequence = listOf(listOf(3, 4, 5, 6), listOf(1, 4))),
        makeItem("4", "4", listOf(1, 4, 5), listOf("grade1Numbers", "grade1LettersNumbers"), standardKey = "4", acceptedInputs = listOf("#d"), textTokenSequences = listOf(listOf("4"), listOf("#", "d")), perkinsSequence = listOf(listOf(3, 4, 5, 6), listOf(1, 4, 5))),
        makeItem("5", "5", listOf(1, 5), listOf("grade1Numbers", "grade1LettersNumbers"), standardKey = "5", acceptedInputs = listOf("#e"), textTokenSequences = listOf(listOf("5"), listOf("#", "e")), perkinsSequence = listOf(listOf(3, 4, 5, 6), listOf(1, 5))),
        makeItem("6", "6", listOf(1, 2, 4), listOf("grade1Numbers", "grade1LettersNumbers"), standardKey = "6", acceptedInputs = listOf("#f"), textTokenSequences = listOf(listOf("6"), listOf("#", "f")), perkinsSequence = listOf(listOf(3, 4, 5, 6), listOf(1, 2, 4))),
        makeItem("7", "7", listOf(1, 2, 4, 5), listOf("grade1Numbers", "grade1LettersNumbers"), standardKey = "7", acceptedInputs = listOf("#g"), textTokenSequences = listOf(listOf("7"), listOf("#", "g")), perkinsSequence = listOf(listOf(3, 4, 5, 6), listOf(1, 2, 4, 5))),
        makeItem("8", "8", listOf(1, 2, 5), listOf("grade1Numbers", "grade1LettersNumbers"), standardKey = "8", acceptedInputs = listOf("#h"), textTokenSequences = listOf(listOf("8"), listOf("#", "h")), perkinsSequence = listOf(listOf(3, 4, 5, 6), listOf(1, 2, 5))),
        makeItem("9", "9", listOf(2, 4), listOf("grade1Numbers", "grade1LettersNumbers"), standardKey = "9", acceptedInputs = listOf("#i"), textTokenSequences = listOf(listOf("9"), listOf("#", "i")), perkinsSequence = listOf(listOf(3, 4, 5, 6), listOf(2, 4))),
        makeItem("0", "0", listOf(2, 4, 5), listOf("grade1Numbers", "grade1LettersNumbers"), standardKey = "0", acceptedInputs = listOf("#j"), textTokenSequences = listOf(listOf("0"), listOf("#", "j")), perkinsSequence = listOf(listOf(3, 4, 5, 6), listOf(2, 4, 5))),
    )

    val all: List<BrailleItem> = grade1Letters + grade1Numbers

    fun filteredModeOptions(inputMode: InputMode): List<ModeOption> {
        return when (inputMode) {
            InputMode.BRAILLE_DISPLAY ->
                modeOptions.filterNot { it.id == "grade2Symbols" }
            else -> modeOptions
        }
    }

    fun sanitizedModeId(modeId: String, inputMode: InputMode): String {
        val allowedModeIds = filteredModeOptions(inputMode).map { it.id }.toSet()
        return if (modeId in allowedModeIds) modeId else filteredModeOptions(inputMode).first().id
    }

    fun getItemsForMode(modeId: String): List<BrailleItem> {
        return when (modeId) {
            "grade1Letters" -> grade1Letters
            "grade1Numbers" -> grade1Numbers
            "grade1LettersNumbers" -> all
            else -> all.filter { modeId in it.modeTags }
        }
    }

    fun brailleUnicodeForDots(dots: List<Int>): String {
        val scalar = BRAILLE_PATTERN_BASE + dotsToMask(dots)
        return String(Character.toChars(scalar))
    }
}
