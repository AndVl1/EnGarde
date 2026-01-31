package com.andvl1.engrade.ui.group.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.domain.model.FencerInput
import com.andvl1.engrade.domain.model.Weapon
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSetupScreen(component: GroupSetupComponent) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Group Setup") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Fencer Count")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (5..8).forEach { count ->
                    FilterChip(
                        selected = state.fencerCount == count,
                        onClick = { component.onEvent(GroupSetupEvent.SetFencerCount(count)) },
                        label = { Text("$count") }
                    )
                }
            }

            Text("Mode")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 5).forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { component.onEvent(GroupSetupEvent.SetMode(mode)) },
                        label = { Text("$mode") }
                    )
                }
            }

            Text("Weapon")
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

            Text("Fencers", style = MaterialTheme.typography.titleMedium)

            state.fencers.forEachIndexed { index, fencer ->
                OutlinedTextField(
                    value = fencer.name,
                    onValueChange = {
                        component.onEvent(
                            GroupSetupEvent.UpdateFencer(index, fencer.copy(name = it))
                        )
                    },
                    label = { Text("Fencer ${index + 1}") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                    Text("Create Pool")
                }
            }
        }
    }
}
