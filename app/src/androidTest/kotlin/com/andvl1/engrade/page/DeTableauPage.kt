package com.andvl1.engrade.page

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.extensions.*
import com.atiurin.ultron.page.Page

object DeTableauPage : Page<DeTableauPage>() {
    val screenTitle = hasTestTag("de_text_screenTitle")
    val backButton = hasTestTag("de_button_back")
    val loading = hasTestTag("de_loading")
    val classificationTitle = hasTestTag("de_text_classificationTitle")

    fun roundTitle(round: Int) = hasTestTag("de_text_roundTitle_$round")
    fun matchCard(matchId: Int) = hasTestTag("de_match_$matchId")
    fun slotTop(matchId: Int) = hasTestTag("de_slot_top_$matchId")
    fun slotBottom(matchId: Int) = hasTestTag("de_slot_bottom_$matchId")
    fun playButton(matchId: Int) = hasTestTag("de_button_playMatch_$matchId")
    fun classificationEntry(place: Int) = hasTestTag("de_text_classification_$place")
}
