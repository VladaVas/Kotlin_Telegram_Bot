package org.example.additional

import java.io.File
import java.nio.charset.StandardCharsets
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseUserDictionary(
    private val userId: Long,
    private val learningThreshold: Int = CORRECT_ANSWERS,
) : IUserDictionary {

    init {
        try {
            createWordsTable()
            ensureUserExists()
            ensureUserDictionary()
        } catch (e: Exception) {
            println("INIT ERROR:")
            e.printStackTrace()
            throw e
        }
    }

    override fun getNumOfLearnedWords(): Int =
        safeDbCall {
            queryInt(
                "SELECT COUNT(*) FROM words WHERE user_id = ? AND correct_answers_count >= ?",
                userId, learningThreshold
            )
        }

    override fun getSize(): Int =
        safeDbCall {
            queryInt("SELECT COUNT(*) FROM words WHERE user_id = ?", userId)
        }

    override fun getLearnedWords(): List<Word> =
        safeDbCall {
            queryWords(
                "SELECT text, translate, correct_answers_count, file_id FROM words WHERE user_id = ? AND correct_answers_count >= ?",
                userId, learningThreshold
            )
        }

    override fun getUnlearnedWords(): List<Word> =
        safeDbCall {
            queryWords(
                "SELECT text, translate, correct_answers_count, file_id FROM words WHERE user_id = ? AND correct_answers_count < ?",
                userId, learningThreshold
            )
        }

    override fun getAllWords(): List<Word> =
        safeDbCall {
            queryWords(
                "SELECT text, translate, correct_answers_count, file_id FROM words WHERE user_id = ? ORDER BY id",
                userId
            )
        }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        safeDbCall {
            DriverManager.getConnection(DB_URL).use { connection ->
                connection.prepareStatement(
                    "UPDATE words SET correct_answers_count = ? WHERE user_id = ? AND text = ?"
                ).use { stmt ->
                    stmt.setInt(1, correctAnswersCount)
                    stmt.setLong(2, userId)
                    stmt.setString(3, word)
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun resetUserProgress() {
        safeDbCall {
            DriverManager.getConnection(DB_URL).use { connection ->
                connection.prepareStatement(
                    "UPDATE words SET correct_answers_count = 0 WHERE user_id = ?"
                ).use { stmt ->
                    stmt.setLong(1, userId)
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun clearDictionary() {
        safeDbCall {
            DriverManager.getConnection(DB_URL).use { connection ->
                connection.prepareStatement(
                    "DELETE FROM words WHERE user_id = ?"
                ).use { stmt ->
                    stmt.setLong(1, userId)
                    stmt.executeUpdate()
                }
            }
        }
    }

    override fun addWordsFromFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val lines = file.readLines(StandardCharsets.UTF_8)

        safeDbCall {
            DriverManager.getConnection(DB_URL).use { connection ->
                connection.autoCommit = false

                connection.prepareStatement(
                    "INSERT OR IGNORE INTO words (user_id, text, translate, correct_answers_count) VALUES (?, ?, ?, 0)"
                ).use { stmt ->
                    for (line in lines) {
                        val parts = line.split(DICTIONARY_SEPARATOR)
                        if (parts.size < 2) continue

                        val word = decodeUnicode(parts[0].trim())
                        val translate = decodeUnicode(parts[1].trim())

                        if (word.isNotBlank() && translate.isNotBlank()) {
                            stmt.setLong(1, userId)
                            stmt.setString(2, word)
                            stmt.setString(3, translate)
                            stmt.addBatch()
                        }
                    }
                    stmt.executeBatch()
                }

                connection.commit()
            }
        }
    }

    override fun getCorrectAnswersCount(word: String): Int =
        safeDbCall {
            queryInt(
                "SELECT correct_answers_count FROM words WHERE user_id = ? AND text = ?",
                userId, word
            ).let { if (it >= 0) it else 0 }
        }

    override fun updateWordFileId(word: String?, fileId: String) {
        if (word == null) return

        safeDbCall {
            DriverManager.getConnection(DB_URL).use { connection ->
                connection.prepareStatement(
                    "UPDATE words SET file_id = ? WHERE user_id = ? AND text = ?"
                ).use { stmt ->
                    stmt.setString(1, fileId)
                    stmt.setLong(2, userId)
                    stmt.setString(3, word)
                    stmt.executeUpdate()
                }
            }
        }
    }

    private fun ensureUserDictionary() {
        if (getSize() > 0) return

        println("Initializing dictionary for user: $userId")
        val defaultDictionary = File(DEFAULT_DICTIONARY_FILE)
        if (defaultDictionary.exists() && defaultDictionary.isFile) {
            addWordsFromFile(defaultDictionary.absolutePath)
            println("Dictionary initialized for user: $userId")
        } else {
            System.err.println(
                "Default dictionary file not found at path: ${defaultDictionary.absolutePath}. " +
                    "User $userId starts with an empty dictionary."
            )
        }
    }

    private fun ensureUserExists() {
        safeDbCall {
            DriverManager.getConnection(DB_URL).use { connection ->
                connection.prepareStatement(
                    "INSERT OR IGNORE INTO users (chat_id) VALUES (?)"
                ).use { stmt ->
                    stmt.setLong(1, userId)
                    stmt.executeUpdate()
                }
            }
        }
    }

    private fun queryInt(sql: String, vararg params: Any): Int {
        DriverManager.getConnection(DB_URL).use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { i, p ->
                    when (p) {
                        is Long -> stmt.setLong(i + 1, p)
                        is Int -> stmt.setInt(i + 1, p)
                        is String -> stmt.setString(i + 1, p)
                    }
                }
                val rs = stmt.executeQuery()
                return if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    private fun queryWords(sql: String, vararg params: Any): List<Word> {
        DriverManager.getConnection(DB_URL).use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { i, p ->
                    when (p) {
                        is Long -> stmt.setLong(i + 1, p)
                        is Int -> stmt.setInt(i + 1, p)
                        is String -> stmt.setString(i + 1, p)
                    }
                }
                val rs = stmt.executeQuery()
                val list = mutableListOf<Word>()

                while (rs.next()) {
                    list.add(
                        Word(
                            word = rs.getString("text"),
                            translation = rs.getString("translate"),
                            correctAnswersCount = rs.getInt("correct_answers_count"),
                            imagePath = null,
                            fileId = rs.getString("file_id")?.takeIf { it.isNotBlank() }
                        )
                    )
                }
                return list
            }
        }
    }

    private fun <T> safeDbCall(block: () -> T): T =
        try {
            block()
        } catch (e: SQLException) {
            println("DATABASE ERROR:")
            e.printStackTrace()
            throw RuntimeException(e)
        }
}