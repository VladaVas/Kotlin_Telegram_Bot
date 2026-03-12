package additional

import org.example.additional.InputValidator
import org.example.additional.SecurityLogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SecurityAndValidationTest {

    @Test
    @DisplayName("Корректный текст проходит без ошибок")
    fun `validateUserText should accept safe text`() {
        val input = "Привет, хочу выучить английские слова 123-ABC"
        val result = InputValidator.validateUserText(input)
        assertEquals(input.trim(), result)
    }

    @Test
    @DisplayName("SQL-инъекция вызывает IllegalArgumentException")
    fun `validateUserText should reject sql injection`() {
        val maliciousInputs = listOf(
            "'; DROP TABLE users; --",
            "admin' OR '1'='1",
            "' UNION SELECT * FROM users --",
            "test'; DELETE FROM orders WHERE 1=1; --"
        )

        maliciousInputs.forEach { payload ->
            assertThrows(IllegalArgumentException::class.java) {
                InputValidator.validateUserText(payload)
            }
        }
    }

    @Test
    @DisplayName("Корректные callback данные проходят без ошибок")
    fun `validateCallbackData should accept safe data`() {
        val input = "answer_1|word_mark_learned|reset-123"
        val result = InputValidator.validateCallbackData(input)
        assertEquals(input.trim(), result)
    }

    @Test
    @DisplayName("Подозрительные callback данные отклоняются")
    fun `validateCallbackData should reject suspicious data`() {
        val maliciousInputs = listOf(
            "answer_1'; DROP TABLE users; --",
            "reset|admin' OR '1'='1",
            "union|select|drop"
        )

        maliciousInputs.forEach { payload ->
            assertThrows(IllegalArgumentException::class.java) {
                InputValidator.validateCallbackData(payload)
            }
        }
    }

    @Test
    @DisplayName("SecurityLogger распознаёт типичные SQL-инъекции")
    fun `SecurityLogger should detect sql injection patterns`() {
        val maliciousInputs = listOf(
            "'; DROP TABLE users; --",
            "admin' OR '1'='1",
            "' UNION SELECT * FROM users --",
            "test'; DELETE FROM orders WHERE 1=1; --"
        )

        maliciousInputs.forEach { payload ->
            assertTrue(SecurityLogger.isSuspicious(payload), "Payload should be suspicious: $payload")
        }
    }

    @Test
    @DisplayName("SecurityLogger не помечает обычный текст как подозрительный")
    fun `SecurityLogger should not flag normal text`() {
        val safeInputs = listOf(
            "Привет, как дела?",
            "Хочу добавить новые слова в словарь",
            "reset_progress",
            "learn_word_click"
        )

        safeInputs.forEach { payload ->
            assertFalse(SecurityLogger.isSuspicious(payload), "Payload should NOT be suspicious: $payload")
        }
    }
}

