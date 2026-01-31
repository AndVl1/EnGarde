package com.andvl1.engrade.ui.group.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.domain.model.MatrixCell
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDashboardScreen(component: GroupDashboardComponent) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Stage") },
                navigationIcon = {
                    IconButton(onClick = { component.onEvent(GroupDashboardEvent.NavigateBack) }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { component.onEvent(GroupDashboardEvent.NavigateToBoutsList) }) {
                        Icon(Icons.Default.List, "Bouts List")
                    }
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
                CircularProgressIndicator()
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
                            "Progress: ${state.completedBoutsCount} / ${state.totalBoutsCount} bouts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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

                // Start next bout button
                if (state.currentBoutInfo != null) {
                    Button(
                        onClick = { component.onEvent(GroupDashboardEvent.StartNextBout) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Start Next Bout", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // FIE Result Matrix
                Text("Result Matrix", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                MatrixTable(
                    matrix = state.matrix,
                    fencerNames = state.fencerNames,
                    fencerCount = state.fencerCount
                )

                // Rankings Table
                Text("Rankings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                RankingsTable(rankings = state.rankings)
            }
        }
    }
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
