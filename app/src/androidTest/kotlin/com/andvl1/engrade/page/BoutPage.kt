package com.andvl1.engrade.page

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.extensions.*
import com.atiurin.ultron.page.Page

object BoutPage : Page<BoutPage>() {
    // Timer
    val timerText = hasTestTag("bout_text_timer")
    val timerBox = hasTestTag("bout_box_timer")

    // Section title
    val sectionTitle = hasTestTag("bout_text_sectionTitle")

    // Left fencer
    val leftName = hasTestTag("bout_text_leftName")
    val leftScoreButton = hasTestTag("bout_button_leftScore")
    val leftScore = hasTestTag("bout_text_leftScore")
    val leftCardButton = hasTestTag("bout_button_leftCard")
    val leftWinner = hasTestTag("bout_text_leftWinner")

    // Right fencer
    val rightName = hasTestTag("bout_text_rightName")
    val rightScoreButton = hasTestTag("bout_button_rightScore")
    val rightScore = hasTestTag("bout_text_rightScore")
    val rightCardButton = hasTestTag("bout_button_rightCard")
    val rightWinner = hasTestTag("bout_text_rightWinner")

    // Actions
    val doubleTouchButton = hasTestTag("bout_button_doubleTouch")
    val undoButton = hasTestTag("bout_button_undo")
    val skipSectionButton = hasTestTag("bout_button_skipSection")
    val resetButton = hasTestTag("bout_button_reset")
    val settingsButton = hasTestTag("bout_button_settings")

    // Card dialog
    val yellowCardButton = hasTestTag("bout_button_yellowCard")
    val redCardButton = hasTestTag("bout_button_redCard")
    val cancelCardButton = hasTestTag("bout_button_cancelCard")
}
