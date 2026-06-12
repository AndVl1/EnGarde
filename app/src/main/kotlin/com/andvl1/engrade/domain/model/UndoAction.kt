package com.andvl1.engrade.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents an action that can be undone in the bout.
 * Matches the action codes from original MainActivity.
 */
@Serializable
sealed class UndoAction {
    @Serializable
    data class LeftScored(
        val previousSection: SectionType,
        val previousNextSection: SectionType,
        val previousTime: Long
    ) : UndoAction()  // 0

    @Serializable
    data class RightScored(
        val previousSection: SectionType,
        val previousNextSection: SectionType,
        val previousTime: Long
    ) : UndoAction()  // 1

    @Serializable
    data class BothScored(
        val previousSection: SectionType,
        val previousNextSection: SectionType,
        val previousTime: Long
    ) : UndoAction()  // 2

    @Serializable
    data object LeftYellowCard : UndoAction()  // 3

    @Serializable
    data class LeftRedCard(
        val previousSection: SectionType,
        val previousNextSection: SectionType,
        val previousTime: Long
    ) : UndoAction()  // 4

    @Serializable
    data object RightYellowCard : UndoAction()  // 5

    @Serializable
    data class RightRedCard(
        val previousSection: SectionType,
        val previousNextSection: SectionType,
        val previousTime: Long
    ) : UndoAction()  // 6

    @Serializable
    data class SectionSkipped(
        val previousTime: Long,
        val previousSection: SectionType,
        val previousPeriod: Int
    ) : UndoAction()  // 7

    // FIE t.114 yellow→red escalation: restores yellow state, removes the opponent point, and
    // restores section state in case applySabreBreakAt8IfNeeded mutated it.
    @Serializable
    data class LeftYellowToRedEscalation(
        val previousSection: SectionType,
        val previousNextSection: SectionType,
        val previousTime: Long
    ) : UndoAction()   // 8

    @Serializable
    data class RightYellowToRedEscalation(
        val previousSection: SectionType,
        val previousNextSection: SectionType,
        val previousTime: Long
    ) : UndoAction()  // 9

    // Black card (exclusion): restores bout to active state, no score change
    @Serializable
    data object LeftBlackCard : UndoAction()   // 10

    @Serializable
    data object RightBlackCard : UndoAction()  // 11
}
