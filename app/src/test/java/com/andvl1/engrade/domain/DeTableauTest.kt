package com.andvl1.engrade.domain

import com.andvl1.engrade.domain.model.DeBracket
import com.andvl1.engrade.domain.model.DeSlot
import com.andvl1.engrade.domain.model.FencerRanking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DeTableau] — pure domain logic for FIE Direct Elimination.
 *
 * Соглашения:
 * - JUnit 4, assertEquals/assertTrue — как в FieBoutOrderTest и PoolEngineTest.
 * - Комментарии на русском для сценариев.
 * - [fakeFencers] создаёт N фехтовальщиков, где place == seedNumber (1..N).
 * - [simulateAllHigherSeedWins] разыгрывает турнир: всегда побеждает фехтовальщик с меньшим seed.
 */
class DeTableauTest {

    // =========================================================================
    // Tableau sizing
    // =========================================================================

    @Test
    fun `tableauSize N=2 returns 2`() {
        assertEquals("N=2 → T=2", 2, DeTableau.tableauSize(2))
    }

    @Test
    fun `tableauSize N=5 returns 8`() {
        assertEquals("N=5 → T=8", 8, DeTableau.tableauSize(5))
    }

    @Test
    fun `tableauSize N=6 returns 8`() {
        assertEquals("N=6 → T=8", 8, DeTableau.tableauSize(6))
    }

    @Test
    fun `tableauSize N=8 returns 8`() {
        assertEquals("N=8 → T=8", 8, DeTableau.tableauSize(8))
    }

    @Test
    fun `tableauSize N=9 returns 16`() {
        assertEquals("N=9 → T=16", 16, DeTableau.tableauSize(9))
    }

    @Test
    fun `tableauSize N=16 returns 16`() {
        assertEquals("N=16 → T=16", 16, DeTableau.tableauSize(16))
    }

    @Test
    fun `tableauSize N=64 returns 64`() {
        assertEquals("N=64 → T=64", 64, DeTableau.tableauSize(64))
    }

    @Test
    fun `tableauSize N=65 returns 128`() {
        assertEquals("N=65 → T=128", 128, DeTableau.tableauSize(65))
    }

    // =========================================================================
    // Seeding spread — T=8
    // =========================================================================

    @Test
    fun `T=8 seeding positions are correct canonical sequence`() {
        // Канонический результат рекурсивного алгоритма outer-bracket для T=8
        val expected = listOf(1, 8, 4, 5, 2, 7, 3, 6)
        assertEquals(expected, DeTableau.seedingPositions(8))
    }

    @Test
    fun `T=8 seeds 1 and 2 are in opposite halves`() {
        val positions = DeTableau.seedingPositions(8)
        val topHalf = positions.subList(0, 4).toSet()
        val bottomHalf = positions.subList(4, 8).toSet()
        assertTrue(
            "Seed 1 и 2 должны быть в разных половинах",
            (1 in topHalf && 2 in bottomHalf) || (1 in bottomHalf && 2 in topHalf)
        )
    }

    @Test
    fun `T=8 top 4 seeds in distinct quarters`() {
        val positions = DeTableau.seedingPositions(8)
        // Четверти: позиции [0..1], [2..3], [4..5], [6..7]
        val quarters = (0 until 4).map { q -> positions.subList(q * 2, q * 2 + 2).toSet() }
        for (seed in 1..4) {
            val inQuarters = quarters.count { seed in it }
            assertEquals("Seed $seed должен быть ровно в 1 четверти", 1, inQuarters)
        }
    }

    @Test
    fun `T=8 seeds 1 and 2 only meet in final via simulation`() {
        // Симуляция: побеждает всегда меньший seed → победитель seed1, вице-чемпион seed2
        val fencers = fakeFencers(8)
        val bracket = simulateAllHigherSeedWins(DeTableau.buildBracket(fencers))
        val classification = DeTableau.finalClassification(bracket)

        assertEquals("Победитель — seed 1", 1, classification.first { it.place == 1 }.seed)
        assertEquals("Финалист — seed 2", 2, classification.first { it.place == 2 }.seed)
    }

