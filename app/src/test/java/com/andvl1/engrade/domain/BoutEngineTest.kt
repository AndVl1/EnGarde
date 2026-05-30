package com.andvl1.engrade.domain

import com.andvl1.engrade.domain.model.BoutConfig
import com.andvl1.engrade.domain.model.FencerSide
import com.andvl1.engrade.domain.model.SectionType
import com.andvl1.engrade.domain.model.Weapon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * Unit tests for BoutEngine — страховка перед рефакторингом.
 * Покрывает: scoring, mode limit, double touch, undo, cards, sabre break.
 */
class BoutEngineTest {

    // === Фабричные методы для конфигов ===

    private fun config5() = BoutConfig(
        mode = 5,
        weapon = Weapon.FOIL_EPEE,
        periodLengthMs = 180_000L,
        breakLengthMs = 60_000L,
        priorityLengthMs = 60_000L
    )

    private fun config15() = BoutConfig(
        mode = 15,
        weapon = Weapon.FOIL_EPEE,
        periodLengthMs = 180_000L,
        breakLengthMs = 60_000L,
        priorityLengthMs = 60_000L
    )

    private fun config15Sabre() = BoutConfig(
        mode = 15,
        weapon = Weapon.SABRE,
        periodLengthMs = 180_000L,
        breakLengthMs = 60_000L,
        priorityLengthMs = 60_000L
    )

    // === Начальное состояние ===

    @Test
    fun `initial state has zero scores and isOver false`() {
        val engine = BoutEngine(config5())
        assertEquals(0, engine.leftFencer.score)
        assertEquals(0, engine.rightFencer.score)
        assertFalse(engine.isOver)
        assertFalse(engine.canUndo)
    }

    // === addScoreLeft / addScoreRight: базовое приращение ===

    @Test
    fun `addScoreLeft increments left score`() {
        val engine = BoutEngine(config5())
        engine.addScoreLeft()
        assertEquals(1, engine.leftFencer.score)
        assertEquals(0, engine.rightFencer.score)
        assertFalse(engine.isOver)
    }

    @Test
    fun `addScoreRight increments right score`() {
        val engine = BoutEngine(config5())
        engine.addScoreRight()
        assertEquals(0, engine.leftFencer.score)
        assertEquals(1, engine.rightFencer.score)
        assertFalse(engine.isOver)
    }

    // === Mode limit → isOver ===

    @Test
    fun `left scores reach mode 5 causes GameOver LEFT`() {
        val engine = BoutEngine(config5())
        repeat(4) { engine.addScoreLeft() }
        assertFalse(engine.isOver)

        val result = engine.addScoreLeft()
        assertTrue(result is ScoreResult.GameOver)
        assertEquals(FencerSide.LEFT, (result as ScoreResult.GameOver).winner)
        assertTrue(engine.isOver)
        assertEquals(5, engine.leftFencer.score)
        assertTrue(engine.leftFencer.isWinner)
        assertFalse(engine.rightFencer.isWinner)
    }

    @Test
    fun `right scores reach mode 5 causes GameOver RIGHT`() {
        val engine = BoutEngine(config5())
        repeat(4) { engine.addScoreRight() }
        val result = engine.addScoreRight()
        assertTrue(result is ScoreResult.GameOver)
        assertEquals(FencerSide.RIGHT, (result as ScoreResult.GameOver).winner)
        assertTrue(engine.isOver)
        assertTrue(engine.rightFencer.isWinner)
    }

    @Test
    fun `left scores reach mode 15 causes GameOver LEFT`() {
        val engine = BoutEngine(config15())
        repeat(14) { engine.addScoreLeft() }
        val result = engine.addScoreLeft()
        assertTrue(result is ScoreResult.GameOver)
        assertEquals(FencerSide.LEFT, (result as ScoreResult.GameOver).winner)
        assertTrue(engine.isOver)
    }

    @Test
    fun `scored result returned when score below mode`() {
        val engine = BoutEngine(config5())
        val result = engine.addScoreLeft()
        assertEquals(ScoreResult.Scored, result)
    }

    // === Double touch ===

    @Test
    fun `double touch allowed when not at mode-1 each`() {
        val engine = BoutEngine(config5())
        // 3:3 — оба не на mode-1 (mode-1=4)
        repeat(3) { engine.addScoreLeft() }
        repeat(3) { engine.addScoreRight() }

        val result = engine.addDoubleTouch()
        assertEquals(ScoreResult.Scored, result)
        assertEquals(4, engine.leftFencer.score)
        assertEquals(4, engine.rightFencer.score)
    }

