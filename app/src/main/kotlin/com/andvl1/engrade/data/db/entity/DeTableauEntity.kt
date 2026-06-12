package com.andvl1.engrade.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.andvl1.engrade.domain.model.DeTableauStatus

@Entity(
    tableName = "de_tableau",
    foreignKeys = [
        ForeignKey(
            entity = PoolEntity::class,
            parentColumns = ["id"],
            childColumns = ["poolId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("poolId")]
)
data class DeTableauEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val poolId: Long,
    val tableauSize: Int,
    val fencerCount: Int,
    val weapon: String,
    val mode: Int,
    val status: DeTableauStatus,
    val createdAt: Long,
    val updatedAt: Long
)
