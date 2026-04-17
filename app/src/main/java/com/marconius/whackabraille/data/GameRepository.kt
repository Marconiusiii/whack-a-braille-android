package com.marconius.whackabraille.data

class GameRepository(
    private val preferences: GamePreferences,
) {

    fun getTotalTickets(): Int = preferences.totalTickets

    fun saveTotalTickets(value: Int) {
        preferences.totalTickets = value
    }

    fun getPrizeShelfCount(): Int = preferences.prizeShelfCount

    fun savePrizeShelfCount(value: Int) {
        preferences.prizeShelfCount = value
    }
}