    @Test
    fun `double touch NOT allowed when both at mode-1`() {
        val engine = BoutEngine(config5())
        // оба на 4 (mode-1 = 4)
        repeat(4) { engine.addScoreLeft() }
        repeat(4) { engine.addScoreRight() }

        val result = engine.addDoubleTouch()
        assertEquals(ScoreResult.DoubleNotAllowed, result)
        // Счёт не должен измениться
        assertEquals(4, engine.leftFencer.score)
        assertEquals(4, engine.rightFencer.score)
    }

    @Test
    fun `double touch allowed in PRIORITY section even at mode-1 each`() {
        // В PRIORITY секции double touch разрешён
        val engine = BoutEngine(config5())
        repeat(4) { engine.addScoreLeft() }
        repeat(4) { engine.addScoreRight() }

        // Переводим engine в PRIORITY секцию через endSection после равного счёта в финальном периоде
        // mode=5 → maxPeriods=1, поэтому handleFinalPeriodEnd при равном счёте → PRIORITY
        engine.endSection() // 4:4 — переход в PRIORITY
        engine.proceedToNextSection()

        assertEquals(SectionType.PRIORITY, engine.currentSection)
        // Теперь double touch должен быть разрешён
        val result = engine.addDoubleTouch()
        // В PRIORITY оба забивают → один из них выходит за mode → GameOver
        assertTrue(
            "В PRIORITY double touch должен быть разрешён (Scored или GameOver, но не DoubleNotAllowed)",
            result != ScoreResult.DoubleNotAllowed
        )
    }

    // === Sabre break-at-8 ===

    @Test
    fun `sabre break at 8 triggers BREAK section when left reaches 8 first`() {
        val engine = BoutEngine(config15Sabre())
        // Правый фехтовальщик на 3 (< 8)
        repeat(3) { engine.addScoreRight() }
        // Левый идёт к 8
        repeat(7) { engine.addScoreLeft() }
        assertEquals(SectionType.PERIOD, engine.currentSection)

        // 8-й очко левому → break-at-8
        engine.addScoreLeft()

        assertEquals(8, engine.leftFencer.score)
        assertEquals(SectionType.BREAK, engine.currentSection)
        assertFalse(engine.isOver)
    }

    @Test
    fun `sabre break at 8 triggers BREAK section when right reaches 8 first`() {
        val engine = BoutEngine(config15Sabre())
        repeat(3) { engine.addScoreLeft() }
        repeat(7) { engine.addScoreRight() }

        engine.addScoreRight()

        assertEquals(8, engine.rightFencer.score)
        assertEquals(SectionType.BREAK, engine.currentSection)
        assertFalse(engine.isOver)
    }

    @Test
    fun `sabre break at 8 does NOT trigger when both at 8`() {
        val engine = BoutEngine(config15Sabre())
        // Оба на 7
        repeat(7) { engine.addScoreLeft() }
        repeat(7) { engine.addScoreRight() }
        // Когда left добавляет очко до 8 → left.score=8, right.score=7 < 8 → break
        // Но если right тоже 8 — break не срабатывает для СЛЕДУЮЩЕГО
        // Сначала left до 8 → break
        val result = engine.addScoreLeft() // left=8, right=7 → break
        assertEquals(SectionType.BREAK, engine.currentSection)
        // Потом right до 8 (после proceedToNextSection) — проверяем что break at 8 не срабатывает,
        // когда left уже >= 8
        engine.proceedToNextSection()
        val result2 = engine.addScoreRight() // right=8, left=8 → NO break (left не < 8)
        // currentSection остаётся PERIOD (не BREAK снова от sabre rule)
        assertEquals(
            "Sabre break не должен срабатывать, если оба на 8",
            SectionType.PERIOD, engine.currentSection
        )
    }

    @Test
    fun `sabre break at 8 not triggered for foil epee`() {
        val engine = BoutEngine(config15()) // FOIL_EPEE
        repeat(7) { engine.addScoreLeft() }
        val sectionBefore = engine.currentSection
        engine.addScoreLeft() // left=8

        assertEquals("Для foil/epee секция не меняется на BREAK при score=8", sectionBefore, engine.currentSection)
    }

    // === Yellow card ===

    @Test
    fun `yellow card given to left fencer`() {
        val engine = BoutEngine(config5())
        val result = engine.giveYellowCard(FencerSide.LEFT)

        assertEquals(CardResult.CardGiven, result)
        assertTrue(engine.leftFencer.hasYellowCard)
        // Счёт не меняется при жёлтой карточке
        assertEquals(0, engine.leftFencer.score)
        assertEquals(0, engine.rightFencer.score)
    }

