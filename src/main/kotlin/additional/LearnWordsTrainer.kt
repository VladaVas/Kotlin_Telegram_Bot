package org.example.additional

import java.io.File

data class Word(
    val word: String,
    val translation: String,
    var correctAnswersCount: Int = 0,
    var imagePath: String? = null,
    var fileId: String? = null,
)

data class Statistics(
    val learnedWords: Int,
    val totalCount: Int,
    val percent: Int,
)

data class Question(
    val questionWords: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer private constructor(
    private val dictionaryFileName: String,
) {
    constructor(chatId: Long? = null) : this("word_$chatId.txt")

    companion object {
        fun fromDictionaryFile(filePath: String) = LearnWordsTrainer(filePath)
    }

    var question: Question? = null
    val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val totalCount = dictionary.size

        val learnedWords = dictionary.filter { it.correctAnswersCount >= CORRECT_ANSWERS }.size
        val percent = if (totalCount > 0) {
            (learnedWords * 100) / totalCount
        } else 0

        return Statistics(learnedWords, totalCount, percent)

    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < CORRECT_ANSWERS }
        if (notLearnedList.isEmpty()) return null

        val correctAnswer = notLearnedList.random()
        val remainingNotLearned = notLearnedList.filter { it != correctAnswer }
        val neededCount = QUESTION_ANSWERS - 1

        val additionalWords: List<Word> = if (remainingNotLearned.size >= neededCount) {
            remainingNotLearned.shuffled().take(neededCount)
        } else {
            val learnedWords =
                dictionary.filter { it.correctAnswersCount >= CORRECT_ANSWERS && it != correctAnswer }.shuffled()
            (remainingNotLearned + learnedWords).take(neededCount)
        }
        val questionWords = (additionalWords + correctAnswer).shuffled()

        question = Question(
            questionWords = questionWords,
            correctAnswer = correctAnswer,
        )
        return question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        if (question == null || userAnswerIndex == null) return false
        val correctAnswerId: Int? = question?.questionWords?.indexOf(question?.correctAnswer)

        if (correctAnswerId == userAnswerIndex) {
            question?.correctAnswer?.correctAnswersCount++
            saveDictionary(dictionary)
            return true
        } else {
            return false
        }
    }

    fun resetProgress() {
        dictionary.forEach { it.correctAnswersCount = 0 }
        saveDictionary(dictionary)
    }

    fun save() {
        saveDictionary(dictionary)
    }

    fun addWordsFromFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split("|")
            if (parts.size < 2) continue
            val word = Word(
                word = parts[0].trim(),
                translation = parts[1].trim(),
                correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                imagePath = parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() },
                fileId = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }
            )
            if (word.word.isNotBlank() && word.translation.isNotBlank()) {
                dictionary.add(word)
            }
        }
        saveDictionary(dictionary)
    }

    private fun loadDictionary(): MutableList<Word> {
        try {
            val wordsFile = File(dictionaryFileName)

            if (!wordsFile.exists()) {
                val copyFile = File("word.txt")
                copyFile.copyTo(wordsFile)
            }

            val dictionary: MutableList<Word> = mutableListOf()
            val lines: List<String> = wordsFile.readLines()

            for (line in lines) {
                val parts = line.split("|")
                if (parts.size < 2) continue
                val word = Word(
                    word = parts[0].trim(),
                    translation = parts[1].trim(),
                    correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                    imagePath = parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() },
                    fileId = parts.getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }
                )
                if (word.word.isNotBlank() && word.translation.isNotBlank()) {
                    dictionary.add(word)
                }
            }
            return dictionary
        } catch (_: IndexOutOfBoundsException) {
            throw IllegalStateException("Некорректный файл.\nНевозможно загрузить словарь.")
        }
    }

    private fun saveDictionary(dictionary: List<Word>) {
        val file = File(dictionaryFileName)
        file.printWriter().use { out ->
            dictionary.forEach { word ->
                out.println(
                    "${word.word}|${word.translation}|${word.correctAnswersCount}|${word.imagePath ?: ""}|${word.fileId ?: ""}"
                )
            }
        }
    }
}