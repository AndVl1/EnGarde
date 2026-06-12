package com.andvl1.engrade.domain

import com.andvl1.engrade.domain.model.DeBracket
import com.andvl1.engrade.domain.model.DeClassification
import com.andvl1.engrade.domain.model.DeMatch
import com.andvl1.engrade.domain.model.DeSlot
import com.andvl1.engrade.domain.model.FencerRanking

/**
 * Pure domain engine for FIE Direct Elimination (DE) tableau.
 *
 * All public functions are **pure** — no I/O, no Android dependencies, no mutable state.
 *
 * ---
 * ## Seeding sequence — outer-bracket recursive algorithm
 *
 * ```
 * seedingPositions(1)  = [1]
 * seedingPositions(2k) = interleave(seedingPositions(k), [(2k+1 − x) for x in seedingPositions(k)])
 * ```
 *
 * Concrete values:
 * - T=2:  [1, 2]         → final only
 * - T=4:  [1, 4, 2, 3]   → R1 pairs: (1v4), (2v3)
 * - T=8:  [1, 8, 4, 5, 2, 7, 3, 6]  → R1 pairs: (1v8), (4v5), (2v7), (3v6)
 * - T=16: [1,16,8,9,4,13,5,12,2,15,7,10,3,14,6,11]
 *
 * This guarantees:
 * - Seeds 1 and 2 are in opposite halves and can only meet in the final.
 * - Seeds 1, 2, 3, 4 are in distinct quarters.
 * - Seeds 1..2^k are in distinct 2^k-ths of the bracket.
 *
 * ---
 * ## Bye placement
 *
 * With N fencers in T slots, the top (T − N) seeds receive byes in round 1.
 * The seeding sequence naturally places bye slots at positions where the
 * canonical seed number exceeds N (i.e. the "ghost" opponents for top seeds).
 * Byes auto-resolve at bracket construction — no [recordWinner] call needed.
 *
 * ---
 * ## Classification (FIE rules, no 3rd-place bout)
 *
 * | Place | Fencers                                      |
 * |-------|----------------------------------------------|
 * | 1st   | DE winner                                    |
 * | 2nd   | finalist                                     |
 * | 3rd (shared) | both semifinal losers               |
 * | 5th (shared) | all quarterfinal losers              |
 * | …     | round losers continuing in powers of 2       |
 *
 * Within a shared place, fencers are ordered by DE seed (lower = better pool rank).
 * Bye auto-advances do not produce "losers" — only real bouts contribute eliminations.
 *
 * ---
 * ## Match ID scheme
 *
 * For tableau size T, match ids are assigned sequentially by round then position:
 * - Round 1: ids 1 .. T/2
 * - Round 2: ids T/2+1 .. T/2+T/4
 * - Round r: ids offset(r)+1 .. offset(r)+T/2^r
 * - Final:   id T-1
 *
 * Where offset(r) = T/2 + T/4 + … + T/2^(r-1).
 */
object DeTableau {

    // -------------------------------------------------------------------------
    // Sizing
    // -------------------------------------------------------------------------

    /**
     * Returns the tableau size T — the smallest power of two ≥ [fencerCount].
     *
     * Examples: N=5→8, N=6→8, N=8→8, N=9→16, N=16→16, N=17→32.
     *
     * @throws IllegalArgumentException if [fencerCount] < 2.
     */
    fun tableauSize(fencerCount: Int): Int {
        require(fencerCount >= 2) { "At least 2 fencers required, got $fencerCount" }
        var size = 1
        while (size < fencerCount) size = size shl 1
        return size
    }

    /**
     * Returns the number of elimination rounds for a bracket of [tableauSize].
     * Equals log₂([tableauSize]).
     */
    fun totalRounds(tableauSize: Int): Int {
        require(tableauSize >= 1 && isPowerOfTwo(tableauSize)) {
            "tableauSize must be a power of 2, got $tableauSize"
        }
        var rounds = 0
        var t = tableauSize
        while (t > 1) {
            rounds++
            t = t shr 1
        }
        return rounds
    }

    // -------------------------------------------------------------------------
    // Seeding positions (exposed for testing)
    // -------------------------------------------------------------------------

    /**
     * Returns the canonical seeding slot order for a bracket of [tableauSize].
     *
     * The returned list has [tableauSize] entries; `result[i]` is the seed number
     * that occupies the (i+1)-th slot (1-based). Consecutive pairs of slots form
     * round-1 matches: slots 1&2 → match at position 1, slots 3&4 → position 2, etc.
     *
     * @throws IllegalArgumentException if [tableauSize] is not a positive power of 2.
     */
    fun seedingPositions(tableauSize: Int): List<Int> {
        require(tableauSize >= 1 && isPowerOfTwo(tableauSize)) {
            "tableauSize must be a power of 2, got $tableauSize"
        }
        if (tableauSize == 1) return listOf(1)
        val half = tableauSize / 2
        val prev = seedingPositions(half)
        val result = mutableListOf<Int>()
        for (x in prev) {
            result.add(x)
            result.add(tableauSize + 1 - x)
        }
        return result
    }

