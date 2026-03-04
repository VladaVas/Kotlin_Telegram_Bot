package additional

import org.example.additional.Question
import org.example.additional.Word
import org.example.additional.asConsoleString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class AsConsoleStringTest {

    private fun captureConsoleOutput(block: () -> Unit): String {
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))

        return try {
            block()
            val normalized = outContent.toString()
                .replace("\r\n", "\n")
                .replace("\r", "\n")
            if (normalized.endsWith("\n")) normalized.dropLast(1) else normalized
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    @DisplayName("Обычный кейс с 4 вариантами")

    fun fourOptionsList() {
        val options = listOf(
            Word(word = "apple", translation = "яблоко"),
            Word(word = "orange", translation = "апельсин"),
            Word(word = "banana", translation = "банан"),
            Word(word = "pear", translation = "груша"),
        )

        val question = Question(
            questionWords = options,
            correctAnswer = options[0],
        )

        val output = captureConsoleOutput { question.asConsoleString() }

        val expected = """
        apple:
        1 - яблоко
        2 - апельсин
        3 - банан
        4 - груша
        """.trimIndent()

        assertEquals(expected, output)
    }

    @Test
    @DisplayName("Кейс с изменением порядка вариантов ответа")

    fun changeTheOrderOfOptions() {
        val options = listOf(
            Word(word = "apple", translation = "яблоко"),
            Word(word = "orange", translation = "апельсин"),
            Word(word = "banana", translation = "банан"),
            Word(word = "pear", translation = "груша"),
        )

        val shuffledOptions = listOf(
            options[3],
            options[1],
            options[0],
            options[2],
        )

        val question = Question(
            questionWords = shuffledOptions,
            correctAnswer = options[0],
        )

        val output = captureConsoleOutput { question.asConsoleString() }

        val expected = """
        apple:
        1 - груша
        2 - апельсин
        3 - яблоко
        4 - банан
        """.trimIndent()

        assertEquals(expected, output)
    }

    @Test
    @DisplayName("Кейс на пустой список вариантов")

    fun emptyOptionsList() {

        val question = Question(
            questionWords = emptyList(),
            correctAnswer = Word("apple", "яблоко"),
        )

        val output = captureConsoleOutput { question.asConsoleString() }

        val expected = "apple:"

        assertEquals(expected, output)
    }

    @Test
    @DisplayName("Кейс на список из 10 элементов")

    fun tenOptionsList() {

        val options = (1..10).map { index ->
            Word(
                word = "word$index",
                translation = "translation$index",
            )
        }

        val question = Question(
            questionWords = options,
            correctAnswer = options[0],
        )

        val output = captureConsoleOutput { question.asConsoleString() }

        val expectedLines = buildList {
            add("word1:")
            options.forEachIndexed { index, option ->
                add("${index + 1} - ${option.translation}")
            }
        }

        val expectedLine = expectedLines.joinToString("\n")

        assertEquals(expectedLine, output)
    }

    @Test
    @DisplayName("200 вариантов не приводят к падению и выводится только ограниченное количество")
    fun twoHundredOptions() {
        val options = (1..200).map { index ->
            Word(
                word = "word$index",
                translation = "перевод$index",
            )
        }

        val question = Question(
            questionWords = options,
            correctAnswer = options[0],
        )

        val output = captureConsoleOutput { question.asConsoleString() }
        val lines = output.split("\n")

        assertEquals(11, lines.size)
        assertEquals("word1:", lines.first())
        assertEquals("10 - перевод10", lines.last())
    }

    @Test
    @DisplayName("Спецсимволы в словах и переводах выводятся корректно")
    fun specialCharactersInWordsAndTranslations() {
        val options = listOf(
            Word(word = "apple().", translation = "яблоко().!"),
            Word(word = "orange][*", translation = "апельсин[]*?/"),
            Word(word = "banana$%{}", translation = "банан$%{}#@"),
            Word(word = "pear^&~", translation = "груша^&~|"),
        )

        val question = Question(
            questionWords = options,
            correctAnswer = options[0],
        )

        val output = captureConsoleOutput { question.asConsoleString() }

        val expected = """
        apple().:
        1 - яблоко().!
        2 - апельсин[]*?/
        3 - банан$%{}#@
        4 - груша^&~|
        """.trimIndent()

        assertEquals(expected, output)
    }

    @Test
    @DisplayName("Переводы, состоящие только из пробелов, сохраняются как есть")
    fun translationsWithOnlySpaces() {
        val options = listOf(
            Word(word = "w1", translation = "    "),
            Word(word = "w2", translation = "  "),
        )

        val question = Question(
            questionWords = options,
            correctAnswer = options[0],
        )

        val output = captureConsoleOutput { question.asConsoleString() }

        val expected = """
        w1:
        1 -     
        2 -   
        """.trimIndent()

        assertEquals(expected, output)
    }

    @Test
    @DisplayName("Корректный ответ не входит в список вариантов")
    fun correctAnswerNotInOptions() {
        val options = listOf(
            Word(word = "w1", translation = "t1"),
            Word(word = "w2", translation = "t2"),
        )
        val correctAnswer = Word(word = "w3", translation = "t3")
        val question = Question(
            questionWords = options,
            correctAnswer = correctAnswer,
        )

        val output = captureConsoleOutput { question.asConsoleString() }
        val lines = output.split("\n")
        val expectedLines = listOf(
            "w3:",
            "1 - t1",
            "2 - t2"
        )

        assertEquals(expectedLines, lines)
    }
}
