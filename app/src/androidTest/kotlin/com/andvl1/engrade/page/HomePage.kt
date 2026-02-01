package com.andvl1.engrade.page

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.extensions.*
import com.atiurin.ultron.page.Page

object HomePage : Page<HomePage>() {
    val settingsButton = hasTestTag("home_button_settings")
    val singleBoutButton = hasTestTag("home_button_singleBout")
    val groupStageButton = hasTestTag("home_button_groupStage")
    val continuePoolButton = hasTestTag("home_button_continuePool")
    val title = hasTestTag("home_text_title")
}
