package org.example.additional

import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Random

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

    fun getFile(fileId: String, json: Json): String {
        val urlGetFile = "$TELEGRAM_BASE_URL$botToken/getFile"
        val requestBody = GetFileRequest(fileId = fileId)
        val requestBodyString = json.encodeToString(requestBody)
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlGetFile))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
        return response.body()
    }

    fun downloadFile(filePath: String, fileName: String) {
        val urlGetFile = "$BOT_FILE_URL$botToken/$filePath"
        println(urlGetFile)
        val request = HttpRequest
            .newBuilder()
            .uri(URI.create(urlGetFile))
            .GET()
            .build()

        val response: HttpResponse<InputStream> = HttpClient
            .newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofInputStream())

        println("status code: " + response.statusCode())
        response.body().use { input ->
            File(fileName).outputStream().use { output ->
                input.copyTo(output, 16 * 1024)
            }
        }
    }

    fun sendMessage(chatId: Long, text: String): String {
        val encodedText = URLEncoder.encode(text, "utf-8")
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedText"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun sendPhoto(file: File, chatId: Long, hasSpoiler: Boolean = false): String {
        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = file
        data["has_spoiler"] = hasSpoiler
        val boundary: String = BigInteger(35, Random()).toString(36)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$TELEGRAM_BASE_URL$botToken/sendPhoto"))
            .postMultipartFormData(boundary, data)
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean = false): String {
        val body = "chat_id=$chatId&photo=${URLEncoder.encode(fileId, "UTF-8")}&has_spoiler=$hasSpoiler"
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$TELEGRAM_BASE_URL$botToken/sendPhoto"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendPhotoForWord(
        chatId: Long,
        word: Word,
        imageDir: File,
        json: Json,
        hasSpoiler: Boolean = true,
        onFileIdSaved: () -> Unit = {}
    ) {
        val imageFile = findImageFile(word, imageDir) ?: return
        val cachedFileId = word.fileId

        if (cachedFileId != null) {
            sendPhotoByFileId(cachedFileId, chatId, hasSpoiler)
            return
        }
        val responseBody = sendPhoto(imageFile, chatId, hasSpoiler)
        val sendPhotoResponse = json.decodeFromString<SendPhotoResponse>(responseBody)
        val fileId = sendPhotoResponse.result?.photo?.maxByOrNull { it.fileSize }?.fileId
        if (fileId != null) {
            word.fileId = fileId
            onFileIdSaved()
        }
    }

    private fun findImageFile(word: Word, imageDir: File): File? {
        if (!imageDir.isDirectory) return null
        word.imagePath?.let { path ->
            val file = File(imageDir, path)
            if (file.exists() && file.isFile) return file
        }
        for (ext in IMAGE_EXTENSIONS) {
            val file = File(imageDir, "${word.word}.$ext")
            if (file.exists() && file.isFile) return file
        }
        return null
    }

    private fun HttpRequest.Builder.postMultipartFormData(boundary: String, data: Map<String, Any>): HttpRequest.Builder {
        val byteArrays = ArrayList<ByteArray>()
        val separator = "--$boundary\r\nContent-Disposition: form-data; name=".toByteArray(StandardCharsets.UTF_8)

        for (entry in data.entries) {
            byteArrays.add(separator)
            when (entry.value) {
                is File -> {
                    val file = entry.value as File
                    val path = file.toPath()
                    val mimeType = Files.probeContentType(path) ?: "image/jpeg"
                    byteArrays.add(
                        "\"${entry.key}\"; filename=\"${path.fileName}\"\r\nContent-Type: $mimeType\r\n\r\n".toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )
                    byteArrays.add(Files.readAllBytes(path))
                    byteArrays.add("\r\n".toByteArray(StandardCharsets.UTF_8))
                }
                else -> byteArrays.add("\"${entry.key}\"\r\n\r\n${entry.value}\r\n".toByteArray(StandardCharsets.UTF_8))
            }
        }
        byteArrays.add("--$boundary--".toByteArray(StandardCharsets.UTF_8))

        header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
        return this
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
                    listOf(InlineKeyboard(text = "Сбросить прогресc \uD83E\uDDE9", callbackData = RESET_PROGRESS_CALLBACK)),
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
        chatId: Long,
        imageDir: File,
        json: Json
    ) {
        val nextQuestion = trainer?.getNextQuestion()
        if (nextQuestion == null) {
            telegramBotService.sendMessage(chatId, ALL_WORDS_ARE_LEARNED)
        } else {
            telegramBotService.sendPhotoForWord(
                chatId = chatId,
                word = nextQuestion.correctAnswer,
                imageDir = imageDir,
                json = json,
                hasSpoiler = true,
                onFileIdSaved = { trainer?.save() }
            )
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
            botService.sendMessage(chatId, "Неправильно!\n${correctWord?.word} – это ${correctWord?.translation}")
        }
    }
}