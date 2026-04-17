package com.marconius.whackabraille.data

import android.content.Context

class GamePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var totalTickets: Int
        get() = prefs.getInt(KEY_TOTAL_TICKETS, 0)
        set(value) {
            prefs.edit().putInt(KEY_TOTAL_TICKETS, value).apply()
        }

    var prizeShelfCount: Int
        get() = prefs.getInt(KEY_PRIZE_SHELF_COUNT, 0)
        set(value) {
            prefs.edit().putInt(KEY_PRIZE_SHELF_COUNT, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "whack_a_braille"
        private const val KEY_TOTAL_TICKETS = "total_tickets"
        private const val KEY_PRIZE_SHELF_COUNT = "prize_shelf_count"
    }
}
