package com.andvl1.engrade.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents an action that can be undone in the bout.
 * Matches the action codes from original MainActivity.
 */
@Serializable
sealed class UndoAction {
    @Serializable
    data object LeftScored : UndoAction()  // 0

    @Serializable
    data object RightScored : UndoAction()  // 1

    @Serializable
    data object BothScored : UndoAction()  // 2

    @Serializable
    data object LeftYellowCard : UndoAction()  // 3

    @Serializable
    data object LeftRedCard : UndoAction()  // 4

    @Serializable
    data object RightYellowCard : UndoAction()  // 5

    @Serializable
    data object RightRedCard : UndoAction()  // 6

    @Serializable
    data class SectionSkipped(
        val previousTime: Long,
        val previousSection: SectionType,
        val previousPeriod: Int
    ) : UndoAction()  // 7
}
