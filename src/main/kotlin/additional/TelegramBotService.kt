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

    fun sendMessage(chatId: String, text: String): String {
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
        val questionText = "\uD83C\uDDEC\uD83C\uDDE7 ${question.correctAnswer.word}\n\nВыберите правильный ответ:"
        val questionBody = """
             {
              "chat_id": "$chatId",
              "text": "$questionText",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    {
                      "text": "${question.questionWords[0].translation}",
                      "callback_data": "$CALLBACK_DATA_ANSWER_PREFIX"
                    }
                  ],
                  [
                    {
                     "text": "${question.questionWords[1].translation}",
                      "callback_data": "$CALLBACK_DATA_ANSWER_PREFIX"
                    }
                  ],
                  [
                    {
                      "text": "${question.questionWords[2].translation}",
                      "callback_data": "$CALLBACK_DATA_ANSWER_PREFIX"
                    }
                  ],
                  [
                    {
                     "text": "${question.questionWords[3].translation}",
                      "callback_data": "$CALLBACK_DATA_ANSWER_PREFIX"
                    }
                  ]
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
        chatId: String
    ) {
        val nextQuestion = trainer.getNextQuestion()
        if (nextQuestion == null) {
            telegramBotService.sendMessage(chatId, ALL_WORDS_ARE_LEARNED)
        } else {
           telegramBotService.sendQuestion(chatId, nextQuestion )
        }

    }
}