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
import kotlinx.coroutines.flow.first
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
    val excludedSeeds: Set<Int> = emptySet(),
    val completedBoutsCount: Int = 0,
    val totalBoutsCount: Int = 0,
    val currentBoutInfo: String? = null,
    val nextBoutInfo: String? = null,
    val showEditScoreDialog: EditScoreDialogState? = null,
    val showForfeitDialog: ForfeitDialogState? = null,
    val isLoading: Boolean = true
)

data class EditScoreDialogState(
    val boutId: Long,
    val leftName: String,
    val rightName: String,
    val leftScore: Int,
    val rightScore: Int
)

data class ForfeitDialogState(
    val boutId: Long,
    val leftName: String,
    val rightName: String,
    val leftSeed: Int,
    val rightSeed: Int
)

sealed class GroupDashboardEvent {
    data object StartNextBout : GroupDashboardEvent()
    data object NavigateToBoutsList : GroupDashboardEvent()
    data object NavigateBack : GroupDashboardEvent()
    data object ExportPdf : GroupDashboardEvent()
    data class ShowEditScoreDialog(val boutId: Long) : GroupDashboardEvent()
    data object DismissEditScoreDialog : GroupDashboardEvent()
    data class UpdateBoutScore(val boutId: Long, val leftScore: Int, val rightScore: Int) : GroupDashboardEvent()
    data object ShowForfeitDialog : GroupDashboardEvent()
    data object DismissForfeitDialog : GroupDashboardEvent()
    data class RecordForfeit(val boutId: Long, val absentSide: String) : GroupDashboardEvent()
    data class ExcludeFencer(val seedNumber: Int) : GroupDashboardEvent()
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
            poolRepository.getPoolFencersWithNames(poolId).collect { poolFencers ->
                val fencerNames = poolFencers.associate {
                    it.poolFencer.seedNumber to it.fencerName
                }
                val excludedSeeds = poolFencers
                    .filter { it.poolFencer.excluded }
                    .map { it.poolFencer.seedNumber }
                    .toSet()
                _state.value = _state.value.copy(
                    fencerCount = poolFencers.size,
                    fencerNames = fencerNames,
                    excludedSeeds = excludedSeeds
                )
                recalculateStandings()
            }
        }

        scope.launch {
            poolRepository.getPoolBoutsWithNames(poolId).collect { bouts ->
                val completed = bouts.count { it.bout.status == "COMPLETED" || it.bout.status == "FORFEIT" }
                val total = bouts.size

                val pendingBouts = bouts.filter { it.bout.status == "PENDING" }
                val currentBout = pendingBouts.firstOrNull()
                val currentInfo = currentBout?.let {
                    "Bout #${it.bout.boutOrder}: ${it.leftFencerName} vs ${it.rightFencerName}"
                }

                val nextBout = pendingBouts.drop(1).firstOrNull()
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
            val boutList = poolRepository.getPoolBoutsWithNames(poolId).first()
            val currentState = _state.value

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
                fencerCount = currentState.fencerCount,
                bouts = completedBouts,
                fencerNames = currentState.fencerNames,
                excludedSeeds = currentState.excludedSeeds
            )

            val matrix = poolEngine.buildMatrix(
                fencerCount = currentState.fencerCount,
                bouts = completedBouts,
                excludedSeeds = currentState.excludedSeeds
            )

            _state.value = _state.value.copy(
                rankings = rankings,
                matrix = matrix
            )
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
                            val poolEntity = poolRepository.getPoolById(poolId).first()
                            val fencersList = poolRepository.getPoolFencersWithNames(poolId).first()
                            val boutsList = poolRepository.getPoolBoutsWithNames(poolId).first()
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
                            e.printStackTrace()
                        }
                    }
                }
            }
            is GroupDashboardEvent.ShowEditScoreDialog -> {
                scope.launch {
                    val boutsList = poolRepository.getPoolBoutsWithNames(poolId).first()
                    val bout = boutsList.find { it.bout.id == event.boutId }
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
            GroupDashboardEvent.ShowForfeitDialog -> {
                scope.launch {
                    val nextBout = poolRepository.getNextPendingBout(poolId)
                    nextBout?.let { bout ->
                        val boutsList = poolRepository.getPoolBoutsWithNames(poolId).first()
                        val boutWithNames = boutsList.find { it.bout.id == bout.id }
                        boutWithNames?.let {
                            _state.value = _state.value.copy(
                                showForfeitDialog = ForfeitDialogState(
                                    boutId = it.bout.id,
                                    leftName = it.leftFencerName,
                                    rightName = it.rightFencerName,
                                    leftSeed = it.bout.leftFencerSeed,
                                    rightSeed = it.bout.rightFencerSeed
                                )
                            )
                        }
                    }
                }
            }
            GroupDashboardEvent.DismissForfeitDialog -> {
                _state.value = _state.value.copy(showForfeitDialog = null)
            }
            is GroupDashboardEvent.RecordForfeit -> {
                scope.launch {
                    poolRepository.recordForfeit(
                        boutId = event.boutId,
                        absentSide = event.absentSide,
                        maxScore = _state.value.mode
                    )
                    _state.value = _state.value.copy(showForfeitDialog = null)
                }
            }
            is GroupDashboardEvent.ExcludeFencer -> {
                scope.launch {
                    poolRepository.excludeFencer(poolId, event.seedNumber)
                }
            }
        }
    }
}
