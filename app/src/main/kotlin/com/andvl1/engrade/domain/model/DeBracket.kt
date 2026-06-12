package com.andvl1.engrade.domain.model

/**
 * Immutable state of a Direct Elimination bracket.
 *
 * @param tableauSize  T — smallest power of 2 ≥ [fencerCount].
 * @param fencerCount  N — number of real seeded fencers (seed numbers 1..N).
 * @param totalRounds  log₂([tableauSize]) — number of elimination rounds.
 * @param matches      All matches in all rounds, ordered by (round ASC, position ASC).
 */
data class DeBracket(
    val tableauSize: Int,
    val fencerCount: Int,
    val totalRounds: Int,
    val matches: List<DeMatch>
) {
    /** True when the final match (round == totalRounds) has a winner. */
    val isComplete: Boolean
        get() = matches.any { it.round == totalRounds && it.winner != null }
}
