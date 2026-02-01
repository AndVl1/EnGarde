package com.andvl1.engrade.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pool_fencer",
    foreignKeys = [
        ForeignKey(
            entity = PoolEntity::class,
            parentColumns = ["id"],
            childColumns = ["poolId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FencerEntity::class,
            parentColumns = ["id"],
            childColumns = ["fencerId"]
        )
    ],
    indices = [Index("poolId"), Index("fencerId")]
)
data class PoolFencerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val poolId: Long,
    val fencerId: Long,
    val seedNumber: Int,
    val excluded: Boolean = false
)
