package com.andvl1.engrade.ui.group.setup

import com.andvl1.engrade.data.PoolRepository
import com.andvl1.engrade.domain.model.FencerInput
import com.andvl1.engrade.domain.model.Weapon
import com.andvl1.engrade.platform.componentScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch

interface GroupSetupComponent {
    val state: Value<GroupSetupState>
    fun onEvent(event: GroupSetupEvent)
}

data class GroupSetupState(
    val fencerCount: Int = 5,
    val mode: Int = 5,
    val weapon: Weapon = Weapon.SABRE,
    val fencers: List<FencerInput> = List(5) { FencerInput("") },
    val isCreating: Boolean = false
)

sealed class GroupSetupEvent {
    data class SetFencerCount(val count: Int) : GroupSetupEvent()
    data class SetMode(val mode: Int) : GroupSetupEvent()
    data class SetWeapon(val weapon: Weapon) : GroupSetupEvent()
    data class UpdateFencer(val index: Int, val input: FencerInput) : GroupSetupEvent()
    data object CreatePool : GroupSetupEvent()
    data object NavigateBack : GroupSetupEvent()
}

class DefaultGroupSetupComponent(
    componentContext: ComponentContext,
    private val poolRepository: PoolRepository,
    private val onPoolCreated: (Long) -> Unit,
    private val onBack: () -> Unit
) : GroupSetupComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _state = MutableValue(GroupSetupState())
    override val state: Value<GroupSetupState> = _state

    override fun onEvent(event: GroupSetupEvent) {
        when (event) {
            is GroupSetupEvent.SetFencerCount -> {
                _state.value = _state.value.copy(
                    fencerCount = event.count,
                    fencers = List(event.count) { index ->
                        _state.value.fencers.getOrElse(index) { FencerInput("") }
                    }
                )
            }
            is GroupSetupEvent.SetMode -> {
                _state.value = _state.value.copy(mode = event.mode)
            }
            is GroupSetupEvent.SetWeapon -> {
                _state.value = _state.value.copy(weapon = event.weapon)
            }
            is GroupSetupEvent.UpdateFencer -> {
                val updatedFencers = _state.value.fencers.toMutableList()
                updatedFencers[event.index] = event.input
                _state.value = _state.value.copy(fencers = updatedFencers)
            }
            GroupSetupEvent.CreatePool -> {
                scope.launch {
                    _state.value = _state.value.copy(isCreating = true)
                    try {
                        val poolId = poolRepository.createPool(
                            mode = _state.value.mode,
                            weapon = _state.value.weapon,
                            fencers = _state.value.fencers
                        )
                        onPoolCreated(poolId)
                    } finally {
                        _state.value = _state.value.copy(isCreating = false)
                    }
                }
            }
            GroupSetupEvent.NavigateBack -> onBack()
        }
    }
}
