package com.andvl1.engrade.ui.settings

import com.andvl1.engrade.data.SettingsRepository
import com.andvl1.engrade.domain.model.Weapon
import com.andvl1.engrade.platform.componentScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch

interface SettingsComponent {
    val state: Value<SettingsState>
    fun onEvent(event: SettingsEvent)
}

data class SettingsState(
    val weapon: Weapon = Weapon.SABRE,
    val mode: Int = 5,
    val showDouble: Boolean = true,
    val anywhereToStart: Boolean = true,
    val blackBackground: Boolean = false
)

sealed class SettingsEvent {
    data class WeaponChanged(val weapon: Weapon) : SettingsEvent()
    data class ModeChanged(val mode: Int) : SettingsEvent()
    data class ShowDoubleChanged(val show: Boolean) : SettingsEvent()
    data class AnywhereToStartChanged(val anywhere: Boolean) : SettingsEvent()
    data class BlackBackgroundChanged(val black: Boolean) : SettingsEvent()
    data object BackPressed : SettingsEvent()
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val settingsRepository: SettingsRepository,
    private val onBack: () -> Unit
) : SettingsComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private val _state = MutableValue(SettingsState())
    override val state: Value<SettingsState> = _state

    init {
        scope.launch {
            settingsRepository.boutConfigFlow.collect { config ->
                _state.value = SettingsState(
                    weapon = config.weapon,
                    mode = config.mode,
                    showDouble = config.showDoubleTouchButton,
                    anywhereToStart = config.anywhereToStart,
                    blackBackground = _state.value.blackBackground
                )
            }
        }

        scope.launch {
            settingsRepository.useBlackBackground.collect { black ->
                _state.value = _state.value.copy(blackBackground = black)
            }
        }
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.WeaponChanged -> {
                scope.launch {
                    settingsRepository.updateWeapon(event.weapon)
                }
            }
            is SettingsEvent.ModeChanged -> {
                scope.launch {
                    settingsRepository.updateMode(event.mode)
                }
            }
            is SettingsEvent.ShowDoubleChanged -> {
                scope.launch {
                    settingsRepository.updateShowDouble(event.show)
                }
            }
            is SettingsEvent.AnywhereToStartChanged -> {
                scope.launch {
                    settingsRepository.updateAnywhereToStart(event.anywhere)
                }
            }
            is SettingsEvent.BlackBackgroundChanged -> {
                scope.launch {
                    settingsRepository.updateBlackBackground(event.black)
                }
            }
            SettingsEvent.BackPressed -> onBack()
        }
    }
}