    // -------------------------------------------------------------------------
    // Bracket construction
    // -------------------------------------------------------------------------

    /**
     * Builds an initial [DeBracket] from pool rankings.
     *
     * [fencers] must contain exactly N entries with [FencerRanking.place] values 1..N
     * (as produced by [PoolEngine.calculateRankings]). [FencerRanking.place] is used
     * as the DE seed — place 1 = top pool performer = seed 1.
     *
     * Bye matches (for the top T−N seeds) are auto-resolved at construction time;
     * callers do not need to call [recordWinner] for them.
     *
     * @throws IllegalArgumentException if [fencers] is empty or contains fewer than 2 entries.
     */
    fun buildBracket(fencers: List<FencerRanking>): DeBracket {
        val n = fencers.size
        require(n >= 2) { "At least 2 fencers required, got $n" }

        val t = tableauSize(n)
        val rounds = totalRounds(t)
        val slots = seedingPositions(t)       // slots[i] = DE seed for the (i+1)-th slot
        val seedToName = fencers.associateBy({ it.place }, { it.name })

        val matchesList = mutableListOf<DeMatch>()

        // Round 1: assign fencers and byes from the seeding order
        val r1Count = t / 2
        for (pos in 1..r1Count) {
            val topSeed = slots[(pos - 1) * 2]
            val bottomSeed = slots[(pos - 1) * 2 + 1]

            val topSlot: DeSlot = if (topSeed <= n) {
                DeSlot.Fencer(topSeed, seedToName.getValue(topSeed))
            } else DeSlot.Bye

            val bottomSlot: DeSlot = if (bottomSeed <= n) {
                DeSlot.Fencer(bottomSeed, seedToName.getValue(bottomSeed))
            } else DeSlot.Bye

            // Auto-resolve bye — only one slot can be a bye in a valid seeding
            val autoWinner: DeSlot.Fencer? = when {
                topSlot is DeSlot.Bye && bottomSlot is DeSlot.Fencer -> bottomSlot
                bottomSlot is DeSlot.Bye && topSlot is DeSlot.Fencer -> topSlot
                else -> null
            }

            matchesList.add(
                DeMatch(
                    id = calcMatchId(1, pos, t),
                    round = 1,
                    position = pos,
                    topSlot = topSlot,
                    bottomSlot = bottomSlot,
                    winner = autoWinner
                )
            )
        }

        // Rounds 2+: all slots TBD initially
        for (round in 2..rounds) {
            val count = t / (1 shl round)   // T / 2^round
            for (pos in 1..count) {
                matchesList.add(
                    DeMatch(
                        id = calcMatchId(round, pos, t),
                        round = round,
                        position = pos,
                        topSlot = DeSlot.Tbd,
                        bottomSlot = DeSlot.Tbd,
                        winner = null
                    )
                )
            }
        }

        var bracket = DeBracket(
            tableauSize = t,
            fencerCount = n,
            totalRounds = rounds,
            matches = matchesList
        )

        // Propagate round-1 bye winners into round-2 slots
        if (rounds >= 2) {
            for (r1Match in matchesList.filter { it.round == 1 && it.isBye }) {
                val winner = r1Match.winner ?: continue
                bracket = placeInNextRound(bracket, r1Match.round, r1Match.position, winner)
            }
        }

        return bracket
    }

    // -------------------------------------------------------------------------
    // Match progression
    // -------------------------------------------------------------------------

    /**
     * Records the winner of a real (non-bye) match and returns a **new** [DeBracket]
     * with the winner advanced into the correct slot of the next round.
     *
     * Idempotency: calling this twice for the same match with the same winner is
     * safe only if the match is not already resolved; the function rejects re-resolution.
     *
     * @param matchId  The [DeMatch.id] of the match to resolve.
     * @param winner   The winning fencer; their [DeSlot.Fencer.seed] must match one
     *                 of the two participating fencers.
     *
     * @throws IllegalArgumentException if [matchId] is not found, if the match is a bye,
     *                                  if the match is already resolved, or if [winner]
     *                                  is not a participant.
     */
    fun recordWinner(bracket: DeBracket, matchId: Int, winner: DeSlot.Fencer): DeBracket {
        val match = bracket.matches.firstOrNull { it.id == matchId }
            ?: throw IllegalArgumentException("Match id=$matchId not found in bracket")

        require(!match.isBye) {
            "Cannot record winner for a bye match (id=$matchId); it auto-resolves at construction"
        }
        require(match.winner == null) {
            "Match id=$matchId is already resolved (winner seed=${match.winner!!.seed})"
        }

        val topFencer = match.topSlot as? DeSlot.Fencer
        val bottomFencer = match.bottomSlot as? DeSlot.Fencer

        require(topFencer?.seed == winner.seed || bottomFencer?.seed == winner.seed) {
            "Winner seed=${winner.seed} is not a participant in match id=$matchId " +
                "(top=${topFencer?.seed}, bottom=${bottomFencer?.seed})"
        }

        // Use the authoritative slot object (preserves the canonical name)
        val resolvedWinner: DeSlot.Fencer =
            if (topFencer?.seed == winner.seed) topFencer!! else bottomFencer!!

        // Update the resolved match
        val updatedMatches = bracket.matches.map { m ->
            if (m.id == matchId) m.copy(winner = resolvedWinner) else m
        }.toMutableList()

        var result = bracket.copy(matches = updatedMatches)

        // Advance winner to next round (unless this is the final)
        if (match.round < bracket.totalRounds) {
            result = placeInNextRound(result, match.round, match.position, resolvedWinner)
        }

        return result
    }

