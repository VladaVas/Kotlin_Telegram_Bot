package org.example.additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val botService = TelegramBotService(botToken)

    println(botService.getMe(botToken))

    while (true) {
        Thread.sleep(2000)
        val chatId = null
        val updates: String = botService.getUpdates(botToken, updateId)
        println(updates)

        val updateIdReg: Regex = "\"update_id\":(\\d+)".toRegex()
        val updateIdMatchResult: MatchResult? = updateIdReg.find(updates)
        val updateIdGroups = updateIdMatchResult?.groups
        val updateIdString = updateIdGroups?.get(1)?.value

        if (updateIdString != null) {
            updateId = updateIdString.toInt() + 1
        }

        val messageTextReg: Regex = "\"text\":\"(.+?)\"".toRegex()
        val textMatchResult: MatchResult? = messageTextReg.find(updates)
        val textGroups = textMatchResult?.groups
        val text = textGroups?.get(1)?.value

        if (text != null) {
            println(text)
        } else {
            println("Нет новых сообщений")
        }

        val sendMessageReg: Regex = "\"chat_id\":\"(.+?)\"".toRegex()
        val sendMessageMatchResult: MatchResult? = sendMessageReg.find(updates)
        val sendMessageGroups = sendMessageMatchResult?.groups
        val sendMessageString = sendMessageGroups?.get(1)?.value

        if (sendMessageString != null && text == "Hello") {
            val chatId = sendMessageString.toInt() + 1
            val response = sendMessage(botToken, chatId, "Hello")
            println(response)
        }
    }

}

fun sendMessage(botToken: String, chatId: Int, text: String): String {
    val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$text"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
    val response: HttpResponse<String> =
        client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}