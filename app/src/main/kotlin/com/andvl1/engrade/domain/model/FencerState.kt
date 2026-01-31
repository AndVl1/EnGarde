package com.andvl1.engrade.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FencerState(
    val score: Int = 0,
    val hasYellowCard: Boolean = false,
    val hasRedCard: Boolean = false,
    val hasPriority: Boolean = false,
    val isWinner: Boolean = false
) {
    fun withScore(newScore: Int) = copy(score = newScore)
    fun withYellowCard() = copy(hasYellowCard = true)
    fun withoutYellowCard() = copy(hasYellowCard = false)
    fun withRedCard() = copy(hasRedCard = true)
    fun withoutRedCard() = copy(hasRedCard = false)
    fun withPriority() = copy(hasPriority = true)
    fun withoutPriority() = copy(hasPriority = false)
    fun withWinner() = copy(isWinner = true)
    fun withoutWinner() = copy(isWinner = false)

    fun incrementScore() = copy(score = score + 1)
    fun decrementScore() = copy(score = maxOf(0, score - 1))
}
