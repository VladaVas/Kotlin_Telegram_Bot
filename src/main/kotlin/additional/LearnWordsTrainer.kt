package org.example.additional

import java.io.File

data class Statistics(
    val learnedWords: Int,
    val totalCount: Int,
    val percent: Int,
)

data class Question(
    val questionWords: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer {

    private var question: Question? = null
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

    private fun loadDictionary(): MutableList<Word> {
        try {
            val wordsFile = File("word.txt")
            if (!wordsFile.exists()) return mutableListOf()
            val dictionary: MutableList<Word> = mutableListOf()
            val lines: List<String> = wordsFile.readLines()

            for (line in lines) {
                val parts = line.split("|")
                val word = Word(
                    word = parts[0],
                    translation = parts[1],
                    correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
                )
                dictionary.add(word)
            }
            return dictionary
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalStateException("Некорректный файл.\nНевозможно загрузить словарь.")
        }
    }

    private fun saveDictionary(dictionary: List<Word>) {
        val file = File("word.txt")
        file.printWriter().use { out ->
            dictionary.forEach { word ->
                out.println("${word.word}|${word.translation}|${word.correctAnswersCount}")
            }
        }
    }
}