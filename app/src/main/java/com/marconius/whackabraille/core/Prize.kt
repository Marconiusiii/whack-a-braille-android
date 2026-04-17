package com.marconius.whackabraille.core

enum class PrizeTier(val detailLabel: String) {
    TIER_1("Tier 1"),
    TIER_2("Tier 2"),
    TIER_3("Tier 3"),
    TIER_4("Tier 4"),
    TIER_5("Tier 5"),
}

data class Prize(
    val id: String,
    val label: String,
    val minTickets: Int,
    val maxTickets: Int?,
    val category: String,
    val flavorText: String,
) {
    val ticketCost: Int
        get() = maxOf(1, minTickets)

    val tier: PrizeTier
        get() = when {
            id.startsWith("tier1_") -> PrizeTier.TIER_1
            id.startsWith("tier2_") -> PrizeTier.TIER_2
            id.startsWith("tier3_") -> PrizeTier.TIER_3
            id.startsWith("tier4_") -> PrizeTier.TIER_4
            id.startsWith("tier5_") -> PrizeTier.TIER_5
            minTickets < 10 -> PrizeTier.TIER_1
            minTickets < 25 -> PrizeTier.TIER_2
            minTickets < 50 -> PrizeTier.TIER_3
            minTickets < 100 -> PrizeTier.TIER_4
            else -> PrizeTier.TIER_5
        }
}

data class PrizeShelfEntry(
    val prizeId: String,
    val quantity: Int,
    val latestClaimedAtEpochMillis: Long? = null,
)
