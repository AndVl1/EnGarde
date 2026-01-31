package com.andvl1.engrade.ui.group.dashboard

import com.andvl1.engrade.data.PoolRepository
import com.andvl1.engrade.domain.PoolEngine
import com.andvl1.engrade.domain.model.BoutResultData
import com.andvl1.engrade.domain.model.FencerRanking
import com.andvl1.engrade.domain.model.MatrixCell
import com.andvl1.engrade.platform.PdfExporter
import com.andvl1.engrade.platform.componentScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface GroupDashboardComponent {
    val state: Value<GroupDashboardState>
    fun onEvent(event: GroupDashboardEvent)
}

data class GroupDashboardState(
    val poolId: Long = 0,
    val fencerCount: Int = 0,
    val mode: Int = 5,
    val weapon: String = "SABRE",
    val matrix: List<List<MatrixCell?>> = emptyList(),
    val rankings: List<FencerRanking> = emptyList(),
    val fencerNames: Map<Int, String> = emptyMap(),
    val completedBoutsCount: Int = 0,
    val totalBoutsCount: Int = 0,
    val currentBoutInfo: String? = null,
    val nextBoutInfo: String? = null,
    val showEditScoreDialog: EditScoreDialogState? = null,
    val isLoading: Boolean = true
)

data class EditScoreDialogState(
    val boutId: Long,
    val leftName: String,
    val rightName: String,
    val leftScore: Int,
    val rightScore: Int
)

sealed class GroupDashboardEvent {
    data object StartNextBout : GroupDashboardEvent()
    data object NavigateToBoutsList : GroupDashboardEvent()
    data object NavigateBack : GroupDashboardEvent()
    data object ExportPdf : GroupDashboardEvent()
    data class ShowEditScoreDialog(val boutId: Long) : GroupDashboardEvent()
    data object DismissEditScoreDialog : GroupDashboardEvent()
    data class UpdateBoutScore(val boutId: Long, val leftScore: Int, val rightScore: Int) : GroupDashboardEvent()
}

