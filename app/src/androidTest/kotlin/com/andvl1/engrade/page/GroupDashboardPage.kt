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
    val matrix = hasTestTag("dashboard_matrix")
    fun matrixHeaderCol(col: Int) = hasTestTag("matrix_header_col_$col")

    // Proceed to Direct Elimination (Wave 4c)
    val proceedToDeButton = hasTestTag("groupDashboard_button_proceedToDe")

    // Quick entry dialog (Wave 3)
    val quickScoreLeftInput = hasTestTag("dashboard_input_quickScoreLeft")
    val quickScoreRightInput = hasTestTag("dashboard_input_quickScoreRight")
    val quickScoreConfirmButton = hasTestTag("dashboard_button_quickScoreConfirm")
    val quickScoreCancelButton = hasTestTag("dashboard_button_quickScoreCancel")
    fun matrixCell(row: Int, col: Int) = hasTestTag("matrix_cell_${row}_${col}")
    fun matrixScore(row: Int, col: Int) = hasTestTag("matrix_score_${row}_${col}")
}
