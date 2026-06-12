package com.andvl1.engrade.ui.group.dashboard

import com.andvl1.engrade.data.DeRepository
import com.andvl1.engrade.data.PoolRepository
import com.andvl1.engrade.domain.PoolEngine
import com.andvl1.engrade.domain.model.BoutResultData
import com.andvl1.engrade.domain.model.BoutStatus
import com.andvl1.engrade.domain.model.FencerRanking
import com.andvl1.engrade.domain.model.MatrixCell
import com.andvl1.engrade.platform.CsvExporter
import com.andvl1.engrade.platform.PdfExporter
import com.andvl1.engrade.platform.componentScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface GroupDashboardComponent {
    val state: Value<GroupDashboardState>
    fun onEvent(event: GroupDashboardEvent)
}

/**
 * Structured bout info passed to the Composable layer for localized formatting.
 * Avoids hardcoded English format strings inside the component.
 */
data class BoutDisplayInfo(
    val boutOrder: Int,
    val leftName: String,
    val rightName: String
)

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
    val currentBout: BoutDisplayInfo? = null,
    val nextBout: BoutDisplayInfo? = null,
    val showEditScoreDialog: EditScoreDialogState? = null,
    val showForfeitDialog: ForfeitDialogState? = null,
    val showQuickEntryDialog: QuickEntryDialogState? = null,
    val isLoading: Boolean = true,
    val exportError: String? = null,
    val editScoreError: String? = null
)

data class EditScoreDialogState(
    val boutId: Long,
    val leftName: String,
    val rightName: String,
    val leftScore: Int,
    val rightScore: Int
)

data class QuickEntryDialogState(
    val boutId: Long,
    val leftName: String,
    val rightName: String,
    val mode: Int
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
    /** Navigate to the Direct Elimination bracket; creates the tableau if not yet present. */
    data object ProceedToDE : GroupDashboardEvent()
    data object ExportPdf : GroupDashboardEvent()
    data object ExportCsv : GroupDashboardEvent()
    data object DismissExportError : GroupDashboardEvent()
    data object DismissEditScoreError : GroupDashboardEvent()
    data class ShowEditScoreDialog(val boutId: Long) : GroupDashboardEvent()
    data object DismissEditScoreDialog : GroupDashboardEvent()
    data class UpdateBoutScore(val boutId: Long, val leftScore: Int, val rightScore: Int) : GroupDashboardEvent()
    data object ShowForfeitDialog : GroupDashboardEvent()
    data object DismissForfeitDialog : GroupDashboardEvent()
    data class RecordForfeit(val boutId: Long, val absentSide: String) : GroupDashboardEvent()
    data class ExcludeFencer(val seedNumber: Int) : GroupDashboardEvent()
    data class ShowQuickEntryDialog(val leftSeed: Int, val rightSeed: Int) : GroupDashboardEvent()
    data object DismissQuickEntryDialog : GroupDashboardEvent()
    data class RecordQuickScore(val boutId: Long, val leftScore: Int, val rightScore: Int) : GroupDashboardEvent()
}

