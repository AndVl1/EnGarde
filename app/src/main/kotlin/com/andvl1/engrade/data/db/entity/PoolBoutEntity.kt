package com.andvl1.engrade.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pool_bout",
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
data class PoolBoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val poolId: Long,
    val boutOrder: Int,
    val leftFencerSeed: Int,
    val rightFencerSeed: Int,
    val leftScore: Int? = null,
    val rightScore: Int? = null,
    val winner: String? = null,    // "LEFT" or "RIGHT"
    val status: String = "PENDING" // "PENDING", "COMPLETED", "FORFEIT"
)
