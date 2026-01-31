package com.andvl1.engrade.ui.group.boutresult

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.R
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@Composable
fun BoutResultScreen(component: BoutResultComponent) {
    val state by component.state.subscribeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.bout_result_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left fencer
                Column(
                    horizontalAlignment = if (state.winner == "LEFT") Alignment.CenterHorizontally else Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        state.leftName,
                        style = if (state.winner == "LEFT") {
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = if (state.winner == "LEFT") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (state.winner == "LEFT") "V${state.leftScore}" else "D${state.leftScore}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (state.winner == "LEFT") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                Text(
                    "—",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Right fencer
                Column(
                    horizontalAlignment = if (state.winner == "RIGHT") Alignment.CenterHorizontally else Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        state.rightName,
                        style = if (state.winner == "RIGHT") {
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = if (state.winner == "RIGHT") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (state.winner == "RIGHT") "V${state.rightScore}" else "D${state.rightScore}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (state.winner == "RIGHT") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { component.onEvent(BoutResultEvent.Continue) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.continue_text), style = MaterialTheme.typography.titleMedium)
        }
    }
}