    @Test
    fun `T=8 seeds 1 2 3 4 never collide before their expected rounds`() {
        // Если seed i всегда побеждает seed j (i < j):
        // seed 1 встречает seed 2 только в финале (round 3)
        // seed 1 встречает seed 3/4 в полуфинале (round 2)
        val positions = DeTableau.seedingPositions(8)
        // Полуфиналисты (победители каждой половины):
        //   top half  = позиции 0..3 → seeds {1,8,4,5} — победитель seed 1
        //   bottom half = позиции 4..7 → seeds {2,7,3,6} — победитель seed 2
        val topHalfSeeds = positions.subList(0, 4).toSet()
        val bottomHalfSeeds = positions.subList(4, 8).toSet()
        assertTrue("Seed 1 в верхней или нижней половине", 1 in topHalfSeeds || 1 in bottomHalfSeeds)
        assertTrue("Seed 2 в противоположной половине от seed 1",
            (1 in topHalfSeeds && 2 in bottomHalfSeeds) ||
            (1 in bottomHalfSeeds && 2 in topHalfSeeds)
        )
        // Seed 3 и 4 тоже в противоположных частях
        val q1 = positions.subList(0, 2).toSet()
        val q2 = positions.subList(2, 4).toSet()
        val q3 = positions.subList(4, 6).toSet()
        val q4 = positions.subList(6, 8).toSet()
        val quartersForTopSeeds = listOf(q1, q2, q3, q4)
            .mapIndexed { i, q -> i to (1..4).filter { it in q } }
        // Каждая четверть содержит ровно один из топ-4 seeds
        quartersForTopSeeds.forEach { (_, seeds) ->
            assertEquals("Каждая четверть должна содержать ровно 1 топ-seed", 1, seeds.size)
        }
    }

    // =========================================================================
    // Seeding spread — T=16
    // =========================================================================

    @Test
    fun `T=16 seeds 1 and 2 are in opposite halves`() {
        val positions = DeTableau.seedingPositions(16)
        val topHalf = positions.subList(0, 8).toSet()
        val bottomHalf = positions.subList(8, 16).toSet()
        assertTrue(
            "T=16: seed 1 и 2 в разных половинах",
            (1 in topHalf && 2 in bottomHalf) || (1 in bottomHalf && 2 in topHalf)
        )
    }

    @Test
    fun `T=16 top 4 seeds in distinct quarters`() {
        val positions = DeTableau.seedingPositions(16)
        val quarters = (0 until 4).map { q -> positions.subList(q * 4, q * 4 + 4).toSet() }
        val occupied = quarters.map { q -> (1..4).count { it in q } }
        assertEquals("T=16: каждая четверть содержит ровно 1 из топ-4 seeds",
            listOf(1, 1, 1, 1), occupied)
    }

    @Test
    fun `T=16 top 8 seeds in distinct eighths`() {
        val positions = DeTableau.seedingPositions(16)
        val eighths = (0 until 8).map { e -> positions.subList(e * 2, e * 2 + 2).toSet() }
        val occupied = eighths.map { e -> (1..8).count { it in e } }
        assertEquals("T=16: каждая восьмушка содержит ровно 1 из топ-8 seeds",
            List(8) { 1 }, occupied)
    }

    @Test
    fun `T=16 seeds 1 and 2 meet only in final via simulation`() {
        val fencers = fakeFencers(16)
        val bracket = simulateAllHigherSeedWins(DeTableau.buildBracket(fencers))
        val classification = DeTableau.finalClassification(bracket)
        assertEquals(1, classification.first { it.place == 1 }.seed)
        assertEquals(2, classification.first { it.place == 2 }.seed)
    }

    // =========================================================================
    // Byes — количество и получатели
    // =========================================================================

    @Test
    fun `N=5 T=8 gives exactly 3 bye matches in round 1`() {
        val bracket = DeTableau.buildBracket(fakeFencers(5))
        val byeMatches = bracket.matches.filter { it.round == 1 && it.isBye }
        assertEquals("N=5: ровно 3 bye-матча", 3, byeMatches.size)
    }

    @Test
    fun `N=5 byes go to seeds 1 2 3`() {
        val bracket = DeTableau.buildBracket(fakeFencers(5))
        val byeWinnerSeeds = bracket.matches
            .filter { it.round == 1 && it.isBye }
            .mapNotNull { it.winner?.seed }
            .toSet()
        assertEquals("N=5: bye получают seeds 1, 2, 3", setOf(1, 2, 3), byeWinnerSeeds)
    }

    @Test
    fun `N=6 gives exactly 2 bye matches in round 1`() {
        val bracket = DeTableau.buildBracket(fakeFencers(6))
        val byeMatches = bracket.matches.filter { it.round == 1 && it.isBye }
        assertEquals("N=6: ровно 2 bye-матча", 2, byeMatches.size)
    }

    @Test
    fun `N=6 byes go to seeds 1 and 2`() {
        val bracket = DeTableau.buildBracket(fakeFencers(6))
        val byeWinnerSeeds = bracket.matches
            .filter { it.round == 1 && it.isBye }
            .mapNotNull { it.winner?.seed }
            .toSet()
        assertEquals("N=6: bye получают seeds 1, 2", setOf(1, 2), byeWinnerSeeds)
    }

