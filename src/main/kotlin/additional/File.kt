package org.example.additional

data class Word(
    val word: String,
    val translation: String,
    var correctAnswersCount: Int = 0,
)

fun Question.asConsoleString() {
    println("${this.correctAnswer.word}:")
    this.questionWords.forEachIndexed { index, translation ->
        println("${index + 1} - ${translation.translation}")
    }
}

fun main() {
    val trainer = try {
        LearnWordsTrainer()
    } catch (e: Exception) {
        println("ERROR: ${e.message}")
        return
    }
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
                        question.asConsoleString()
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