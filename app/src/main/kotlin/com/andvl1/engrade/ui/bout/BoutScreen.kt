package com.andvl1.engrade.ui.bout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.R
import com.andvl1.engrade.domain.model.*
import com.andvl1.engrade.ui.theme.BlackCardSurface
import com.andvl1.engrade.ui.theme.RedTimer
import com.andvl1.engrade.ui.theme.Yellow
import com.arkivanov.decompose.extensions.compose.subscribeAsState

// NOTE: BoutScreen intentionally has NO back navigation button.
// Accidental back gestures mid-match would abort the bout and lose all scoring data.
// The timer TopAppBar is the only top-level chrome; back is blocked by Decompose handleBackButton.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoutScreen(component: BoutComponent) {
    val state = component.state.subscribeAsState()
    val leftDisplayName = state.value.leftFencerName.ifBlank { stringResource(R.string.fencer_left_default) }
    val rightDisplayName = state.value.rightFencerName.ifBlank { stringResource(R.string.fencer_right_default) }

    // Local UI state for confirmation dialogs — pure view state, no business logic.
    var showResetConfirm by remember { mutableStateOf(false) }
    var showSkipConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (state.value.currentSection) {
                            SectionType.PERIOD -> stringResource(R.string.period_n, state.value.periodNumber)
                            SectionType.BREAK -> stringResource(R.string.break_n, state.value.periodNumber)
                            SectionType.PRIORITY -> stringResource(R.string.priority)
                        },
                        modifier = Modifier.testTag("bout_text_sectionTitle")
                    )
                },
                actions = {
                    // Undo button — always rendered; disabled and dimmed when no action to undo.
                    IconButton(
                        onClick = { component.onEvent(BoutEvent.Undo) },
                        enabled = state.value.canUndo,
                        modifier = Modifier
                            .testTag("bout_button_undo")
                            .alpha(if (state.value.canUndo) 1f else 0.3f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.action_undo))
                    }

                    // Skip section — gated by confirm dialog.
                    IconButton(
                        onClick = { showSkipConfirm = true },
                        modifier = Modifier.testTag("bout_button_skipSection")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.action_skip_section))
                    }

                    // Reset — gated by confirm dialog.
                    IconButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.testTag("bout_button_reset")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_reset))
                    }

                    // Settings
                    IconButton(
                        onClick = { component.onEvent(BoutEvent.OpenSettings) },
                        modifier = Modifier.testTag("bout_button_settings")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
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
                        .clickable { component.onEvent(BoutEvent.TimerClicked) }
                        .testTag("bout_box_timer"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatTime(state.value.timeRemainingMs),
                        style = MaterialTheme.typography.displayLarge,
                        color = if (state.value.timeRemainingMs == 0L) RedTimer
                        else MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("bout_text_timer")
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
                        fencerName = leftDisplayName,
                        side = FencerSide.LEFT,
                        modifier = Modifier.weight(1f),
                        onScoreClick = { component.onEvent(BoutEvent.LeftScored) },
                        onCardClick = { component.onEvent(BoutEvent.ShowCardDialog(FencerSide.LEFT)) }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right fencer
                    FencerScoreCard(
                        fencer = state.value.rightFencer,
                        fencerName = rightDisplayName,
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
                            .testTag("bout_button_doubleTouch")
                    ) {
                        Text(stringResource(R.string.double_touch), style = MaterialTheme.typography.titleLarge)
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

            // Reset confirmation dialog
            if (showResetConfirm) {
                BoutConfirmDialog(
                    title = stringResource(R.string.confirm_reset_title),
                    message = stringResource(R.string.confirm_reset_message),
                    dialogTag = "bout_dialog_resetConfirm",
                    confirmTag = "bout_button_resetConfirm",
                    cancelTag = "bout_button_resetCancel",
                    onConfirm = {
                        showResetConfirm = false
                        component.onEvent(BoutEvent.Reset)
                    },
                    onDismiss = { showResetConfirm = false }
                )
            }

            // Skip section confirmation dialog
            if (showSkipConfirm) {
                BoutConfirmDialog(
                    title = stringResource(R.string.confirm_skip_title),
                    message = stringResource(R.string.confirm_skip_message),
                    dialogTag = "bout_dialog_skipConfirm",
                    confirmTag = "bout_button_skipConfirm",
                    cancelTag = "bout_button_skipCancel",
                    onConfirm = {
                        showSkipConfirm = false
                        component.onEvent(BoutEvent.SkipSection)
                    },
                    onDismiss = { showSkipConfirm = false }
                )
            }
        }
    }
}

