package org.example.additional

const val CORRECT_ANSWERS = 3
const val QUESTION_ANSWERS = 4

data class Word(
    val word: String,
    val translation: String,
    var correctAnswersCount: Int = 0,
)

fun printQuestion(question: Question) {
    println("${question.correctAnswer.word}:")
    question.questionWords.forEachIndexed { index, translation ->
        println("${index + 1} - ${translation.translation}")
    }
}

fun main() {
    val trainer = LearnWordsTrainer()
    println("Загружено слов: ${trainer.dictionary.size}")


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
                    val question = trainer.getNextQuestion()

                    if (question == null) {
                        println("Все слова в словаре выучены")
                        break
                    } else {
                        printQuestion(question)
                    }

                    println("-----------")
                    println("0 - Меню")
                    println()
                    println("Ваш ответ: ")

                    val userAnswerInput = readln().toIntOrNull()
                    if (userAnswerInput == 0)
                        break

                    if (trainer.checkAnswer(userAnswerInput?.minus(1))) {
                        println("Правильно!")
                        println()
                    } else {
                        println("Неправильно! ${question.correctAnswer.word} – это ${question.correctAnswer.translation}")
                        println()
                    }
                }
            }

            "2" -> {
                println("Статистика:")
                val statistics = trainer.getStatistics()
                println("Выучено ${statistics.learnedWords} из ${statistics.totalCount} слов | ${statistics.percent}%")
            }

            "0" -> {
                println("Выход из программы")
                break
            }

            else -> println("Введите число 1, 2 или 0")
        }
    }
}