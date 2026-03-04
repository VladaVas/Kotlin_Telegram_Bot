package additional

import org.example.additional.LearnWordsTrainer
import org.example.additional.Word
import org.example.additional.CORRECT_ANSWERS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class LearnWordsTrainerTest {

    private fun testFilePath(name: String): String {
        return "src/test/kotlin/$name"
    }

    @Test
    @DisplayName("getStatistics: 4 выученных из 7 слов")
    fun `test statistics with 4 words of 7`() {
        val trainer = LearnWordsTrainer.fromDictionaryFile(testFilePath("4_words_of_7.txt"))

        val statistics = trainer.getStatistics()

        assertEquals(4, statistics.learnedWords)
        assertEquals(7, statistics.totalCount)
        assertEquals(57, statistics.percent)
    }

    @Test
    @DisplayName("getStatistics: пустой словарь")
    fun `test statistics with empty dictionary`() {
        val emptyFile = File.createTempFile("empty_dictionary", ".txt").apply {
            writeText("")
            deleteOnExit()
        }

        val trainer = LearnWordsTrainer.fromDictionaryFile(emptyFile.absolutePath)
        val statistics = trainer.getStatistics()

        assertEquals(0, statistics.learnedWords)
        assertEquals(0, statistics.totalCount)
        assertEquals(0, statistics.percent)
    }

    @Test
    @DisplayName("loadDictionary: некорректный файл приводит к IllegalStateException")
    fun `test statistics with corrupted file`() {
        val corruptedFile = File.createTempFile("corrupted_dictionary", ".txt").apply {
            writeText("only_one_part_without_separator")
            deleteOnExit()
        }
        assertTrue(corruptedFile.exists())
        assertTrue(corruptedFile.readText().isNotEmpty())

        val exception = assertThrows(IllegalStateException::class.java) {
            LearnWordsTrainer.fromDictionaryFile(corruptedFile.absolutePath)
        }

        assertTrue(exception.message!!.contains("Некорректный файл"))
    }

    @Test
    @DisplayName("getNextQuestion: возвращает вопрос, когда есть хотя бы 1 невыученное слово")
    fun `test getNextQuestion() with unlearned words`() {

        val file = File.createTempFile("test_dictionary", ".txt").apply {
            writeText("""
                define|определять|0
                serve|служить|0
                empty|пустой|$CORRECT_ANSWERS
                full|полный|$CORRECT_ANSWERS
            """.trimIndent()
            )
            deleteOnExit()
        }

        val trainer = LearnWordsTrainer.fromDictionaryFile(file.absolutePath)
        val question = trainer.getNextQuestion()

        assertNotNull(question)
        assertEquals(org.example.additional.QUESTION_ANSWERS, question!!.questionWords.size)
        assertTrue(question.questionWords.contains(question.correctAnswer))
        assertTrue(question.correctAnswer.correctAnswersCount < CORRECT_ANSWERS)
    }

    @Test
    @DisplayName("getNextQuestion: null, когда все слова выучены")
    fun `test getNextQuestion() with all words learned`() {
        val source = File(testFilePath("4_words_of_7.txt"))
        val file = File.createTempFile("all_learned_dictionary", ".txt").apply {
            val allLearnedLines = source.readLines().map { line ->
                val parts = line.split("|")
                "${parts[0]}|${parts[1]}|$CORRECT_ANSWERS"
            }
            writeText(allLearnedLines.joinToString("\n"))
            deleteOnExit()
        }

        val trainer = LearnWordsTrainer.fromDictionaryFile(file.absolutePath)
        val question = trainer.getNextQuestion()

        assertNull(question)
    }

    @Test
    @DisplayName("checkAnswer: true, когда индекс ответа совпадает с индексом правильного слова")
    fun `test checkAnswer() with true`() {
        val source = File(testFilePath("4_words_of_7.txt"))
        val file = File.createTempFile("check_answer_true", ".txt").apply {
            writeText(source.readText())
            deleteOnExit()
        }

        val trainer = LearnWordsTrainer.fromDictionaryFile(file.absolutePath)
        val question = trainer.getNextQuestion()!!

        val correctIndex = question.questionWords.indexOf(question.correctAnswer)
        val result = trainer.checkAnswer(correctIndex)

        assertTrue(result)
        assertEquals(1, question.correctAnswer.correctAnswersCount)
    }

    @Test
    @DisplayName("checkAnswer: false, когда индекс ответа неверный или null")
    fun `test checkAnswer() with false`() {
        val source = File(testFilePath("4_words_of_7.txt"))
        val file = File.createTempFile("check_answer_false", ".txt").apply {
            writeText(source.readText())
            deleteOnExit()
        }

        val trainer = LearnWordsTrainer.fromDictionaryFile(file.absolutePath)
        val question = trainer.getNextQuestion()!!

        val wrongIndex = (question.questionWords.indexOf(question.correctAnswer) + 1) % question.questionWords.size
        val resultWrong = trainer.checkAnswer(wrongIndex)
        assertFalse(resultWrong)

        val resultNull = trainer.checkAnswer(null)
        assertFalse(resultNull)
    }

    @Test
    @DisplayName("resetProgress: обнуляет счётчики выученных слов и сохраняет в файл")
    fun `test resetProgress() with 2 words in dictionary`() {
        val source = File(testFilePath("4_words_of_7.txt"))
        val file = File.createTempFile("reset_progress", ".txt").apply {
            writeText(source.readText())
            deleteOnExit()
        }

        val trainer = LearnWordsTrainer.fromDictionaryFile(file.absolutePath)
        assertTrue(trainer.dictionary.any { it.correctAnswersCount >= CORRECT_ANSWERS })
        trainer.resetProgress()

        assertTrue(trainer.dictionary.all { it.correctAnswersCount == 0 })

        val lines = file.readLines()
        val fromFile = lines.map { line ->
            val parts = line.split("|")
            Word(
                word = parts[0],
                translation = parts[1],
                correctAnswersCount = parts[2].toInt()
            )
        }

        assertTrue(fromFile.all { it.correctAnswersCount == 0 })
    }
}