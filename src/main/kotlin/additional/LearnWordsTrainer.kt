package org.example.additional

data class Word(
    val word: String,
    val translation: String,
    var correctAnswersCount: Int = 0,
    var imagePath: String? = null,
    var fileId: String? = null,
)

data class Statistics(
    val learnedWords: Int,
    val totalCount: Int,
    val percent: Int,
)

data class Question(
    val questionWords: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer private constructor(
    private val userDictionary: IUserDictionary,
) {
    constructor(chatId: Long? = null) : this(
        if (chatId != null) DatabaseUserDictionary(chatId)
        else FileUserDictionary()
    )

    companion object {
        fun fromDictionaryFile(filePath: String) = LearnWordsTrainer(FileUserDictionary(filePath))
    }

    var question: Question? = null

    val dictionary: List<Word>
        get() = userDictionary.getAllWords()

    fun getStatistics(): Statistics {
        val totalCount = userDictionary.getSize()
        val learnedWords = userDictionary.getNumOfLearnedWords()
        val percent = if (totalCount > 0) (learnedWords * 100) / totalCount else 0
        return Statistics(learnedWords, totalCount, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = userDictionary.getUnlearnedWords()
        if (notLearnedList.isEmpty()) return null
        val correctAnswer = notLearnedList.random()
        val remainingNotLearned = notLearnedList.filter { it != correctAnswer }
        val neededCount = QUESTION_ANSWERS - 1
        val learnedWords = userDictionary.getLearnedWords().filter { it != correctAnswer }.shuffled()
        val additionalWords: List<Word> = if (remainingNotLearned.size >= neededCount) {
            remainingNotLearned.shuffled().take(neededCount)
        } else {
            (remainingNotLearned + learnedWords).take(neededCount)
        }
        val questionWords = (additionalWords + correctAnswer).shuffled()
        question = Question(questionWords = questionWords, correctAnswer = correctAnswer)
        return question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        val q = question ?: return false
        if (userAnswerIndex == null || q.questionWords.getOrNull(userAnswerIndex) != q.correctAnswer) return false
        val newCount = q.correctAnswer.correctAnswersCount + 1
        q.correctAnswer.correctAnswersCount = newCount
        userDictionary.setCorrectAnswersCount(q.correctAnswer.word, newCount)
        return true
    }

    fun resetProgress() {
        userDictionary.resetUserProgress()
    }

    fun save() {
        question?.correctAnswer?.fileId?.let { fileId ->
            userDictionary.updateWordFileId(question!!.correctAnswer.word, fileId)
        }
    }

    fun markWordAsLearned(wordText: String): Boolean {
        val word = userDictionary.getAllWords().find { it.word == wordText } ?: return false
        userDictionary.setCorrectAnswersCount(wordText, CORRECT_ANSWERS)
        word.correctAnswersCount = CORRECT_ANSWERS
        return true
    }

    fun resetWordProgress(wordText: String): Boolean {
        val word = userDictionary.getAllWords().find { it.word == wordText } ?: return false
        userDictionary.setCorrectAnswersCount(wordText, 0)
        word.correctAnswersCount = 0
        return true
    }

    fun addWordsFromFile(filePath: String) {
        userDictionary.addWordsFromFile(filePath)
    }

    fun getCorrectAnswersCount(word: String): Int = userDictionary.getCorrectAnswersCount(word)
}
