package com.marconius.whackabraille.core

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameLoop(
    private val random: Random = Random.Default,
    private val speechDurationProvider: (String) -> Int = ::estimateSpeechDurationMs,
) {

    enum class FeedbackKind {
        HIT,
        MISS,
    }

    data class Options(
        val modeId: String,
        val durationSeconds: Int,
        val inputMode: InputMode,
        val difficulty: Difficulty,
        val speakBrailleDots: Boolean,
        val characterEcho: Boolean,
        val timerMusicEnabled: Boolean,
        val spatialMoleMappingEnabled: Boolean,
    )

    var onRoundEnded: ((RoundResult) -> Unit)? = null
    var onScoreUpdated: ((Int, Int) -> Unit)? = null
    var onActiveMoleChanged: ((Int?, BrailleItem?) -> Unit)? = null
    var onInputResetRequested: (() -> Unit)? = null
    var onMoleFeedback: ((Int, FeedbackKind) -> Unit)? = null
    var onAnnouncementRequested: ((String) -> Unit)? = null

    var isRunning: Boolean = false
        private set
    var roundEnding: Boolean = false
        private set
    var score: Int = 0
        private set
    var hitStreak: Int = 0
        private set
    var currentMoleId: Int = 0
        private set

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val laneCount = 5
    private val maxSameLaneInRow = 2
    private val trainingMoleCap = 15

    private val startIntervalMs = 1_000
    private val endIntervalMs = 360
    private val startUpTimeMs = 760
    private val endUpTimeMs = 340

    private val difficultyMultipliers = mapOf(
        Difficulty.BEGINNER to 1.5,
        Difficulty.NORMAL to 1.0,
        Difficulty.SUPREME to 0.5,
    )

    private val qwertyLaneMap = mapOf(
        "1" to 0, "2" to 0,
        "3" to 1, "4" to 1,
        "5" to 2, "6" to 2,
        "7" to 3, "8" to 3,
        "9" to 4, "0" to 4,
        "q" to 0, "a" to 0, "z" to 0, "w" to 0, "s" to 0, "x" to 0,
        "e" to 1, "d" to 1, "c" to 1, "r" to 1, "f" to 1, "v" to 1,
        "t" to 2, "g" to 2, "b" to 2, "y" to 2, "h" to 2, "n" to 2,
        "u" to 3, "j" to 3, "m" to 3, "i" to 3, "k" to 3, "," to 3,
        "o" to 4, "l" to 4, "." to 4, "p" to 4, ";" to 4, "/" to 4, "[" to 4, "]" to 4, "\\" to 4, "'" to 4,
    )

    private var currentOptions = Options(
        modeId = "grade1LettersNumbers",
        durationSeconds = 30,
        inputMode = InputMode.STANDARD,
        difficulty = Difficulty.NORMAL,
        speakBrailleDots = false,
        characterEcho = false,
        timerMusicEnabled = true,
        spatialMoleMappingEnabled = true,
    )

    private var availableItems: List<BrailleItem> = emptyList()
    private var roundItems: List<BrailleItem> = emptyList()
    private var roundLaneItems: List<BrailleItem?> = emptyList()
    private var invasionActiveItem: BrailleItem? = null

    private var activeLane: Int? = null
    private var missRegisteredForMole = false
    private var activeMoleShownAtMs = 0
    private var activeMoleUpTimeMs = 0

    private var hitsThisRound = 0
    private var missesThisRound = 0
    private var escapesThisRound = 0
    private var streakBonusCount = 0
    private var speedHitCount = 0
    private var speedBonusTickets = 0
    private var trainingMolesCompleted = 0
    private var lastTrainingMissAtMs = 0

    private var roundDurationMs = 30_000
    private var roundStartTimeMs = 0
    private var lastLaneIndex: Int? = null
    private var sameLaneRunCount = 0
    private var lastItemId: String? = null
    private var sameItemRunCount = 0
    private var pendingTextTokens: List<String> = emptyList()
    private var pendingPerkinsMasks: List<Int> = emptyList()

    private var roundTimer: ScheduledFuture<*>? = null
    private var moleTimer: ScheduledFuture<*>? = null
    private var moleUpTimer: ScheduledFuture<*>? = null

    @Synchronized
    fun startRound(options: Options) {
        if (isRunning) return

        currentOptions = options
        availableItems = BrailleRegistry.getItemsForMode(options.modeId, options.inputMode)
        roundItems = pickRoundItems(options.modeId, availableItems, options.spatialMoleMappingEnabled)
        roundLaneItems = buildRoundLaneItems(options.modeId, roundItems, options.spatialMoleMappingEnabled)

        score = 0
        hitStreak = 0
        hitsThisRound = 0
        missesThisRound = 0
        escapesThisRound = 0
        streakBonusCount = 0
        speedHitCount = 0
        speedBonusTickets = 0
        trainingMolesCompleted = 0
        lastTrainingMissAtMs = 0

        roundDurationMs = options.durationSeconds * 1000
        roundStartTimeMs = TimeUtils.nowMs()
        lastLaneIndex = null
        sameLaneRunCount = 0
        lastItemId = null
        sameItemRunCount = 0
        activeLane = null
        invasionActiveItem = null
        currentMoleId = 0
        missRegisteredForMole = false
        activeMoleShownAtMs = 0
        activeMoleUpTimeMs = 0
        pendingTextTokens = emptyList()
        pendingPerkinsMasks = emptyList()

        isRunning = true
        roundEnding = false

        cancelTimers()
        onScoreUpdated?.invoke(score, hitStreak)
        onActiveMoleChanged?.invoke(null, null)

        if (options.difficulty == Difficulty.TRAINING) {
            scheduleNextTrainingMole(0)
            return
        }

        scheduleRoundEnd()
        scheduleNextMole(0)
    }

    @Synchronized
    fun stopRound() {
        endRoundNow(canceled = true)
    }

    @Synchronized
    fun finishRoundEarly() {
        endRoundNow(canceled = false)
    }

    @Synchronized
    fun repeatCurrentTarget() {
        val item = currentItem ?: return
        onAnnouncementRequested?.invoke(buildAnnounceText(item))
    }

    @Synchronized
    fun handleAttempt(attempt: Attempt) {
        if (!isRunning || roundEnding) return
        val lane = activeLane ?: return
        val currentItem = laneItem(lane) ?: return
        if (attempt.moleId != currentMoleId) return
        if (attempt.key == "`") {
            repeatCurrentTarget()
            return
        }

        if (currentOptions.inputMode == InputMode.PERKINS && attempt.type == InputMode.STANDARD) {
            return
        }

        when (attempt.type) {
            InputMode.PERKINS -> handlePerkinsAttempt(attempt.dotMask, currentItem)
            InputMode.STANDARD -> handleTextAttemptToken(normalize(attempt.key), currentItem)
            InputMode.BRAILLE_TEXT, InputMode.BRAILLE_DISPLAY -> handleSubmittedText(normalize(attempt.text), currentItem)
        }
    }

    fun shutdown() {
        cancelTimers()
        scheduler.shutdownNow()
    }

    private val currentItem: BrailleItem?
        get() = if (isInvasionMode) invasionActiveItem else activeLane?.let(::laneItem)

    private fun laneItem(lane: Int): BrailleItem? {
        if (isInvasionMode && lane == activeLane) {
            return invasionActiveItem
        }
        return roundLaneItems.getOrNull(lane)
    }

    private fun scheduleRoundEnd() {
        roundTimer = scheduler.schedule({ synchronized(this) { requestRoundEnd() } }, roundDurationMs.toLong(), TimeUnit.MILLISECONDS)
    }

    @Synchronized
    private fun requestRoundEnd() {
        if (!isRunning) return

        roundEnding = true
        moleTimer?.cancel(false)
        moleTimer = null

        roundTimer?.cancel(false)
        roundTimer = scheduler.schedule({ synchronized(this) { endRoundNow(canceled = false) } }, computeRoundEndGraceMs().toLong(), TimeUnit.MILLISECONDS)
    }

    @Synchronized
    private fun endRoundNow(canceled: Boolean) {
        if (!isRunning) return

        isRunning = false
        roundEnding = false

        cancelTimers()
        clearActiveMole()
        onInputResetRequested?.invoke()

        val isTraining = currentOptions.difficulty == Difficulty.TRAINING
        val rawBaseTickets = if (isTraining) 0 else TicketRules.scoreToTickets(score)
        val rawStreakTickets = if (isTraining) 0 else streakBonusCount
        val rawSpeedTickets = if (isTraining) 0 else speedBonusTickets

        onRoundEnded?.invoke(
            RoundResult(
                modeId = currentOptions.modeId,
                inputMode = currentOptions.inputMode,
                durationSeconds = currentOptions.durationSeconds,
                isTraining = isTraining,
                trainingMolesCompleted = trainingMolesCompleted,
                score = score,
                hits = hitsThisRound,
                misses = missesThisRound,
                escapes = escapesThisRound,
                streakBonusCount = streakBonusCount,
                canceled = canceled,
                baseTickets = adjustedTickets(rawBaseTickets),
                streakBonusTickets = adjustedTickets(rawStreakTickets),
                speedBonusTickets = adjustedTickets(rawSpeedTickets),
            )
        )
    }

    private fun scheduleNextMole(extraDelayMs: Int) {
        if (!isRunning || roundEnding) return
        val delay = getCurrentInterval() + random.nextInt(120) + extraDelayMs
        moleTimer?.cancel(false)
        moleTimer = scheduler.schedule({ synchronized(this) { showRandomMole() } }, delay.toLong(), TimeUnit.MILLISECONDS)
    }

    private fun scheduleNextTrainingMole(extraDelayMs: Int) {
        if (!isRunning || roundEnding) return
        if (trainingMolesCompleted >= trainingMoleCap) {
            endRoundNow(canceled = false)
            return
        }
        moleTimer?.cancel(false)
        moleTimer = scheduler.schedule({ synchronized(this) { showTrainingMole() } }, max(0, extraDelayMs).toLong(), TimeUnit.MILLISECONDS)
    }

    private fun showTrainingMole() {
        showMole(trainingMode = true)
    }

    private fun showRandomMole() {
        showMole(trainingMode = false)
    }

    @Synchronized
    private fun showMole(trainingMode: Boolean) {
        if (!isRunning || roundEnding) return

        clearActiveMole()
        currentMoleId += 1
        missRegisteredForMole = false

        val lane = pickNextLaneIndex()
        val item = if (isInvasionMode) {
            pickNextInvasionItem().also { invasionActiveItem = it }
        } else {
            laneItem(lane)
        }

        if (item == null) {
            clearActiveMole()
            scheduleFollowUp(trainingMode)
            return
        }

        activeLane = lane
        onActiveMoleChanged?.invoke(null, null)

        val announceText = buildAnnounceText(item)
        onAnnouncementRequested?.invoke(announceText)
        val speechDurationMs = speechDurationProvider(announceText)
        val baseUpTimeMs = if (trainingMode) 0 else getCurrentUpTime()

        scheduler.schedule({
            synchronized(this) {
                if (!isRunning || roundEnding) return@schedule
                if (activeLane != lane || currentItem?.id != item.id) return@schedule
                activeMoleShownAtMs = TimeUtils.nowMs()
                onActiveMoleChanged?.invoke(lane, item)
            }
        }, 80, TimeUnit.MILLISECONDS)

        if (trainingMode) {
            activeMoleUpTimeMs = 0
            return
        }

        activeMoleUpTimeMs = computeMoleWindowMs(item, baseUpTimeMs, speechDurationMs)
        moleUpTimer?.cancel(false)
        moleUpTimer = scheduler.schedule({
            synchronized(this) {
                if (!isRunning || roundEnding) return@schedule
                if (activeLane != lane || currentItem?.id != item.id) return@schedule

                escapesThisRound += 1
                hitStreak = 0
                onScoreUpdated?.invoke(score, hitStreak)
                onInputResetRequested?.invoke()
                clearActiveMole()
                scheduleNextMole(0)
            }
        }, activeMoleUpTimeMs.toLong(), TimeUnit.MILLISECONDS)
    }

    private fun handleHit() {
        val lane = activeLane ?: return

        if (currentOptions.difficulty == Difficulty.TRAINING) {
            onMoleFeedback?.invoke(lane, FeedbackKind.HIT)
            hitsThisRound += 1
            trainingMolesCompleted += 1
            onInputResetRequested?.invoke()
            missRegisteredForMole = true
            moleUpTimer?.cancel(false)
            moleUpTimer = null
            clearActiveMole()
            scheduleNextTrainingMole(180)
            return
        }

        hitsThisRound += 1
        hitStreak += 1
        score += 10

        if (hitStreak % 5 == 0) {
            score += 10
            streakBonusCount += 1
        }

        val reactionMs = TimeUtils.nowMs() - activeMoleShownAtMs
        val speedThresholdMs = (activeMoleUpTimeMs * 0.55).toInt()
        if (reactionMs <= speedThresholdMs) {
            speedHitCount += 1
            if (speedHitCount % 3 == 0 && speedBonusTickets < 5) {
                speedBonusTickets += 1
            }
        }

        onMoleFeedback?.invoke(lane, FeedbackKind.HIT)
        onInputResetRequested?.invoke()
        missRegisteredForMole = true
        moleUpTimer?.cancel(false)
        moleUpTimer = null
        clearActiveMole()
        onScoreUpdated?.invoke(score, hitStreak)
        scheduleNextMole(0)
    }

    private fun handleTrainingMiss() {
        val now = TimeUtils.nowMs()
        if (now - lastTrainingMissAtMs < 200) return
        lastTrainingMissAtMs = now
        activeLane?.let { onMoleFeedback?.invoke(it, FeedbackKind.MISS) }
    }

    private fun handleMiss() {
        val lane = activeLane ?: return
        missesThisRound += 1
        hitStreak = 0
        score = max(0, score - 2)
        onMoleFeedback?.invoke(lane, FeedbackKind.MISS)
        onInputResetRequested?.invoke()
        onScoreUpdated?.invoke(score, hitStreak)
    }

    private fun handlePerkinsAttempt(dotMask: Int?, currentItem: BrailleItem) {
        dotMask ?: return
        val updatedSequence = pendingPerkinsMasks + dotMask

        if (currentItem.expectedPerkinsCellCount <= 1) {
            pendingPerkinsMasks = emptyList()
            resolveAttemptOutcome(updatedSequence == listOf(currentItem.dotMask))
            return
        }

        if (currentItem.perkinsSequenceMasks.take(updatedSequence.size) == updatedSequence) {
            pendingPerkinsMasks = updatedSequence
            if (updatedSequence.size == currentItem.perkinsSequenceMasks.size) {
                pendingPerkinsMasks = emptyList()
                resolveAttemptOutcome(true)
            }
            return
        }

        pendingPerkinsMasks = emptyList()
        resolveAttemptOutcome(false)
    }

    private fun handleTextAttemptToken(token: String, currentItem: BrailleItem) {
        if (token.isEmpty()) return
        val updatedTokens = pendingTextTokens + token
        val sequences = currentItem.textInputTokenSequences

        if (sequences.contains(updatedTokens)) {
            pendingTextTokens = emptyList()
            resolveAttemptOutcome(true)
            return
        }

        if (sequences.any { it.take(updatedTokens.size) == updatedTokens }) {
            pendingTextTokens = updatedTokens
            return
        }

        pendingTextTokens = emptyList()
        resolveAttemptOutcome(false)
    }

    private fun handleSubmittedText(input: String, currentItem: BrailleItem) {
        if (input.isEmpty()) return
        pendingTextTokens = emptyList()
        pendingPerkinsMasks = emptyList()
        resolveAttemptOutcome(matchesInput(input, currentItem))
    }

    private fun resolveAttemptOutcome(hit: Boolean) {
        if (hit) {
            handleHit()
            return
        }

        if (currentOptions.difficulty == Difficulty.TRAINING) {
            handleTrainingMiss()
            return
        }

        if (missRegisteredForMole) return
        missRegisteredForMole = true
        handleMiss()
    }

    private fun buildAnnounceText(item: BrailleItem): String {
        var text = item.announceText
        if ("grade1Letters" in item.modeTags && currentOptions.characterEcho && item.nato != null) {
            text += ", ${item.nato}"
        }

        if (currentOptions.difficulty == Difficulty.TRAINING && currentOptions.speakBrailleDots) {
            val cells = if (item.perkinsSequenceDots.isEmpty()) listOf(item.dots) else item.perkinsSequenceDots
            val spokenCells = cells.mapNotNull(::dotsPhrase)
            if (spokenCells.isNotEmpty()) {
                text += ", " + spokenCells.joinToString(", then ")
            }
        }

        return text
    }

    private fun scheduleFollowUp(afterTraining: Boolean) {
        if (afterTraining) scheduleNextTrainingMole(0) else scheduleNextMole(0)
    }

    private fun pickRoundItems(modeId: String, pool: List<BrailleItem>, useSpatialMapping: Boolean): List<BrailleItem> {
        if (isInvasionMode(modeId)) return emptyList()
        if (!useSpatialMapping || !isSpatialMappingEligibleMode(modeId)) return pickFiveItems(pool)

        val copy = pool.shuffled(random)
        val selected = mutableListOf<BrailleItem>()
        val occupied = mutableSetOf<Int>()

        for (item in copy) {
            val lane = laneForItem(item) ?: continue
            if (!occupied.add(lane)) continue
            selected += item
            if (selected.size >= laneCount) break
        }

        return if (selected.isEmpty()) pickFiveItems(pool) else selected
    }

    private fun buildRoundLaneItems(modeId: String, items: List<BrailleItem>, useSpatialMapping: Boolean): List<BrailleItem?> {
        if (isInvasionMode(modeId)) return List(laneCount) { null }

        val lanes = MutableList<BrailleItem?>(laneCount) { null }
        if (!useSpatialMapping || !isSpatialMappingEligibleMode(modeId)) {
            for (index in 0 until min(laneCount, items.size)) {
                lanes[index] = items[index]
            }
            return lanes
        }

        val occupied = mutableSetOf<Int>()
        for (item in items) {
            val lane = laneForItem(item) ?: continue
            if (!occupied.add(lane)) continue
            lanes[lane] = item
        }
        return lanes
    }

    private fun pickFiveItems(pool: List<BrailleItem>): List<BrailleItem> {
        return pool.shuffled(random).take(min(laneCount, pool.size))
    }

    private fun isSpatialMappingEligibleMode(modeId: String): Boolean {
        return modeId in setOf(
            "typingSimpleHomeRow",
            "typingHomeRow",
            "typingHomeTopRow",
            "typingHomeBottomRow",
            "letters-aj",
            "letters-at",
            "grade1Letters",
            "grade1Numbers",
            "grade1LettersNumbers",
        )
    }

    private fun laneForItem(item: BrailleItem): Int? {
        val key = item.standardKey?.lowercase() ?: return null
        return qwertyLaneMap[key]
    }

    private fun pickNextLaneIndex(): Int {
        val candidates = if (isInvasionMode) {
            (0 until laneCount).toList()
        } else {
            roundLaneItems.indices.filter { roundLaneItems[it] != null }
        }
        if (candidates.isEmpty()) return 0
        if (candidates.size == 1) return candidates.first()

        var filtered = candidates.filter { it != activeLane }

        if (lastLaneIndex != null && sameLaneRunCount >= maxSameLaneInRow) {
            val withoutRepeat = filtered.filter { it != lastLaneIndex }
            if (withoutRepeat.isNotEmpty()) {
                filtered = withoutRepeat
            }
        }

        if (filtered.isEmpty()) filtered = candidates
        val lane = filtered.random(random)

        if (lane == lastLaneIndex) {
            sameLaneRunCount += 1
        } else {
            lastLaneIndex = lane
            sameLaneRunCount = 1
        }
        return lane
    }

    private fun clearActiveMole() {
        activeLane = null
        invasionActiveItem = null
        activeMoleShownAtMs = 0
        pendingTextTokens = emptyList()
        pendingPerkinsMasks = emptyList()
        onActiveMoleChanged?.invoke(null, null)
    }

    private fun getProgress(): Double {
        if (roundDurationMs <= 0) return 0.0
        val elapsed = TimeUtils.nowMs() - roundStartTimeMs
        var progress = min(elapsed.toDouble() / roundDurationMs.toDouble(), 1.0)

        if (roundDurationMs >= 45_000) {
            progress = when {
                progress > 0.7 -> 0.9
                progress > 0.3 -> 0.3 + ((progress - 0.3) * 1.6)
                else -> progress
            }
        }
        return min(progress, 1.0)
    }

    private fun getCurrentInterval(): Int {
        var interval = TimeUtils.lerp(startIntervalMs, endIntervalMs, getProgress())
        if (getProgress() > 0.7) {
            interval = (interval * 0.6).toInt()
        }
        val multiplier = difficultyMultipliers[currentOptions.difficulty] ?: 1.0
        return max((interval * multiplier).toInt(), 240)
    }

    private fun getCurrentUpTime(): Int {
        val base = TimeUtils.lerp(startUpTimeMs, endUpTimeMs, getProgress())
        val multiplier = difficultyMultipliers[currentOptions.difficulty] ?: 1.0
        return (base * multiplier).toInt()
    }

    private fun computeMoleWindowMs(item: BrailleItem, baseUpTimeMs: Int, speechDurationMs: Int): Int {
        val reactionBufferMs = 380
        val minUpTimeMs = 550
        val maxUpTimeMs = 2_700
        val effectiveSpeechDurationMs = max(300, speechDurationMs)
        val submissionBufferMs = when (currentOptions.inputMode) {
            InputMode.BRAILLE_DISPLAY -> 520
            InputMode.BRAILLE_TEXT -> 320
            InputMode.STANDARD, InputMode.PERKINS -> 0
        }
        val additionalCellBufferMs = max(0, item.expectedPerkinsCellCount - 1) * 320
        return min(
            max(baseUpTimeMs + effectiveSpeechDurationMs + reactionBufferMs + submissionBufferMs + additionalCellBufferMs, minUpTimeMs),
            maxUpTimeMs
        )
    }

    private fun computeRoundEndGraceMs(): Int = min(max(350, 0), 750)

    private val isInvasionMode: Boolean
        get() = isInvasionMode(currentOptions.modeId)

    private fun isInvasionMode(modeId: String): Boolean {
        return modeId == "grade1MoleInvasion" || modeId == "grade2MoleInvasion"
    }

    private fun pickNextInvasionItem(): BrailleItem? {
        if (availableItems.isEmpty()) return null
        var candidates = availableItems

        if (lastItemId != null && sameItemRunCount >= 3) {
            val filtered = candidates.filter { it.id != lastItemId }
            if (filtered.isNotEmpty()) {
                candidates = filtered
            }
        }

        val item = candidates.random(random)
        if (item.id == lastItemId) {
            sameItemRunCount += 1
        } else {
            lastItemId = item.id
            sameItemRunCount = 1
        }
        return item
    }

    private fun adjustedTickets(tickets: Int): Int {
        if (tickets <= 0) return tickets

        return when (currentOptions.modeId) {
            "grade2MoleInvasion" -> ceil(tickets * 1.5).toInt()
            "grade1MoleInvasion" -> ceil(tickets * 1.25).toInt()
            else -> tickets
        }
    }

    private fun cancelTimers() {
        roundTimer?.cancel(false)
        moleTimer?.cancel(false)
        moleUpTimer?.cancel(false)
        roundTimer = null
        moleTimer = null
        moleUpTimer = null
    }

    private fun normalize(value: String?): String {
        return value.orEmpty().trim().lowercase()
    }

    private fun dotsPhrase(dots: List<Int>): String? {
        if (dots.isEmpty()) return null
        return if (dots.size == 1) {
            "Dot ${dots.first()}"
        } else {
            "Dots ${dots.joinToString(" ")}"
        }
    }

    private fun matchesInput(input: String, item: BrailleItem): Boolean {
        return input.isNotEmpty() && input in item.acceptedTextInputs
    }

    companion object {
        private fun estimateSpeechDurationMs(text: String): Int {
            val normalized = text.trim()
            if (normalized.isEmpty()) return 300
            val words = normalized.split(Regex("\\s+")).size.coerceAtLeast(1)
            val characterEstimate = normalized.length * 38.0
            val wordEstimate = words * 240.0
            val baseline = max(300.0, max(characterEstimate, wordEstimate))
            return baseline.toInt().coerceIn(300, 1_800)
        }
    }
}
