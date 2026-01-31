package com.andvl1.engrade.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pool")
data class PoolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val mode: Int,          // 4 or 5
    val weapon: String,     // "SABRE" or "FOIL_EPEE"
    val status: String      // "IN_PROGRESS" or "COMPLETED"
)
