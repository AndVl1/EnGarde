package com.andvl1.engrade.ui.group.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.R
import com.andvl1.engrade.domain.model.FencerInput
import com.andvl1.engrade.domain.model.Weapon
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSetupScreen(component: GroupSetupComponent) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(stringResource(R.string.group_setup)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.fencer_count))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (5..8).forEach { count ->
                    FilterChip(
                        selected = state.fencerCount == count,
                        onClick = { component.onEvent(GroupSetupEvent.SetFencerCount(count)) },
                        label = { Text("$count") }
                    )
                }
            }

            Text(stringResource(R.string.bout_mode))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 5).forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { component.onEvent(GroupSetupEvent.SetMode(mode)) },
                        label = { Text("$mode") }
                    )
                }
            }

            Text(stringResource(R.string.weapon_label))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Weapon.entries.forEach { weapon ->
                    FilterChip(
                        selected = state.weapon == weapon,
                        onClick = { component.onEvent(GroupSetupEvent.SetWeapon(weapon)) },
                        label = { Text(weapon.name) }
                    )
                }
            }

            HorizontalDivider()

            Text(stringResource(R.string.participants), style = MaterialTheme.typography.titleMedium)

            state.fencers.forEachIndexed { index, fencer ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fencer.name,
                            onValueChange = {
                                component.onEvent(
                                    GroupSetupEvent.UpdateFencer(index, fencer.copy(name = it))
                                )
                            },
                            label = { Text(stringResource(R.string.fencer_name, index + 1)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = fencer.organization ?: "",
                            onValueChange = {
                                component.onEvent(
                                    GroupSetupEvent.UpdateFencer(index, fencer.copy(organization = it.ifBlank { null }))
                                )
                            },
                            label = { Text(stringResource(R.string.organization)) },
                            placeholder = { Text(stringResource(R.string.organization)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = fencer.region ?: "",
                            onValueChange = {
                                component.onEvent(
                                    GroupSetupEvent.UpdateFencer(index, fencer.copy(region = it.ifBlank { null }))
                                )
                            },
                            label = { Text(stringResource(R.string.region)) },
                            placeholder = { Text(stringResource(R.string.region)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { component.onEvent(GroupSetupEvent.CreatePool) },
                enabled = !state.isCreating && state.fencers.all { it.name.isNotBlank() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.create_group))
                }
            }
        }
    }
}
