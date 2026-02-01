package com.andvl1.engrade.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.andvl1.engrade.data.db.entity.PoolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PoolDao {
    @Insert
    suspend fun insert(pool: PoolEntity): Long

    @Update
    suspend fun update(pool: PoolEntity)

    @Query("SELECT * FROM pool WHERE id = :id")
    suspend fun getById(id: Long): PoolEntity?

    @Query("SELECT * FROM pool ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PoolEntity>>

    @Query("SELECT * FROM pool WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun getActivePool(): Flow<PoolEntity?>
}
