package com.andvl1.engrade.page

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.extensions.*
import com.atiurin.ultron.page.Page

object BoutsListPage : Page<BoutsListPage>() {
    val backButton = hasTestTag("boutsList_button_back")
    val loading = hasTestTag("boutsList_loading")
    val list = hasTestTag("boutsList_list")
    fun boutItem(number: Int) = hasTestTag("boutsList_item_$number")
}
