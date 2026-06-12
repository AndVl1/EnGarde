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
        // F1: бой завершён — добавление очков запрещено
        if (_isOver) return ScoreResult.Scored

        // Save state before making changes
        val previousSection = _currentSection
        val previousNextSection = _nextSection
        val previousTime = _timeRemaining

        _leftFencer = _leftFencer.incrementScore()
        _undoStack.addLast(
            UndoAction.LeftScored(
                previousSection = previousSection,
                previousNextSection = previousNextSection,
                previousTime = previousTime
            )
        )

        // F5: Sabre break-at-8 rule (общий helper)
        applySabreBreakAt8IfNeeded()

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
        // F1: бой завершён — добавление очков запрещено
        if (_isOver) return ScoreResult.Scored

        // Save state before making changes
        val previousSection = _currentSection
        val previousNextSection = _nextSection
        val previousTime = _timeRemaining

        _rightFencer = _rightFencer.incrementScore()
        _undoStack.addLast(
            UndoAction.RightScored(
                previousSection = previousSection,
                previousNextSection = previousNextSection,
                previousTime = previousTime
            )
        )

        // F5: Sabre break-at-8 rule (общий helper)
        applySabreBreakAt8IfNeeded()

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
     * In PRIORITY section, simultaneous action is annulled per FIE rules.
     */
    fun addDoubleTouch(): ScoreResult {
        // F1: бой завершён — добавление очков запрещено
        if (_isOver) return ScoreResult.Scored

        // F2: FIE — одновременное действие в минуту приоритета аннулируется (никто не засчитывает)
        if (_currentSection == SectionType.PRIORITY) {
            return ScoreResult.Scored
        }

        // Prevent double touch when both at mode-1
        if (_leftFencer.score == _rightFencer.score &&
            _leftFencer.score == config.mode - 1
        ) {
            return ScoreResult.DoubleNotAllowed
        }

        // Save state before making changes
        val previousSection = _currentSection
        val previousNextSection = _nextSection
        val previousTime = _timeRemaining

        _leftFencer = _leftFencer.incrementScore()
        _rightFencer = _rightFencer.incrementScore()
        _undoStack.addLast(
            UndoAction.BothScored(
                previousSection = previousSection,
                previousNextSection = previousNextSection,
                previousTime = previousTime
            )
        )

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
     *
     * FIE t.114 group-1 penalty escalation chain:
     *  • No prior card  → yellow card (warning, no touch)
     *  • Has yellow     → auto-escalate to red: opponent +1 touch (sabre break-at-8 applies)
     *  • Has red        → auto-escalate to black: immediate exclusion, opponent wins
     *  • Has black      → no-op (already excluded)
     */
    fun giveYellowCard(side: FencerSide): CardResult {
        when (side) {
            FencerSide.LEFT -> {
                return when {
                    _leftFencer.hasBlackCard -> CardResult.AlreadyHasCard
                    _leftFencer.hasRedCard -> escalateToBlack(FencerSide.LEFT)
                    _leftFencer.hasYellowCard -> escalateYellowToRed(FencerSide.LEFT)
                    else -> {
                        _leftFencer = _leftFencer.withYellowCard()
                        _undoStack.addLast(UndoAction.LeftYellowCard)
                        CardResult.CardGiven
                    }
                }
            }
            FencerSide.RIGHT -> {
                return when {
                    _rightFencer.hasBlackCard -> CardResult.AlreadyHasCard
                    _rightFencer.hasRedCard -> escalateToBlack(FencerSide.RIGHT)
                    _rightFencer.hasYellowCard -> escalateYellowToRed(FencerSide.RIGHT)
                    else -> {
                        _rightFencer = _rightFencer.withYellowCard()
                        _undoStack.addLast(UndoAction.RightYellowCard)
                        CardResult.CardGiven
                    }
                }
            }
        }
    }

    /**
     * Yellow → Red escalation (FIE t.114, second group-1 offence).
     * Awards one touch to the opponent; sabre break-at-8 helper is applied.
     * Pushes a distinct undo action so the escalation reverses cleanly.
     */
    private fun escalateYellowToRed(side: FencerSide): CardResult {
        when (side) {
            FencerSide.LEFT -> {
                _leftFencer = _leftFencer.withRedCard()
                if (_rightFencer.score < config.mode) {
                    _rightFencer = _rightFencer.incrementScore()
                }
                applySabreBreakAt8IfNeeded()
                _undoStack.addLast(UndoAction.LeftYellowToRedEscalation)
                if (_rightFencer.score >= config.mode) {
                    _rightFencer = _rightFencer.withWinner()
                    _leftFencer = _leftFencer.withoutWinner()
                    _isOver = true
                    return CardResult.GameOver(FencerSide.RIGHT)
                }
            }
            FencerSide.RIGHT -> {
                _rightFencer = _rightFencer.withRedCard()
                if (_leftFencer.score < config.mode) {
                    _leftFencer = _leftFencer.incrementScore()
                }
                applySabreBreakAt8IfNeeded()
                _undoStack.addLast(UndoAction.RightYellowToRedEscalation)
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

    /**
     * Red → Black escalation (FIE t.114, third group-1 offence, or direct group-3/4 offence).
     * Ends the bout immediately; opponent is declared winner. No score change.
     * Internal helper shared by giveYellowCard (escalation) and giveBlackCard (direct).
     */
    private fun escalateToBlack(side: FencerSide): CardResult {
        when (side) {
            FencerSide.LEFT -> {
                _leftFencer = _leftFencer.withBlackCard()
                _rightFencer = _rightFencer.withWinner()
                _leftFencer = _leftFencer.withoutWinner()
                _isOver = true
                _undoStack.addLast(UndoAction.LeftBlackCard)
                return CardResult.GameOver(FencerSide.RIGHT)
            }
            FencerSide.RIGHT -> {
                _rightFencer = _rightFencer.withBlackCard()
                _leftFencer = _leftFencer.withWinner()
                _rightFencer = _rightFencer.withoutWinner()
                _isOver = true
                _undoStack.addLast(UndoAction.RightBlackCard)
                return CardResult.GameOver(FencerSide.LEFT)
            }
        }
    }

    /**
     * Give black card to a fencer (group 3/4 offence = exclusion, FIE t.86).
     * Ends the bout immediately with the opponent as winner.
     * No point is awarded — exclusion is the penalty.
     * Undoable: undo restores the bout to its pre-exclusion state.
     *
     * Pool-level exclusion (PoolFencerEntity annulment) is handled separately.
     * TODO(wave-follow-up): wire black card to pool exclusion
     */
    fun giveBlackCard(side: FencerSide): CardResult {
        if (_isOver) return CardResult.CardGiven
        return escalateToBlack(side)
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
                // F5: штрафное очко может сработать правило сабли перерыв-на-8
                applySabreBreakAt8IfNeeded()
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
                // F5: штрафное очко может сработать правило сабли перерыв-на-8
                applySabreBreakAt8IfNeeded()
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
        // Toggle between period and break
        _nextSection = if (_currentSection == SectionType.PERIOD) {
            SectionType.BREAK
        } else {
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
        if (!_isOver) {
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

    // === INTERNAL HELPERS ===

    /**
     * F5: Sabre break-at-8 rule.
     * Если оружие — сабля и один фехтовальщик достигает ровно 8 очков, пока другой ниже 8,
     * срабатывает обязательный перерыв (согласно правилам ФИЭ).
     * Вызывается из addScoreLeft, addScoreRight и giveRedCard.
     */
    private fun applySabreBreakAt8IfNeeded() {
        if (config.weapon != Weapon.SABRE) return
        when {
            _leftFencer.score == 8 && _rightFencer.score < 8 -> {
                _timeRemaining = config.breakLengthMs
                _currentSection = SectionType.BREAK
                _nextSection = SectionType.PERIOD
            }
            _rightFencer.score == 8 && _leftFencer.score < 8 -> {
                _timeRemaining = config.breakLengthMs
                _currentSection = SectionType.BREAK
                _nextSection = SectionType.PERIOD
            }
        }
    }

    /**
     * Recomputes _isOver and winner flags from the current score/section state.
     * Called after any undo that affects scores, so the bout status stays consistent.
     *
     * A bout is still over if:
     *  - left score >= mode, OR
     *  - right score >= mode, OR
     *  - the current section is PRIORITY (any score difference settles it — but
     *    PRIORITY winner is only set by endSection/handlePriorityEnd, not here).
     * If neither condition holds, isOver becomes false and all winner flags are cleared.
     */
    private fun recomputeOverState() {
        when {
            _leftFencer.score >= config.mode -> {
                _leftFencer = _leftFencer.withWinner()
                _rightFencer = _rightFencer.withoutWinner()
                _isOver = true
            }
            _rightFencer.score >= config.mode -> {
                _rightFencer = _rightFencer.withWinner()
                _leftFencer = _leftFencer.withoutWinner()
                _isOver = true
            }
            else -> {
                _isOver = false
                _leftFencer = _leftFencer.withoutWinner()
                _rightFencer = _rightFencer.withoutWinner()
            }
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
                // Restore section state
                _currentSection = action.previousSection
                _nextSection = action.previousNextSection
                _timeRemaining = action.previousTime
                // F1: recomputeOverState вместо жёсткого _isOver = false — корректно
                // обрабатывает случай, когда счёт после декремента всё ещё >= mode
                recomputeOverState()
                UndoResult.Undone
            }
            is UndoAction.RightScored -> {
                _rightFencer = _rightFencer.decrementScore()
                // Restore section state
                _currentSection = action.previousSection
                _nextSection = action.previousNextSection
                _timeRemaining = action.previousTime
                // F1: recomputeOverState вместо жёсткого _isOver = false
                recomputeOverState()
                UndoResult.Undone
            }
            is UndoAction.BothScored -> {
                _leftFencer = _leftFencer.decrementScore()
                _rightFencer = _rightFencer.decrementScore()
                // Restore section state
                _currentSection = action.previousSection
                _nextSection = action.previousNextSection
                _timeRemaining = action.previousTime
                // F1: recomputeOverState вместо жёсткого _isOver = false
                recomputeOverState()
                UndoResult.Undone
            }
            is UndoAction.LeftYellowCard -> {
                _leftFencer = _leftFencer.withoutYellowCard()
                UndoResult.Undone
            }
            is UndoAction.LeftRedCard -> {
                _leftFencer = _leftFencer.withoutRedCard()
                _rightFencer = _rightFencer.decrementScore()
                recomputeOverState()
                UndoResult.Undone
            }
            is UndoAction.RightYellowCard -> {
                _rightFencer = _rightFencer.withoutYellowCard()
                UndoResult.Undone
            }
            is UndoAction.RightRedCard -> {
                _rightFencer = _rightFencer.withoutRedCard()
                _leftFencer = _leftFencer.decrementScore()
                recomputeOverState()
                UndoResult.Undone
            }
            is UndoAction.LeftYellowToRedEscalation -> {
                // Reverse yellow→red escalation: restore to yellow state, remove opponent point.
                // hasYellowCard remains true (was already set when escalation triggered).
                _leftFencer = _leftFencer.withoutRedCard()
                _rightFencer = _rightFencer.decrementScore()
                recomputeOverState()
                UndoResult.Undone
            }
            is UndoAction.RightYellowToRedEscalation -> {
                _rightFencer = _rightFencer.withoutRedCard()
                _leftFencer = _leftFencer.decrementScore()
                recomputeOverState()
                UndoResult.Undone
            }
            is UndoAction.LeftBlackCard -> {
                // Reverse black card exclusion: restore bout to active state.
                // No score change — black card does not award a point.
                _leftFencer = _leftFencer.withoutBlackCard()
                _rightFencer = _rightFencer.withoutWinner()
                _isOver = false
                UndoResult.Undone
            }
            is UndoAction.RightBlackCard -> {
                _rightFencer = _rightFencer.withoutBlackCard()
                _leftFencer = _leftFencer.withoutWinner()
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
