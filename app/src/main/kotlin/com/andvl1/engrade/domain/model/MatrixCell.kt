package com.andvl1.engrade.domain.model

data class MatrixCell(
    val leftSeed: Int,
    val rightSeed: Int,
    val leftScore: Int?,
    val rightScore: Int?,
    val isVictory: Boolean?,
    val status: BoutStatus
)
