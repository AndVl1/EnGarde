package com.andvl1.engrade.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.andvl1.engrade.domain.model.BoutStatus
import com.andvl1.engrade.domain.model.FencerSide

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
    val winner: FencerSide? = null,    // stored as TEXT matching enum.name — no schema change
    val status: BoutStatus = BoutStatus.PENDING // stored as TEXT matching enum.name — no schema change
)
