package com.andvl1.engrade.domain.model

/**
 * A single match (bout) within the DE bracket.
 *
 * @param id         Globally unique match id within the bracket (computed by round/position).
 * @param round      Round number: 1 = first elimination round, [DeBracket.totalRounds] = final.
 * @param position   1-based position within the round.
 * @param topSlot    Upper bracket slot — [DeSlot.Fencer], [DeSlot.Bye], or [DeSlot.Tbd].
 * @param bottomSlot Lower bracket slot.
 * @param winner     The match winner; null until the match is resolved.
 */
data class DeMatch(
    val id: Int,
    val round: Int,
    val position: Int,
    val topSlot: DeSlot,
    val bottomSlot: DeSlot,
    val winner: DeSlot.Fencer? = null
) {
    /** True when the match has been decided. */
    val isResolved: Boolean get() = winner != null

    /** True when one slot is a bye (match auto-resolves without a real bout). */
    val isBye: Boolean get() = topSlot is DeSlot.Bye || bottomSlot is DeSlot.Bye
}
