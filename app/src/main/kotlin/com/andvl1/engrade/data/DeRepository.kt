package com.andvl1.engrade.data

import androidx.room.withTransaction
import com.andvl1.engrade.data.db.EnGardeDatabase
import com.andvl1.engrade.data.db.entity.DeMatchEntity
import com.andvl1.engrade.data.db.entity.DeTableauEntity
import com.andvl1.engrade.domain.DeTableau
import com.andvl1.engrade.domain.PoolEngine
import com.andvl1.engrade.domain.model.BoutResultData
import com.andvl1.engrade.domain.model.BoutStatus
import com.andvl1.engrade.domain.model.DeBracket
import com.andvl1.engrade.domain.model.DeClassification
import com.andvl1.engrade.domain.model.DeMatch
import com.andvl1.engrade.domain.model.DeMatchStatus
import com.andvl1.engrade.domain.model.DeSlot
import com.andvl1.engrade.domain.model.DeSlotType
import com.andvl1.engrade.domain.model.DeTableauStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repository for Direct Elimination (DE) tableau persistence.
 *
 * Coordinates between the pure domain engine ([DeTableau]) and Room entities.
 * All state mutations go through a DB transaction. The domain engine is kept pure —
 * no I/O touches [DeTableau] itself.
 */
class DeRepository(
    private val db: EnGardeDatabase,
    private val poolEngine: PoolEngine
) {

    // -------------------------------------------------------------------------
    // (a) Create DE tableau for a pool
    // -------------------------------------------------------------------------

    /**
     * Builds a DE tableau from a pool's final rankings and persists it to the DB.
     *
     * Steps:
     * 1. Load all pool fencers (including excluded) and their names.
     * 2. Load completed pool bouts as [BoutResultData].
     * 3. Call [PoolEngine.calculateRankings] to get FIE rankings.
     * 4. Call [DeTableau.buildBracket] — byes are auto-resolved.
     * 5. Persist [DeTableauEntity] + all [DeMatchEntity] rows in one transaction.
     *
     * @return The DB id of the newly created [DeTableauEntity].
     * @throws IllegalStateException if pool [poolId] is not found.
     */
    suspend fun createTableauForPool(poolId: Long): Long {
        return db.withTransaction {
            val pool = db.poolDao().getById(poolId)
                ?: error("Pool $poolId not found")

            // All pool fencers (including excluded) ordered by seedNumber
            val allPoolFencers = db.poolFencerDao().getByPoolIdOnce(poolId)
            val excludedSeeds = allPoolFencers.filter { it.excluded }.map { it.seedNumber }.toSet()

            // Batch fencer name lookup
            val fencerIds = allPoolFencers.map { it.fencerId }
            val fencerEntities = db.fencerDao().getByIds(fencerIds)
            val fencerNames: Map<Int, String> = allPoolFencers.associate { pf ->
                pf.seedNumber to (fencerEntities.find { it.id == pf.fencerId }?.name ?: "Unknown")
            }

            // All pool bouts; only completed/forfeit count toward rankings
            val boutEntities = db.poolBoutDao().getByPoolIdOnce(poolId)
            val bouts: List<BoutResultData> = boutEntities
                .filter { it.status == BoutStatus.COMPLETED || it.status == BoutStatus.FORFEIT }
                .map { bout ->
                    BoutResultData(
                        leftSeed = bout.leftFencerSeed,
                        rightSeed = bout.rightFencerSeed,
                        leftScore = bout.leftScore ?: 0,
                        rightScore = bout.rightScore ?: 0,
                        status = bout.status
                    )
                }

            // FIE rankings → DE bracket (byes auto-resolved at construction)
            val rankings = poolEngine.calculateRankings(
                fencerCount = allPoolFencers.size,
                bouts = bouts,
                fencerNames = fencerNames,
                excludedSeeds = excludedSeeds
            )
            val bracket = DeTableau.buildBracket(rankings)

            // Persist tableau header
            val tableauId = db.deTableauDao().insertTableau(
                DeTableauEntity(
                    poolId = poolId,
                    tableauSize = bracket.tableauSize,
                    fencerCount = bracket.fencerCount,
                    weapon = pool.weapon,
                    mode = pool.mode,
                    status = DeTableauStatus.IN_PROGRESS,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Persist all matches (round 1 byes already have winner set)
            val matchEntities = bracket.matches.map { match -> matchToEntity(tableauId, match) }
            db.deTableauDao().insertMatches(matchEntities)

            tableauId
        }
    }

    // -------------------------------------------------------------------------
    // (b) Observe bracket as Flow<DeBracket?>
    // -------------------------------------------------------------------------

    /**
     * Observes the DE tableau for a given pool as a reactive [DeBracket].
     *
     * Emits null if no tableau has been created yet for this pool.
     * Re-emits whenever any match result is recorded.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeBracket(poolId: Long): Flow<DeBracket?> =
        db.deTableauDao().getTableauByPoolId(poolId)
            .flatMapLatest { tableau ->
                if (tableau == null) {
                    flowOf(null)
                } else {
                    db.deTableauDao().getMatchesByTableauId(tableau.id)
                        .map { matches -> reconstructBracket(tableau, matches) }
                }
            }

    // -------------------------------------------------------------------------
    // (c) Record a DE match result
    // -------------------------------------------------------------------------

    /**
     * Records the result of a real (non-bye) DE match and propagates the winner
     * into the correct slot of the next-round match.
     *
     * Internally calls [DeTableau.recordWinner] on the reconstructed bracket to
     * validate the result and compute slot advancement, then persists the changes.
     *
     * @param tableauId  DB id of the [DeTableauEntity].
     * @param matchId    Domain [DeMatch.id] within the bracket.
     * @param winnerSeed DE seed of the winner.
     * @param topScore   Score for the top-slot fencer in this match.
     * @param bottomScore Score for the bottom-slot fencer in this match.
     *
     * @throws IllegalStateException if the tableau or match are not found.
     * @throws IllegalArgumentException (from domain) if the result is invalid.
     */
    suspend fun recordMatchResult(
        tableauId: Long,
        matchId: Int,
        winnerSeed: Int,
        topScore: Int,
        bottomScore: Int
    ) {
        db.withTransaction {
            val tableau = db.deTableauDao().getTableauById(tableauId)
                ?: error("Tableau $tableauId not found")
            val matchEntities = db.deTableauDao().getMatchesByTableauIdOnce(tableauId)
            val bracket = reconstructBracket(tableau, matchEntities)

            // Locate the domain match and resolve the winner slot object
            val domainMatch = bracket.matches.firstOrNull { it.id == matchId }
                ?: error("Match matchId=$matchId not found in tableau $tableauId")

            val winnerSlot: DeSlot.Fencer = when {
                (domainMatch.topSlot as? DeSlot.Fencer)?.seed == winnerSeed ->
                    domainMatch.topSlot as DeSlot.Fencer
                (domainMatch.bottomSlot as? DeSlot.Fencer)?.seed == winnerSeed ->
                    domainMatch.bottomSlot as DeSlot.Fencer
                else -> error("Winner seed $winnerSeed not a participant in match matchId=$matchId")
            }

            // Domain engine validates and advances the bracket
            val updatedBracket = DeTableau.recordWinner(bracket, matchId, winnerSlot)

            // Persist the resolved match
            val matchEntity = matchEntities.first { it.matchId == matchId }
            db.deTableauDao().updateMatch(
                matchEntity.copy(
                    winnerSeed = winnerSeed,
                    winnerFencerName = winnerSlot.name,
                    topScore = topScore,
                    bottomScore = bottomScore,
                    status = DeMatchStatus.COMPLETED
                )
            )

            // Propagate winner into the next round match entity (if not the final)
            if (domainMatch.round < bracket.totalRounds) {
                val nextRound = domainMatch.round + 1
                val nextPos = (domainMatch.position + 1) / 2
                val isTopSlot = domainMatch.position % 2 == 1

                val nextMatchEntity = matchEntities.firstOrNull {
                    it.round == nextRound && it.position == nextPos
                } ?: error("Next round match not found at round=$nextRound pos=$nextPos in tableau $tableauId")

                db.deTableauDao().updateMatch(
                    if (isTopSlot) {
                        nextMatchEntity.copy(
                            topSlotType = DeSlotType.FENCER,
                            topSeed = winnerSeed,
                            topFencerName = winnerSlot.name
                        )
                    } else {
                        nextMatchEntity.copy(
                            bottomSlotType = DeSlotType.FENCER,
                            bottomSeed = winnerSeed,
                            bottomFencerName = winnerSlot.name
                        )
                    }
                )
            }

            // Update tableau status
            val newTableauStatus =
                if (updatedBracket.isComplete) DeTableauStatus.COMPLETED
                else DeTableauStatus.IN_PROGRESS
            db.deTableauDao().updateTableauStatus(tableauId, newTableauStatus, System.currentTimeMillis())
        }
    }

    // -------------------------------------------------------------------------
    // (d) Final classification
    // -------------------------------------------------------------------------

    /**
     * Returns a [Flow] of [DeClassification] entries for the DE tableau associated
     * with [poolId]. Emits an empty list if no tableau exists or the bracket is empty.
     *
     * Classification follows FIE rules (no 3rd-place bout). See [DeTableau.finalClassification].
     */
    fun observeFinalClassification(poolId: Long): Flow<List<DeClassification>> =
        observeBracket(poolId).map { bracket ->
            if (bracket == null) emptyList()
            else DeTableau.finalClassification(bracket)
        }

    // -------------------------------------------------------------------------
    // Entity ↔ domain mapping
    // -------------------------------------------------------------------------

    private fun matchToEntity(tableauId: Long, match: DeMatch): DeMatchEntity {
        val topSlotType = when (match.topSlot) {
            is DeSlot.Fencer -> DeSlotType.FENCER
            DeSlot.Bye -> DeSlotType.BYE
            DeSlot.Tbd -> DeSlotType.TBD
        }
        val bottomSlotType = when (match.bottomSlot) {
            is DeSlot.Fencer -> DeSlotType.FENCER
            DeSlot.Bye -> DeSlotType.BYE
            DeSlot.Tbd -> DeSlotType.TBD
        }
        val status = when {
            match.isBye -> DeMatchStatus.BYE
            match.winner != null -> DeMatchStatus.COMPLETED
            else -> DeMatchStatus.PENDING
        }
        return DeMatchEntity(
            tableauId = tableauId,
            matchId = match.id,
            round = match.round,
            position = match.position,
            topSlotType = topSlotType,
            topSeed = (match.topSlot as? DeSlot.Fencer)?.seed,
            topFencerName = (match.topSlot as? DeSlot.Fencer)?.name,
            bottomSlotType = bottomSlotType,
            bottomSeed = (match.bottomSlot as? DeSlot.Fencer)?.seed,
            bottomFencerName = (match.bottomSlot as? DeSlot.Fencer)?.name,
            topScore = null,
            bottomScore = null,
            winnerSeed = match.winner?.seed,
            winnerFencerName = match.winner?.name,
            isBye = match.isBye,
            status = status
        )
    }

    private fun entityToDeMatch(entity: DeMatchEntity): DeMatch {
        val topSlot: DeSlot = when (entity.topSlotType) {
            DeSlotType.FENCER -> DeSlot.Fencer(
                seed = entity.topSeed!!,
                name = entity.topFencerName!!
            )
            DeSlotType.BYE -> DeSlot.Bye
            DeSlotType.TBD -> DeSlot.Tbd
        }
        val bottomSlot: DeSlot = when (entity.bottomSlotType) {
            DeSlotType.FENCER -> DeSlot.Fencer(
                seed = entity.bottomSeed!!,
                name = entity.bottomFencerName!!
            )
            DeSlotType.BYE -> DeSlot.Bye
            DeSlotType.TBD -> DeSlot.Tbd
        }
        val winner: DeSlot.Fencer? =
            if (entity.winnerSeed != null && entity.winnerFencerName != null) {
                DeSlot.Fencer(seed = entity.winnerSeed, name = entity.winnerFencerName)
            } else null

        return DeMatch(
            id = entity.matchId,
            round = entity.round,
            position = entity.position,
            topSlot = topSlot,
            bottomSlot = bottomSlot,
            winner = winner
        )
    }

    private fun reconstructBracket(
        tableau: DeTableauEntity,
        matches: List<DeMatchEntity>
    ): DeBracket = DeBracket(
        tableauSize = tableau.tableauSize,
        fencerCount = tableau.fencerCount,
        totalRounds = DeTableau.totalRounds(tableau.tableauSize),
        matches = matches.map { entityToDeMatch(it) }
    )
}
