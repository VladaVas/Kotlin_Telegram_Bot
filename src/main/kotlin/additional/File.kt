package org.example.additional

const val CORRECT_ANSWERS = 3
const val QUESTION_ANSWERS = 4

fun main() {
    val dictionary = loadDictionary()
    println("Загружено слов: ${dictionary.size}")

    while (true) {
        println()
        println("Меню: ")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выход")
        println("Ваш выбор: ")

        val input = readln()

        when (input) {
            "1" -> {
                println("Учить слова:")

                while (true) {

                    val notLearnedList = dictionary.filter { it.correctAnswersCount < CORRECT_ANSWERS }

                    if (notLearnedList.isEmpty()) {
                        println("Все слова в словаре выучены")
                        break
                    }

                    val correctAnswer = notLearnedList.random()
                    val remainingNotLearned = notLearnedList.filter { it != correctAnswer }

                    val additionalWords: List<Word> = if (remainingNotLearned.size >= QUESTION_ANSWERS - 1) {
                        remainingNotLearned.shuffled()
                            .take(QUESTION_ANSWERS - 1)
                    } else {
                        val learnedWords = dictionary.filter { it !in notLearnedList && it != correctAnswer }
                            .shuffled()
                        (remainingNotLearned + learnedWords).take(QUESTION_ANSWERS - 1)
                    }

                    val questionWords = (additionalWords + correctAnswer).shuffled()
                    val correctAnswerId: Int = questionWords.indexOf(correctAnswer)

                    println()
                    println("${correctAnswer.word}:")
                    questionWords.forEachIndexed { index, translation ->
                        println("${index + 1} - ${translation.translation}")
                    }
                    println("-----------")
                    println("0 - Меню")
                    println()
                    println("Введите номер правильного ответа: ")

                    val userAnswerInput = readln()
                    if (userAnswerInput == "0") break

                    val userAnswer = userAnswerInput.toIntOrNull()
                    if (userAnswer == null || userAnswer !in 1..QUESTION_ANSWERS) {
                        println("Введите число от 1 до $QUESTION_ANSWERS")
                        continue
                    }

                    if (userAnswer - 1 == correctAnswerId) {
                        println("Правильно!")
                        correctAnswer.correctAnswersCount++
                        saveDictionary(dictionary)
                    } else {
                        println("Неправильно! ${correctAnswer.word} – это ${correctAnswer.translation}")
                    }
                }
            }

            "2" -> {
                println("Статистика:")
                val totalCount = dictionary.size
                println("Всего слов в словаре: $totalCount")
                val learnedWords = dictionary.filter { it.correctAnswersCount >= CORRECT_ANSWERS }
                val learnedCount = learnedWords.size
                val percent = if (totalCount > 0) {
                    (learnedCount * 100) / totalCount
                } else 0
                println("Выучено $learnedCount из $totalCount слов | $percent%")
            }

            "0" -> {
                println("Выход из программы")
                break
            }

            else -> println("'Введите число 1, 2 или 0")
        }
    }
}