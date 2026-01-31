package com.andvl1.engrade.ui.bout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.domain.model.*
import com.andvl1.engrade.ui.theme.RedTimer
import com.andvl1.engrade.ui.theme.Yellow
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoutScreen(component: BoutComponent) {
    val state = component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.value.currentSection) {
                            SectionType.PERIOD -> "Период ${state.value.periodNumber}"
                            SectionType.BREAK -> "Перерыв ${state.value.periodNumber}"
                            SectionType.PRIORITY -> "Приоритет"
                        }
                    )
                },
                actions = {
                    // Undo button
                    if (state.value.canUndo) {
                        IconButton(onClick = { component.onEvent(BoutEvent.Undo) }) {
                            Icon(Icons.Default.Undo, "Undo")
                        }
                    }

                    // Skip section
                    IconButton(onClick = { component.onEvent(BoutEvent.SkipSection) }) {
                        Icon(Icons.Default.SkipNext, "Skip Section")
                    }

                    // Reset
                    IconButton(onClick = { component.onEvent(BoutEvent.Reset) }) {
                        Icon(Icons.Default.Refresh, "Reset")
                    }

                    // Settings
                    IconButton(onClick = { component.onEvent(BoutEvent.OpenSettings) }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (state.value.config.anywhereToStart) {
                        component.onEvent(BoutEvent.TimerClicked)
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Timer (large, centered)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { component.onEvent(BoutEvent.TimerClicked) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatTime(state.value.timeRemainingMs),
                        style = MaterialTheme.typography.displayLarge,
                        color = if (state.value.timeRemainingMs == 0L) RedTimer else Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scores row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left fencer
                    FencerScoreCard(
                        fencer = state.value.leftFencer,
                        side = FencerSide.LEFT,
                        modifier = Modifier.weight(1f),
                        onScoreClick = { component.onEvent(BoutEvent.LeftScored) },
                        onCardClick = { component.onEvent(BoutEvent.ShowCardDialog(FencerSide.LEFT)) }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right fencer
                    FencerScoreCard(
                        fencer = state.value.rightFencer,
                        side = FencerSide.RIGHT,
                        modifier = Modifier.weight(1f),
                        onScoreClick = { component.onEvent(BoutEvent.RightScored) },
                        onCardClick = { component.onEvent(BoutEvent.ShowCardDialog(FencerSide.RIGHT)) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Double touch button
                if (state.value.config.showDoubleTouchButton) {
                    Button(
                        onClick = { component.onEvent(BoutEvent.DoubleTouch) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .height(56.dp)
                    ) {
                        Text("Обоюдный укол", style = MaterialTheme.typography.titleLarge)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Card dialog
            state.value.showCardDialog?.let { dialogState ->
                CardDialog(
                    fencerSide = dialogState.fencerSide,
                    onCardSelected = { type ->
                        component.onEvent(BoutEvent.CardSelected(dialogState.fencerSide, type))
                    },
                    onDismiss = { component.onEvent(BoutEvent.DismissCardDialog) }
                )
            }
        }
    }
}

@Composable
fun FencerScoreCard(
    fencer: FencerState,
    side: FencerSide,
    modifier: Modifier = Modifier,
    onScoreClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Winner indicator
        if (fencer.isWinner) {
            Text(
                "ПОБЕДИТЕЛЬ",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Green
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Score (clickable)
        Surface(
            onClick = onScoreClick,
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = fencer.score.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Indicators row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card indicator
            if (fencer.hasRedCard || fencer.hasYellowCard) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (fencer.hasRedCard) Color.Red else Yellow,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable(onClick = onCardClick)
                )
            } else {
                // Invisible placeholder or card button
                IconButton(onClick = onCardClick) {
                    Icon(Icons.Default.Flag, "Give Card", tint = Color.Gray)
                }
            }

            // Priority indicator
            if (fencer.hasPriority) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color.Green,
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
    }
}

@Composable
fun CardDialog(
    fencerSide: FencerSide,
    onCardSelected: (CardType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (fencerSide) {
                    FencerSide.LEFT -> "Карточка левому"
                    FencerSide.RIGHT -> "Карточка правому"
                }
            )
        },
        text = {
            Column {
                Button(
                    onClick = { onCardSelected(CardType.YELLOW) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Yellow)
                ) {
                    Text("Жёлтая карточка", color = Color.Black)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onCardSelected(CardType.RED) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Красная карточка")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

fun formatTime(milliseconds: Long): String {
    val minutes = milliseconds / 60000
    val seconds = (milliseconds / 1000) % 60
    val millis = (milliseconds % 1000) / 10
    return "%d:%02d.%02d".format(minutes, seconds, millis)
}
