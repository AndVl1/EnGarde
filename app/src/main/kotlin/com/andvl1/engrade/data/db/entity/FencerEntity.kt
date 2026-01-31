package com.andvl1.engrade.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fencer")
data class FencerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val organization: String? = null,
    val region: String? = null
)
