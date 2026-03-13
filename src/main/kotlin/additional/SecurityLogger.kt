package org.example.additional

object SecurityLogger {
    private val suspiciousSubstrings = listOf(
        "'", "\"", ";", "--", "/*", "*/",
        "union", "select", "drop", "delete", "insert", "update"
    )

    fun isSuspicious(input: String?): Boolean {
        if (input.isNullOrBlank()) return false
        val lower = input.lowercase()
        return suspiciousSubstrings.any { lower.contains(it) }
    }

    fun logSuspiciousActivity(context: String, input: String?) {
        if (!isSuspicious(input)) return
        val normalized = input.orEmpty()
            .replace("\\s+".toRegex(), " ")
            .take(500)
        println("WARN: Suspicious input detected in $context: \"$normalized\"")
    }
}

