package org.example.additional
const val CORRECTANSWERS = 3

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
                    val notLearnedList = dictionary.filter { it.correctAnswersCount < 3 }
                    if (notLearnedList.isEmpty()) {
                        println("Все слова в словаре выучены")
                        break
                    } else {
                        val questionWords: List<Word> = notLearnedList.take(4).shuffled()
                        val correctAnswer = questionWords.random()
                        val options = questionWords.map { it.translation }.shuffled()
                        println()
                        println("${correctAnswer.word}:")
                        options.forEachIndexed { index, transletion ->
                            println("${index + 1} - $transletion")
                        }
                        println("Введите номер правильного ответа: ")
                        val userInput = readln()
                        println("Ваш ответ: $userInput")
                    }
                }
            }
            "2" -> {
                println("Статистика:")
                val totalCount = dictionary.size
                println("Всего слов в словаре: $totalCount")
                val learnedWords = dictionary.filter { it.correctAnswersCount >= CORRECTANSWERS }
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