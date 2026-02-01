package com.andvl1.engrade.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.domain.model.Weapon
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state = component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(
                        onClick = { component.onEvent(SettingsEvent.BackPressed) },
                        modifier = Modifier.testTag("settings_button_back")
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weapon selection
            Text("Оружие", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.value.weapon == Weapon.SABRE,
                    onClick = { component.onEvent(SettingsEvent.WeaponChanged(Weapon.SABRE)) },
                    label = { Text("Сабля") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_chip_sabre")
                )
                FilterChip(
                    selected = state.value.weapon == Weapon.FOIL_EPEE,
                    onClick = { component.onEvent(SettingsEvent.WeaponChanged(Weapon.FOIL_EPEE)) },
                    label = { Text("Рапира/Шпага") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_chip_foilEpee")
                )
            }

            Divider()

            // Mode selection
            Text("Режим (до скольки уколов)", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.value.mode == 5,
                    onClick = { component.onEvent(SettingsEvent.ModeChanged(5)) },
                    label = { Text("До 5") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_chip_mode5")
                )
                FilterChip(
                    selected = state.value.mode == 15,
                    onClick = { component.onEvent(SettingsEvent.ModeChanged(15)) },
                    label = { Text("До 15") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_chip_mode15")
                )
            }

            Divider()

            // Show double touch button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Показывать кнопку обоюдного укола")
                Switch(
                    checked = state.value.showDouble,
                    onCheckedChange = { component.onEvent(SettingsEvent.ShowDoubleChanged(it)) },
                    modifier = Modifier.testTag("settings_switch_showDouble")
                )
            }

            // Anywhere to start
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Запуск таймера касанием по экрану")
                Switch(
                    checked = state.value.anywhereToStart,
                    onCheckedChange = { component.onEvent(SettingsEvent.AnywhereToStartChanged(it)) },
                    modifier = Modifier.testTag("settings_switch_anywhereToStart")
                )
            }

            // Black background (AMOLED)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Чёрный фон (AMOLED)")
                Switch(
                    checked = state.value.blackBackground,
                    onCheckedChange = { component.onEvent(SettingsEvent.BlackBackgroundChanged(it)) },
                    modifier = Modifier.testTag("settings_switch_blackBackground")
                )
            }
        }
    }
}
