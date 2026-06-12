package com.andvl1.engrade.ui.group.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.R
import com.andvl1.engrade.domain.model.BoutStatus
import com.andvl1.engrade.domain.model.MatrixCell
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import java.util.Locale

/**
 * Converts a [DashboardError] to a localized user-facing message.
 * Must be called in Composable scope so that [stringResource] is available.
 */
@Composable
private fun DashboardError.toLocalizedMessage(): String = when (this) {
    is DashboardError.DrawProhibited ->
        stringResource(R.string.error_draw_prohibited, leftScore, rightScore)
    DashboardError.PdfExportFailed ->
        stringResource(R.string.error_pdf_export_failed)
    DashboardError.CsvExportFailed ->
        stringResource(R.string.error_csv_export_failed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDashboardScreen(component: GroupDashboardComponent) {
    val state by component.state.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // M2: Show export error as Snackbar — resolve DashboardError to string in composable scope.
    val exportError = state.exportError
    val exportErrorMsg = exportError?.toLocalizedMessage()
    LaunchedEffect(exportError) {
        if (exportErrorMsg != null) {
            snackbarHostState.showSnackbar(
                message = exportErrorMsg,
                duration = SnackbarDuration.Long
            )
            component.onEvent(GroupDashboardEvent.DismissExportError)
        }
    }

    // F3: Show edit-score draw validation error as Snackbar.
    val editScoreError = state.editScoreError
    val editScoreErrorMsg = editScoreError?.toLocalizedMessage()
    LaunchedEffect(editScoreError) {
        if (editScoreErrorMsg != null) {
            snackbarHostState.showSnackbar(
                message = editScoreErrorMsg,
                duration = SnackbarDuration.Short
            )
            component.onEvent(GroupDashboardEvent.DismissEditScoreError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_stage_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { component.onEvent(GroupDashboardEvent.NavigateBack) },
                        modifier = Modifier.testTag("dashboard_button_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { component.onEvent(GroupDashboardEvent.ExportPdf) },
                        modifier = Modifier.testTag("dashboard_button_exportPdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, stringResource(R.string.export_pdf))
                    }
                    IconButton(
                        onClick = { component.onEvent(GroupDashboardEvent.ExportCsv) },
                        modifier = Modifier.testTag("groupDashboard_button_exportCsv")
                    ) {
                        Icon(Icons.Default.TableChart, stringResource(R.string.export_csv))
                    }
                    IconButton(
                        onClick = { component.onEvent(GroupDashboardEvent.NavigateToBoutsList) },
                        modifier = Modifier.testTag("dashboard_button_boutsList")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.bouts_list_title))
                    }
                    OverflowMenu(component = component, state = state)
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag("dashboard_loading"))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress info
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.bouts_progress, state.completedBoutsCount, state.totalBoutsCount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("dashboard_text_progress")
                        )
                        state.currentBout?.let { bout ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.bout_display, bout.boutOrder, bout.leftName, bout.rightName),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        state.nextBout?.let { bout ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.next_bout_display, bout.leftName, bout.rightName),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Action buttons
                if (state.currentBout != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { component.onEvent(GroupDashboardEvent.StartNextBout) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("dashboard_button_startBout")
                        ) {
                            Text(stringResource(R.string.start_next_bout))
                        }
                        OutlinedButton(
                            onClick = { component.onEvent(GroupDashboardEvent.ShowForfeitDialog) },
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("dashboard_button_forfeit")
                        ) {
                            Text(stringResource(R.string.forfeit))
                        }
                    }
                }

                // FIE Result Matrix
                Text(
                    text = stringResource(R.string.result_matrix),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("dashboard_text_matrixTitle")
                )

                MatrixTable(
                    matrix = state.matrix,
                    fencerNames = state.fencerNames,
                    fencerCount = state.fencerCount,
                    onPendingCellClick = { cell ->
                        component.onEvent(
                            GroupDashboardEvent.ShowQuickEntryDialog(cell.leftSeed, cell.rightSeed)
                        )
                    }
                )

                // Rankings Table
                Text(
                    text = stringResource(R.string.rankings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("dashboard_text_rankingsTitle")
                )

                RankingsTable(rankings = state.rankings)

                // Proceed to Direct Elimination (enabled only when all pool bouts are done)
                val isDeReady = state.totalBoutsCount > 0 &&
                    state.completedBoutsCount == state.totalBoutsCount

                Button(
                    onClick = { component.onEvent(GroupDashboardEvent.ProceedToDE) },
                    enabled = isDeReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("groupDashboard_button_proceedToDe")
                ) {
                    Text(stringResource(R.string.proceed_to_de))
                }
            }
        }

        // Edit Score Dialog
        state.showEditScoreDialog?.let { dialog ->
            EditScoreDialog(
                dialog = dialog,
                onDismiss = { component.onEvent(GroupDashboardEvent.DismissEditScoreDialog) },
                onSave = { leftScore, rightScore ->
                    component.onEvent(GroupDashboardEvent.UpdateBoutScore(dialog.boutId, leftScore, rightScore))
                }
            )
        }

        // Forfeit Dialog
        state.showForfeitDialog?.let { dialog ->
            ForfeitDialog(
                dialog = dialog,
                onDismiss = { component.onEvent(GroupDashboardEvent.DismissForfeitDialog) },
                onForfeit = { absentSide ->
                    component.onEvent(GroupDashboardEvent.RecordForfeit(dialog.boutId, absentSide))
                }
            )
        }

        // Quick Score Entry Dialog (for pending matrix cells)
        state.showQuickEntryDialog?.let { dialog ->
            QuickEntryDialog(
                dialog = dialog,
                onDismiss = { component.onEvent(GroupDashboardEvent.DismissQuickEntryDialog) },
                onConfirm = { leftScore, rightScore ->
                    component.onEvent(
                        GroupDashboardEvent.RecordQuickScore(dialog.boutId, leftScore, rightScore)
                    )
                }
            )
        }
    }
}

