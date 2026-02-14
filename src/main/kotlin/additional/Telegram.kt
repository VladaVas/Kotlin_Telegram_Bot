package org.example.additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botToken = args[0]
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates"

    val builder: HttpClient.Builder = HttpClient.newBuilder()
    val client: HttpClient = builder.build()

    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    println(response.body())

}