    @Test
    fun `yellow card given to right fencer`() {
        val engine = BoutEngine(config5())
        val result = engine.giveYellowCard(FencerSide.RIGHT)

        assertEquals(CardResult.CardGiven, result)
        assertTrue(engine.rightFencer.hasYellowCard)
        assertEquals(0, engine.leftFencer.score)
    }

    @Test
    fun `giving yellow card when already has card returns AlreadyHasCard`() {
        val engine = BoutEngine(config5())
        engine.giveYellowCard(FencerSide.LEFT)

        val result = engine.giveYellowCard(FencerSide.LEFT)
        assertEquals(CardResult.AlreadyHasCard, result)
    }

    // === Red card ===

    @Test
    fun `red card to left gives point to right`() {
        val engine = BoutEngine(config5())
        val result = engine.giveRedCard(FencerSide.LEFT)

        assertTrue(engine.leftFencer.hasRedCard)
        assertEquals(1, engine.rightFencer.score) // right получает очко
        assertEquals(0, engine.leftFencer.score)
        assertFalse(engine.isOver)
        assertTrue(result == CardResult.CardGiven)
    }

    @Test
    fun `red card to right gives point to left`() {
        val engine = BoutEngine(config5())
        val result = engine.giveRedCard(FencerSide.RIGHT)

        assertTrue(engine.rightFencer.hasRedCard)
        assertEquals(1, engine.leftFencer.score)
        assertFalse(engine.isOver)
    }

    @Test
    fun `red card to left causes GameOver RIGHT when right reaches mode`() {
        val engine = BoutEngine(config5())
        // Правый на 4 (mode-1)
        repeat(4) { engine.addScoreRight() }
        // Красная карточка левому → правый достигает 5 → конец
        val result = engine.giveRedCard(FencerSide.LEFT)

        assertTrue(result is CardResult.GameOver)
        assertEquals(FencerSide.RIGHT, (result as CardResult.GameOver).winner)
        assertTrue(engine.isOver)
        assertTrue(engine.rightFencer.isWinner)
    }

    @Test
    fun `red card to right causes GameOver LEFT when left reaches mode`() {
        val engine = BoutEngine(config5())
        repeat(4) { engine.addScoreLeft() }
        val result = engine.giveRedCard(FencerSide.RIGHT)

        assertTrue(result is CardResult.GameOver)
        assertEquals(FencerSide.LEFT, (result as CardResult.GameOver).winner)
        assertTrue(engine.isOver)
    }

    // === Undo: LeftScored ===

    @Test
    fun `undo LeftScored decrements left score`() {
        val engine = BoutEngine(config5())
        engine.addScoreLeft()
        engine.addScoreLeft()
        assertEquals(2, engine.leftFencer.score)

        val result = engine.undo()
        assertEquals(UndoResult.Undone, result)
        assertEquals(1, engine.leftFencer.score)
    }

    @Test
    fun `undo LeftScored restores isOver false after winning touch`() {
        val engine = BoutEngine(config5())
        repeat(4) { engine.addScoreLeft() }
        engine.addScoreLeft() // GameOver
        assertTrue(engine.isOver)

        engine.undo()
        assertFalse(engine.isOver)
        assertEquals(4, engine.leftFencer.score)
        assertFalse(engine.leftFencer.isWinner)
    }

    // === Undo: RightScored ===

    @Test
    fun `undo RightScored decrements right score`() {
        val engine = BoutEngine(config5())
        engine.addScoreRight()
        engine.undo()
        assertEquals(0, engine.rightFencer.score)
    }

    @Test
    fun `undo RightScored restores isOver false after winning touch`() {
        val engine = BoutEngine(config5())
        repeat(4) { engine.addScoreRight() }
        engine.addScoreRight() // GameOver
        assertTrue(engine.isOver)

        engine.undo()
        assertFalse(engine.isOver)
        assertEquals(4, engine.rightFencer.score)
    }

    // === Undo: BothScored ===

    @Test
    fun `undo BothScored decrements both scores`() {
        val engine = BoutEngine(config5())
        engine.addDoubleTouch()
        assertEquals(1, engine.leftFencer.score)
        assertEquals(1, engine.rightFencer.score)

        engine.undo()
        assertEquals(0, engine.leftFencer.score)
        assertEquals(0, engine.rightFencer.score)
    }