@Composable
fun BoutConfirmDialog(
    title: String,
    message: String,
    dialogTag: String,
    confirmTag: String,
    cancelTag: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(dialogTag),
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(confirmTag)
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(cancelTag)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun FencerScoreCard(
    fencer: FencerState,
    fencerName: String,
    side: FencerSide,
    modifier: Modifier = Modifier,
    onScoreClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val sideTag = side.name.lowercase()
    val cdGiveCard = stringResource(R.string.cd_give_card)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fencer name
        Text(
            fencerName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.testTag("bout_text_${sideTag}Name")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Winner indicator
        if (fencer.isWinner) {
            Text(
                stringResource(R.string.winner),
                style = MaterialTheme.typography.titleSmall,
                color = Color.Green,
                modifier = Modifier.testTag("bout_text_${sideTag}Winner")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Score (clickable)
        Surface(
            onClick = onScoreClick,
            modifier = Modifier
                .size(120.dp)
                .testTag("bout_button_${sideTag}Score"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = fencer.score.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("bout_text_${sideTag}Score")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Indicators row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card indicator — priority: black > red > yellow
            if (fencer.hasBlackCard || fencer.hasRedCard || fencer.hasYellowCard) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .background(
                            color = when {
                                fencer.hasBlackCard -> BlackCardSurface
                                fencer.hasRedCard -> Color.Red
                                else -> Yellow
                            },
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable(onClick = onCardClick)
                        .testTag("bout_button_${sideTag}Card")
                        .semantics { contentDescription = cdGiveCard }
                        .then(
                            when {
                                fencer.hasBlackCard -> Modifier.testTag("bout_indicator_${sideTag}BlackCard")
                                fencer.hasRedCard -> Modifier.testTag("bout_indicator_${sideTag}RedCard")
                                else -> Modifier.testTag("bout_indicator_${sideTag}YellowCard")
                            }
                        )
                )
            } else {
                // Give card button when no card assigned yet
                IconButton(
                    onClick = onCardClick,
                    modifier = Modifier
                        .testTag("bout_button_${sideTag}Card")
                        .semantics { contentDescription = cdGiveCard }
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        .testTag("bout_indicator_${sideTag}Priority")
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
        modifier = Modifier.testTag("bout_dialog_card"),
        title = {
            Text(
                when (fencerSide) {
                    FencerSide.LEFT -> stringResource(R.string.card_left)
                    FencerSide.RIGHT -> stringResource(R.string.card_right)
                }
            )
        },
        text = {
            val sideLabel = if (fencerSide == FencerSide.LEFT) "Left" else "Right"
            Column {
                Button(
                    onClick = { onCardSelected(CardType.YELLOW) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bout_button_yellowCard"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Yellow,
                        contentColor = Color.Black
                    )
                ) {
                    Text(stringResource(R.string.yellow_card))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onCardSelected(CardType.RED) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bout_button_redCard"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.red_card))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onCardSelected(CardType.BLACK) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bout_button_blackCard$sideLabel"),
                    colors = ButtonDefaults.buttonColors(containerColor = BlackCardSurface)
                ) {
                    Text(stringResource(R.string.black_card))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("bout_button_cancelCard")
            ) {
                Text(stringResource(R.string.cancel))
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