    @Test
    fun `N=8 gives no bye matches`() {
        val bracket = DeTableau.buildBracket(fakeFencers(8))
        assertEquals("N=8: 0 bye-матчей", 0, bracket.matches.count { it.isBye })
    }

    @Test
    fun `N=5 bye winners propagated into round-2 slots`() {
        // Seed 1, 2, 3 получают bye → должны быть заполнены в слотах round 2
        val bracket = DeTableau.buildBracket(fakeFencers(5))
        val r2Seeds = bracket.matches
            .filter { it.round == 2 }
            .flatMap { m ->
                listOfNotNull(
                    (m.topSlot as? DeSlot.Fencer)?.seed,
                    (m.bottomSlot as? DeSlot.Fencer)?.seed
                )
            }
            .toSet()
        assertTrue("Seed 1 должен быть в round-2 после bye", 1 in r2Seeds)
        assertTrue("Seed 2 должен быть в round-2 после bye", 2 in r2Seeds)
        assertTrue("Seed 3 должен быть в round-2 после bye", 3 in r2Seeds)
    }

    // =========================================================================
    // Edge case: N=2 (только финал)
    // =========================================================================

    @Test
    fun `N=2 bracket has exactly 1 match`() {
        val bracket = DeTableau.buildBracket(fakeFencers(2))
        assertEquals("N=2: 1 матч (финал)", 1, bracket.matches.size)
    }

    @Test
    fun `N=2 bracket has no byes`() {
        val bracket = DeTableau.buildBracket(fakeFencers(2))
        assertEquals("N=2: нет bye", 0, bracket.matches.count { it.isBye })
    }

    @Test
    fun `N=2 match has seeds 1 and 2 in top and bottom slots`() {
        val bracket = DeTableau.buildBracket(fakeFencers(2))
        val match = bracket.matches.first()
        val topSeed = (match.topSlot as? DeSlot.Fencer)?.seed
        val bottomSeed = (match.bottomSlot as? DeSlot.Fencer)?.seed
        assertEquals("N=2: topSlot = seed 1", 1, topSeed)
        assertEquals("N=2: bottomSlot = seed 2", 2, bottomSeed)
    }

    @Test
    fun `N=2 recording winner completes bracket`() {
        val bracket = DeTableau.buildBracket(fakeFencers(2))
        val match = bracket.matches.first()
        val seed1 = match.topSlot as DeSlot.Fencer

        val completed = DeTableau.recordWinner(bracket, match.id, seed1)

        assertTrue("N=2: bracket должен быть complete", completed.isComplete)
    }

    @Test
    fun `N=2 final classification has 1st and 2nd places`() {
        val bracket = DeTableau.buildBracket(fakeFencers(2))
        val match = bracket.matches.first()
        val seed1 = match.topSlot as DeSlot.Fencer
        val completed = DeTableau.recordWinner(bracket, match.id, seed1)

        val classification = DeTableau.finalClassification(completed)
        assertEquals("N=2: seed 1 на 1-м месте", 1, classification.first { it.place == 1 }.seed)
        assertEquals("N=2: seed 2 на 2-м месте", 2, classification.first { it.place == 2 }.seed)
    }

    // =========================================================================
    // Edge case: N — степень двойки (no byes)
    // =========================================================================

    @Test
    fun `power-of-two N gives no byes`() {
        for (n in listOf(2, 4, 8, 16, 32)) {
            val bracket = DeTableau.buildBracket(fakeFencers(n))
            assertEquals("N=$n (степень 2): нет bye", 0, bracket.matches.count { it.isBye })
        }
    }

    // =========================================================================
    // Progression: N=6, высший seed всегда побеждает
    // =========================================================================

    @Test
    fun `N=6 higher-seed-always-wins produces ranking order 1 through 6`() {
        val bracket = simulateAllHigherSeedWins(DeTableau.buildBracket(fakeFencers(6)))
        val classification = DeTableau.finalClassification(bracket)

        assertEquals("N=6: победитель — seed 1", 1, classification.first { it.place == 1 }.seed)
        assertEquals("N=6: финалист — seed 2", 2, classification.first { it.place == 2 }.seed)
        // seeds 3 и 4 делят 3-е место
        val thirdSeeds = classification.filter { it.place == 3 }.map { it.seed }.toSet()
        assertEquals("N=6: seeds 3 и 4 делят 3-е место (полуфинал)", setOf(3, 4), thirdSeeds)
        // seed 5 получает 5-е место (единственный проигравший в 1 раунде)
        val fifthSeeds = classification.filter { it.place == 5 }.map { it.seed }
        assertTrue("N=6: seed 5 на 5-м месте", 5 in fifthSeeds)
    }