    @Test
    fun `undo BothScored restores isOver false`() {
        val engine = BoutEngine(config5())
        // 3:3 → double touch → оба 4:4 (< mode, не GameOver)
        // затем ещё 3:3 → double touch → оба 4:4 снова
        // Нужна ситуация где double touch даёт GameOver:
        // left и right оба на 4 — double touch заблокирован (DoubleNotAllowed).
        // Вместо этого: left=4, right=3 → double touch → left=5 → GameOver LEFT
        repeat(4) { engine.addScoreLeft() }
        repeat(3) { engine.addScoreRight() }
        // left=4, right=3 — double touch разрешён, left достигает 5 → GameOver
        val result = engine.addDoubleTouch()
        assertTrue("Ожидается GameOver при double touch с left=4", result is ScoreResult.GameOver)
        assertEquals(FencerSide.LEFT, (result as ScoreResult.GameOver).winner)
        assertTrue(engine.isOver)
        assertEquals(5, engine.leftFencer.score)

        engine.undo()
        assertFalse(engine.isOver)
        assertEquals(4, engine.leftFencer.score)
        assertEquals(3, engine.rightFencer.score)
    }

    // === Undo: LeftYellowCard ===

    @Test
    fun `undo LeftYellowCard removes yellow card from left`() {
        val engine = BoutEngine(config5())
        engine.giveYellowCard(FencerSide.LEFT)
        assertTrue(engine.leftFencer.hasYellowCard)

        engine.undo()
        assertFalse(engine.leftFencer.hasYellowCard)
    }

    // === Undo: RightYellowCard ===

    @Test
    fun `undo RightYellowCard removes yellow card from right`() {
        val engine = BoutEngine(config5())
        engine.giveYellowCard(FencerSide.RIGHT)
        engine.undo()
        assertFalse(engine.rightFencer.hasYellowCard)
    }

    // === Undo: LeftRedCard ===

    @Test
    fun `undo LeftRedCard removes red card and decrements right score`() {
        val engine = BoutEngine(config5())
        engine.giveRedCard(FencerSide.LEFT)
        assertEquals(1, engine.rightFencer.score)

        engine.undo()
        assertFalse(engine.leftFencer.hasRedCard)
        assertEquals(0, engine.rightFencer.score)
    }

    // === Undo: RightRedCard ===

    @Test
    fun `undo RightRedCard removes red card and decrements left score`() {
        val engine = BoutEngine(config5())
        engine.giveRedCard(FencerSide.RIGHT)
        assertEquals(1, engine.leftFencer.score)

        engine.undo()
        assertFalse(engine.rightFencer.hasRedCard)
        assertEquals(0, engine.leftFencer.score)
    }

    // === Undo: NothingToUndo ===

    @Test
    fun `undo with empty stack returns NothingToUndo`() {
        val engine = BoutEngine(config5())
        val result = engine.undo()
        assertEquals(UndoResult.NothingToUndo, result)
    }

    // === canUndo ===

    @Test
    fun `canUndo is true after action and false after undoing all`() {
        val engine = BoutEngine(config5())
        assertFalse(engine.canUndo)

        engine.addScoreLeft()
        assertTrue(engine.canUndo)

        engine.undo()
        assertFalse(engine.canUndo)
    }

    // === Undo: SectionSkipped ===

    @Test
    fun `undo SectionSkipped restores section and period`() {
        val engine = BoutEngine(config15()) // maxPeriods=3
        val sectionBefore = engine.currentSection
        val periodBefore = engine.periodNumber
        val timeBefore = engine.timeRemaining

        engine.skipSection()
        // После skipSection секция должна была измениться
        val result = engine.undo()
        assertEquals(UndoResult.Undone, result)
        assertEquals(sectionBefore, engine.currentSection)
        assertEquals(periodBefore, engine.periodNumber)
        assertEquals(timeBefore, engine.timeRemaining)
    }

    // === H4: BUG — undo красной карточки в завершённом бою ===

