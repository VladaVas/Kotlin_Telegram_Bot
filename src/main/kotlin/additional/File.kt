package org.example.additional

import java.io.File

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

        val input = readln();

        when (input) {
            "1" -> println("Учить слова")
            "2" -> println("Статистика")
            "0" -> {
                println("Выход из программы")
                break
            }

            else -> println("'Введите число 1, 2 или 0")
        }

    }
}