package com.andvl1.engrade.ui.group.boutslist

import com.andvl1.engrade.data.PoolBoutWithNames
import com.andvl1.engrade.data.PoolRepository
import com.andvl1.engrade.platform.componentScope
import com.andvl1.engrade.ui.group.dashboard.EditScoreDialogState
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
    val isLoading: Boolean = true,
    val showEditScoreDialog: EditScoreDialogState? = null
)

sealed class BoutsListEvent {
    data class BoutClicked(val boutId: Long) : BoutsListEvent()
    data object DismissEditScoreDialog : BoutsListEvent()
    data class UpdateBoutScore(val boutId: Long, val leftScore: Int, val rightScore: Int) : BoutsListEvent()
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
                val bout = _state.value.bouts.find { it.bout.id == event.boutId }
                bout?.let {
                    _state.value = _state.value.copy(
                        showEditScoreDialog = EditScoreDialogState(
                            boutId = it.bout.id,
                            leftName = it.leftFencerName,
                            rightName = it.rightFencerName,
                            leftScore = it.bout.leftScore ?: 0,
                            rightScore = it.bout.rightScore ?: 0
                        )
                    )
                }
            }
            BoutsListEvent.DismissEditScoreDialog -> {
                _state.value = _state.value.copy(showEditScoreDialog = null)
            }
            is BoutsListEvent.UpdateBoutScore -> {
                scope.launch {
                    poolRepository.updateBoutScore(
                        boutId = event.boutId,
                        leftScore = event.leftScore,
                        rightScore = event.rightScore
                    )
                    _state.value = _state.value.copy(showEditScoreDialog = null)
                }
            }
            BoutsListEvent.NavigateBack -> onBack()
        }
    }
}
