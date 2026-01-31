package com.andvl1.engrade.ui.group.boutslist

import com.andvl1.engrade.data.PoolBoutWithNames
import com.andvl1.engrade.data.PoolRepository
import com.andvl1.engrade.platform.componentScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch

interface BoutsListComponent {
    val state: Value<BoutsListState>
    fun onEvent(event: BoutsListEvent)
}

data class BoutsListState(
    val bouts: List<PoolBoutWithNames> = emptyList(),
    val isLoading: Boolean = true
)

sealed class BoutsListEvent {
    data class BoutClicked(val boutId: Long) : BoutsListEvent()
    data object NavigateBack : BoutsListEvent()
}

class DefaultBoutsListComponent(
    componentContext: ComponentContext,
    private val poolId: Long,
    private val poolRepository: PoolRepository,
    private val onBack: () -> Unit
) : BoutsListComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _state = MutableValue(BoutsListState())
    override val state: Value<BoutsListState> = _state

    init {
        loadBouts()
    }

    private fun loadBouts() {
        scope.launch {
            poolRepository.getPoolBoutsWithNames(poolId).collect { bouts ->
                _state.value = _state.value.copy(
                    bouts = bouts,
                    isLoading = false
                )
            }
        }
    }

    override fun onEvent(event: BoutsListEvent) {
        when (event) {
            is BoutsListEvent.BoutClicked -> {
                // TODO: implement edit score for completed bouts
            }
            BoutsListEvent.NavigateBack -> onBack()
        }
    }
}
