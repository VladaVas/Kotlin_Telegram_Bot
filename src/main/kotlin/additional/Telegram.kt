package org.example.additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botToken = args[0]
    val urlGetMe = "$TELEGRAM_BASE_URL.$botToken/getMe"
    val urlGetUpdates = "$TELEGRAM_BASE_URL.$botToken/getUpdates"

    val builder: HttpClient.Builder = HttpClient.newBuilder()
    val client: HttpClient = builder.build()

    val requestGetMe: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetMe)).build()
    val responseGetMe: HttpResponse<String> = client.send(requestGetMe, HttpResponse.BodyHandlers.ofString())
    println(responseGetMe.body())

    val requestGetUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val responseGetUpdates: HttpResponse<String> = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())
    println(responseGetUpdates.body())

}