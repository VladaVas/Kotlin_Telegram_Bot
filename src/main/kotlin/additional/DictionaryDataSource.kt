package org.example.additional

import java.io.File
import java.nio.charset.StandardCharsets
import java.sql.DriverManager
import java.sql.SQLException

private val CREATE_WORDS_TABLE = """
    CREATE TABLE IF NOT EXISTS words (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL DEFAULT 0,
        text VARCHAR NOT NULL,
        translate VARCHAR NOT NULL,
        correct_answers_count INTEGER NOT NULL DEFAULT 0,
        file_id VARCHAR,
        UNIQUE(user_id, text)
    );
""".trimIndent()

fun createWordsTable() {
    DriverManager.getConnection(DB_URL).use { connection ->
        connection.createStatement().use { statement ->
            statement.queryTimeout = 30
            statement.executeUpdate(CREATE_WORDS_TABLE)
        }
    }
}

private fun decodeUnicode(text: String): String {
    val regex = Regex("""\\u([0-9A-Fa-f]{4})""")
    return regex.replace(text) {
        val code = it.groupValues[1].toInt(16)
        code.toChar().toString()
    }
}

fun updateDictionary(wordsFile: File): Int {
    if (!wordsFile.exists()) return 0
    createWordsTable()
    val lines = wordsFile.readLines(StandardCharsets.UTF_8)
    var inserted = 0
    DriverManager.getConnection(DB_URL).use { connection ->
        connection.prepareStatement("INSERT OR IGNORE INTO words (user_id, text, translate) VALUES (0, ?, ?)").use { statement ->
            for (line in lines) {
                val parts = line.split(DICTIONARY_SEPARATOR)
                if (parts.size < 2) continue
                val word = decodeUnicode(parts[0].trim())
                val translate = decodeUnicode(parts[1].trim())
                if (word.isNotBlank() && translate.isNotBlank()) {
                    statement.setString(1, word)
                    statement.setString(2, translate)
                    statement.executeUpdate()
                    inserted++
                }
            }
        }
    }
    return inserted
}

fun main() {
    try {
        createWordsTable()
        val count = updateDictionary(File("word.txt"))
        println("Импортировано слов: $count")
    } catch (e: SQLException) {
        e.printStackTrace(System.err)
    }
}
