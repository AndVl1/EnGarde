package com.andvl1.engrade.ui.group.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.R
import com.andvl1.engrade.domain.model.FencerInput
import com.andvl1.engrade.domain.model.Weapon
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@Composable
private fun weaponLabel(weapon: Weapon): String = when (weapon) {
    Weapon.SABRE -> stringResource(R.string.sabre)
    Weapon.FOIL_EPEE -> stringResource(R.string.epee_foil)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSetupScreen(component: GroupSetupComponent) {
    val state by component.state.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Localized error strings resolved in composable scope
    val blankNamesMsg = stringResource(R.string.name_required)
    val dupNamesMsg = stringResource(R.string.duplicate_names)
    val createFailedMsg = stringResource(R.string.error_create_pool_failed)

    val error = state.error
    LaunchedEffect(error) {
        if (error != null) {
            val msg = when (error) {
                GroupSetupError.BlankNames -> blankNamesMsg
                GroupSetupError.DuplicateNames -> dupNamesMsg
                GroupSetupError.CreateFailed -> createFailedMsg
            }
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Long)
            component.onEvent(GroupSetupEvent.DismissError)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, modifier = Modifier.testTag("groupSetup_snackbar"))
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.group_setup)) },
                navigationIcon = {
                    IconButton(
                        onClick = { component.onEvent(GroupSetupEvent.NavigateBack) },
                        modifier = Modifier.testTag("groupSetup_button_back")
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
                        label = { Text("$count") },
                        modifier = Modifier.testTag("groupSetup_chip_count_$count")
                    )
                }
            }

            Text(stringResource(R.string.bout_mode))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 5).forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { component.onEvent(GroupSetupEvent.SetMode(mode)) },
                        label = {
                            Text(
                                if (mode == 4) stringResource(R.string.touches_4)
                                else stringResource(R.string.touches_5)
                            )
                        },
                        modifier = Modifier.testTag("groupSetup_chip_mode_$mode")
                    )
                }
            }

            Text(stringResource(R.string.weapon_label))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Weapon.entries.forEach { weapon ->
                    FilterChip(
                        selected = state.weapon == weapon,
                        onClick = { component.onEvent(GroupSetupEvent.SetWeapon(weapon)) },
                        label = { Text(weaponLabel(weapon)) },
                        modifier = Modifier.testTag("groupSetup_chip_weapon_${weapon.name}")
                    )
                }
            }

            HorizontalDivider()

            Text(stringResource(R.string.participants), style = MaterialTheme.typography.titleMedium)

            state.fencers.forEachIndexed { index, fencer ->
                val isLastFencer = index == state.fencers.size - 1
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Name field with autocomplete
                        Box {
                            OutlinedTextField(
                                value = fencer.name,
                                onValueChange = { newName ->
                                    component.onEvent(
                                        GroupSetupEvent.UpdateFencer(index, fencer.copy(name = newName))
                                    )
                                    component.onEvent(
                                        GroupSetupEvent.SearchFencerName(index, newName)
                                    )
                                },
                                label = { Text(stringResource(R.string.fencer_name, index + 1)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("groupSetup_input_name_$index")
                            )

                            // Suggestions dropdown
                            DropdownMenu(
                                expanded = state.activeSuggestionIndex == index && state.suggestions.isNotEmpty(),
                                onDismissRequest = { component.onEvent(GroupSetupEvent.DismissSuggestions) },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                state.suggestions.forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(suggestion.name, style = MaterialTheme.typography.bodyMedium)
                                                suggestion.organization?.let {
                                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        },
                                        onClick = {
                                            component.onEvent(
                                                GroupSetupEvent.SelectSuggestion(index, suggestion)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = fencer.organization ?: "",
                            onValueChange = {
                                component.onEvent(
                                    GroupSetupEvent.UpdateFencer(index, fencer.copy(organization = it.ifBlank { null }))
                                )
                            },
                            label = { Text(stringResource(R.string.organization)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("groupSetup_input_org_$index")
                        )

                        OutlinedTextField(
                            value = fencer.region ?: "",
                            onValueChange = {
                                component.onEvent(
                                    GroupSetupEvent.UpdateFencer(index, fencer.copy(region = it.ifBlank { null }))
                                )
                            },
                            label = { Text(stringResource(R.string.region)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (isLastFencer) ImeAction.Done else ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("groupSetup_input_region_$index")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { component.onEvent(GroupSetupEvent.CreatePool) },
                enabled = !state.isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("groupSetup_button_create")
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
