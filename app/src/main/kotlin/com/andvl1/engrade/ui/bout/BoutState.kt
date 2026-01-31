package com.andvl1.engrade.ui.bout

import com.andvl1.engrade.domain.model.*

data class BoutState(
    val leftFencer: FencerState = FencerState(),
    val rightFencer: FencerState = FencerState(),
    val leftFencerName: String = "Left",
    val rightFencerName: String = "Right",
    val timeRemainingMs: Long = 180_000L,
    val periodNumber: Int = 1,
    val currentSection: SectionType = SectionType.PERIOD,
    val isTimerRunning: Boolean = false,
    val isOver: Boolean = false,
    val canUndo: Boolean = false,
    val config: BoutConfig = BoutConfig.DEFAULT,
    val showCardDialog: CardDialogState? = null
)

data class CardDialogState(
    val fencerSide: FencerSide
)