    // -------------------------------------------------------------------------
    // Final classification
    // -------------------------------------------------------------------------

    /**
     * Computes the FIE final classification from a complete or partial [bracket].
     *
     * Only fencers whose elimination match has been resolved appear in the result.
     * For a partial bracket the list will be incomplete; call again after more
     * matches are recorded.
     *
     * Classification rules:
     * - 1st: winner of the final.
     * - 2nd: finalist (loser of the final).
     * - 3rd (shared): losers of round = [DeBracket.totalRounds] − 1 (semifinals).
     * - 5th (shared): losers of round = [DeBracket.totalRounds] − 2 (quarterfinals).
     * - …continuing in powers of 2 back to round 1.
     *
     * Bye matches (round-1 auto-advances) do not produce eliminated fencers and
     * are excluded from the loser accounting.
     *
     * Within a shared place, entries are ordered by [DeSlot.Fencer.seed] ascending
     * (lower seed = better pool rank = listed first).
     */
    fun finalClassification(bracket: DeBracket): List<DeClassification> {
        val result = mutableListOf<DeClassification>()
        val totalRounds = bracket.totalRounds

        // --- Final: winner (place 1) and finalist (place 2) ---
        val finalMatch = bracket.matches.firstOrNull { it.round == totalRounds }
        val champion = finalMatch?.winner
        if (champion != null) {
            result.add(DeClassification(place = 1, seed = champion.seed, name = champion.name, eliminatedInRound = null))

            val finalist = losersOf(finalMatch).firstOrNull()
            if (finalist != null) {
                result.add(DeClassification(place = 2, seed = finalist.seed, name = finalist.name, eliminatedInRound = totalRounds))
            }
        }

        // --- Earlier rounds: shared places starting at 3 ---
        var nextPlace = 3
        for (round in totalRounds - 1 downTo 1) {
            val losers = bracket.matches
                .filter { it.round == round && it.winner != null && !it.isBye }
                .flatMap { losersOf(it) }
                .sortedBy { it.seed }

            losers.forEach { loser ->
                result.add(
                    DeClassification(
                        place = nextPlace,
                        seed = loser.seed,
                        name = loser.name,
                        eliminatedInRound = round
                    )
                )
            }
            if (losers.isNotEmpty()) nextPlace += losers.size
        }

        return result.sortedWith(compareBy({ it.place }, { it.seed }))
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Computes the globally unique match id for a match at [round] and [position]
     * within a bracket of [tableauSize].
     *
     * The offset is the cumulative number of matches in all prior rounds:
     * offset(r) = T/2 + T/4 + … + T/2^(r-1).
     */
    private fun calcMatchId(round: Int, position: Int, tableauSize: Int): Int {
        var offset = 0
        var count = tableauSize / 2
        repeat(round - 1) {
            offset += count
            count /= 2
        }
        return offset + position
    }

    /**
     * Returns the [DeSlot.Fencer] participants who lost [match] (i.e., the non-winner
     * slot if it is a [DeSlot.Fencer]). Returns an empty list for unresolved matches.
     */
    private fun losersOf(match: DeMatch): List<DeSlot.Fencer> {
        val winner = match.winner ?: return emptyList()
        val losers = mutableListOf<DeSlot.Fencer>()
        val top = match.topSlot as? DeSlot.Fencer
        val bottom = match.bottomSlot as? DeSlot.Fencer
        if (top != null && top.seed != winner.seed) losers.add(top)
        if (bottom != null && bottom.seed != winner.seed) losers.add(bottom)
        return losers
    }

    /**
     * Places [winner] into the appropriate slot of the next-round match that follows
     * the match at ([fromRound], [fromPosition]) and returns a new [DeBracket].
     *
     * - Next round: [fromRound] + 1
     * - Next position: ([fromPosition] + 1) / 2  (ceiling division)
     * - Slot: topSlot if [fromPosition] is odd, bottomSlot if even
     */
    private fun placeInNextRound(
        bracket: DeBracket,
        fromRound: Int,
        fromPosition: Int,
        winner: DeSlot.Fencer
    ): DeBracket {
        val nextRound = fromRound + 1
        val nextPos = (fromPosition + 1) / 2
        val isTopSlot = fromPosition % 2 == 1
        val nextId = calcMatchId(nextRound, nextPos, bracket.tableauSize)

        val updatedMatches = bracket.matches.map { m ->
            if (m.id == nextId) {
                if (isTopSlot) m.copy(topSlot = winner) else m.copy(bottomSlot = winner)
            } else m
        }
        return bracket.copy(matches = updatedMatches)
    }

    private fun isPowerOfTwo(n: Int): Boolean = n > 0 && (n and (n - 1)) == 0
}
