package org.example.additional

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

    fun getUpdates(updateId: Int): String {
        val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun sendMessage(chatId: String?, text: String): String {
        val encodedText = URLEncoder.encode(text, "utf-8")
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedText"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun sendMenu(chatId: String?): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"
        val sendMenuBody = """
            {
              "chat_id": "$chatId",
              "text": "Основное меню:",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    {
                      "text": "Учить слова 📚",
                      "callback_data": "$LEARN_WORDS_CALLBACK"
                    }
                  ],
                  [
                    {
                      "text": "Статистика 📊",
                      "callback_data": "$STATISTICS_CALLBACK"
                    },
                     {
                      "text": "Сбросить прогресс обучения 📊",
                      "callback_data": "$STATISTICS_CALLBACK"
                    }
                  ],
                  [
                {
                  "text": "Выход ❌",
                  "callback_data": "$EXIT_BUTTON"
                }
              ]
                ]
              }
            }
        """.trimIndent()
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlSendMessage))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
            .build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun sendQuestion(chatId: String?, question: Question): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"
        val questionText = "\uD83C\uDDEC\uD83C\uDDE7 ${question.correctAnswer.word}\n\nВыбери правильный ответ:"
        val keyboardButton = question.questionWords.mapIndexed { index, word ->
            """
                [
                    {
                        "text": "${word.translation}",
                        "callback_data": "${CALLBACK_DATA_ANSWER_PREFIX}$index"
                    }
                ],
                [
                    {
                        "text": "⬅ Меню",
                        "callback_data": "$MENU_BUTTON"
                    }
                ]  
            """.trimIndent()
        }.joinToString(",")

        val questionBody = """
             {
              "chat_id": "$chatId",
              "text": "$questionText",
              "reply_markup": {
                "inline_keyboard": [
                  $keyboardButton
                ]
              }
            }
        """.trimIndent()

        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlSendMessage))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(questionBody))
            .build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun checkNextQuestionAndSend(
        trainer: LearnWordsTrainer,
        telegramBotService: TelegramBotService,
        chatId: String?
    ) {
        val nextQuestion = trainer.getNextQuestion()
        if (nextQuestion == null) {
            telegramBotService.sendMessage(chatId, ALL_WORDS_ARE_LEARNED)
        } else {
            telegramBotService.sendQuestion(chatId, nextQuestion)
        }
    }

    fun checkAnswer(
        chatId: String?,
        callbackData: String?,
        trainer: LearnWordsTrainer,
        botService: TelegramBotService,
        correctWord: Word?,
    ) {
        val userAnswerIndex = callbackData?.substringAfter(CALLBACK_DATA_ANSWER_PREFIX)?.toIntOrNull()
        val isCorrectAnswer = trainer.checkAnswer(userAnswerIndex)

        if (userAnswerIndex != null && isCorrectAnswer) {
            botService.sendMessage(chatId, CORRECT_ANSWER)
        } else {
            botService.sendMessage(chatId, "Неправильно! ${correctWord?.word} – это ${correctWord?.translation}")
        }
    }
}