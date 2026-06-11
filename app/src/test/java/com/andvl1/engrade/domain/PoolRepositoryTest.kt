package com.andvl1.engrade.domain

import com.andvl1.engrade.data.PoolRepository
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit tests for PoolRepository validation logic (F3).
 * Проверяет правило ФИЭ: ничья в бое запрещена.
 * Тесты не используют Room DB — проверяется только чистая валидационная логика.
 */
class PoolRepositoryTest {

    // === validateNoDraw ===

    @Test
    fun `validateNoDraw выбрасывает IllegalArgumentException при равных очках`() {
        assertThrows(IllegalArgumentException::class.java) {
            PoolRepository.validateNoDraw(4, 4)
        }
    }

    @Test
    fun `validateNoDraw выбрасывает при нулевых равных очках`() {
        assertThrows(IllegalArgumentException::class.java) {
            PoolRepository.validateNoDraw(0, 0)
        }
    }

    @Test
    fun `validateNoDraw не выбрасывает исключение при разных очках левый больше`() {
        // Не должно вызывать исключение
        PoolRepository.validateNoDraw(5, 3)
    }

    @Test
    fun `validateNoDraw не выбрасывает исключение при разных очках правый больше`() {
        PoolRepository.validateNoDraw(2, 5)
    }

    @Test
    fun `validateNoDraw сообщение содержит оба счёта`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            PoolRepository.validateNoDraw(3, 3)
        }
        val message = exception.message ?: ""
        assert(message.contains("3")) {
            "Сообщение об ошибке должно содержать счёт: $message"
        }
    }
}
