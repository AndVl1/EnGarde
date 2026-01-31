package com.andvl1.engrade.ui.group.boutconfirm

import com.andvl1.engrade.data.PoolRepository
import com.andvl1.engrade.platform.componentScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch

interface BoutConfirmComponent {
    val state: Value<BoutConfirmState>
    fun onEvent(event: BoutConfirmEvent)
}

data class BoutConfirmState(
    val boutNumber: Int = 0,
    val leftName: String = "",
    val rightName: String = "",
    val mode: Int = 5,
    val weapon: String = "SABRE",
    val isLoading: Boolean = true
)

sealed class BoutConfirmEvent {
    data object StartBout : BoutConfirmEvent()
    data object Cancel : BoutConfirmEvent()
}

class DefaultBoutConfirmComponent(
    componentContext: ComponentContext,
    private val poolId: Long,
    private val boutId: Long,
    private val poolRepository: PoolRepository,
    private val onStartBout: (Long, Long, String, String, Int, String) -> Unit,
    private val onBack: () -> Unit
) : BoutConfirmComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _state = MutableValue(BoutConfirmState())
    override val state: Value<BoutConfirmState> = _state

    init {
        loadBoutInfo()
    }

    private fun loadBoutInfo() {
        scope.launch {
            // Get pool info for mode and weapon
            poolRepository.getPoolById(poolId).collect { pool ->
                pool?.let {
                    _state.value = _state.value.copy(
                        mode = it.mode,
                        weapon = it.weapon
                    )
                }
            }
        }

        scope.launch {
            // Get bout info
            poolRepository.getPoolBoutsWithNames(poolId).collect { bouts ->
                val boutWithNames = bouts.find { it.bout.id == boutId }
                boutWithNames?.let {
                    _state.value = _state.value.copy(
                        boutNumber = it.bout.boutOrder,
                        leftName = it.leftFencerName,
                        rightName = it.rightFencerName,
                        isLoading = false
                    )
                }
            }
        }
    }

    override fun onEvent(event: BoutConfirmEvent) {
        when (event) {
            BoutConfirmEvent.StartBout -> {
                onStartBout(
                    poolId,
                    boutId,
                    _state.value.leftName,
                    _state.value.rightName,
                    _state.value.mode,
                    _state.value.weapon
                )
            }
            BoutConfirmEvent.Cancel -> onBack()
        }
    }
}
