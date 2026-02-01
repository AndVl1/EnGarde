package com.andvl1.engrade.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andvl1.engrade.data.db.entity.FencerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FencerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(fencer: FencerEntity): Long

    @Query("SELECT * FROM fencer WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<FencerEntity>>

    @Query("SELECT * FROM fencer WHERE id = :id")
    suspend fun getById(id: Long): FencerEntity?

    @Query("SELECT * FROM fencer WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): FencerEntity?
}
