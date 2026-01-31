package com.andvl1.engrade.ui.bout

import com.andvl1.engrade.domain.model.CardType
import com.andvl1.engrade.domain.model.FencerSide

sealed class BoutEvent {
    // Timer
    data object TimerClicked : BoutEvent()

    // Scoring
    data object LeftScored : BoutEvent()
    data object RightScored : BoutEvent()
    data object DoubleTouch : BoutEvent()

    // Cards
    data class ShowCardDialog(val side: FencerSide) : BoutEvent()
    data object DismissCardDialog : BoutEvent()
    data class CardSelected(val side: FencerSide, val type: CardType) : BoutEvent()

    // Actions
    data object Undo : BoutEvent()
    data object Reset : BoutEvent()
    data object SkipSection : BoutEvent()
    data object OpenSettings : BoutEvent()
}