@Composable
private fun OverflowMenu(component: GroupDashboardComponent, state: GroupDashboardState) {
    var expanded by remember { mutableStateOf(false) }
    var showExcludeDialog by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("dashboard_button_overflow")
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.exclude)) },
                onClick = {
                    expanded = false
                    showExcludeDialog = true
                },
                modifier = Modifier.testTag("dashboard_menuItem_exclude")
            )
        }
    }

    if (showExcludeDialog) {
        ExcludeFencerDialog(
            fencerNames = state.fencerNames,
            excludedSeeds = state.excludedSeeds,
            onDismiss = { showExcludeDialog = false },
            onExclude = { seedNumber ->
                showExcludeDialog = false
                component.onEvent(GroupDashboardEvent.ExcludeFencer(seedNumber))
            }
        )
    }
}

@Composable
fun EditScoreDialog(
    dialog: EditScoreDialogState,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var leftScore by remember { mutableStateOf(dialog.leftScore.toString()) }
    var rightScore by remember { mutableStateOf(dialog.rightScore.toString()) }
    val isValid = leftScore.toIntOrNull() != null && rightScore.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_score)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = leftScore,
                    onValueChange = { leftScore = it },
                    label = { Text(dialog.leftName) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rightScore,
                    onValueChange = { rightScore = it },
                    label = { Text(dialog.rightName) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val left = leftScore.toIntOrNull() ?: 0
                    val right = rightScore.toIntOrNull() ?: 0
                    onSave(left, right)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ForfeitDialog(
    dialog: ForfeitDialogState,
    onDismiss: () -> Unit,
    onForfeit: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_forfeit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.forfeit_question))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onForfeit("LEFT") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(dialog.leftName)
                    }
                    OutlinedButton(
                        onClick = { onForfeit("RIGHT") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(dialog.rightName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ExcludeFencerDialog(
    fencerNames: Map<Int, String>,
    excludedSeeds: Set<Int>,
    onDismiss: () -> Unit,
    onExclude: (Int) -> Unit
) {
    val activeSeeds = fencerNames.keys.filter { it !in excludedSeeds }.sorted()
    var pendingSeed by remember { mutableStateOf<Int?>(null) }

    // First-step: select fencer to exclude
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_exclude)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.exclude_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                activeSeeds.forEach { seed ->
                    TextButton(
                        onClick = { pendingSeed = seed },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("excludeDialog_button_fencer_$seed")
                    ) {
                        Text("${seed}. ${fencerNames[seed] ?: ""}")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    // Second-step: confirm exclusion for the selected fencer
    pendingSeed?.let { seed ->
        val fencerName = fencerNames[seed] ?: ""
        AlertDialog(
            onDismissRequest = { pendingSeed = null },
            title = { Text(stringResource(R.string.confirm_exclude)) },
            text = { Text(stringResource(R.string.confirm_exclude_fencer, fencerName), modifier = Modifier.testTag("excludeConfirmDialog_text")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingSeed = null
                        onExclude(seed)
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSeed = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun QuickEntryDialog(
    dialog: QuickEntryDialogState,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var leftScore by remember { mutableStateOf("") }
    var rightScore by remember { mutableStateOf("") }
    val isValid = leftScore.toIntOrNull() != null && rightScore.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_score_entry)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = leftScore,
                    onValueChange = { leftScore = it },
                    label = { Text(dialog.leftName) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_input_quickScoreLeft")
                )
                OutlinedTextField(
                    value = rightScore,
                    onValueChange = { rightScore = it },
                    label = { Text(dialog.rightName) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_input_quickScoreRight")
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val left = leftScore.toIntOrNull() ?: 0
                    val right = rightScore.toIntOrNull() ?: 0
                    onConfirm(left, right)
                },
                modifier = Modifier.testTag("dashboard_button_quickScoreConfirm")
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dashboard_button_quickScoreCancel")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun MatrixTable(
    matrix: List<List<MatrixCell?>>,
    fencerNames: Map<Int, String>,
    fencerCount: Int,
    onPendingCellClick: ((MatrixCell) -> Unit)? = null
) {
    val cellSize = 60.dp
    val nameColumnWidth = 120.dp

    // M5 fix: horizontalScroll on the outer Row with NO fillMaxWidth — fillMaxWidth
    // would constrain content to screen width and disable scrolling. Fixed-size cells
    // (nameColumnWidth + fencerCount * cellSize) naturally exceed screen width for 5+
    // fencers, which activates the scroll.
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .testTag("dashboard_matrix")
    ) {
        // First column - fencer names (sticky-like: scrolls with content)
        Column {
            // Top-left corner cell
            Box(
                modifier = Modifier
                    .size(nameColumnWidth, cellSize)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("#", fontWeight = FontWeight.Bold)
            }

            // Name cells
            for (row in 1..fencerCount) {
                Box(
                    modifier = Modifier
                        .size(nameColumnWidth, cellSize)
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "${row}. ${fencerNames[row] ?: "Unknown"}",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        maxLines = 1
                    )
                }
            }
        }

        // Matrix cells column (header + data rows)
        Column {
            // Header row with numbers
            Row {
                for (col in 1..fencerCount) {
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("matrix_header_col_$col"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$col", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Matrix rows
            matrix.forEach { row ->
                Row {
                    row.forEach { cell ->
                        MatrixCellView(
                            cell = cell,
                            size = cellSize,
                            onClick = if (cell != null && cell.status == BoutStatus.PENDING) {
                                { onPendingCellClick?.invoke(cell) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixCellView(
    cell: MatrixCell?,
    size: androidx.compose.ui.unit.Dp,
    onClick: (() -> Unit)? = null
) {
    val isPending = cell != null && cell.leftScore == null
    val backgroundColor = when {
        cell == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Diagonal
        cell.isVictory == true -> MaterialTheme.colorScheme.primaryContainer
        cell.isVictory == false -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val cellTag = if (cell != null) "matrix_cell_${cell.leftSeed}_${cell.rightSeed}" else ""
    val baseModifier = Modifier
        .size(size)
        .border(
            width = if (isPending && onClick != null) 2.dp else 1.dp,
            color = if (isPending && onClick != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
        )
        .background(backgroundColor)
        .then(if (cellTag.isNotEmpty()) Modifier.testTag(cellTag) else Modifier)

    val finalModifier = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        if (cell != null && cell.leftScore != null && cell.rightScore != null) {
            val label = if (cell.isVictory == true) "V" else "D"
            Text(
                "$label${cell.leftScore}",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("matrix_score_${cell.leftSeed}_${cell.rightSeed}")
            )
        }
    }
}

@Composable
fun RankingsTable(rankings: List<com.andvl1.engrade.domain.model.FencerRanking>) {
    Card {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("#", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                Text(stringResource(R.string.ranking_header_name), fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("V", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                Text("V/M%", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("TD", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                Text("TR", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                Text("Ind", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
            }

            HorizontalDivider()

            // Rankings
            rankings.forEach { ranking ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${ranking.place}", modifier = Modifier.weight(0.5f))
                    Text(ranking.name, modifier = Modifier.weight(2f))
                    Text("${ranking.victories}", modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                    // M8 fix: explicit Locale.US for decimal separator consistency
                    Text("${String.format(Locale.US, "%.1f", ranking.vmPercent)}%", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("${ranking.touchesDelivered}", modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                    Text("${ranking.touchesReceived}", modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                    val indexSign = if (ranking.index >= 0) "+" else ""
                    Text("$indexSign${ranking.index}", modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                }
                if (ranking != rankings.last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}
