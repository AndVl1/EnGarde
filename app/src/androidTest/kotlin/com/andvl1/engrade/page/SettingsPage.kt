package com.andvl1.engrade.page

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.extensions.*
import com.atiurin.ultron.page.Page

object SettingsPage : Page<SettingsPage>() {
    val backButton = hasTestTag("settings_button_back")
    val sabreChip = hasTestTag("settings_chip_sabre")
    val foilEpeeChip = hasTestTag("settings_chip_foilEpee")
    val mode5Chip = hasTestTag("settings_chip_mode5")
    val mode15Chip = hasTestTag("settings_chip_mode15")
    val showDoubleSwitch = hasTestTag("settings_switch_showDouble")
    val anywhereToStartSwitch = hasTestTag("settings_switch_anywhereToStart")
    val blackBackgroundSwitch = hasTestTag("settings_switch_blackBackground")
}
