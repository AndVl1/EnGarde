package com.andvl1.engrade.domain

import com.andvl1.engrade.domain.model.BoutResultData
import com.andvl1.engrade.domain.model.BoutStatus
import com.andvl1.engrade.domain.model.FencerRanking
import com.andvl1.engrade.domain.model.MatrixCell

/**
 * Pure business logic for pool/group ranking and matrix generation.
 * Implements full FIE ranking algorithm with tiebreakers.
 */
class PoolEngine {

    /**
     * Calculate FIE rankings for all active fencers.
     *
     * Algorithm:
     * 1. Calculate V (victories), M (matches), TD, TR for each fencer
     * 2. Calculate V/M% and Index (TD - TR)
     * 3. Sort by: V/M% DESC, Index DESC, TD DESC
     * 4. Apply head-to-head tiebreaker for remaining ties
     * 5. Assign places
     */
    fun calculateRankings(
        fencerCount: Int,
        bouts: List<BoutResultData>,
        fencerNames: Map<Int, String>,
        excludedSeeds: Set<Int>
    ): List<FencerRanking> {
        val activeSeeds = (1..fencerCount).filterNot { it in excludedSeeds }

        // Filter out bouts involving excluded fencers
        val activeBouts = bouts.filter {
            it.leftSeed !in excludedSeeds && it.rightSeed !in excludedSeeds
        }

        // Calculate stats for each active fencer
        val stats = activeSeeds.associateWith { seed ->
            var victories = 0
            var matches = 0
            var touchesDelivered = 0
            var touchesReceived = 0

            activeBouts.forEach { bout ->
                when {
                    bout.leftSeed == seed -> {
                        matches++
                        touchesDelivered += bout.leftScore
                        touchesReceived += bout.rightScore
                        if (bout.leftScore > bout.rightScore) victories++
                    }
                    bout.rightSeed == seed -> {
                        matches++
                        touchesDelivered += bout.rightScore
                        touchesReceived += bout.leftScore
                        if (bout.rightScore > bout.leftScore) victories++
                    }
                }
            }

            Stats(victories, matches, touchesDelivered, touchesReceived)
        }

        // Create initial rankings
        var rankings = activeSeeds.map { seed ->
            val s = stats[seed]!!
            val vmPercent = if (s.matches > 0) (s.victories.toDouble() / s.matches) * 100 else 0.0
            val index = s.touchesDelivered - s.touchesReceived

            FencerRanking(
                seedNumber = seed,
                name = fencerNames[seed] ?: "Unknown",
                victories = s.victories,
                matches = s.matches,
                vmPercent = vmPercent,
                touchesDelivered = s.touchesDelivered,
                touchesReceived = s.touchesReceived,
                index = index,
                place = 0 // will be assigned after sorting
            )
        }

        // Sort by FIE criteria
        rankings = rankings.sortedWith(
            compareByDescending<FencerRanking> { it.vmPercent }
                .thenByDescending { it.index }
                .thenByDescending { it.touchesDelivered }
        )

        // Apply head-to-head tiebreaker for exact ties
        rankings = applyHeadToHeadTiebreaker(rankings, activeBouts)

        // Assign places
        rankings = rankings.mapIndexed { index, ranking ->
            ranking.copy(place = index + 1)
        }

        return rankings
    }

    /**
     * Apply head-to-head tiebreaker for fencers with identical stats.
     */
    private fun applyHeadToHeadTiebreaker(
        rankings: List<FencerRanking>,
        bouts: List<BoutResultData>
    ): List<FencerRanking> {
        val result = rankings.toMutableList()

        // Group by V/M%, Index, and TD to find ties
        val groups = result.groupBy { Triple(it.vmPercent, it.index, it.touchesDelivered) }

        groups.values.forEach { group ->
            if (group.size > 1) {
                // Apply head-to-head
                val sorted = group.sortedByDescending { fencer ->
                    // Count head-to-head victories against other tied fencers
                    val tiedSeeds = group.map { it.seedNumber }.toSet()
                    var h2hVictories = 0

                    bouts.forEach { bout ->
                        when {
                            bout.leftSeed == fencer.seedNumber && bout.rightSeed in tiedSeeds -> {
                                if (bout.leftScore > bout.rightScore) h2hVictories++
                            }
                            bout.rightSeed == fencer.seedNumber && bout.leftSeed in tiedSeeds -> {
                                if (bout.rightScore > bout.leftScore) h2hVictories++
                            }
                        }
                    }

                    h2hVictories
                }

                // Replace tied group with sorted version
                val firstIndex = result.indexOf(group.first())
                result.removeAll(group)
                result.addAll(firstIndex, sorted)
            }
        }

        return result
    }

    /**
     * Build N×N matrix of bout results.
     * Returns list of rows, each row is list of cells.
     * Diagonal cells are null (fencer doesn't bout themselves).
     */
    fun buildMatrix(
        fencerCount: Int,
        bouts: List<BoutResultData>,
        excludedSeeds: Set<Int>
    ): List<List<MatrixCell?>> {
        val matrix = mutableListOf<List<MatrixCell?>>()

        for (row in 1..fencerCount) {
            val rowCells = mutableListOf<MatrixCell?>()

            for (col in 1..fencerCount) {
                if (row == col) {
                    // Diagonal - no bout
                    rowCells.add(null)
                } else {
                    // Find bout between row and col
                    val bout = bouts.find {
                        (it.leftSeed == row && it.rightSeed == col) ||
                        (it.leftSeed == col && it.rightSeed == row)
                    }

                    if (bout == null) {
                        // Bout not yet fought
                        rowCells.add(
                            MatrixCell(
                                leftSeed = row,
                                rightSeed = col,
                                leftScore = null,
                                rightScore = null,
                                isVictory = null,
                                status = BoutStatus.PENDING
                            )
                        )
                    } else {
                        // Bout completed - map to row's perspective
                        val (leftScore, rightScore) = if (bout.leftSeed == row) {
                            bout.leftScore to bout.rightScore
                        } else {
                            bout.rightScore to bout.leftScore
                        }

                        val isVictory = leftScore > rightScore

                        rowCells.add(
                            MatrixCell(
                                leftSeed = row,
                                rightSeed = col,
                                leftScore = leftScore,
                                rightScore = rightScore,
                                isVictory = isVictory,
                                status = bout.status
                            )
                        )
                    }
                }
            }

            matrix.add(rowCells)
        }

        return matrix
    }

    private data class Stats(
        val victories: Int,
        val matches: Int,
        val touchesDelivered: Int,
        val touchesReceived: Int
    )
}
