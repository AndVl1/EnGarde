package com.andvl1.engrade.ui.home

import com.andvl1.engrade.data.PoolRepository
import com.andvl1.engrade.data.db.entity.PoolEntity
import com.andvl1.engrade.platform.componentScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch

interface HomeComponent {
    val state: Value<HomeState>
    fun onEvent(event: HomeEvent)
}

data class HomeState(
    val activePool: PoolEntity? = null
)

sealed class HomeEvent {
    data object NavigateToSingleBout : HomeEvent()
    data object NavigateToGroupStage : HomeEvent()
    data object NavigateToContinuePool : HomeEvent()
    data object NavigateToSettings : HomeEvent()
}

class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val poolRepository: PoolRepository,
    private val onNavigateToSingleBout: () -> Unit,
    private val onNavigateToGroupStage: () -> Unit,
    private val onNavigateToContinuePool: (Long) -> Unit,
    private val onNavigateToSettings: () -> Unit
) : HomeComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _state = MutableValue(HomeState())
    override val state: Value<HomeState> = _state

    init {
        scope.launch {
            poolRepository.getActivePool().collect { pool ->
                _state.value = _state.value.copy(activePool = pool)
            }
        }
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.NavigateToSingleBout -> onNavigateToSingleBout()
            HomeEvent.NavigateToGroupStage -> onNavigateToGroupStage()
            HomeEvent.NavigateToContinuePool -> {
                _state.value.activePool?.let { onNavigateToContinuePool(it.id) }
            }
            HomeEvent.NavigateToSettings -> onNavigateToSettings()
        }
    }
}
