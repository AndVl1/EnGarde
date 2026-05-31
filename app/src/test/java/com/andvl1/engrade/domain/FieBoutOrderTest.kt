package com.andvl1.engrade.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit tests for FieBoutOrder — страховка перед рефакторингом.
 * Фиксирует текущее корректное поведение алгоритма Бергера.
 */
class FieBoutOrderTest {

    // === Количество боёв ===

    @Test
    fun `n5 should produce 10 bouts`() {
        val bouts = FieBoutOrder.getBoutOrder(5)
        assertEquals("N=5 должен давать C(5,2)=10 боёв", 10, bouts.size)
    }

    @Test
    fun `n6 should produce 15 bouts`() {
        val bouts = FieBoutOrder.getBoutOrder(6)
        assertEquals("N=6 должен давать C(6,2)=15 боёв", 15, bouts.size)
    }

    @Test
    fun `n7 should produce 21 bouts`() {
        val bouts = FieBoutOrder.getBoutOrder(7)
        assertEquals("N=7 должен давать C(7,2)=21 бой", 21, bouts.size)
    }

    @Test
    fun `n8 should produce 28 bouts`() {
        val bouts = FieBoutOrder.getBoutOrder(8)
        assertEquals("N=8 должен давать C(8,2)=28 боёв", 28, bouts.size)
    }

    // === Уникальность пар (неупорядоченных) ===

    @Test
    fun `n5 all pairs are unique as unordered pairs`() {
        val bouts = FieBoutOrder.getBoutOrder(5)
        val unorderedPairs = bouts.map { (a, b) -> setOf(a, b) }
        val unique = unorderedPairs.toSet()
        assertEquals("N=5: все пары должны быть уникальными (неупорядоченными)", bouts.size, unique.size)
    }

    @Test
    fun `n6 all pairs are unique as unordered pairs`() {
        val bouts = FieBoutOrder.getBoutOrder(6)
        val unorderedPairs = bouts.map { (a, b) -> setOf(a, b) }
        val unique = unorderedPairs.toSet()
        assertEquals("N=6: все пары должны быть уникальными", bouts.size, unique.size)
    }

    @Test
    fun `n7 all pairs are unique as unordered pairs`() {
        val bouts = FieBoutOrder.getBoutOrder(7)
        val unorderedPairs = bouts.map { (a, b) -> setOf(a, b) }
        val unique = unorderedPairs.toSet()
        assertEquals("N=7: все пары должны быть уникальными", bouts.size, unique.size)
    }

    @Test
    fun `n8 all pairs are unique as unordered pairs`() {
        val bouts = FieBoutOrder.getBoutOrder(8)
        val unorderedPairs = bouts.map { (a, b) -> setOf(a, b) }
        val unique = unorderedPairs.toSet()
        assertEquals("N=8: все пары должны быть уникальными", bouts.size, unique.size)
    }

    // === Полнота покрытия — все C(N,2) сочетаний присутствуют ===

    @Test
    fun `n5 contains all possible combinations`() {
        val bouts = FieBoutOrder.getBoutOrder(5)
        val actual = bouts.map { (a, b) -> setOf(a, b) }.toSet()
        val expected = allCombinations(5)
        assertEquals("N=5: должны присутствовать все сочетания", expected, actual)
    }

    @Test
    fun `n6 contains all possible combinations`() {
        val bouts = FieBoutOrder.getBoutOrder(6)
        val actual = bouts.map { (a, b) -> setOf(a, b) }.toSet()
        val expected = allCombinations(6)
        assertEquals("N=6: должны присутствовать все сочетания", expected, actual)
    }

    @Test
    fun `n7 contains all possible combinations`() {
        val bouts = FieBoutOrder.getBoutOrder(7)
        val actual = bouts.map { (a, b) -> setOf(a, b) }.toSet()
        val expected = allCombinations(7)
        assertEquals("N=7: должны присутствовать все сочетания", expected, actual)
    }

    @Test
    fun `n8 contains all possible combinations`() {
        val bouts = FieBoutOrder.getBoutOrder(8)
        val actual = bouts.map { (a, b) -> setOf(a, b) }.toSet()
        val expected = allCombinations(8)
        assertEquals("N=8: должны присутствовать все сочетания", expected, actual)
    }

