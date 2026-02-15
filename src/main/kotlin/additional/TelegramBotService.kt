package org.example.additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TelegramBotService(private val botToken: String) {
    val client: HttpClient = HttpClient.newBuilder().build()

    fun getMe(botToken: String): String {
        val urlGetMe = "$TELEGRAM_BASE_URL$botToken/getMe"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetMe)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun getUpdates(botToken: String, updateId: Int): String {
        val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

}