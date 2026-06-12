package com.andvl1.engrade.data.db

import androidx.room.TypeConverter
import com.andvl1.engrade.domain.model.BoutStatus
import com.andvl1.engrade.domain.model.DeMatchStatus
import com.andvl1.engrade.domain.model.DeSlotType
import com.andvl1.engrade.domain.model.DeTableauStatus
import com.andvl1.engrade.domain.model.FencerSide
import com.andvl1.engrade.domain.model.PoolStatus

/**
 * Room TypeConverters for domain enums.
 *
 * IMPORTANT: The TEXT columns in SQLite store enum constant names exactly as-is
 * (e.g., "IN_PROGRESS", "COMPLETED", "PENDING", "FORFEIT", "LEFT", "RIGHT").
 * These converters use enum.name ↔ string mapping, so the physical DB schema does
 * NOT change for existing enums — no migration required for those.
 *
 * The DE tableau enums (DeTableauStatus, DeMatchStatus, DeSlotType) are new and
 * used only in the new tables introduced in Migration(1, 2).
 *
 * For future schema changes that alter column types or add/remove columns,
 * bump database version and add an explicit Migration(from, to).
 */
class Converters {

    @TypeConverter
    fun fromPoolStatus(status: PoolStatus): String = status.name

    @TypeConverter
    fun toPoolStatus(value: String): PoolStatus = PoolStatus.valueOf(value)

    @TypeConverter
    fun fromBoutStatus(status: BoutStatus): String = status.name

    @TypeConverter
    fun toBoutStatus(value: String): BoutStatus = BoutStatus.valueOf(value)

    @TypeConverter
    fun fromFencerSide(side: FencerSide?): String? = side?.name

    @TypeConverter
    fun toFencerSide(value: String?): FencerSide? = value?.let { FencerSide.valueOf(it) }

    // ---- DE Tableau enums (version 2) ----

    @TypeConverter
    fun fromDeTableauStatus(status: DeTableauStatus): String = status.name

    @TypeConverter
    fun toDeTableauStatus(value: String): DeTableauStatus = DeTableauStatus.valueOf(value)

    @TypeConverter
    fun fromDeMatchStatus(status: DeMatchStatus): String = status.name

    @TypeConverter
    fun toDeMatchStatus(value: String): DeMatchStatus = DeMatchStatus.valueOf(value)

    @TypeConverter
    fun fromDeSlotType(type: DeSlotType): String = type.name

    @TypeConverter
    fun toDeSlotType(value: String): DeSlotType = DeSlotType.valueOf(value)
}
