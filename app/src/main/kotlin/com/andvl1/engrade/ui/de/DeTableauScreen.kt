package com.andvl1.engrade.ui.de

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.R
import com.andvl1.engrade.domain.model.DeClassification
import com.andvl1.engrade.domain.model.DeMatch
import com.andvl1.engrade.domain.model.DeSlot
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeTableauScreen(component: DeTableauComponent) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.de_screen_title),
                        modifier = Modifier.testTag("de_text_screenTitle")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { component.onEvent(DeTableauEvent.NavigateBack) },
                        modifier = Modifier.testTag("de_button_back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag("de_loading"))
                }
            }
            // Error state: bracket could not be loaded (null after loading completed, or error set).
            state.bracket == null || state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.de_load_error),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag("de_text_loadError")
                        )
                        Button(
                            onClick = { component.onEvent(DeTableauEvent.Retry) },
                            modifier = Modifier.testTag("de_button_retry")
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            else -> {
                val bracket = state.bracket!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val groupedByRound = bracket.matches.groupBy { it.round }
                    for (round in 1..bracket.totalRounds) {
                        val matches = groupedByRound[round] ?: emptyList()

                        Text(
                            text = roundLabel(round, bracket.totalRounds),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("de_text_roundTitle_$round")
                        )

                        matches.forEach { match ->
                            DeMatchCard(
                                match = match,
                                onPlay = if (isMatchReady(match)) {
                                    { component.onEvent(DeTableauEvent.PlayMatch(match.id)) }
                                } else null
                            )
                        }
                    }

                    // Final classification (shown when bracket is complete)
                    if (bracket.isComplete && state.classification.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = stringResource(R.string.de_classification_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("de_text_classificationTitle")
                        )

                        state.classification.forEach { entry ->
                            DeClassificationRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeMatchCard(
    match: DeMatch,
    onPlay: (() -> Unit)?
) {
    val topFencer = match.topSlot as? DeSlot.Fencer
    val bottomFencer = match.bottomSlot as? DeSlot.Fencer
    val topIsWinner = topFencer != null && topFencer.seed == match.winner?.seed
    val bottomIsWinner = bottomFencer != null && bottomFencer.seed == match.winner?.seed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("de_match_${match.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slots column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top slot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = slotDisplayText(match.topSlot),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (topIsWinner) FontWeight.Bold else FontWeight.Normal,
                        color = if (topIsWinner) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("de_slot_top_${match.id}")
                    )
                    if (match.winner != null && match.topScore != null) {
                        Text(
                            text = "${match.topScore}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (topIsWinner) FontWeight.Bold else FontWeight.Normal,
                            color = if (topIsWinner) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("de_score_top_${match.id}")
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp)

                // Bottom slot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = slotDisplayText(match.bottomSlot),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (bottomIsWinner) FontWeight.Bold else FontWeight.Normal,
                        color = if (bottomIsWinner) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("de_slot_bottom_${match.id}")
                    )
                    if (match.winner != null && match.bottomScore != null) {
                        Text(
                            text = "${match.bottomScore}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (bottomIsWinner) FontWeight.Bold else FontWeight.Normal,
                            color = if (bottomIsWinner) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("de_score_bottom_${match.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play button (only for ready matches)
            if (onPlay != null) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.testTag("de_button_playMatch_${match.id}")
                ) {
                    Text(stringResource(R.string.de_play_match))
                }
            }
        }
    }
}

@Composable
private fun DeClassificationRow(entry: DeClassification) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${entry.place}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = 32.dp)
        )
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .testTag("de_text_classification_${entry.place}")
        )
        Text(
            text = "(${stringResource(R.string.de_seed_label, entry.seed)})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun roundLabel(round: Int, totalRounds: Int): String = when (totalRounds - round) {
    0 -> stringResource(R.string.de_final)
    1 -> stringResource(R.string.de_semifinal)
    2 -> stringResource(R.string.de_quarterfinal)
    else -> stringResource(R.string.de_round_n, round)
}

@Composable
private fun slotDisplayText(slot: DeSlot): String = when (slot) {
    is DeSlot.Fencer -> "${slot.name} (${slot.seed})"
    DeSlot.Bye -> stringResource(R.string.de_slot_bye)
    DeSlot.Tbd -> stringResource(R.string.de_slot_tbd)
}

/** A match is "ready" when both slots are real fencers and the match has not been played yet. */
private fun isMatchReady(match: DeMatch): Boolean =
    match.topSlot is DeSlot.Fencer &&
        match.bottomSlot is DeSlot.Fencer &&
        match.winner == null &&
        !match.isBye
