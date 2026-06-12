package com.andvl1.engrade.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.andvl1.engrade.data.db.entity.DeMatchEntity
import com.andvl1.engrade.data.db.entity.DeTableauEntity
import com.andvl1.engrade.domain.model.DeTableauStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DeTableauDao {

    @Insert
    suspend fun insertTableau(tableau: DeTableauEntity): Long

    @Insert
    suspend fun insertMatches(matches: List<DeMatchEntity>)

    @Query("SELECT * FROM de_tableau WHERE poolId = :poolId LIMIT 1")
    fun getTableauByPoolId(poolId: Long): Flow<DeTableauEntity?>

    @Query("SELECT * FROM de_tableau WHERE id = :tableauId")
    suspend fun getTableauById(tableauId: Long): DeTableauEntity?

    /**
     * Observe all matches for a tableau as a reactive Flow.
     * Ordered by round then position to match domain ordering.
     */
    @Query("SELECT * FROM de_match WHERE tableauId = :tableauId ORDER BY round ASC, position ASC")
    fun getMatchesByTableauId(tableauId: Long): Flow<List<DeMatchEntity>>

    /**
     * One-shot suspend read of all matches for a tableau.
     * Used inside transactions when a Flow is not appropriate.
     */
    @Query("SELECT * FROM de_match WHERE tableauId = :tableauId ORDER BY round ASC, position ASC")
    suspend fun getMatchesByTableauIdOnce(tableauId: Long): List<DeMatchEntity>

    @Update
    suspend fun updateMatch(match: DeMatchEntity)

    @Query(
        "UPDATE de_tableau SET status = :status, updatedAt = :updatedAt WHERE id = :tableauId"
    )
    suspend fun updateTableauStatus(
        tableauId: Long,
        status: DeTableauStatus,
        updatedAt: Long
    )
}
