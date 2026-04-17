package com.marconius.whackabraille.core

object TicketRules {

    fun scoreToTickets(score: Int): Int = when {
        score >= 200 -> 20
        score >= 150 -> 15
        score >= 100 -> 10
        score >= 50 -> 5
        else -> 0
    }
}
