package com.andvl1.engrade.ui.bout

import android.app.PendingIntent
import com.andvl1.engrade.data.SettingsRepository
import com.andvl1.engrade.domain.*
import com.andvl1.engrade.domain.model.*
import com.andvl1.engrade.platform.NotificationHelper
import com.andvl1.engrade.platform.SoundManager
import com.andvl1.engrade.platform.componentScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.*

interface BoutComponent {
    val state: Value<BoutState>
    fun onEvent(event: BoutEvent)
}

class DefaultBoutComponent(
    componentContext: ComponentContext,
    private val settingsRepository: SettingsRepository,
    private val soundManager: SoundManager,
    private val notificationHelper: NotificationHelper,
    private val notificationPendingIntent: PendingIntent,
    private val onNavigateToSettings: () -> Unit
) : BoutComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private var engine: BoutEngine = BoutEngine(BoutConfig.DEFAULT)
    private var timerJob: Job? = null

    private val _state = MutableValue(BoutState())
    override val state: Value<BoutState> = _state

    init {
        // Load settings and initialize engine
        scope.launch {
            settingsRepository.boutConfigFlow.collect { config ->
                // Recreate engine with new config
                engine = BoutEngine(config)
                engine.resetAll()
                updateState()
            }
        }
    }

    override fun onEvent(event: BoutEvent) {
        when (event) {
            BoutEvent.TimerClicked -> handleTimerClick()
            BoutEvent.LeftScored -> handleLeftScore()
            BoutEvent.RightScored -> handleRightScore()
            BoutEvent.DoubleTouch -> handleDoubleTouch()
            is BoutEvent.ShowCardDialog -> showCardDialog(event.side)
            BoutEvent.DismissCardDialog -> dismissCardDialog()
            is BoutEvent.CardSelected -> handleCard(event.side, event.type)
            BoutEvent.Undo -> handleUndo()
            BoutEvent.Reset -> handleReset()
            BoutEvent.SkipSection -> handleSkipSection()
            BoutEvent.OpenSettings -> onNavigateToSettings()
        }
    }

    private fun handleTimerClick() {
        val currentState = _state.value

        // Toggle timer during any section
        if (currentState.isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()

        timerJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val initialRemaining = engine.timeRemaining

            _state.value = _state.value.copy(isTimerRunning = true)

            while (isActive) {
                delay(10) // 10ms precision like original

                val elapsed = System.currentTimeMillis() - startTime
                val newRemaining = maxOf(0L, initialRemaining - elapsed)

                engine.tickTimer(newRemaining)
                updateState()

                if (newRemaining <= 0L) {
                    // Timer expired
                    handleTimerExpired()
                    break
                }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _state.value = _state.value.copy(isTimerRunning = false)
    }

    private fun handleTimerExpired() {
        pauseTimer()
        val result = engine.endSection()
        updateState()

        // Play sound and show notification
        soundManager.vibrateEnd()
        soundManager.playAlarm()
        notificationHelper.showTimerExpiredNotification(notificationPendingIntent)
    }

    private fun handleLeftScore() {
        pauseTimer()
        engine.addScoreLeft()
        updateState()
    }

    private fun handleRightScore() {
        pauseTimer()
        engine.addScoreRight()
        updateState()
    }

    private fun handleDoubleTouch() {
        pauseTimer()
        val result = engine.addDoubleTouch()
        if (result is ScoreResult.DoubleNotAllowed) {
            // Show toast in UI layer
        }
        updateState()
    }

    private fun showCardDialog(side: FencerSide) {
        pauseTimer()
        _state.value = _state.value.copy(
            showCardDialog = CardDialogState(side)
        )
    }

    private fun dismissCardDialog() {
        _state.value = _state.value.copy(showCardDialog = null)
    }

    private fun handleCard(side: FencerSide, type: CardType) {
        pauseTimer()
        when (type) {
            CardType.YELLOW -> engine.giveYellowCard(side)
            CardType.RED -> engine.giveRedCard(side)
        }
        dismissCardDialog()
        updateState()
    }

    private fun handleUndo() {
        pauseTimer()
        engine.undo()
        updateState()
    }

    private fun handleReset() {
        pauseTimer()
        engine.resetAll()
        updateState()
    }

    private fun handleSkipSection() {
        if (!engine.isOver) {
            engine.skipSection()
            updateState()
        }
    }

    private fun updateState() {
        _state.value = BoutState(
            leftFencer = engine.leftFencer,
            rightFencer = engine.rightFencer,
            timeRemainingMs = engine.timeRemaining,
            periodNumber = engine.periodNumber,
            currentSection = engine.currentSection,
            isTimerRunning = timerJob?.isActive == true,
            isOver = engine.isOver,
            canUndo = engine.canUndo,
            config = _state.value.config,
            showCardDialog = _state.value.showCardDialog
        )
    }
}
