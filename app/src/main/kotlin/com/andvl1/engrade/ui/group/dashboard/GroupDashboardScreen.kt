package com.andvl1.engrade.ui.group.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.R
import com.andvl1.engrade.domain.model.MatrixCell
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDashboardScreen(component: GroupDashboardComponent) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_stage_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { component.onEvent(GroupDashboardEvent.NavigateBack) },
                        modifier = Modifier.testTag("dashboard_button_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_settings))
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
                        state.currentBoutInfo?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        state.nextBoutInfo?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Action buttons
                if (state.currentBoutInfo != null) {
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
                    fencerCount = state.fencerCount
                )

                // Rankings Table
                Text(
                    text = stringResource(R.string.rankings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("dashboard_text_rankingsTitle")
                )

                RankingsTable(rankings = state.rankings)
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
    }
}

@Composable
private fun OverflowMenu(component: GroupDashboardComponent, state: GroupDashboardState) {
    var expanded by remember { mutableStateOf(false) }
    var showExcludeDialog by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
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
                }
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
                        onClick = { onExclude(seed) },
                        modifier = Modifier.fillMaxWidth()
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
}

@Composable
fun MatrixTable(
    matrix: List<List<MatrixCell?>>,
    fencerNames: Map<Int, String>,
    fencerCount: Int
) {
    val cellSize = 60.dp
    val nameColumnWidth = 120.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        // First column - fencer names (sticky)
        Column {
            // Top-left corner cell
            Box(
                modifier = Modifier
                    .size(nameColumnWidth, cellSize)
                    .border(1.dp, Color.Gray)
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
                        .border(1.dp, Color.Gray)
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

        // Scrollable matrix cells
        Column {
            // Header row with numbers
            Row {
                for (col in 1..fencerCount) {
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .border(1.dp, Color.Gray)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
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
                        MatrixCellView(cell, cellSize)
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixCellView(cell: MatrixCell?, size: androidx.compose.ui.unit.Dp) {
    val backgroundColor = when {
        cell == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Diagonal
        cell.isVictory == true -> MaterialTheme.colorScheme.primaryContainer
        cell.isVictory == false -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .size(size)
            .border(1.dp, Color.Gray)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (cell != null && cell.leftScore != null && cell.rightScore != null) {
            val label = if (cell.isVictory == true) "V" else "D"
            Text(
                "$label${cell.leftScore}",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
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
                Text("Name", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
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
                    Text("${String.format("%.1f", ranking.vmPercent)}%", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
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
