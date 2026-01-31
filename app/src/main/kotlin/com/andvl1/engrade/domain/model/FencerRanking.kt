package com.andvl1.engrade.domain.model

data class FencerRanking(
    val seedNumber: Int,
    val name: String,
    val victories: Int,
    val matches: Int,
    val vmPercent: Double,
    val touchesDelivered: Int,
    val touchesReceived: Int,
    val index: Int,
    val place: Int
)
