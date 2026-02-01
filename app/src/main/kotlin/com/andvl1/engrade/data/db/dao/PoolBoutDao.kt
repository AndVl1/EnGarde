package com.andvl1.engrade.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.andvl1.engrade.data.db.entity.PoolBoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PoolBoutDao {
    @Insert
    suspend fun insertAll(bouts: List<PoolBoutEntity>)

    @Update
    suspend fun update(bout: PoolBoutEntity)

    @Query("SELECT * FROM pool_bout WHERE poolId = :poolId ORDER BY boutOrder ASC")
    fun getByPoolId(poolId: Long): Flow<List<PoolBoutEntity>>

    @Query("""
        SELECT * FROM pool_bout
        WHERE poolId = :poolId AND status = 'PENDING'
        ORDER BY boutOrder ASC
        LIMIT 1
    """)
    suspend fun getNextPendingBout(poolId: Long): PoolBoutEntity?

    @Query("SELECT * FROM pool_bout WHERE id = :boutId")
    suspend fun getById(boutId: Long): PoolBoutEntity?

    @Query("""
        UPDATE pool_bout
        SET leftScore = :leftScore, rightScore = :rightScore, winner = :winner, status = :status
        WHERE id = :boutId
    """)
    suspend fun updateResult(boutId: Long, leftScore: Int, rightScore: Int, winner: String?, status: String)

    @Query("""
        UPDATE pool_bout
        SET leftScore = NULL, rightScore = NULL, winner = NULL, status = 'PENDING'
        WHERE poolId = :poolId AND (leftFencerSeed = :seedNumber OR rightFencerSeed = :seedNumber)
    """)
    suspend fun annulBoutsForSeed(poolId: Long, seedNumber: Int)
}