    @Test
    fun `N=6 one upset is reflected in classification`() {
        // Seed 5 побеждает seed 4 в 1 раунде (расстройство)
        var bracket = DeTableau.buildBracket(fakeFencers(6))

        // Найти реальный матч 1 раунда, где участвуют seeds 4 и 5
        val r1Real = bracket.matches.filter { it.round == 1 && !it.isBye }
        assertEquals("N=6: должно быть 2 реальных матча в 1 раунде", 2, r1Real.size)

        for (match in r1Real) {
            val top = match.topSlot as? DeSlot.Fencer ?: continue
            val bottom = match.bottomSlot as? DeSlot.Fencer ?: continue
            // Если матч 4 vs 5 — seed 5 побеждает (upset); иначе — меньший seed
            val winner = if ((top.seed == 4 && bottom.seed == 5) ||
                             (top.seed == 5 && bottom.seed == 4)) {
                if (top.seed == 5) top else bottom
            } else {
                if (top.seed < bottom.seed) top else bottom
            }
            bracket = DeTableau.recordWinner(bracket, match.id, winner)
        }

        // Продолжаем турнир: меньший seed побеждает
        bracket = simulateAllHigherSeedWins(bracket)
        val classification = DeTableau.finalClassification(bracket)

        // Seed 4 выбыл в 1 раунде → должен быть на 5-м месте или ниже
        val seed4Result = classification.first { it.seed == 4 }
        assertTrue("Seed 4 (проиграл в R1) должен быть на 5-м месте или ниже",
            seed4Result.place >= 5)
        assertEquals("Seed 4 выбыл в round 1", 1, seed4Result.eliminatedInRound)

        // Seed 5 (победил seed 4) дошёл дальше
        val seed5Result = classification.first { it.seed == 5 }
        assertTrue("Seed 5 (upset) должен быть выше seed 4",
            seed5Result.place < seed4Result.place)
    }

    // =========================================================================
    // Classification rules
    // =========================================================================

    @Test
    fun `semifinal losers share 3rd place for N=8`() {
        val bracket = simulateAllHigherSeedWins(DeTableau.buildBracket(fakeFencers(8)))
        val thirdPlaces = DeTableau.finalClassification(bracket).filter { it.place == 3 }
        assertEquals("N=8: ровно 2 фехтовальщика на 3-м месте (полуфиналисты)", 2, thirdPlaces.size)
    }

    @Test
    fun `quarterfinal losers share 5th place for N=8`() {
        val bracket = simulateAllHigherSeedWins(DeTableau.buildBracket(fakeFencers(8)))
        val fifthPlaces = DeTableau.finalClassification(bracket).filter { it.place == 5 }
        assertEquals("N=8: ровно 4 фехтовальщика на 5-м месте (четвертьфиналисты)", 4, fifthPlaces.size)
    }

    @Test
    fun `within shared place ordering is by seed ascending`() {
        val bracket = simulateAllHigherSeedWins(DeTableau.buildBracket(fakeFencers(8)))
        val classification = DeTableau.finalClassification(bracket)

        val thirdPlaces = classification.filter { it.place == 3 }
        assertEquals("3-е место: сортировка по seed", thirdPlaces.sortedBy { it.seed }, thirdPlaces)

        val fifthPlaces = classification.filter { it.place == 5 }
        assertEquals("5-е место: сортировка по seed", fifthPlaces.sortedBy { it.seed }, fifthPlaces)
    }

    @Test
    fun `no 4th place — it jumps from 3rd to 5th`() {
        val bracket = simulateAllHigherSeedWins(DeTableau.buildBracket(fakeFencers(8)))
        val classification = DeTableau.finalClassification(bracket)
        val places = classification.map { it.place }.toSortedSet()
        assertTrue("4-е место не существует по правилам ФИЭ", 4 !in places)
    }

    @Test
    fun `complete N=8 tournament has 8 classified fencers`() {
        val bracket = simulateAllHigherSeedWins(DeTableau.buildBracket(fakeFencers(8)))
        val classification = DeTableau.finalClassification(bracket)
        assertEquals("N=8: 8 фехтовальщиков в классификации", 8, classification.size)
    }

