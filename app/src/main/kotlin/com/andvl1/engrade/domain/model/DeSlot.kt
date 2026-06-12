package com.andvl1.engrade.domain.model

/**
 * A slot in a DE bracket match.
 */
sealed class DeSlot {
    /** A concrete fencer occupying this slot. [seed] is the DE seed (= pool ranking place). */
    data class Fencer(val seed: Int, val name: String) : DeSlot()

    /** Bye slot — no opponent; the paired fencer auto-advances without a bout. */
    data object Bye : DeSlot()

    /** To-be-determined — the prior match has not yet been resolved. */
    data object Tbd : DeSlot()
}
