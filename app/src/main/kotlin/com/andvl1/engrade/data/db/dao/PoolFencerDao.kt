package com.andvl1.engrade.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.andvl1.engrade.data.db.entity.PoolFencerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PoolFencerDao {
    @Insert
    suspend fun insertAll(poolFencers: List<PoolFencerEntity>)

    @Query("SELECT * FROM pool_fencer WHERE poolId = :poolId ORDER BY seedNumber ASC")
    fun getByPoolId(poolId: Long): Flow<List<PoolFencerEntity>>

    @Query("SELECT * FROM pool_fencer WHERE poolId = :poolId AND seedNumber = :seedNumber LIMIT 1")
    suspend fun getBySeed(poolId: Long, seedNumber: Int): PoolFencerEntity?

    @Query("UPDATE pool_fencer SET excluded = :excluded WHERE poolId = :poolId AND seedNumber = :seedNumber")
    suspend fun setExcluded(poolId: Long, seedNumber: Int, excluded: Boolean)

    @Query("SELECT * FROM pool_fencer WHERE poolId = :poolId AND excluded = 0 ORDER BY seedNumber ASC")
    fun getActiveFencers(poolId: Long): Flow<List<PoolFencerEntity>>
}
