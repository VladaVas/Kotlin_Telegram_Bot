package org.example.additional

import java.io.File

data class Word(
    val word: String,
    val translation: String,
    var correctAnswersCount: Int = 0,
)

fun loadDictionary(): MutableList<Word> {
    val wordsFile: File = File("word.txt")
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
}

fun saveDictionary(dictionary: List<Word>) {
    val file = File("word.txt")
    file.printWriter().use { out ->
        dictionary.forEach { word ->
            out.println("${word.word}|${word.translation}|${word.correctAnswersCount}")
        }
    }
}