package com.andvl1.engrade.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.R
import com.andvl1.engrade.domain.model.Weapon
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state = component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = { component.onEvent(SettingsEvent.BackPressed) },
                        modifier = Modifier.testTag("settings_button_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
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
            Text(stringResource(R.string.weapon_label), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.value.weapon == Weapon.SABRE,
                    onClick = { component.onEvent(SettingsEvent.WeaponChanged(Weapon.SABRE)) },
                    label = { Text(stringResource(R.string.sabre)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_chip_sabre")
                )
                FilterChip(
                    selected = state.value.weapon == Weapon.FOIL_EPEE,
                    onClick = { component.onEvent(SettingsEvent.WeaponChanged(Weapon.FOIL_EPEE)) },
                    label = { Text(stringResource(R.string.epee_foil)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_chip_foilEpee")
                )
            }

            HorizontalDivider()

            // Mode selection
            Text(stringResource(R.string.settings_mode_label), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.value.mode == 5,
                    onClick = { component.onEvent(SettingsEvent.ModeChanged(5)) },
                    label = { Text(stringResource(R.string.settings_mode_to_5)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_chip_mode5")
                )
                FilterChip(
                    selected = state.value.mode == 15,
                    onClick = { component.onEvent(SettingsEvent.ModeChanged(15)) },
                    label = { Text(stringResource(R.string.settings_mode_to_15)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_chip_mode15")
                )
            }

            HorizontalDivider()

            // Show double touch button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.pref_show_double))
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
                Text(stringResource(R.string.pref_anywhere_to_start))
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
                Text(stringResource(R.string.pref_black))
                Switch(
                    checked = state.value.blackBackground,
                    onCheckedChange = { component.onEvent(SettingsEvent.BlackBackgroundChanged(it)) },
                    modifier = Modifier.testTag("settings_switch_blackBackground")
                )
            }
        }
    }
}
