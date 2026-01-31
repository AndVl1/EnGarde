package com.andvl1.engrade.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BoutConfig(
    val mode: Int,                // Target score (5 or 15)
    val weapon: Weapon,           // Sabre or Foil/Epee
    val periodLengthMs: Long,     // Period duration (default: 3 min)
    val breakLengthMs: Long,      // Break duration (default: 1 min)
    val priorityLengthMs: Long,   // Priority duration (default: 1 min)
    val showDoubleTouchButton: Boolean = true,
    val anywhereToStart: Boolean = true
) {
    val maxPeriods: Int
        get() = when {
            mode == 5 -> 1
            mode == 15 && weapon == Weapon.FOIL_EPEE -> 3
            mode == 15 && weapon == Weapon.SABRE -> 2
            else -> 1
        }

    companion object {
        val DEFAULT = BoutConfig(
            mode = 5,
            weapon = Weapon.SABRE,
            periodLengthMs = 3 * 60 * 1000L,
            breakLengthMs = 1 * 60 * 1000L,
            priorityLengthMs = 1 * 60 * 1000L,
            showDoubleTouchButton = true,
            anywhereToStart = true
        )
    }
}
