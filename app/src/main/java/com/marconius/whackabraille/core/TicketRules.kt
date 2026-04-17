package com.marconius.whackabraille.core

object TicketRules {

    fun scoreToTickets(score: Int): Int = when {
        score >= 200 -> 20
        score >= 180 -> 18
        score >= 160 -> 16
        score >= 140 -> 14
        score >= 120 -> 12
        score >= 100 -> 10
        score >= 80 -> 8
        score >= 60 -> 6
        score >= 40 -> 4
        score >= 20 -> 2
        score > 0 -> 1
        else -> 0
    }
}
