package com.andvl1.engrade.data

import androidx.room.withTransaction
import com.andvl1.engrade.data.db.EnGardeDatabase
import com.andvl1.engrade.data.db.entity.FencerEntity
import com.andvl1.engrade.data.db.entity.PoolBoutEntity
import com.andvl1.engrade.data.db.entity.PoolEntity
import com.andvl1.engrade.data.db.entity.PoolFencerEntity
import com.andvl1.engrade.domain.FieBoutOrder
import com.andvl1.engrade.domain.model.BoutStatus
import com.andvl1.engrade.domain.model.FencerInput
import com.andvl1.engrade.domain.model.FencerSide
import com.andvl1.engrade.domain.model.PoolStatus
import com.andvl1.engrade.domain.model.Weapon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PoolRepository(private val db: EnGardeDatabase) {

    /**
     * Create a new pool with fencers and generate all bouts.
     * All inserts are wrapped in a single Room transaction (H6).
     * Returns the pool ID.
     *
     * Valid modes: 4 (touches) or 5 (touches) — matches GroupSetupScreen UI options (M1).
     */
    suspend fun createPool(mode: Int, weapon: Weapon, fencers: List<FencerInput>): Long {
        require(fencers.size in 5..8) { "Pool must have 5-8 fencers" }
        require(mode in VALID_MODES) { "Mode must be one of $VALID_MODES" }

        return db.withTransaction {
            // Create or get fencer entities
            val fencerIds = mutableListOf<Long>()
            fencers.forEach { input ->
                val existing = db.fencerDao().getByName(input.name)
                val fencerId = if (existing != null) {
                    existing.id
                } else {
                    db.fencerDao().insert(
                        FencerEntity(
                            name = input.name,
                            organization = input.organization,
                            region = input.region
                        )
                    )
                }
                fencerIds.add(fencerId)
            }

            // Create pool
            val poolId = db.poolDao().insert(
                PoolEntity(
                    createdAt = System.currentTimeMillis(),
                    mode = mode,
                    weapon = weapon.name,
                    status = PoolStatus.IN_PROGRESS
                )
            )

            // Create pool fencers with seed numbers
            val poolFencers = fencerIds.mapIndexed { index, fencerId ->
                PoolFencerEntity(
                    poolId = poolId,
                    fencerId = fencerId,
                    seedNumber = index + 1,
                    excluded = false
                )
            }
            db.poolFencerDao().insertAll(poolFencers)

            // Generate bouts using FIE order
            val boutOrder = FieBoutOrder.getBoutOrder(fencers.size)
            val bouts = boutOrder.mapIndexed { index, (left, right) ->
                PoolBoutEntity(
                    poolId = poolId,
                    boutOrder = index + 1,
                    leftFencerSeed = left,
                    rightFencerSeed = right,
                    status = BoutStatus.PENDING
                )
            }
            db.poolBoutDao().insertAll(bouts)

            poolId
        }
    }

    /**
     * Get pool by ID as a reactive Flow (C1 fix — was ignoring the parameter).
     */
    fun getPoolById(poolId: Long): Flow<PoolEntity?> =
        db.poolDao().getByIdFlow(poolId)

    /**
     * Get pool fencers with names.
     */
    fun getPoolFencersWithNames(poolId: Long): Flow<List<PoolFencerWithName>> {
        return db.poolFencerDao().getByPoolId(poolId).combine(
            db.fencerDao().searchByName("")
        ) { poolFencers, allFencers ->
            val fencerMap = allFencers.associateBy { it.id }
            poolFencers.map { pf ->
                PoolFencerWithName(
                    poolFencer = pf,
                    fencerName = fencerMap[pf.fencerId]?.name ?: "Unknown"
                )
            }
        }
    }

    /**
     * Get pool bouts with fencer names.
     */
    fun getPoolBoutsWithNames(poolId: Long): Flow<List<PoolBoutWithNames>> {
        return combine(
            db.poolBoutDao().getByPoolId(poolId),
            db.poolFencerDao().getByPoolId(poolId)
        ) { bouts, poolFencers ->
            val fencerIds = poolFencers.map { it.fencerId }
            // Single batch query instead of N individual getById calls (H3 N+1 fix)
            val fencers = db.fencerDao().getByIds(fencerIds)
            val fencerMap = poolFencers.associate { pf ->
                pf.seedNumber to fencers.find { it.id == pf.fencerId }?.name
            }

            bouts.map { bout ->
                PoolBoutWithNames(
                    bout = bout,
                    leftFencerName = fencerMap[bout.leftFencerSeed] ?: "Unknown",
                    rightFencerName = fencerMap[bout.rightFencerSeed] ?: "Unknown"
                )
            }
        }
    }

    /**
     * Get next pending bout.
     */
    suspend fun getNextPendingBout(poolId: Long): PoolBoutEntity? {
        return db.poolBoutDao().getNextPendingBout(poolId)
    }

    /**
     * Record bout result (normal completion).
     *
     * FIE rule: draws are not allowed in pool bouts (M3).
     * Finalizing a bout with leftScore == rightScore is rejected here at the data layer.
     */
    suspend fun recordBoutResult(boutId: Long, leftScore: Int, rightScore: Int) {
        validateNoDraw(leftScore, rightScore)

        val winner = when {
            leftScore > rightScore -> FencerSide.LEFT
            else -> FencerSide.RIGHT
        }

        db.poolBoutDao().updateResult(
            boutId = boutId,
            leftScore = leftScore,
            rightScore = rightScore,
            winner = winner,
            status = BoutStatus.COMPLETED
        )
    }

    /**
     * Record forfeit (one fencer absent).
     */
    suspend fun recordForfeit(boutId: Long, absentSide: String, maxScore: Int) {
        val absentFencerSide = FencerSide.valueOf(absentSide)
        val (leftScore, rightScore, winner) = when (absentFencerSide) {
            FencerSide.LEFT -> Triple(0, maxScore, FencerSide.RIGHT)
            FencerSide.RIGHT -> Triple(maxScore, 0, FencerSide.LEFT)
        }

        db.poolBoutDao().updateResult(
            boutId = boutId,
            leftScore = leftScore,
            rightScore = rightScore,
            winner = winner,
            status = BoutStatus.FORFEIT
        )
    }

    /**
     * Exclude a fencer and annul all their bouts.
     */
    suspend fun excludeFencer(poolId: Long, seedNumber: Int) {
        db.poolFencerDao().setExcluded(poolId, seedNumber, true)
        db.poolBoutDao().annulBoutsForSeed(poolId, seedNumber)
    }

    /**
     * Update bout score (for editing completed bouts).
     *
     * F3: FIE rule — draws are not allowed. Equal scores are rejected here and
     * at the UI layer (callers validate before calling to avoid crashing).
     */
    suspend fun updateBoutScore(boutId: Long, leftScore: Int, rightScore: Int) {
        validateNoDraw(leftScore, rightScore)
        val winner = when {
            leftScore > rightScore -> FencerSide.LEFT
            rightScore > leftScore -> FencerSide.RIGHT
            else -> null
        }

        db.poolBoutDao().updateResult(
            boutId = boutId,
            leftScore = leftScore,
            rightScore = rightScore,
            winner = winner,
            status = BoutStatus.COMPLETED
        )
    }

    /**
     * Get active pool (IN_PROGRESS). Used for "Continue Pool" feature.
     */
    fun getActivePool(): Flow<PoolEntity?> {
        return db.poolDao().getActivePool()
    }

    /**
     * Search fencers by name.
     */
    fun searchFencers(query: String): Flow<List<FencerEntity>> {
        return db.fencerDao().searchByName(query)
    }

    /**
     * Complete the pool.
     */
    suspend fun completePool(poolId: Long) {
        val pool = db.poolDao().getById(poolId) ?: return
        db.poolDao().update(pool.copy(status = PoolStatus.COMPLETED))
    }

    companion object {
        /** Valid bout mode values (touches). Matches GroupSetupScreen UI options. */
        val VALID_MODES = listOf(4, 5)

        /**
         * F3: Validates that bout scores don't form an illegal FIE draw.
         * Exposed as internal so unit tests can verify the invariant without a real DB.
         * @throws IllegalArgumentException if leftScore == rightScore
         */
        @Throws(IllegalArgumentException::class)
        internal fun validateNoDraw(leftScore: Int, rightScore: Int) {
            require(leftScore != rightScore) {
                "FIE: ничья в бое запрещена (счёт $leftScore:$rightScore равный)"
            }
        }
    }
}

data class PoolFencerWithName(
    val poolFencer: PoolFencerEntity,
    val fencerName: String
)

data class PoolBoutWithNames(
    val bout: PoolBoutEntity,
    val leftFencerName: String,
    val rightFencerName: String
)
