package org.example.additional

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
            "1" -> println("Учить слова.")
            "2" -> {
                println("Статистика:")
                val totalCount = dictionary.size
                println("Всего слов в словаре: $totalCount")
                val learnedWords = dictionary.filter { it.correctAnswersCount >= 3 }
                val learnedCount = learnedWords.size
                val percent = if (learnedCount > 0) {
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