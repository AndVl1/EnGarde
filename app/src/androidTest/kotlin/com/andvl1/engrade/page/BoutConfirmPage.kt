package com.andvl1.engrade.page

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.extensions.*
import com.atiurin.ultron.page.Page

object BoutConfirmPage : Page<BoutConfirmPage>() {
    val boutNumber = hasTestTag("boutConfirm_text_boutNumber")
    val leftName = hasTestTag("boutConfirm_text_leftName")
    val rightName = hasTestTag("boutConfirm_text_rightName")
    val swapButton = hasTestTag("boutConfirm_button_swap")
    val startButton = hasTestTag("boutConfirm_button_start")
    val cancelButton = hasTestTag("boutConfirm_button_cancel")
    val loading = hasTestTag("boutConfirm_loading")
}