    // === Нет фехтовальщиков вне диапазона 1..N ===

    @Test
    fun `n5 no fencer number exceeds n`() {
        val bouts = FieBoutOrder.getBoutOrder(5)
        bouts.forEach { (a, b) ->
            assertTrue("Номер $a вне диапазона 1..5", a in 1..5)
            assertTrue("Номер $b вне диапазона 1..5", b in 1..5)
        }
    }

    @Test
    fun `n6 no fencer number exceeds n`() {
        val bouts = FieBoutOrder.getBoutOrder(6)
        bouts.forEach { (a, b) ->
            assertTrue("Номер $a вне диапазона 1..6", a in 1..6)
            assertTrue("Номер $b вне диапазона 1..6", b in 1..6)
        }
    }

    @Test
    fun `n7 no fencer number exceeds n`() {
        val bouts = FieBoutOrder.getBoutOrder(7)
        bouts.forEach { (a, b) ->
            assertTrue("Номер $a вне диапазона 1..7", a in 1..7)
            assertTrue("Номер $b вне диапазона 1..7", b in 1..7)
        }
    }

    @Test
    fun `n8 no fencer number exceeds n`() {
        val bouts = FieBoutOrder.getBoutOrder(8)
        bouts.forEach { (a, b) ->
            assertTrue("Номер $a вне диапазона 1..8", a in 1..8)
            assertTrue("Номер $b вне диапазона 1..8", b in 1..8)
        }
    }

    // === Нет пары (x, x) ===

    @Test
    fun `no pair contains same fencer twice for all n`() {
        for (n in 5..8) {
            val bouts = FieBoutOrder.getBoutOrder(n)
            bouts.forEach { (a, b) ->
                assertTrue("N=$n: найдена пара ($a, $b) — фехтовальщик бьётся сам с собой", a != b)
            }
        }
    }

    // === Точный порядок (snapshot) ===

    @Test
    fun `n5 exact order matches FIE berger table snapshot`() {
        val expected = listOf(
            2 to 5, 3 to 4,
            1 to 5, 2 to 3,
            1 to 4, 5 to 3,
            1 to 3, 4 to 2,
            1 to 2, 4 to 5
        )
        val actual = FieBoutOrder.getBoutOrder(5)
        assertEquals("N=5: точный порядок боёв не совпадает с эталоном", expected, actual)
    }

    @Test
    fun `n8 exact order matches FIE berger table snapshot`() {
        val expected = listOf(
            1 to 8, 2 to 7, 3 to 6, 4 to 5,
            1 to 7, 8 to 6, 2 to 5, 3 to 4,
            1 to 6, 7 to 5, 8 to 4, 2 to 3,
            1 to 5, 6 to 4, 7 to 3, 8 to 2,
            1 to 4, 5 to 3, 6 to 2, 7 to 8,
            1 to 3, 4 to 2, 5 to 8, 6 to 7,
            1 to 2, 3 to 8, 4 to 7, 5 to 6
        )
        val actual = FieBoutOrder.getBoutOrder(8)
        assertEquals("N=8: точный порядок боёв не совпадает с эталоном", expected, actual)
    }

    // === Граничные случаи: IllegalArgumentException ===

    @Test
    fun `count less than 5 throws IllegalArgumentException`() {
        for (n in listOf(0, 1, 2, 3, 4)) {
            try {
                FieBoutOrder.getBoutOrder(n)
                fail("Ожидался IllegalArgumentException для count=$n")
            } catch (e: IllegalArgumentException) {
                // ожидаемо
            }
        }
    }

    @Test
    fun `count greater than 8 throws IllegalArgumentException`() {
        for (n in listOf(9, 10, 100)) {
            try {
                FieBoutOrder.getBoutOrder(n)
                fail("Ожидался IllegalArgumentException для count=$n")
            } catch (e: IllegalArgumentException) {
                // ожидаемо
            }
        }
    }

    // === Вспомогательные функции ===

    private fun allCombinations(n: Int): Set<Set<Int>> {
        val result = mutableSetOf<Set<Int>>()
        for (i in 1..n) {
            for (j in i + 1..n) {
                result.add(setOf(i, j))
            }
        }
        return result
    }
}
