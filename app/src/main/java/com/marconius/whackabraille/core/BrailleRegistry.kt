package com.marconius.whackabraille.core

object BrailleRegistry {

    data class ModeOption(val id: String, val label: String)

    private const val braillePatternBase = 0x2800

    private val qwertyModeIds = setOf(
        "typingSimpleHomeRow",
        "typingHomeRow",
        "typingHomeTopRow",
        "typingHomeBottomRow",
    )

    private val qwertyUnsupportedBrailleModeIds = setOf("grade2Dot456Initials")
    private val bsiExcludedModeIds = qwertyModeIds
    private val bufferedTextUnsupportedModeIds = setOf("grade2Suffixes")

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

    private fun normalizePerkinsSequence(rawSequence: List<List<Int>>?, fallbackDots: List<Int>): List<List<Int>> {
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
        val normalizedPerkinsSequence = normalizePerkinsSequence(perkinsSequence, dots)
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

    private fun makeTypingItem(
        key: String,
        announce: String,
        modes: List<String>,
    ): BrailleItem {
        return BrailleItem(
            id = key,
            displayLabel = key,
            announceText = announce,
            dots = emptyList(),
            dotMask = 0,
            perkinsKeys = emptyList(),
            perkinsSequenceDots = emptyList(),
            perkinsSequenceMasks = emptyList(),
            expectedPerkinsCellCount = 1,
            standardKey = key,
            acceptedTextInputs = listOf(key),
            textInputTokenSequences = listOf(listOf(key.lowercase())),
            modeTags = modes.toSet(),
            nato = null,
        )
    }

    private fun makeSequenceItem(
        id: String,
        announce: String,
        sequenceDots: List<List<Int>>,
        modes: List<String>,
        acceptedInputs: List<String> = emptyList(),
        textTokenSequences: List<List<String>> = emptyList(),
    ): BrailleItem {
        val finalDots = sequenceDots.lastOrNull() ?: emptyList()
        return makeItem(
            id = id,
            announce = announce,
            dots = finalDots,
            modes = modes,
            acceptedInputs = acceptedInputs,
            textTokenSequences = textTokenSequences,
            perkinsSequence = sequenceDots,
        )
    }

    private fun letterInRange(item: BrailleItem, end: Char): Boolean {
        val first = item.id.uppercase().firstOrNull() ?: return false
        return first in 'A'..end
    }

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

    val grade2Symbols: List<BrailleItem> = listOf(
        makeItem("er", "E R", listOf(1, 2, 4, 5, 6), listOf("grade2Symbols"), acceptedInputs = listOf("}")),
        makeItem("ed", "E D", listOf(1, 2, 4, 6), listOf("grade2Symbols"), acceptedInputs = listOf("$")),
        makeItem("gh", "G H", listOf(1, 2, 6), listOf("grade2Symbols"), acceptedInputs = listOf("<")),
        makeItem("ar", "A R", listOf(3, 4, 5), listOf("grade2Symbols"), acceptedInputs = listOf(">")),
        makeItem("ow", "O W", listOf(2, 4, 6), listOf("grade2Symbols"), acceptedInputs = listOf("{")),
        makeItem("ou", "O U", listOf(1, 2, 5, 6), listOf("grade2Symbols"), acceptedInputs = listOf("|", "out")),
        makeItem("st", "S T", listOf(3, 4), listOf("grade2Symbols"), acceptedInputs = listOf("/", "still")),
        makeItem("ch", "C H", listOf(1, 6), listOf("grade2Symbols"), acceptedInputs = listOf("*", "child")),
        makeItem("wh", "W H", listOf(1, 5, 6), listOf("grade2Symbols"), acceptedInputs = listOf(":")),
        makeItem("ing", "I N G", listOf(3, 4, 6), listOf("grade2Symbols"), acceptedInputs = listOf("+")),
        makeItem("dis", "dis", listOf(2, 5, 6), listOf("grade2Symbols"), acceptedInputs = listOf("4", ".")),
        makeItem("con", "con", listOf(2, 5), listOf("grade2Symbols"), acceptedInputs = listOf("3", ":")),
        makeItem("of", "Of", listOf(1, 2, 3, 5, 6), listOf("grade2Symbols"), acceptedInputs = listOf("(")),
        makeItem("with", "with", listOf(2, 3, 4, 5, 6), listOf("grade2Symbols"), acceptedInputs = listOf(")")),
        makeItem("and", "and", listOf(1, 2, 3, 4, 6), listOf("grade2Symbols"), acceptedInputs = listOf("&")),
        makeItem("for", "for", listOf(1, 2, 3, 4, 5, 6), listOf("grade2Symbols"), acceptedInputs = listOf("=")),
        makeItem("the", "The", listOf(2, 3, 4, 6), listOf("grade2Symbols"), acceptedInputs = listOf("!")),
    )

    val grade2Words: List<BrailleItem> = listOf(
        makeItem("but", "But", listOf(1, 2), listOf("grade2Words"), acceptedInputs = listOf("b")),
        makeItem("can", "Can", listOf(1, 4), listOf("grade2Words"), acceptedInputs = listOf("c")),
        makeItem("do", "Do", listOf(1, 4, 5), listOf("grade2Words"), acceptedInputs = listOf("d")),
        makeItem("every", "Every", listOf(1, 5), listOf("grade2Words"), acceptedInputs = listOf("e")),
        makeItem("from", "From", listOf(1, 2, 4), listOf("grade2Words"), acceptedInputs = listOf("f")),
        makeItem("go", "Go", listOf(1, 2, 4, 5), listOf("grade2Words"), acceptedInputs = listOf("g")),
        makeItem("have", "Have", listOf(1, 2, 5), listOf("grade2Words"), acceptedInputs = listOf("h")),
        makeItem("just", "Just", listOf(2, 4, 5), listOf("grade2Words"), acceptedInputs = listOf("j")),
        makeItem("knowledge", "Knowledge", listOf(1, 3), listOf("grade2Words"), acceptedInputs = listOf("k")),
        makeItem("like", "Like", listOf(1, 2, 3), listOf("grade2Words"), acceptedInputs = listOf("l")),
        makeItem("more", "More", listOf(1, 3, 4), listOf("grade2Words"), acceptedInputs = listOf("m")),
        makeItem("not", "Not", listOf(1, 3, 4, 5), listOf("grade2Words"), acceptedInputs = listOf("n")),
        makeItem("people", "People", listOf(1, 2, 3, 4), listOf("grade2Words"), acceptedInputs = listOf("p")),
        makeItem("quite", "Quite", listOf(1, 2, 3, 4, 5), listOf("grade2Words"), acceptedInputs = listOf("q")),
        makeItem("rather", "Rather", listOf(1, 2, 3, 5), listOf("grade2Words"), acceptedInputs = listOf("r")),
        makeItem("so", "So", listOf(2, 3, 4), listOf("grade2Words"), acceptedInputs = listOf("s")),
        makeItem("that", "That", listOf(2, 3, 4, 5), listOf("grade2Words"), acceptedInputs = listOf("t")),
        makeItem("us", "Us", listOf(1, 3, 6), listOf("grade2Words"), acceptedInputs = listOf("u")),
        makeItem("very", "Very", listOf(1, 2, 3, 6), listOf("grade2Words"), acceptedInputs = listOf("v")),
        makeItem("will", "Will", listOf(2, 4, 5, 6), listOf("grade2Words"), acceptedInputs = listOf("w")),
        makeItem("it", "It", listOf(1, 3, 4, 6), listOf("grade2Words"), acceptedInputs = listOf("x")),
        makeItem("you", "You", listOf(1, 3, 4, 5, 6), listOf("grade2Words"), acceptedInputs = listOf("y")),
        makeItem("as", "As", listOf(1, 3, 5, 6), listOf("grade2Words"), acceptedInputs = listOf("z")),
        makeItem("this", "This", listOf(1, 4, 5, 6), listOf("grade2Words"), acceptedInputs = listOf("?")),
        makeItem("which", "Which", listOf(1, 5, 6), listOf("grade2Words"), acceptedInputs = listOf("w", ":")),
        makeItem("child", "Child", listOf(1, 6), listOf("grade2Words"), acceptedInputs = listOf("c", "*")),
        makeItem("shall", "Shall", listOf(1, 4, 6), listOf("grade2Words"), acceptedInputs = listOf("s", "%")),
    )

    val grade2ShortformWords: List<BrailleItem> = listOf(
        makeItem("be", "Be", listOf(2, 3), listOf("grade2Shortforms"), acceptedInputs = listOf("2"), textTokenSequences = listOf(listOf("2"))),
        makeItem("in", "In", listOf(3, 5), listOf("grade2Shortforms"), acceptedInputs = listOf("9"), textTokenSequences = listOf(listOf("9"))),
        makeItem("enough", "Enough", listOf(2, 6), listOf("grade2Shortforms"), acceptedInputs = listOf("5"), textTokenSequences = listOf(listOf("5"))),
        makeItem("his", "His", listOf(2, 3, 6), listOf("grade2Shortforms"), acceptedInputs = listOf("8"), textTokenSequences = listOf(listOf("8"))),
        makeItem("was", "Was", listOf(3, 5, 6), listOf("grade2Shortforms"), acceptedInputs = listOf("7"), textTokenSequences = listOf(listOf("7"))),
        makeItem("were", "Were", listOf(2, 3, 5, 6), listOf("grade2Shortforms"), acceptedInputs = listOf("0"), textTokenSequences = listOf(listOf("0"))),
        makeSequenceItem("children", "Children", listOf(listOf(1, 6), listOf(1, 3, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("*", "n"))),
        makeSequenceItem("could", "Could", listOf(listOf(1, 4), listOf(1, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("c", "d"))),
        makeSequenceItem("first", "First", listOf(listOf(1, 2, 4), listOf(3, 4)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("f", "/"))),
        makeSequenceItem("good", "Good", listOf(listOf(1, 2, 4, 5), listOf(1, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("g", "d"))),
        makeSequenceItem("letter", "Letter", listOf(listOf(1, 2, 3), listOf(1, 2, 3, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("l", "r"))),
        makeSequenceItem("must", "Must", listOf(listOf(1, 3, 4), listOf(3, 4)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("m", "/"))),
        makeSequenceItem("quick", "Quick", listOf(listOf(1, 2, 3, 4, 5), listOf(1, 3)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("q", "k"))),
        makeSequenceItem("paid", "Paid", listOf(listOf(1, 2, 3, 4), listOf(1, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("p", "d"))),
        makeSequenceItem("said", "Said", listOf(listOf(2, 3, 4), listOf(1, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("s", "d"))),
        makeSequenceItem("would", "Would", listOf(listOf(2, 4, 5, 6), listOf(1, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("w", "d"))),
        makeSequenceItem("should", "Should", listOf(listOf(1, 4, 6), listOf(1, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("%", "d"))),
        makeSequenceItem("its", "Its", listOf(listOf(1, 3, 4, 6), listOf(2, 3, 4)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("x", "s"))),
        makeSequenceItem("your", "Your", listOf(listOf(1, 3, 4, 5, 6), listOf(1, 2, 3, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("y", "r"))),
        makeSequenceItem("him", "Him", listOf(listOf(1, 2, 5), listOf(1, 3, 4)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("h", "m"))),
        makeSequenceItem("much", "Much", listOf(listOf(1, 3, 4), listOf(1, 6)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("m", "*"))),
        makeSequenceItem("such", "Such", listOf(listOf(2, 3, 4), listOf(1, 6)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("s", "*"))),
        makeSequenceItem("because", "Because", listOf(listOf(2, 3), listOf(1, 4)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("2", "c"))),
        makeSequenceItem("before", "Before", listOf(listOf(2, 3), listOf(1, 2, 4)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("2", "f"))),
        makeSequenceItem("behind", "Behind", listOf(listOf(2, 3), listOf(1, 2, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("2", "h"))),
        makeSequenceItem("below", "Below", listOf(listOf(2, 3), listOf(1, 2, 3)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("2", "l"))),
        makeSequenceItem("beneath", "Beneath", listOf(listOf(2, 3), listOf(1, 3, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("2", "n"))),
        makeSequenceItem("beside", "Beside", listOf(listOf(2, 3), listOf(2, 3, 4)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("2", "s"))),
        makeSequenceItem("between", "Between", listOf(listOf(2, 3), listOf(2, 3, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("2", "t"))),
        makeSequenceItem("beyond", "Beyond", listOf(listOf(2, 3), listOf(1, 3, 4, 5, 6)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("2", "y"))),
        makeSequenceItem("today", "Today", listOf(listOf(2, 3, 4, 5), listOf(1, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("t", "d"))),
        makeSequenceItem("tomorrow", "Tomorrow", listOf(listOf(2, 3, 4, 5), listOf(1, 3, 4)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("t", "m"))),
        makeSequenceItem("tonight", "Tonight", listOf(listOf(2, 3, 4, 5), listOf(1, 3, 4, 5)), listOf("grade2Shortforms"), textTokenSequences = listOf(listOf("t", "n"))),
    )

    val grade2Dot5Initials: List<BrailleItem> = listOf(
        makeSequenceItem("day", "Day", listOf(listOf(5), listOf(1, 4, 5)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "d"))),
        makeSequenceItem("ever", "Ever", listOf(listOf(5), listOf(1, 5)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "e"))),
        makeSequenceItem("father", "Father", listOf(listOf(5), listOf(1, 2, 4)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "f"))),
        makeSequenceItem("here", "Here", listOf(listOf(5), listOf(1, 2, 5)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "h"))),
        makeSequenceItem("know", "Know", listOf(listOf(5), listOf(1, 3)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "k"))),
        makeSequenceItem("lord", "Lord", listOf(listOf(5), listOf(1, 2, 3)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "l"))),
        makeSequenceItem("mother", "Mother", listOf(listOf(5), listOf(1, 3, 4)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "m"))),
        makeSequenceItem("name", "Name", listOf(listOf(5), listOf(1, 3, 4, 5)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "n"))),
        makeSequenceItem("one", "One", listOf(listOf(5), listOf(1, 3, 5)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "o"))),
        makeSequenceItem("part", "Part", listOf(listOf(5), listOf(1, 2, 3, 4)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "p"))),
        makeSequenceItem("question", "Question", listOf(listOf(5), listOf(1, 2, 3, 4, 5)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "q"))),
        makeSequenceItem("right", "Right", listOf(listOf(5), listOf(1, 2, 3, 5)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "r"))),
        makeSequenceItem("some", "Some", listOf(listOf(5), listOf(2, 3, 4)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "s"))),
        makeSequenceItem("time", "Time", listOf(listOf(5), listOf(2, 3, 4, 5)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "t"))),
        makeSequenceItem("there", "There", listOf(listOf(5), listOf(2, 3, 4, 6)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "!"))),
        makeSequenceItem("through", "Through", listOf(listOf(5), listOf(1, 4, 5, 6)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "?"))),
        makeSequenceItem("under", "Under", listOf(listOf(5), listOf(1, 3, 6)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "u"))),
        makeSequenceItem("where", "Where", listOf(listOf(5), listOf(1, 5, 6)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", ":"))),
        makeSequenceItem("work", "Work", listOf(listOf(5), listOf(2, 4, 5, 6)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "w"))),
        makeSequenceItem("young", "Young", listOf(listOf(5), listOf(1, 3, 4, 5, 6)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "y"))),
        makeSequenceItem("character", "Character", listOf(listOf(5), listOf(1, 6)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "*"))),
        makeSequenceItem("ought", "Ought", listOf(listOf(5), listOf(1, 2, 5, 6)), listOf("grade2Dot5Initials"), textTokenSequences = listOf(listOf("'", "|"))),
    )

    val grade2Dot45Initials: List<BrailleItem> = listOf(
        makeSequenceItem("upon", "Upon", listOf(listOf(4, 5), listOf(1, 3, 6)), listOf("grade2Dot45Initials"), textTokenSequences = listOf(listOf("~", "u"))),
        makeSequenceItem("word", "Word", listOf(listOf(4, 5), listOf(2, 4, 5, 6)), listOf("grade2Dot45Initials"), textTokenSequences = listOf(listOf("~", "w"))),
        makeSequenceItem("these", "These", listOf(listOf(4, 5), listOf(2, 3, 4, 6)), listOf("grade2Dot45Initials"), textTokenSequences = listOf(listOf("~", "!"))),
        makeSequenceItem("those", "Those", listOf(listOf(4, 5), listOf(1, 4, 5, 6)), listOf("grade2Dot45Initials"), textTokenSequences = listOf(listOf("~", "?"))),
        makeSequenceItem("whose", "Whose", listOf(listOf(4, 5), listOf(1, 5, 6)), listOf("grade2Dot45Initials"), textTokenSequences = listOf(listOf("~", ":"))),
    )

    val grade2Suffixes: List<BrailleItem> = listOf(
        makeSequenceItem("ance", "A N C E", listOf(listOf(4, 6), listOf(1, 5)), listOf("grade2Suffixes"), acceptedInputs = listOf("-ance", "ε", ".e"), textTokenSequences = listOf(listOf(".", "e"))),
        makeSequenceItem("sion", "S I O N", listOf(listOf(4, 6), listOf(1, 3, 4, 5)), listOf("grade2Suffixes"), acceptedInputs = listOf("-sion", ".n"), textTokenSequences = listOf(listOf(".", "n"))),
        makeSequenceItem("less", "L E S S", listOf(listOf(4, 6), listOf(2, 3, 4)), listOf("grade2Suffixes"), acceptedInputs = listOf("-less", ".s"), textTokenSequences = listOf(listOf(".", "s"))),
        makeSequenceItem("ound", "O U N D", listOf(listOf(4, 6), listOf(1, 4, 5)), listOf("grade2Suffixes"), acceptedInputs = listOf("-ound", ".d"), textTokenSequences = listOf(listOf(".", "d"))),
        makeSequenceItem("ount", "O U N T", listOf(listOf(4, 6), listOf(2, 3, 4, 5)), listOf("grade2Suffixes"), acceptedInputs = listOf("-ount", ".t"), textTokenSequences = listOf(listOf(".", "t"))),
        makeSequenceItem("ence", "E N C E", listOf(listOf(5, 6), listOf(1, 5)), listOf("grade2Suffixes"), acceptedInputs = listOf("-ence", ";e", "e"), textTokenSequences = listOf(listOf(";", "e"))),
        makeSequenceItem("ong", "O N G", listOf(listOf(5, 6), listOf(1, 2, 4, 5)), listOf("grade2Suffixes"), acceptedInputs = listOf("-ong", ";g", "g"), textTokenSequences = listOf(listOf(";", "g"))),
        makeSequenceItem("ful", "F U L", listOf(listOf(5, 6), listOf(1, 2, 4)), listOf("grade2Suffixes"), acceptedInputs = listOf("-ful", ";f", "f"), textTokenSequences = listOf(listOf(";", "f"))),
        makeSequenceItem("tion", "T I O N", listOf(listOf(5, 6), listOf(2, 3, 4, 5)), listOf("grade2Suffixes"), acceptedInputs = listOf("-tion", ";t", "t"), textTokenSequences = listOf(listOf(";", "t"))),
        makeSequenceItem("ness", "N E S S", listOf(listOf(5, 6), listOf(1, 3, 4, 5)), listOf("grade2Suffixes"), acceptedInputs = listOf("-ness", ";n", "n"), textTokenSequences = listOf(listOf(";", "n"))),
        makeSequenceItem("ment", "M E N T", listOf(listOf(5, 6), listOf(1, 3, 4)), listOf("grade2Suffixes"), acceptedInputs = listOf("-ment", ";m", "m"), textTokenSequences = listOf(listOf(";", "m"))),
        makeSequenceItem("ity", "I T Y", listOf(listOf(5, 6), listOf(1, 3, 4, 5, 6)), listOf("grade2Suffixes"), acceptedInputs = listOf("-ity", ";y", "y"), textTokenSequences = listOf(listOf(";", "y"))),
    )

    val grade2Dot456Initials: List<BrailleItem> = listOf(
        makeSequenceItem("cannot", "Cannot", listOf(listOf(4, 5, 6), listOf(1, 4)), listOf("grade2Dot456Initials"), textTokenSequences = listOf(listOf("cannot"))),
        makeSequenceItem("had", "Had", listOf(listOf(4, 5, 6), listOf(1, 2, 5)), listOf("grade2Dot456Initials"), textTokenSequences = listOf(listOf("had"))),
        makeSequenceItem("many", "Many", listOf(listOf(4, 5, 6), listOf(1, 3, 4)), listOf("grade2Dot456Initials"), textTokenSequences = listOf(listOf("many"))),
        makeSequenceItem("spirit", "Spirit", listOf(listOf(4, 5, 6), listOf(2, 3, 4)), listOf("grade2Dot456Initials"), textTokenSequences = listOf(listOf("spirit"))),
        makeSequenceItem("their", "Their", listOf(listOf(4, 5, 6), listOf(2, 3, 4, 6)), listOf("grade2Dot456Initials"), textTokenSequences = listOf(listOf("their"))),
        makeSequenceItem("world", "World", listOf(listOf(4, 5, 6), listOf(2, 4, 5, 6)), listOf("grade2Dot456Initials"), textTokenSequences = listOf(listOf("world"))),
    )

    val typingSimpleHomeRowItems: List<BrailleItem> = listOf(
        makeTypingItem("a", "a", listOf("typingSimpleHomeRow")),
        makeTypingItem("s", "s", listOf("typingSimpleHomeRow")),
        makeTypingItem("d", "d", listOf("typingSimpleHomeRow")),
        makeTypingItem("f", "f", listOf("typingSimpleHomeRow")),
        makeTypingItem("j", "j", listOf("typingSimpleHomeRow")),
        makeTypingItem("k", "k", listOf("typingSimpleHomeRow")),
        makeTypingItem("l", "l", listOf("typingSimpleHomeRow")),
        makeTypingItem(";", "semicolon", listOf("typingSimpleHomeRow")),
    )

    val typingHomeRowItems: List<BrailleItem> = listOf(
        makeTypingItem("a", "a", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
        makeTypingItem("s", "s", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
        makeTypingItem("d", "d", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
        makeTypingItem("f", "f", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
        makeTypingItem("g", "g", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
        makeTypingItem("h", "h", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
        makeTypingItem("j", "j", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
        makeTypingItem("k", "k", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
        makeTypingItem("l", "l", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
        makeTypingItem(";", "semicolon", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
        makeTypingItem("'", "apostrophe", listOf("typingHomeRow", "typingHomeTopRow", "typingHomeBottomRow")),
    )

    val typingTopRowItems: List<BrailleItem> = listOf(
        makeTypingItem("q", "q", listOf("typingHomeTopRow")),
        makeTypingItem("w", "w", listOf("typingHomeTopRow")),
        makeTypingItem("e", "e", listOf("typingHomeTopRow")),
        makeTypingItem("r", "r", listOf("typingHomeTopRow")),
        makeTypingItem("t", "t", listOf("typingHomeTopRow")),
        makeTypingItem("y", "y", listOf("typingHomeTopRow")),
        makeTypingItem("u", "u", listOf("typingHomeTopRow")),
        makeTypingItem("i", "i", listOf("typingHomeTopRow")),
        makeTypingItem("o", "o", listOf("typingHomeTopRow")),
        makeTypingItem("p", "p", listOf("typingHomeTopRow")),
        makeTypingItem("[", "left bracket", listOf("typingHomeTopRow")),
        makeTypingItem("]", "right bracket", listOf("typingHomeTopRow")),
        makeTypingItem("\\", "backslash", listOf("typingHomeTopRow")),
    )

    val typingBottomRowItems: List<BrailleItem> = listOf(
        makeTypingItem("z", "z", listOf("typingHomeBottomRow")),
        makeTypingItem("x", "x", listOf("typingHomeBottomRow")),
        makeTypingItem("c", "c", listOf("typingHomeBottomRow")),
        makeTypingItem("v", "v", listOf("typingHomeBottomRow")),
        makeTypingItem("b", "b", listOf("typingHomeBottomRow")),
        makeTypingItem("n", "n", listOf("typingHomeBottomRow")),
        makeTypingItem("m", "m", listOf("typingHomeBottomRow")),
        makeTypingItem(",", "comma", listOf("typingHomeBottomRow")),
        makeTypingItem(".", "period", listOf("typingHomeBottomRow")),
        makeTypingItem("/", "slash", listOf("typingHomeBottomRow")),
    )

    private val brailleOnlyRegistry: List<BrailleItem> =
        grade1Letters +
            grade1Numbers +
            grade2Symbols +
            grade2Words +
            grade2ShortformWords +
            grade2Dot5Initials +
            grade2Dot45Initials +
            grade2Suffixes +
            grade2Dot456Initials

    private val grade1MoleInvasionItems: List<BrailleItem> = grade1Letters + grade1Numbers
    private val grade2MoleInvasionItems: List<BrailleItem> =
        grade2Symbols +
            grade2Words +
            grade2ShortformWords +
            grade2Dot5Initials +
            grade2Dot45Initials +
            grade2Suffixes +
            grade2Dot456Initials

    private val allItems: List<BrailleItem> =
        brailleOnlyRegistry +
            typingSimpleHomeRowItems +
            typingHomeRowItems +
            typingTopRowItems +
            typingBottomRowItems

    val modeOptions: List<ModeOption> = listOf(
        ModeOption("typingSimpleHomeRow", "Simple Home Row"),
        ModeOption("typingHomeRow", "QWERTY Home Row"),
        ModeOption("typingHomeTopRow", "QWERTY Home Row + Top Row"),
        ModeOption("typingHomeBottomRow", "QWERTY Home Row + Bottom Row"),
        ModeOption("letters-aj", "Grade 1 Letters A-J"),
        ModeOption("letters-at", "Grade 1 Letters A-T"),
        ModeOption("grade1Letters", "Letters only (Grade 1)"),
        ModeOption("grade1Numbers", "Numbers only (Grade 1)"),
        ModeOption("grade1LettersNumbers", "Letters and numbers (Grade 1)"),
        ModeOption("grade1MoleInvasion", "Grade 1 Mole Invasion!"),
        ModeOption("grade2Symbols", "Grade 2 contractions (symbols)"),
        ModeOption("grade2Words", "Grade 2 whole-word contractions"),
        ModeOption("grade2Shortforms", "Grade 2 shortform words"),
        ModeOption("grade2Dot5Initials", "Grade 2 dot 5 initials"),
        ModeOption("grade2Dot45Initials", "Grade 2 dot 45 initials"),
        ModeOption("grade2Suffixes", "Grade 2 Suffixes"),
        ModeOption("grade2Dot456Initials", "Grade 2 dot 456 initials"),
        ModeOption("grade2MoleInvasion", "Grade 2 Mole Invasion!"),
    )

    fun filteredModeOptions(inputMode: InputMode): List<ModeOption> {
        return modeOptions.filter { option ->
            when (inputMode) {
                InputMode.STANDARD -> !qwertyUnsupportedBrailleModeIds.contains(option.id)
                InputMode.PERKINS -> !qwertyModeIds.contains(option.id)
                InputMode.BRAILLE_TEXT, InputMode.BRAILLE_DISPLAY ->
                    !bsiExcludedModeIds.contains(option.id) && !bufferedTextUnsupportedModeIds.contains(option.id)
            }
        }
    }

    fun sanitizedModeId(modeId: String, inputMode: InputMode): String {
        val allowed = filteredModeOptions(inputMode)
        if (allowed.any { it.id == modeId }) {
            return modeId
        }

        return when (inputMode) {
            InputMode.STANDARD ->
                if (qwertyUnsupportedBrailleModeIds.contains(modeId)) "grade2Symbols" else modeId
            InputMode.PERKINS, InputMode.BRAILLE_TEXT, InputMode.BRAILLE_DISPLAY -> "grade1Letters"
        }
    }

    fun getItemsForMode(modeId: String, inputMode: InputMode = InputMode.STANDARD): List<BrailleItem> {
        return when (modeId) {
            "letters-aj" -> grade1Letters.filter { letterInRange(it, 'J') }
            "letters-at" -> grade1Letters.filter { letterInRange(it, 'T') }
            "grade1MoleInvasion" -> grade1MoleInvasionItems
            "grade2MoleInvasion" -> filteredGrade2InvasionItems(inputMode)
            else -> allItems.filter { modeId in it.modeTags }
        }
    }

    fun labelFor(modeId: String): String {
        return modeOptions.firstOrNull { it.id == modeId }?.label ?: "Letters and numbers (Grade 1)"
    }

    fun brailleUnicodeForDots(dots: List<Int>): String {
        val scalar = braillePatternBase + dotsToMask(dots)
        return String(Character.toChars(scalar))
    }

    private fun filteredGrade2InvasionItems(inputMode: InputMode): List<BrailleItem> {
        return when (inputMode) {
            InputMode.STANDARD ->
                grade2MoleInvasionItems.filter { "grade2Dot456Initials" !in it.modeTags }
            InputMode.BRAILLE_TEXT, InputMode.BRAILLE_DISPLAY ->
                grade2MoleInvasionItems.filter { "grade2Suffixes" !in it.modeTags }
            InputMode.PERKINS -> grade2MoleInvasionItems
        }
    }
}
