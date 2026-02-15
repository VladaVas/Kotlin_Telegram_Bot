package org.example.additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0

    println(getMe(botToken))

    while (true) {
        Thread.sleep(2000)
        val updates: String = getUpdates(botToken, updateId)
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
        println(text)
    }

}

fun getMe(botToken: String): String {
    val urlGetMe = "$TELEGRAM_BASE_URL$botToken/getMe"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetMe)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}

fun getUpdates(botToken: String, updateId: Int): String {
    val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response: HttpResponse<String> =
        client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}