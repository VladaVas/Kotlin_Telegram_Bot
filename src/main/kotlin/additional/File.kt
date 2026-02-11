package org.example.additional

import java.io.File
// import java.util.Dictionary

fun main() {

    val wordsFile: File = File("word.txt")
    val dictionary: MutableList<Word> = mutableListOf()
    val lines: List<String> = wordsFile.readLines()

    for (line in lines) {
        val line = line.split("|")
        val word = Word(word = line[0], translation = line[1], correctAnswersCount = line.getOrNull(2)?.toIntOrNull() ?: 0)
        dictionary.add(word)
    }
    println(dictionary)

    // wordsFile.createNewFile()
    // wordsFile.writeText("hello - привет\n")
  //  wordsFile.appendText("dog - собака\n")
   // wordsFile.appendText("cat - кошка\n")

   // for (word in wordsFile.readLines()) {
     //   println(word)
    //}
}