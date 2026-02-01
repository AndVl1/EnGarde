package com.andvl1.engrade.page

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.extensions.*
import com.atiurin.ultron.page.Page

object BoutResultPage : Page<BoutResultPage>() {
    val title = hasTestTag("boutResult_text_title")
    val leftName = hasTestTag("boutResult_text_leftName")
    val rightName = hasTestTag("boutResult_text_rightName")
    val leftScore = hasTestTag("boutResult_text_leftScore")
    val rightScore = hasTestTag("boutResult_text_rightScore")
    val continueButton = hasTestTag("boutResult_button_continue")
}
