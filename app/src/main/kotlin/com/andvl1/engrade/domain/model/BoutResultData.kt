package com.andvl1.engrade.domain.model

data class BoutResultData(
    val leftSeed: Int,
    val rightSeed: Int,
    val leftScore: Int,
    val rightScore: Int,
    val status: BoutStatus
)
