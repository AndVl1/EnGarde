package com.andvl1.engrade.domain.model

/**
 * Final placement of a fencer in a DE competition.
 *
 * FIE classification rules (no 3rd-place bout):
 * - place 1: DE winner
 * - place 2: finalist (lost the final)
 * - place 3 (shared × 2): both semifinal losers
 * - place 5 (shared × 4): all quarterfinal losers
 * - place 2^k+1 (shared × 2^k): losers of round = [DeBracket.totalRounds] − k
 *
 * Ties within a shared place are broken by [seed] (lower = better pool rank → listed first).
 *
 * @param place             Final place number (shared places repeat the same number).
 * @param seed              DE seed (= pool ranking position, 1 = best pool performer).
 * @param name              Fencer display name.
 * @param eliminatedInRound The round in which the fencer was eliminated; null for the winner.
 */
data class DeClassification(
    val place: Int,
    val seed: Int,
    val name: String,
    val eliminatedInRound: Int?
)
