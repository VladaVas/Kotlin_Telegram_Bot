package org.example.additional

object InputValidator {
    private const val MAX_TEXT_LENGTH = 1000
    private const val MAX_CALLBACK_LENGTH = 256
    private val allowedTextPattern = Regex("^[^\\p{Cntrl}]{1,1000}$")
    private val allowedCallbackPattern = Regex("^[a-zA-Z0-9_\\-|]+$")

    fun validateUserText(name: String): String {
        val trimmed = name.trim()
        if (SecurityLogger.isSuspicious(trimmed)) {
            throw IllegalArgumentException("Подозрительный ввод в тексте сообщения")
        }
        if (!allowedTextPattern.matches(trimmed)) {
            throw IllegalArgumentException("Недопустимые символы в тексте сообщения")
        }
        if (trimmed.length > MAX_TEXT_LENGTH) {
            throw IllegalArgumentException("Слишком длинный текст сообщения")
        }
        return trimmed
    }

    fun validateCallbackData(data: String): String {
        val trimmed = data.trim()
        if (SecurityLogger.isSuspicious(trimmed)) {
            throw IllegalArgumentException("Подозрительный ввод в callback data")
        }
        if (!allowedCallbackPattern.matches(trimmed)) {
            throw IllegalArgumentException("Недопустимые символы в callback data")
        }
        if (trimmed.length > MAX_CALLBACK_LENGTH) {
            throw IllegalArgumentException("Слишком длинные данные callback")
        }
        return trimmed
    }
}

