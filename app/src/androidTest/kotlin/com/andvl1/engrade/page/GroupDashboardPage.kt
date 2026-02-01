package com.andvl1.engrade.page

import androidx.compose.ui.test.hasTestTag
import com.atiurin.ultron.extensions.*
import com.atiurin.ultron.page.Page

object GroupDashboardPage : Page<GroupDashboardPage>() {
    val backButton = hasTestTag("dashboard_button_back")
    val exportPdfButton = hasTestTag("dashboard_button_exportPdf")
    val boutsListButton = hasTestTag("dashboard_button_boutsList")
    val progressText = hasTestTag("dashboard_text_progress")
    val startBoutButton = hasTestTag("dashboard_button_startBout")
    val forfeitButton = hasTestTag("dashboard_button_forfeit")
    val matrixTitle = hasTestTag("dashboard_text_matrixTitle")
    val rankingsTitle = hasTestTag("dashboard_text_rankingsTitle")
    val loading = hasTestTag("dashboard_loading")
}
