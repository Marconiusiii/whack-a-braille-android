package com.marconius.whackabraille.core

data class RoundResult(
    val modeId: String,
    val inputMode: InputMode,
    val durationSeconds: Int,
    val isTraining: Boolean,
    val trainingMolesCompleted: Int,
    val score: Int,
    val hits: Int,
    val misses: Int,
    val escapes: Int,
    val streakBonusCount: Int,
    val canceled: Boolean,
    val baseTickets: Int,
    val streakBonusTickets: Int,
    val speedBonusTickets: Int,
) {
    val totalTickets: Int
        get() = baseTickets + streakBonusTickets + speedBonusTickets
}
