package org.example.additional

interface IUserDictionary {
    fun getNumOfLearnedWords(): Int
    fun getSize(): Int
    fun getLearnedWords(): List<Word>
    fun getUnlearnedWords(): List<Word>
    fun getAllWords(): List<Word>
    fun setCorrectAnswersCount(word: String, correctAnswersCount: Int)
    fun resetUserProgress()
    fun clearDictionary()
    fun addWordsFromFile(filePath: String)
    fun getCorrectAnswersCount(word: String): Int
    fun updateWordFileId(word: String, fileId: String)
}