class DefaultGroupDashboardComponent(
    componentContext: ComponentContext,
    private val poolId: Long,
    private val poolRepository: PoolRepository,
    private val poolEngine: PoolEngine,
    private val pdfExporter: PdfExporter,
    private val onNavigateToBoutConfirm: (Long, Long) -> Unit,
    private val onNavigateToBoutsList: (Long) -> Unit,
    private val onBack: () -> Unit
) : GroupDashboardComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _state = MutableValue(GroupDashboardState(poolId = poolId))
    override val state: Value<GroupDashboardState> = _state

    init {
        loadPoolData()
    }

    private fun loadPoolData() {
        scope.launch {
            // Get pool info
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
            // Get fencers
            poolRepository.getPoolFencersWithNames(poolId).collect { poolFencers ->
                val fencerNames = poolFencers.associate {
                    it.poolFencer.seedNumber to it.fencerName
                }
                _state.value = _state.value.copy(
                    fencerCount = poolFencers.size,
                    fencerNames = fencerNames
                )
                recalculateStandings()
            }
        }

        scope.launch {
            // Get bouts and recalculate on changes
            poolRepository.getPoolBoutsWithNames(poolId).collect { bouts ->
                val completed = bouts.count { it.bout.status == "COMPLETED" || it.bout.status == "FORFEIT" }
                val total = bouts.size

                val currentBout = bouts.firstOrNull { it.bout.status == "PENDING" }
                val currentInfo = currentBout?.let {
                    "Bout #${it.bout.boutOrder}: ${it.leftFencerName} vs ${it.rightFencerName}"
                }

                val nextBout = bouts.drop(1).firstOrNull { it.bout.status == "PENDING" }
                val nextInfo = nextBout?.let {
                    "Next: ${it.leftFencerName} vs ${it.rightFencerName}"
                }

                _state.value = _state.value.copy(
                    completedBoutsCount = completed,
                    totalBoutsCount = total,
                    currentBoutInfo = currentInfo,
                    nextBoutInfo = nextInfo,
                    isLoading = false
                )

                recalculateStandings()
            }
        }
    }

    private fun recalculateStandings() {
        scope.launch {
            val bouts = poolRepository.getPoolBoutsWithNames(poolId)
            bouts.collect { boutList ->
                val completedBouts = boutList
                    .filter { it.bout.status == "COMPLETED" || it.bout.status == "FORFEIT" }
                    .mapNotNull { boutWithNames ->
                        val bout = boutWithNames.bout
                        bout.leftScore?.let { leftScore ->
                            bout.rightScore?.let { rightScore ->
                                BoutResultData(
                                    leftSeed = bout.leftFencerSeed,
                                    rightSeed = bout.rightFencerSeed,
                                    leftScore = leftScore,
                                    rightScore = rightScore,
                                    status = com.andvl1.engrade.domain.model.BoutStatus.valueOf(bout.status)
                                )
                            }
                        }
                    }

                val rankings = poolEngine.calculateRankings(
                    fencerCount = _state.value.fencerCount,
                    bouts = completedBouts,
                    fencerNames = _state.value.fencerNames,
                    excludedSeeds = emptySet()
                )

                val matrix = poolEngine.buildMatrix(
                    fencerCount = _state.value.fencerCount,
                    bouts = completedBouts,
                    excludedSeeds = emptySet()
                )

                _state.value = _state.value.copy(
                    rankings = rankings,
                    matrix = matrix
                )
            }
        }
    }

    override fun onEvent(event: GroupDashboardEvent) {
        when (event) {
            GroupDashboardEvent.StartNextBout -> {
                scope.launch {
                    val nextBout = poolRepository.getNextPendingBout(poolId)
                    nextBout?.let {
                        onNavigateToBoutConfirm(poolId, it.id)
                    }
                }
            }
            GroupDashboardEvent.NavigateToBoutsList -> {
                onNavigateToBoutsList(poolId)
            }
            GroupDashboardEvent.NavigateBack -> onBack()
            GroupDashboardEvent.ExportPdf -> {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val pool = poolRepository.getPoolById(poolId)
                            val fencers = poolRepository.getPoolFencersWithNames(poolId)
                            val bouts = poolRepository.getPoolBoutsWithNames(poolId)

                            // Collect current snapshot of data
                            var poolEntity: com.andvl1.engrade.data.db.entity.PoolEntity? = null
                            pool.collect { poolEntity = it }

                            var fencersList: List<com.andvl1.engrade.data.PoolFencerWithName> = emptyList()
                            fencers.collect { fencersList = it }

                            var boutsList: List<com.andvl1.engrade.data.PoolBoutWithNames> = emptyList()
                            bouts.collect { boutsList = it }

                            val currentState = _state.value

                            poolEntity?.let { p ->
                                val pdfFile = pdfExporter.exportPoolProtocol(
                                    pool = p,
                                    fencers = fencersList,
                                    bouts = boutsList,
                                    rankings = currentState.rankings,
                                    matrix = currentState.matrix
                                )

                                withContext(Dispatchers.Main) {
                                    pdfExporter.sharePdf(pdfFile)
                                }
                            }
                        } catch (e: Exception) {
                            // Handle error - could show toast or dialog
                            e.printStackTrace()
                        }
                    }
                }
            }
            is GroupDashboardEvent.ShowEditScoreDialog -> {
                // TODO: implement edit score dialog
            }
            GroupDashboardEvent.DismissEditScoreDialog -> {
                _state.value = _state.value.copy(showEditScoreDialog = null)
            }
            is GroupDashboardEvent.UpdateBoutScore -> {
                scope.launch {
                    poolRepository.updateBoutScore(
                        boutId = event.boutId,
                        leftScore = event.leftScore,
                        rightScore = event.rightScore
                    )
                    _state.value = _state.value.copy(showEditScoreDialog = null)
                }
            }
        }
    }
}