    @Test
    fun `complete N=5 tournament has 5 classified fencers`() {
        val bracket = simulateAllHigherSeedWins(DeTableau.buildBracket(fakeFencers(5)))
        val classification = DeTableau.finalClassification(bracket)
        assertEquals("N=5: 5 фехтовальщиков в классификации", 5, classification.size)
    }

    // =========================================================================
    // Bye winner loses in round 2
    // =========================================================================

    @Test
    fun `bye winner losing in round 2 gets semifinal placement (3rd for N=5 T=8)`() {
        // N=5, T=8: seeds 1,2,3 получают bye; реальный матч в R1: seed 4 vs seed 5
        // Сценарий: seed 4 побеждает seed 5 в R1
        //           seed 4 побеждает seed 1 в R2 (полуфинал) — upset
        //           seed 1 (получивший bye) выбывает в R2 → 3-е место
        var bracket = DeTableau.buildBracket(fakeFencers(5))

        // R1 реальный матч: пусть побеждает seed 4
        val r1Real = bracket.matches.filter { it.round == 1 && !it.isBye }
        assertEquals(1, r1Real.size)
        val r1Match = r1Real.first()
        val seed4 = listOf(r1Match.topSlot, r1Match.bottomSlot)
            .filterIsInstance<DeSlot.Fencer>()
            .first { it.seed == 4 }
        bracket = DeTableau.recordWinner(bracket, r1Match.id, seed4)

        // R2: найти матч, где участвует seed 1 (получивший bye)
        val r2WithSeed1 = bracket.matches.filter { it.round == 2 }.first { m ->
            (m.topSlot as? DeSlot.Fencer)?.seed == 1 ||
                (m.bottomSlot as? DeSlot.Fencer)?.seed == 1
        }

        // Победа seed 4 над seed 1 (если seed 4 попал в этот матч)
        val seed4InR2 = listOf(r2WithSeed1.topSlot, r2WithSeed1.bottomSlot)
            .filterIsInstance<DeSlot.Fencer>()
            .firstOrNull { it.seed == 4 }

        if (seed4InR2 != null) {
            // Seed 4 faces seed 1 — seed 4 wins (upset)
            bracket = DeTableau.recordWinner(bracket, r2WithSeed1.id, seed4InR2)

            val seed1Result = DeTableau.finalClassification(bracket).firstOrNull { it.seed == 1 }
            assertNotNull("Seed 1 должен присутствовать в классификации", seed1Result)
            // Для T=8: R2 = полуфинал → 3-е место
            assertEquals("Seed 1 (bye, проиграл в полуфинале) → 3-е место", 3, seed1Result!!.place)
            assertEquals("Seed 1 выбыл в round 2", 2, seed1Result.eliminatedInRound)
        } else {
            // Seed 4 and seed 1 are in different semi matches — just simulate normally
            bracket = simulateAllHigherSeedWins(bracket)
            val seed1Result = DeTableau.finalClassification(bracket).first { it.seed == 1 }
            assertEquals("Seed 1 (bye) должен финишировать минимум на 1-м месте", 1, seed1Result.place)
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Создаёт N фиктивных фехтовальщиков с place=seedNumber=1..N.
     * Статистика не важна для DE-логики; главное — корректные place (DE-seed).
     */
    private fun fakeFencers(n: Int): List<FencerRanking> = (1..n).map { seed ->
        FencerRanking(
            seedNumber = seed,
            name = "Fencer$seed",
            victories = n - seed,
            matches = n - 1,
            vmPercent = if (n > 1) (n - seed).toDouble() / (n - 1) * 100.0 else 0.0,
            touchesDelivered = (n - seed) * 5,
            touchesReceived = (seed - 1) * 5,
            index = (n - seed) * 5 - (seed - 1) * 5,
            place = seed
        )
    }

    /**
     * Симулирует завершение турнира: в каждом нерешённом матче побеждает
     * фехтовальщик с меньшим номером seed (= лучший по пулу).
     *
     * Рестартует цикл при каждом изменении, т.к. [DeBracket] иммутабельный.
     */
    private fun simulateAllHigherSeedWins(initial: DeBracket): DeBracket {
        var bracket = initial
        var progress = true
        while (progress) {
            progress = false
            for (match in bracket.matches) {
                if (match.winner != null || match.isBye) continue
                val top = match.topSlot as? DeSlot.Fencer ?: continue
                val bottom = match.bottomSlot as? DeSlot.Fencer ?: continue
                val winner = if (top.seed < bottom.seed) top else bottom
                bracket = DeTableau.recordWinner(bracket, match.id, winner)
                progress = true
                break   // перезапустить цикл — список изменился
            }
        }
        return bracket
    }
}
