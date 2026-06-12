package com.andvl1.engrade.ui.de

import com.andvl1.engrade.data.DeRepository
import com.andvl1.engrade.domain.model.DeBracket
import com.andvl1.engrade.domain.model.DeClassification
import com.andvl1.engrade.domain.model.DeSlot
import com.andvl1.engrade.platform.componentScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

interface DeTableauComponent {
    val state: Value<DeTableauState>
    fun onEvent(event: DeTableauEvent)
}

data class DeTableauState(
    val poolId: Long = 0,
    val tableauId: Long = 0,
    val bracket: DeBracket? = null,
    val classification: List<DeClassification> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val weapon: String = "SABRE"
)

sealed class DeTableauEvent {
    data class PlayMatch(val matchId: Int) : DeTableauEvent()
    data object NavigateBack : DeTableauEvent()
}

/**
 * Component for the Direct Elimination bracket screen.
 *
 * Observes the [DeRepository] reactively. On [DeTableauEvent.PlayMatch] it invokes
 * [onNavigateToDeBout] with all data the bout screen needs. On match completion the
 * result is recorded by the caller ([DefaultRootComponent]) before navigating back,
 * and the bracket re-emits automatically through Room's Flow.
 *
 * @param weapon The FIE weapon code ("SABRE" / "FOIL_EPEE") inherited from the pool,
 *               carried through the navigation config so the component does not need
 *               an extra DB read.
 */
class DefaultDeTableauComponent(
    componentContext: ComponentContext,
    private val poolId: Long,
    private val weapon: String,
    private val deRepository: DeRepository,
    private val onNavigateToDeBout: (
        tableauId: Long,
        matchId: Int,
        topName: String,
        bottomName: String,
        topSeed: Int,
        bottomSeed: Int,
        weapon: String
    ) -> Unit,
    private val onBack: () -> Unit
) : DeTableauComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _state = MutableValue(DeTableauState(poolId = poolId, weapon = weapon))
    override val state: Value<DeTableauState> = _state

    init {
        observeData()
    }

    private fun observeData() {
        scope.launch {
            combine(
                deRepository.observeTableauId(poolId),
                deRepository.observeBracket(poolId),
                deRepository.observeFinalClassification(poolId)
            ) { tableauId, bracket, classification ->
                Triple(tableauId, bracket, classification)
            }.collect { (tableauId, bracket, classification) ->
                _state.value = _state.value.copy(
                    tableauId = tableauId ?: 0L,
                    bracket = bracket,
                    classification = classification,
                    isLoading = false
                )
            }
        }
    }

    override fun onEvent(event: DeTableauEvent) {
        when (event) {
            is DeTableauEvent.PlayMatch -> {
                val currentState = _state.value
                val bracket = currentState.bracket ?: return
                val match = bracket.matches.firstOrNull { it.id == event.matchId } ?: return
                val topFencer = match.topSlot as? DeSlot.Fencer ?: return
                val bottomFencer = match.bottomSlot as? DeSlot.Fencer ?: return

                onNavigateToDeBout(
                    currentState.tableauId,
                    event.matchId,
                    topFencer.name,
                    bottomFencer.name,
                    topFencer.seed,
                    bottomFencer.seed,
                    currentState.weapon
                )
            }
            DeTableauEvent.NavigateBack -> onBack()
        }
    }
}
