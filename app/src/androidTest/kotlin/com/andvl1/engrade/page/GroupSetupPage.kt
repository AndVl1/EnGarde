package com.andvl1.engrade.page

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.extensions.*
import com.atiurin.ultron.page.Page

object GroupSetupPage : Page<GroupSetupPage>() {
    fun fencerCountChip(count: Int) = hasTestTag("groupSetup_chip_count_$count")
    fun modeChip(mode: Int) = hasTestTag("groupSetup_chip_mode_$mode")
    fun weaponChip(weapon: String) = hasTestTag("groupSetup_chip_weapon_$weapon")
    fun nameInput(index: Int) = hasTestTag("groupSetup_input_name_$index")
    fun orgInput(index: Int) = hasTestTag("groupSetup_input_org_$index")
    fun regionInput(index: Int) = hasTestTag("groupSetup_input_region_$index")
    val createButton = hasTestTag("groupSetup_button_create")
}
