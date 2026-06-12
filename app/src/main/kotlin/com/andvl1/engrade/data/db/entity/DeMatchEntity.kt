package com.andvl1.engrade.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.andvl1.engrade.domain.model.DeMatchStatus
import com.andvl1.engrade.domain.model.DeSlotType

@Entity(
    tableName = "de_match",
    foreignKeys = [
        ForeignKey(
            entity = DeTableauEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableauId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tableauId")]
)
data class DeMatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableauId: Long,
    /** Domain [com.andvl1.engrade.domain.model.DeMatch.id] — unique within a tableau. */
    val matchId: Int,
    val round: Int,
    val position: Int,
    val topSlotType: DeSlotType,
    val topSeed: Int? = null,
    val topFencerName: String? = null,
    val bottomSlotType: DeSlotType,
    val bottomSeed: Int? = null,
    val bottomFencerName: String? = null,
    /** Score of the top-slot fencer in the real bout; null for bye or pending matches. */
    val topScore: Int? = null,
    /** Score of the bottom-slot fencer in the real bout; null for bye or pending matches. */
    val bottomScore: Int? = null,
    val winnerSeed: Int? = null,
    val winnerFencerName: String? = null,
    val isBye: Boolean,
    val status: DeMatchStatus
)
