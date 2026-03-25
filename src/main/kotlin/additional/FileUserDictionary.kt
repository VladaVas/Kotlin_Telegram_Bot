package org.example.additional

import java.io.File
import java.nio.charset.StandardCharsets

private const val DEFAULT_FILE_NAME = "word.txt"

class FileUserDictionary(
    private val fileName: String = DEFAULT_FILE_NAME,
    private val learningThreshold: Int = CORRECT_ANSWERS,
) : IUserDictionary {

    private val dictionary: MutableList<Word> = try {
        loadDictionary().toMutableList()
    } catch (e: IllegalStateException) {
        throw e
    } catch (e: Exception) {
        throw IllegalArgumentException("Некорректный файл", e)
    }

    override fun getNumOfLearnedWords(): Int =
        dictionary.count { it.correctAnswersCount >= learningThreshold }

    override fun getSize(): Int = dictionary.size

    override fun getLearnedWords(): List<Word> =
        dictionary.filter { it.correctAnswersCount >= learningThreshold }

    override fun getUnlearnedWords(): List<Word> =
        dictionary.filter { it.correctAnswersCount < learningThreshold }

    override fun getAllWords(): List<Word> = dictionary.toList()

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        dictionary.find { it.word == word }?.correctAnswersCount = correctAnswersCount
        saveDictionary()
    }

    override fun resetUserProgress() {
        dictionary.forEach { it.correctAnswersCount = 0 }
        saveDictionary()
    }

    override fun clearDictionary() {
        dictionary.clear()
        saveDictionary()
    }

    override fun addWordsFromFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        val rawLines = file.readLines(StandardCharsets.UTF_8)
        val lines = rawLines.mapIndexed { index, line ->
            if (index == 0 && line.startsWith("\uFEFF")) line.drop(1) else line
        }
        for (line in lines) {
            val parts = line.split(DICTIONARY_SEPARATOR)
            if (parts.size < 2) continue
            val w = Word(
                word = decodeUnicode(parts[0].trim()),
                translation = decodeUnicode(parts[1].trim()),
                correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                imagePath = parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() },
                fileId = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }
            )
            if (w.word.isNotBlank() && w.translation.isNotBlank()) {
                dictionary.add(w)
            }
        }
        saveDictionary()
    }

    override fun getCorrectAnswersCount(word: String): Int =
        dictionary.find { it.word == word }?.correctAnswersCount ?: 0

    override fun updateWordFileId(word: String?, fileId: String) {
        dictionary.find { it.word == word }?.fileId = fileId
        saveDictionary()
    }

    private fun encodeUnicode(text: String): String =
        text.map { ch ->
            if (ch.code in 0..127) ch.toString() else "\\u%04X".format(ch.code)
        }.joinToString("")

    private fun loadDictionary(): List<Word> {
        val wordsFile = File(fileName)
        if (!wordsFile.exists()) {
            val copyFile = File(DEFAULT_FILE_NAME)
            if (copyFile.exists()) copyFile.copyTo(wordsFile)
            else return emptyList()
        }
        val result = mutableListOf<Word>()
        val lines = wordsFile.readLines(StandardCharsets.UTF_8)
        for (line in lines) {
            val parts = line.split(DICTIONARY_SEPARATOR)
            if (line.isNotBlank() && parts.size < 2) {
                throw IllegalStateException("Некорректный файл.\nНевозможно загрузить словарь.")
            }
            if (parts.size < 2) continue
            val w = Word(
                word = decodeUnicode(parts[0].trim()),
                translation = decodeUnicode(parts[1].trim()),
                correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                imagePath = parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() },
                fileId = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }
            )
            if (w.word.isNotBlank() && w.translation.isNotBlank()) {
                result.add(w)
            }
        }
        return result
    }

    private fun saveDictionary() {
        val file = File(fileName)
        file.printWriter(StandardCharsets.UTF_8).use { out ->
            dictionary.forEach { word ->
                out.println(
                    listOf(
                        encodeUnicode(word.word),
                        encodeUnicode(word.translation),
                        word.correctAnswersCount.toString(),
                        word.imagePath ?: "",
                        word.fileId ?: ""
                    ).joinToString(DICTIONARY_SEPARATOR)
                )
            }
        }
    }
}