    @Test
    @Ignore("H4 - будет включён после фикса")
    fun `undo red card in finished bout should recompute isOver`() {
        // H4: после undo карты в завершённом бою статус isOver должен пересчитываться.
        // Текущий баг: undo красной карточки жёстко ставит isOver=false,
        // но не проверяет, остаётся ли bout завершённым по другим причинам.
        // Противоположный случай: если после undo красной карточки счёт опускается
        // ниже лимита и нет другого основания для победы — isOver должен стать false
        // (что уже делается), И победитель (isWinner) должен быть сброшен.
        //
        // Сценарий: mode=5, right на 5 очков от красной карточки (бой завершён).
        // Undo красной карточки → right на 4 → isOver должно стать false И isWinner сброшен.

        val engine = BoutEngine(config5())
        // Правый сначала набирает 4 очка честно
        repeat(4) { engine.addScoreRight() }
        // Красная карточка левому → правый получает 5 → GameOver
        engine.giveRedCard(FencerSide.LEFT)
        assertTrue("Бой должен быть завершён", engine.isOver)
        assertTrue("Right должен быть победителем", engine.rightFencer.isWinner)
        assertEquals(5, engine.rightFencer.score)

        // Undo красной карточки → правый возвращается к 4
        engine.undo()

        // ОЖИДАЕМОЕ корректное поведение:
        assertFalse("После undo isOver должен стать false (счёт ниже лимита)", engine.isOver)
        assertFalse("После undo right не должен быть победителем", engine.rightFencer.isWinner)
        assertEquals("После undo right score должен вернуться к 4", 4, engine.rightFencer.score)
    }

    // === resetAll ===

    @Test
    fun `resetAll resets all state to initial`() {
        val engine = BoutEngine(config5())
        repeat(3) { engine.addScoreLeft() }
        repeat(2) { engine.addScoreRight() }
        engine.giveYellowCard(FencerSide.LEFT)

        engine.resetAll()

        assertEquals(0, engine.leftFencer.score)
        assertEquals(0, engine.rightFencer.score)
        assertFalse(engine.leftFencer.hasYellowCard)
        assertFalse(engine.isOver)
        assertFalse(engine.canUndo)
        assertEquals(1, engine.periodNumber)
        assertEquals(SectionType.PERIOD, engine.currentSection)
    }

    // === Секции: endSection и proceedToNextSection ===

    @Test
    fun `endSection in mode5 at tie proceeds to PRIORITY`() {
        val engine = BoutEngine(config5()) // maxPeriods=1
        // mode=5, maxPeriods=1 → при равном счёте → PRIORITY
        val result = engine.endSection()
        // 0:0 — ничья → переход к PRIORITY
        assertTrue(result is SectionEndResult.ProceedToNext)
        val next = (result as SectionEndResult.ProceedToNext).nextSection
        assertEquals(SectionType.PRIORITY, next)
    }

    @Test
    fun `endSection in mode5 with left winning declares LEFT winner`() {
        val engine = BoutEngine(config5()) // maxPeriods=1
        repeat(3) { engine.addScoreLeft() }
        repeat(1) { engine.addScoreRight() }
        // 3:1 — левый ведёт → при finalPeriodEnd → WinnerByTouch LEFT
        val result = engine.endSection()
        assertTrue(result is SectionEndResult.WinnerByTouch)
        assertEquals(FencerSide.LEFT, (result as SectionEndResult.WinnerByTouch).winner)
        assertTrue(engine.isOver)
    }

    @Test
    fun `proceedToNextSection to PERIOD increments period number`() {
        val engine = BoutEngine(config15()) // maxPeriods=3
        engine.endSection() // период 1 → BREAK
        engine.proceedToNextSection() // начинаем BREAK
        engine.endSection() // BREAK → PERIOD
        engine.proceedToNextSection() // начинаем период 2

        assertEquals(2, engine.periodNumber)
        assertEquals(SectionType.PERIOD, engine.currentSection)
    }

    // === skipSection ===

    @Test
    fun `skipSection when not over returns Skipped`() {
        val engine = BoutEngine(config5())
        val result = engine.skipSection()
        assertTrue(result is SkipResult.Skipped)
    }

    @Test
    fun `skipSection when over returns CannotSkipPriority`() {
        val engine = BoutEngine(config5())
        repeat(5) { engine.addScoreLeft() }
        assertTrue(engine.isOver)

        val result = engine.skipSection()
        assertEquals(SkipResult.CannotSkipPriority, result)
    }

    // === Score не уходит ниже нуля при undo ===

    @Test
    fun `undo does not decrement score below zero`() {
        val engine = BoutEngine(config5())
        // Выдаём красную карточку правому → left получает 1
        engine.giveRedCard(FencerSide.RIGHT)
        assertEquals(1, engine.leftFencer.score)

        // Undo
        engine.undo()
        // FencerState.decrementScore() использует maxOf(0, score-1)
        assertEquals(0, engine.leftFencer.score)

        // Повторный undo не должен падать (стек пуст)
        val result = engine.undo()
        assertEquals(UndoResult.NothingToUndo, result)
    }
}
