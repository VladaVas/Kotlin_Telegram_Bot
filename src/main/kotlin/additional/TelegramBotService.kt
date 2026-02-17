package org.example.additional

import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TelegramBotService(private val botToken: String) {
    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getMe(): String {
        val urlGetMe = "$TELEGRAM_BASE_URL$botToken/getMe"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetMe)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun getUpdates(updateId: Long): String {
        val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun sendMessage(chatId: Long, text: String): String {
        val encodedText = URLEncoder.encode(text, "utf-8")
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedText"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun sendMenu(chatId: Long?): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню:",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(InlineKeyboard(text = "Учить слова \uD83D\uDCDA", callbackData = LEARN_WORDS_CALLBACK)),
                    listOf(InlineKeyboard(text = "Статистика \uD83D\uDCCA", callbackData = STATISTICS_CALLBACK)),
                    listOf(
                        InlineKeyboard(
                            text = "Сбросить прогресc \uD83E\uDDE9",
                            callbackData = RESET_PROGRESS_CALLBACK
                        )
                    ),
                    listOf(InlineKeyboard(text = "Сделать паузу ☕\uFE0F", callbackData = EXIT_BUTTON))
                )
            )
        )

        val requestBodyString = Json.encodeToString(requestBody)
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlSendMessage))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun sendQuestion(chatId: Long, question: Question): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"
        val questionText = "\uD83C\uDDEC\uD83C\uDDE7 ${question.correctAnswer.word}\n\nВыбери правильный ответ"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "$questionText:",
            replyMarkup = ReplyMarkup(
                question.questionWords.mapIndexed { index, word ->
                    listOf(
                        InlineKeyboard(
                            text = word.translation,
                            callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
                        )
                    )
                } + listOf(
                    listOf(
                        InlineKeyboard(MENU_BUTTON, MENU_BUTTON_TEXT)
                    )
                )
            ))

        val requestBodyString = Json.encodeToString(requestBody)

        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlSendMessage))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun checkNextQuestionAndSend(
        trainer: LearnWordsTrainer?,
        telegramBotService: TelegramBotService,
        chatId: Long
    ) {
        val nextQuestion = trainer?.getNextQuestion()
        if (nextQuestion == null) {
            telegramBotService.sendMessage(chatId, ALL_WORDS_ARE_LEARNED)
        } else {
            telegramBotService.sendQuestion(chatId, nextQuestion)
        }
    }

    fun checkAnswer(
        chatId: Long,
        callbackData: String?,
        trainer: LearnWordsTrainer?,
        botService: TelegramBotService,
        correctWord: Word?,
    ) {
        val userAnswerIndex = callbackData?.substringAfter(CALLBACK_DATA_ANSWER_PREFIX)?.toIntOrNull()
        val isCorrectAnswer = trainer?.checkAnswer(userAnswerIndex)

        if (userAnswerIndex != null && isCorrectAnswer == true) {
            botService.sendMessage(chatId, CORRECT_ANSWER)
        } else {
            botService.sendMessage(chatId, "Неправильно! ${correctWord?.word} – это ${correctWord?.translation}")
        }
    }

    fun handleUpdate(
        update: Update,
        botService: TelegramBotService,
        trainers: MutableMap<Long, LearnWordsTrainer>,
    ) {
        val message = update.message?.text
        if (message != null) {
            println(message)
        } else {
            println("Нет новых сообщений")
        }

        val chatIdString = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
        trainers.getOrPut(chatIdString) { LearnWordsTrainer() }
        val callBackQueryData = update.callbackQuery?.data
        val callbackChatId = update.callbackQuery?.message?.chat?.id

        if (message?.startsWith(START_BUTTON) == true) {
            trainers.getOrPut(chatIdString) { LearnWordsTrainer() }
            botService.sendMessage(chatIdString, HELLO_TEXT)
            botService.sendMenu(chatIdString)
        }

        if (callbackChatId != null && callBackQueryData?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
            val trainer = trainers.getOrPut(callbackChatId) { LearnWordsTrainer() }
            botService.checkAnswer(
                callbackChatId,
                callBackQueryData,
                trainer,
                botService,
                trainer.question?.correctAnswer
            )
            botService.checkNextQuestionAndSend(trainer, botService, callbackChatId)
            return
        }

        if (callbackChatId != null) {
            val trainer = trainers.getOrPut(callbackChatId) { LearnWordsTrainer() }
            when (callBackQueryData) {

                LEARN_WORDS_CALLBACK -> {
                    botService.checkNextQuestionAndSend(trainer, botService, callbackChatId)
                }

                STATISTICS_CALLBACK -> {
                    val statistics = trainer.getStatistics()
                    val statsMessageBody = """
                        📊 Ваш прогресс:
                        
                        ✅ Выучено слов: ${statistics.learnedWords}
                        📚 Всего слов в словаре: ${statistics.totalCount}
                        📈 Прогресс: ${statistics.percent}%
                    """.trimIndent()
                    botService.sendMessage(callbackChatId, statsMessageBody)
                }

                RESET_PROGRESS_CALLBACK -> {
                    trainer.resetProgress()
                    botService.sendMessage(callbackChatId, RESET_PROGRESS_TEXT)
                    botService.sendMenu(chatIdString)
                }

                MENU_BUTTON -> {
                    botService.sendMenu(callbackChatId)
                }

                EXIT_BUTTON -> {
                    botService.sendMessage(callbackChatId, EXIT_TEXT)
                }
            }
        }
    }
}