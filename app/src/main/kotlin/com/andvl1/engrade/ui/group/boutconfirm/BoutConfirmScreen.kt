package com.andvl1.engrade.ui.group.boutconfirm

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.R
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@Composable
fun BoutConfirmScreen(component: BoutConfirmComponent) {
    val state by component.state.subscribeAsState()

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.testTag("boutConfirm_loading"))
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.bout_number, state.boutNumber),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("boutConfirm_text_boutNumber")
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.left_lane),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.leftName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("boutConfirm_text_leftName")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Swap sides button
                    FilledTonalIconButton(
                        onClick = { component.onEvent(BoutConfirmEvent.SwapSides) },
                        modifier = Modifier.testTag("boutConfirm_button_swap")
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = stringResource(R.string.swap_sides)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.right_lane),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.rightName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("boutConfirm_text_rightName")
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { component.onEvent(BoutConfirmEvent.StartBout) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("boutConfirm_button_start")
            ) {
                Text(stringResource(R.string.start_bout), style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { component.onEvent(BoutConfirmEvent.Cancel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("boutConfirm_button_cancel")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}
