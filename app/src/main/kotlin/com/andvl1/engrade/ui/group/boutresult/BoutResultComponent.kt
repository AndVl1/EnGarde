package com.andvl1.engrade.ui.group.boutresult

import com.andvl1.engrade.data.PoolRepository
import com.andvl1.engrade.platform.componentScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch

interface BoutResultComponent {
    val state: Value<BoutResultState>
    fun onEvent(event: BoutResultEvent)
}

data class BoutResultState(
    val leftName: String,
    val rightName: String,
    val leftScore: Int,
    val rightScore: Int,
    val winner: String
)

sealed class BoutResultEvent {
    data object Continue : BoutResultEvent()
}

class DefaultBoutResultComponent(
    componentContext: ComponentContext,
    private val poolId: Long,
    private val boutId: Long,
    private val leftName: String,
    private val rightName: String,
    private val leftScore: Int,
    private val rightScore: Int,
    private val winner: String,
    private val poolRepository: PoolRepository,
    private val onContinue: () -> Unit
) : BoutResultComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _state = MutableValue(
        BoutResultState(
            leftName = leftName,
            rightName = rightName,
            leftScore = leftScore,
            rightScore = rightScore,
            winner = winner
        )
    )
    override val state: Value<BoutResultState> = _state

    init {
        // Save bout result
        scope.launch {
            poolRepository.recordBoutResult(boutId, leftScore, rightScore)
        }
    }

    override fun onEvent(event: BoutResultEvent) {
        when (event) {
            BoutResultEvent.Continue -> onContinue()
        }
    }
}