class DefaultGroupDashboardComponent(
    componentContext: ComponentContext,
    private val poolId: Long,
    private val poolRepository: PoolRepository,
    private val poolEngine: PoolEngine,
    private val pdfExporter: PdfExporter,
    private val csvExporter: CsvExporter,
    private val deRepository: DeRepository,
    private val onNavigateToBoutConfirm: (Long, Long) -> Unit,
    private val onNavigateToBoutsList: (Long) -> Unit,
    /**
     * Navigate to the DE tableau for this pool.
     *
     * **Qualification default:** all pool fencers qualify, seeded by final pool ranking (FIE
     * standard). Excluded fencers remain in the DE draw per FIE rules — their pool bouts still
     * count toward ranking. A qualification cutoff (e.g. top-N qualify) is a future feature.
     *
     * @param poolId DB id of the pool
     * @param weapon FIE weapon code ("SABRE" / "FOIL_EPEE") so DE bouts inherit pool weapon
     */
    private val onNavigateToDE: (poolId: Long, weapon: String) -> Unit,
    private val onBack: () -> Unit
) : GroupDashboardComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _state = MutableValue(GroupDashboardState(poolId = poolId))
    override val state: Value<GroupDashboardState> = _state

    init {
        loadPoolData()
    }

    /**
     * H3 fix: single combined flow avoids 3 separate collect{} blocks racing each other.
     * Standings are computed in-memory from the snapshot — no extra DB reads.
     * Race on fencerCount=0 is eliminated because combine waits for all sources before emitting.
     */
    private fun loadPoolData() {
        scope.launch {
            combine(
                poolRepository.getPoolById(poolId),
                poolRepository.getPoolFencersWithNames(poolId),
                poolRepository.getPoolBoutsWithNames(poolId)
            ) { pool, poolFencers, bouts ->
                Triple(pool, poolFencers, bouts)
            }.collect { (pool, poolFencers, bouts) ->
                val fencerCount = poolFencers.size
                val fencerNames = poolFencers.associate {
                    it.poolFencer.seedNumber to it.fencerName
                }
                val excludedSeeds = poolFencers
                    .filter { it.poolFencer.excluded }
                    .map { it.poolFencer.seedNumber }
                    .toSet()

                val completed = bouts.count {
                    it.bout.status == BoutStatus.COMPLETED || it.bout.status == BoutStatus.FORFEIT
                }
                val total = bouts.size

                val pendingBouts = bouts.filter { it.bout.status == BoutStatus.PENDING }
                val currentPendingEntry = pendingBouts.firstOrNull()
                val currentBoutDisplayInfo = currentPendingEntry?.let {
                    BoutDisplayInfo(it.bout.boutOrder, it.leftFencerName, it.rightFencerName)
                }
                val nextPendingEntry = pendingBouts.drop(1).firstOrNull()
                val nextBoutDisplayInfo = nextPendingEntry?.let {
                    BoutDisplayInfo(it.bout.boutOrder, it.leftFencerName, it.rightFencerName)
                }

                // Compute standings in-memory from the same snapshot — no extra DB reads
                val completedBouts = bouts
                    .filter { it.bout.status == BoutStatus.COMPLETED || it.bout.status == BoutStatus.FORFEIT }
                    .mapNotNull { boutWithNames ->
                        val bout = boutWithNames.bout
                        bout.leftScore?.let { leftScore ->
                            bout.rightScore?.let { rightScore ->
                                BoutResultData(
                                    leftSeed = bout.leftFencerSeed,
                                    rightSeed = bout.rightFencerSeed,
                                    leftScore = leftScore,
                                    rightScore = rightScore,
                                    status = bout.status
                                )
                            }
                        }
                    }

                val rankings = poolEngine.calculateRankings(
                    fencerCount = fencerCount,
                    bouts = completedBouts,
                    fencerNames = fencerNames,
                    excludedSeeds = excludedSeeds
                )

                val matrix = poolEngine.buildMatrix(
                    fencerCount = fencerCount,
                    bouts = completedBouts
                )

                _state.value = _state.value.copy(
                    mode = pool?.mode ?: _state.value.mode,
                    weapon = pool?.weapon ?: _state.value.weapon,
                    fencerCount = fencerCount,
                    fencerNames = fencerNames,
                    excludedSeeds = excludedSeeds,
                    completedBoutsCount = completed,
                    totalBoutsCount = total,
                    currentBout = currentBoutDisplayInfo,
                    nextBout = nextBoutDisplayInfo,
                    rankings = rankings,
                    matrix = matrix,
                    isLoading = false
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
            GroupDashboardEvent.ProceedToDE -> {
                scope.launch {
                    try {
                        val existingBracket = deRepository.observeBracket(poolId).first()
                        if (existingBracket == null) {
                            deRepository.createTableauForPool(poolId)
                        }
                        onNavigateToDE(poolId, _state.value.weapon)
                    } catch (e: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }
                }
            }
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
                            // M2 fix: log to Crashlytics and surface error to user
                            FirebaseCrashlytics.getInstance().recordException(e)
                            withContext(Dispatchers.Main) {
                                _state.value = _state.value.copy(
                                    exportError = e.message ?: "PDF export failed"
                                )
                            }
                        }
                    }
                }
            }
            GroupDashboardEvent.ExportCsv -> {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val currentState = _state.value
                            val csvFile = csvExporter.exportRankingsCsv(currentState.rankings)
                            withContext(Dispatchers.Main) {
                                csvExporter.shareCsv(csvFile)
                            }
                        } catch (e: Exception) {
                            FirebaseCrashlytics.getInstance().recordException(e)
                            withContext(Dispatchers.Main) {
                                _state.value = _state.value.copy(
                                    exportError = e.message ?: "CSV export failed"
                                )
                            }
                        }
                    }
                }
            }
            GroupDashboardEvent.DismissExportError -> {
                _state.value = _state.value.copy(exportError = null)
            }
            GroupDashboardEvent.DismissEditScoreError -> {
                _state.value = _state.value.copy(editScoreError = null)
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
                // F3: FIE — ничья запрещена; валидируем до вызова репозитория
                if (event.leftScore == event.rightScore) {
                    _state.value = _state.value.copy(
                        editScoreError = "FIE: ничья в бое запрещена (${event.leftScore}:${event.rightScore})"
                    )
                } else {
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
            is GroupDashboardEvent.ShowQuickEntryDialog -> {
                scope.launch {
                    val boutsList = poolRepository.getPoolBoutsWithNames(poolId).first()
                    val bout = boutsList.find { boutWithNames ->
                        val b = boutWithNames.bout
                        b.status == BoutStatus.PENDING &&
                            ((b.leftFencerSeed == event.leftSeed && b.rightFencerSeed == event.rightSeed) ||
                             (b.leftFencerSeed == event.rightSeed && b.rightFencerSeed == event.leftSeed))
                    }
                    bout?.let {
                        _state.value = _state.value.copy(
                            showQuickEntryDialog = QuickEntryDialogState(
                                boutId = it.bout.id,
                                leftName = it.leftFencerName,
                                rightName = it.rightFencerName,
                                mode = _state.value.mode
                            )
                        )
                    }
                }
            }
            GroupDashboardEvent.DismissQuickEntryDialog -> {
                _state.value = _state.value.copy(showQuickEntryDialog = null)
            }
            is GroupDashboardEvent.RecordQuickScore -> {
                if (event.leftScore == event.rightScore) {
                    _state.value = _state.value.copy(
                        editScoreError = "FIE: ничья в бое запрещена (${event.leftScore}:${event.rightScore})"
                    )
                } else {
                    scope.launch {
                        poolRepository.recordBoutResult(
                            boutId = event.boutId,
                            leftScore = event.leftScore,
                            rightScore = event.rightScore
                        )
                        _state.value = _state.value.copy(showQuickEntryDialog = null)
                    }
                }
            }
        }
    }
}
