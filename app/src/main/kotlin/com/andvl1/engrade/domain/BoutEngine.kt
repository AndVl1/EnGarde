package com.andvl1.engrade.domain

import com.andvl1.engrade.domain.model.*
import kotlin.random.Random

/**
 * Pure business logic for fencing bout management.
 * Extracted from MainActivity - NO Android dependencies.
 */
class BoutEngine(
    private val config: BoutConfig
) {
    // Current state
    private var _leftFencer = FencerState()
    private var _rightFencer = FencerState()
    private var _timeRemaining: Long = config.periodLengthMs
    private var _periodNumber: Int = 1
    private var _currentSection: SectionType = SectionType.PERIOD
    private var _nextSection: SectionType = SectionType.BREAK
    private var _isOver: Boolean = false
    private var _undoStack: ArrayDeque<UndoAction> = ArrayDeque()

    // Read-only accessors
    val leftFencer: FencerState get() = _leftFencer
    val rightFencer: FencerState get() = _rightFencer
    val timeRemaining: Long get() = _timeRemaining
    val periodNumber: Int get() = _periodNumber
    val currentSection: SectionType get() = _currentSection
    val nextSection: SectionType get() = _nextSection
    val isOver: Boolean get() = _isOver
    val canUndo: Boolean get() = _undoStack.isNotEmpty()

    // === RESET OPERATIONS ===

    fun resetAll() {
        _leftFencer = FencerState()
        _rightFencer = FencerState()
        _timeRemaining = config.periodLengthMs
        _periodNumber = 1
        _currentSection = SectionType.PERIOD
        _nextSection = SectionType.BREAK
        _isOver = false
        _undoStack.clear()
    }

    fun resetTime() {
        _timeRemaining = when (_currentSection) {
            SectionType.PERIOD -> config.periodLengthMs
            SectionType.BREAK -> config.breakLengthMs
            SectionType.PRIORITY -> config.priorityLengthMs
        }
    }

    // === SCORING ===

    /**
     * Add score to left fencer.
     * Handles sabre break-at-8 rule and winner determination.
     */
    fun addScoreLeft(): ScoreResult {
        _leftFencer = _leftFencer.incrementScore()
        _undoStack.addLast(UndoAction.LeftScored)

        // Sabre break-at-8 rule
        if (config.weapon == Weapon.SABRE &&
            _leftFencer.score == 8 &&
            _rightFencer.score < 8
        ) {
            _timeRemaining = config.breakLengthMs
            _currentSection = SectionType.BREAK
            _nextSection = SectionType.PERIOD
        }

        // Check for winner
        if (_leftFencer.score >= config.mode || _currentSection == SectionType.PRIORITY) {
            _leftFencer = _leftFencer.withWinner()
            _rightFencer = _rightFencer.withoutWinner()
            _isOver = true
            return ScoreResult.GameOver(FencerSide.LEFT)
        }

        return ScoreResult.Scored
    }

    /**
     * Add score to right fencer.
     * Handles sabre break-at-8 rule and winner determination.
     */
    fun addScoreRight(): ScoreResult {
        _rightFencer = _rightFencer.incrementScore()
        _undoStack.addLast(UndoAction.RightScored)

        // Sabre break-at-8 rule
        if (config.weapon == Weapon.SABRE &&
            _rightFencer.score == 8 &&
            _leftFencer.score < 8
        ) {
            _timeRemaining = config.breakLengthMs
            _currentSection = SectionType.BREAK
            _nextSection = SectionType.PERIOD
        }

        // Check for winner
        if (_rightFencer.score >= config.mode || _currentSection == SectionType.PRIORITY) {
            _rightFencer = _rightFencer.withWinner()
            _leftFencer = _leftFencer.withoutWinner()
            _isOver = true
            return ScoreResult.GameOver(FencerSide.RIGHT)
        }

        return ScoreResult.Scored
    }

    /**
     * Add double touch (both fencers score).
     * NOT allowed when both are at (mode - 1).
     */
    fun addDoubleTouch(): ScoreResult {
        // Prevent double touch when both at mode-1
        if (_leftFencer.score == _rightFencer.score &&
            _leftFencer.score == config.mode - 1
        ) {
            return ScoreResult.DoubleNotAllowed
        }

        _leftFencer = _leftFencer.incrementScore()
        _rightFencer = _rightFencer.incrementScore()
        _undoStack.addLast(UndoAction.BothScored)

        // Check for winner
        when {
            _leftFencer.score >= config.mode -> {
                _leftFencer = _leftFencer.withWinner()
                _rightFencer = _rightFencer.withoutWinner()
                _isOver = true
                return ScoreResult.GameOver(FencerSide.LEFT)
            }
            _rightFencer.score >= config.mode -> {
                _rightFencer = _rightFencer.withWinner()
                _leftFencer = _leftFencer.withoutWinner()
                _isOver = true
                return ScoreResult.GameOver(FencerSide.RIGHT)
            }
        }

        return ScoreResult.Scored
    }

    // === CARDS ===

    /**
     * Give yellow card to a fencer.
     * Only recorded if they don't already have a card.
     */
    fun giveYellowCard(side: FencerSide): CardResult {
        when (side) {
            FencerSide.LEFT -> {
                val alreadyHasCard = _leftFencer.hasYellowCard || _leftFencer.hasRedCard
                _leftFencer = _leftFencer.withYellowCard()
                if (!alreadyHasCard) {
                    _undoStack.addLast(UndoAction.LeftYellowCard)
                    return CardResult.CardGiven
                }
                return CardResult.AlreadyHasCard
            }
            FencerSide.RIGHT -> {
                val alreadyHasCard = _rightFencer.hasYellowCard || _rightFencer.hasRedCard
                _rightFencer = _rightFencer.withYellowCard()
                if (!alreadyHasCard) {
                    _undoStack.addLast(UndoAction.RightYellowCard)
                    return CardResult.CardGiven
                }
                return CardResult.AlreadyHasCard
            }
        }
    }

    /**
     * Give red card to a fencer.
     * Opponent gets a point. Can end the match.
     */
    fun giveRedCard(side: FencerSide): CardResult {
        when (side) {
            FencerSide.LEFT -> {
                _leftFencer = _leftFencer.withRedCard()
                // Opponent gets a point (if not at max)
                if (_rightFencer.score < config.mode) {
                    _rightFencer = _rightFencer.incrementScore()
                }
                _undoStack.addLast(UndoAction.LeftRedCard)

                // Check if right fencer wins
                if (_rightFencer.score >= config.mode) {
                    _rightFencer = _rightFencer.withWinner()
                    _leftFencer = _leftFencer.withoutWinner()
                    _isOver = true
                    return CardResult.GameOver(FencerSide.RIGHT)
                }
            }
            FencerSide.RIGHT -> {
                _rightFencer = _rightFencer.withRedCard()
                // Opponent gets a point (if not at max)
                if (_leftFencer.score < config.mode) {
                    _leftFencer = _leftFencer.incrementScore()
                }
                _undoStack.addLast(UndoAction.RightRedCard)

                // Check if left fencer wins
                if (_leftFencer.score >= config.mode) {
                    _leftFencer = _leftFencer.withWinner()
                    _rightFencer = _rightFencer.withoutWinner()
                    _isOver = true
                    return CardResult.GameOver(FencerSide.LEFT)
                }
            }
        }

        return CardResult.CardGiven
    }

    // === TIMER ===

    /**
     * Called every 10ms when timer is running.
     */
    fun tickTimer(newTime: Long) {
        _timeRemaining = newTime
    }

    /**
     * Called when timer reaches zero.
     * Determines next section or winner.
     */
    fun endSection(): SectionEndResult {
        _timeRemaining = 0

        return when {
            _currentSection == SectionType.PRIORITY -> handlePriorityEnd()
            _periodNumber < config.maxPeriods -> handleRegulationEnd()
            else -> handleFinalPeriodEnd()
        }
    }

    private fun handlePriorityEnd(): SectionEndResult {
        // Priority overtime logic
        return when {
            _leftFencer.score == _rightFencer.score -> {
                // Tied - winner by priority
                if (_leftFencer.hasPriority) {
                    _leftFencer = _leftFencer.incrementScore().withWinner()
                    _rightFencer = _rightFencer.withoutWinner()
                    _isOver = true
                    SectionEndResult.WinnerByPriority(FencerSide.LEFT)
                } else if (_rightFencer.hasPriority) {
                    _rightFencer = _rightFencer.incrementScore().withWinner()
                    _leftFencer = _leftFencer.withoutWinner()
                    _isOver = true
                    SectionEndResult.WinnerByPriority(FencerSide.RIGHT)
                } else {
                    SectionEndResult.ProceedToNext(SectionType.PRIORITY)
                }
            }
            _leftFencer.score > _rightFencer.score -> {
                _leftFencer = _leftFencer.withWinner()
                _rightFencer = _rightFencer.withoutWinner()
                _isOver = true
                SectionEndResult.WinnerByTouch(FencerSide.LEFT)
            }
            else -> {
                _rightFencer = _rightFencer.withWinner()
                _leftFencer = _leftFencer.withoutWinner()
                _isOver = true
                SectionEndResult.WinnerByTouch(FencerSide.RIGHT)
            }
        }
    }

    private fun handleRegulationEnd(): SectionEndResult {
        // Next period will also be regulation time
        _nextSection = if (_currentSection == SectionType.PERIOD) {
            _currentSection = SectionType.BREAK
            SectionType.BREAK
        } else {
            _currentSection = SectionType.PERIOD
            SectionType.PERIOD
        }

        return SectionEndResult.ProceedToNext(_nextSection)
    }

    private fun handleFinalPeriodEnd(): SectionEndResult {
        // Last period ended
        return when {
            _leftFencer.score > _rightFencer.score -> {
                _leftFencer = _leftFencer.withWinner()
                _rightFencer = _rightFencer.withoutWinner()
                _isOver = true
                SectionEndResult.WinnerByTouch(FencerSide.LEFT)
            }
            _leftFencer.score < _rightFencer.score -> {
                _rightFencer = _rightFencer.withWinner()
                _leftFencer = _leftFencer.withoutWinner()
                _isOver = true
                SectionEndResult.WinnerByTouch(FencerSide.RIGHT)
            }
            else -> {
                // Tied - go to priority
                _currentSection = SectionType.BREAK
                _nextSection = SectionType.PRIORITY
                SectionEndResult.ProceedToNext(SectionType.PRIORITY)
            }
        }
    }

    /**
     * Start the next section after timer expired.
     * Must be called after endSection().
     */
    fun proceedToNextSection(): ProceedResult {
        when (_nextSection) {
            SectionType.PERIOD -> {
                _timeRemaining = config.periodLengthMs
                _periodNumber++
                _currentSection = SectionType.PERIOD
                _nextSection = SectionType.BREAK
            }
            SectionType.BREAK -> {
                _timeRemaining = config.breakLengthMs
                _currentSection = SectionType.BREAK
                _nextSection = SectionType.PERIOD
            }
            SectionType.PRIORITY -> {
                _timeRemaining = config.priorityLengthMs
                _currentSection = SectionType.PRIORITY
                assignRandomPriority()
            }
        }

        return ProceedResult.Started(_currentSection)
    }

    private fun assignRandomPriority() {
        if (Random.nextDouble() > 0.5) {
            _leftFencer = _leftFencer.withPriority()
            _rightFencer = _rightFencer.withoutPriority()
        } else {
            _rightFencer = _rightFencer.withPriority()
            _leftFencer = _leftFencer.withoutPriority()
        }
    }

    /**
     * Skip current section (for debugging/special cases).
     * Only allowed if bout is not over.
     */
    fun skipSection(): SkipResult {
        if (_isOver) {
            // Save state for undo
            val action = UndoAction.SectionSkipped(
                previousTime = _timeRemaining,
                previousSection = _currentSection,
                previousPeriod = _periodNumber
            )
            _undoStack.addLast(action)

            val endResult = endSection()
            return SkipResult.Skipped(endResult)
        } else {
            return SkipResult.CannotSkipPriority
        }
    }

    // === UNDO ===

    /**
     * Undo the most recent action.
     */
    fun undo(): UndoResult {
        val action = _undoStack.removeLastOrNull() ?: return UndoResult.NothingToUndo

        return when (action) {
            is UndoAction.LeftScored -> {
                _leftFencer = _leftFencer.decrementScore()
                if (_leftFencer.isWinner) {
                    _leftFencer = _leftFencer.withoutWinner()
                }
                _isOver = false
                UndoResult.Undone
            }
            is UndoAction.RightScored -> {
                _rightFencer = _rightFencer.decrementScore()
                if (_rightFencer.isWinner) {
                    _rightFencer = _rightFencer.withoutWinner()
                }
                _isOver = false
                UndoResult.Undone
            }
            is UndoAction.BothScored -> {
                _leftFencer = _leftFencer.decrementScore()
                _rightFencer = _rightFencer.decrementScore()
                if (_leftFencer.isWinner) _leftFencer = _leftFencer.withoutWinner()
                if (_rightFencer.isWinner) _rightFencer = _rightFencer.withoutWinner()
                _isOver = false
                UndoResult.Undone
            }
            is UndoAction.LeftYellowCard -> {
                _leftFencer = _leftFencer.withoutYellowCard()
                UndoResult.Undone
            }
            is UndoAction.LeftRedCard -> {
                _leftFencer = _leftFencer.withoutRedCard()
                _rightFencer = _rightFencer.decrementScore()
                _isOver = false
                UndoResult.Undone
            }
            is UndoAction.RightYellowCard -> {
                _rightFencer = _rightFencer.withoutYellowCard()
                UndoResult.Undone
            }
            is UndoAction.RightRedCard -> {
                _rightFencer = _rightFencer.withoutRedCard()
                _leftFencer = _leftFencer.decrementScore()
                _isOver = false
                UndoResult.Undone
            }
            is UndoAction.SectionSkipped -> {
                _currentSection = action.previousSection
                _periodNumber = action.previousPeriod
                _timeRemaining = action.previousTime

                if (config.maxPeriods == 1) {
                    _currentSection = SectionType.PERIOD
                    _periodNumber = 1
                } else {
                    _nextSection = when (action.previousSection) {
                        SectionType.PERIOD -> {
                            _currentSection = SectionType.PERIOD
                            SectionType.BREAK
                        }
                        SectionType.BREAK -> {
                            _currentSection = SectionType.BREAK
                            SectionType.PERIOD
                        }
                        SectionType.PRIORITY -> {
                            _currentSection = SectionType.PRIORITY
                            SectionType.PRIORITY
                        }
                    }
                }

                _leftFencer = _leftFencer.withoutPriority()
                _rightFencer = _rightFencer.withoutPriority()
                _isOver = false
                UndoResult.Undone
            }
        }
    }
}

// === RESULT TYPES ===

sealed class ScoreResult {
    data object Scored : ScoreResult()
    data object DoubleNotAllowed : ScoreResult()
    data class GameOver(val winner: FencerSide) : ScoreResult()
}

sealed class CardResult {
    data object CardGiven : CardResult()
    data object AlreadyHasCard : CardResult()
    data class GameOver(val winner: FencerSide) : CardResult()
}

sealed class SectionEndResult {
    data class ProceedToNext(val nextSection: SectionType) : SectionEndResult()
    data class WinnerByTouch(val winner: FencerSide) : SectionEndResult()
    data class WinnerByPriority(val winner: FencerSide) : SectionEndResult()
}

sealed class ProceedResult {
    data class Started(val section: SectionType) : ProceedResult()
}

sealed class SkipResult {
    data class Skipped(val endResult: SectionEndResult) : SkipResult()
    data object CannotSkipPriority : SkipResult()
}

sealed class UndoResult {
    data object Undone : UndoResult()
    data object NothingToUndo : UndoResult()
}
