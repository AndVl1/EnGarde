package com.andvl1.engrade.domain

/**
 * Generates FIE standard bout order using Berger tables.
 * Supports groups of 5-8 fencers.
 */
object FieBoutOrder {

    /**
     * Returns list of bout pairings (seed1, seed2) for given number of fencers.
     * Uses Berger rotation algorithm for round-robin scheduling.
     */
    fun getBoutOrder(fencerCount: Int): List<Pair<Int, Int>> {
        require(fencerCount in 5..8) { "Fencer count must be 5-8, got $fencerCount" }

        // Berger table algorithm:
        // 1. If odd count, add dummy fencer
        // 2. Fix position 1, rotate positions 2..n clockwise
        // 3. Generate pairings for each round
        // 4. Filter out bouts with dummy fencer

        val n = if (fencerCount % 2 == 0) fencerCount else fencerCount + 1
        val rounds = n - 1
        val half = n / 2

        val bouts = mutableListOf<Pair<Int, Int>>()
        val positions = (2..n).toMutableList()

        repeat(rounds) {
            // Pair first position (1) with last position in rotation
            bouts.add(1 to positions.last())

            // Pair remaining positions from outside in
            for (i in 0 until half - 1) {
                bouts.add(positions[i] to positions[positions.size - 2 - i])
            }

            // Rotate: move last element to front
            positions.add(0, positions.removeAt(positions.size - 1))
        }

        // Filter out bouts with dummy fencer (if odd count)
        return if (fencerCount % 2 != 0) {
            bouts.filter { it.first <= fencerCount && it.second <= fencerCount }
        } else {
            bouts
        }
    }
}
