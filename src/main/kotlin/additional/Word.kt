package org.example.additional

data class Word(
    val word: String,
    val translation: String,
    val correctAnswersCount: Int? = 0,
) {
}