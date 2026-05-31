package com.andvl1.engrade.data.db

import androidx.room.TypeConverter
import com.andvl1.engrade.domain.model.BoutStatus
import com.andvl1.engrade.domain.model.FencerSide
import com.andvl1.engrade.domain.model.PoolStatus

/**
 * Room TypeConverters for domain enums.
 *
 * IMPORTANT: The TEXT columns in SQLite store enum constant names exactly as-is
 * (e.g., "IN_PROGRESS", "COMPLETED", "PENDING", "FORFEIT", "LEFT", "RIGHT").
 * These converters use enum.name ↔ string mapping, so the physical DB schema does
 * NOT change — no migration required (version stays at 1).
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
}
