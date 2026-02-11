package org.example.additional

import java.io.File

fun main() {

    val wordsFile: File = File("word.txt")
    wordsFile.createNewFile()
    wordsFile.writeText("hello - привет\n")
    wordsFile.appendText("dog - собака\n")
    wordsFile.appendText("cat - кошка\n")

    for (word in wordsFile.readLines()) {
        println(word)
    }
}