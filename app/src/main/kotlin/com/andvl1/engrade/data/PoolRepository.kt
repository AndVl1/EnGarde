package com.andvl1.engrade.data

import com.andvl1.engrade.data.db.EnGardeDatabase
import com.andvl1.engrade.data.db.entity.FencerEntity
import com.andvl1.engrade.data.db.entity.PoolBoutEntity
import com.andvl1.engrade.data.db.entity.PoolEntity
import com.andvl1.engrade.data.db.entity.PoolFencerEntity
import com.andvl1.engrade.domain.FieBoutOrder
import com.andvl1.engrade.domain.model.FencerInput
import com.andvl1.engrade.domain.model.Weapon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PoolRepository(private val db: EnGardeDatabase) {

    /**
     * Create a new pool with fencers and generate all bouts.
     * Returns the pool ID.
     */
    suspend fun createPool(mode: Int, weapon: Weapon, fencers: List<FencerInput>): Long {
        require(fencers.size in 5..8) { "Pool must have 5-8 fencers" }
        require(mode in listOf(4, 5)) { "Mode must be 4 or 5" }

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
                status = "IN_PROGRESS"
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
                status = "PENDING"
            )
        }
        db.poolBoutDao().insertAll(bouts)

        return poolId
    }

    /**
     * Get pool by ID.
     */
    fun getPoolById(poolId: Long): Flow<PoolEntity?> =
        db.poolDao().getActivePool() // For simplicity, returns active pool

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
            val fencers = fencerIds.mapNotNull { db.fencerDao().getById(it) }
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
     */
    suspend fun recordBoutResult(boutId: Long, leftScore: Int, rightScore: Int) {
        val winner = when {
            leftScore > rightScore -> "LEFT"
            rightScore > leftScore -> "RIGHT"
            else -> null
        }

        db.poolBoutDao().updateResult(
            boutId = boutId,
            leftScore = leftScore,
            rightScore = rightScore,
            winner = winner,
            status = "COMPLETED"
        )
    }

    /**
     * Record forfeit (one fencer absent).
     */
    suspend fun recordForfeit(boutId: Long, absentSide: String, maxScore: Int) {
        val (leftScore, rightScore, winner) = when (absentSide) {
            "LEFT" -> Triple(0, maxScore, "RIGHT")
            "RIGHT" -> Triple(maxScore, 0, "LEFT")
            else -> error("Invalid absent side: $absentSide")
        }

        db.poolBoutDao().updateResult(
            boutId = boutId,
            leftScore = leftScore,
            rightScore = rightScore,
            winner = winner,
            status = "FORFEIT"
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
     */
    suspend fun updateBoutScore(boutId: Long, leftScore: Int, rightScore: Int) {
        val winner = when {
            leftScore > rightScore -> "LEFT"
            rightScore > leftScore -> "RIGHT"
            else -> null
        }

        db.poolBoutDao().updateResult(
            boutId = boutId,
            leftScore = leftScore,
            rightScore = rightScore,
            winner = winner,
            status = "COMPLETED"
        )
    }

    /**
     * Get active pool.
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
        db.poolDao().update(pool.copy(status = "COMPLETED"))
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
