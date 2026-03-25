package org.example.additional

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.math.BigInteger
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Duration
import java.util.Random

class TelegramBotService(private val botToken: String) {
    private val client: HttpClient = HttpClient.newBuilder().build()
    private val json = Json { ignoreUnknownKeys = true }
    private val lastMessageIds = mutableMapOf<Long, Long>()

    fun getLastMessageId(chatId: Long): Long? = lastMessageIds[chatId]

    fun getUpdates(updateId: Long): String? {
        val url = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"

        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                println("HTTP error: ${response.statusCode()}")
                return null
            }

            response.body()
        } catch (e: Exception) {
            println("Network error: ${e.message}")
            null
        }
    }

    fun getFile(fileId: String, json: Json): String {
        val requestBody = json.encodeToString(GetFileRequest(fileId = fileId))
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$TELEGRAM_BASE_URL$botToken/getFile"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    fun downloadFile(filePath: String, fileName: String) {
        val encodedPath = filePath.split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20")
        }
        val request = HttpRequest.newBuilder().uri(URI.create("$BOT_FILE_URL$botToken/$encodedPath")).GET().build()
        client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body().use { input ->
            File(fileName).outputStream().use { output -> input.copyTo(output, 16 * 1024) }
        }
    }

    fun sendMessage(chatId: Long, text: String, parseMode: String? = null): String {
        val encodedText = URLEncoder.encode(text, "utf-8")
        val parseModeParam = parseMode?.let { "&parse_mode=$it" }.orEmpty()
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedText$parseModeParam"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())
        val body = response.body()
        val parsed = runCatching { json.decodeFromString<SendMessageResponse>(body) }.getOrNull()
        when {
            parsed == null -> System.err.println("sendMessage parse error for chat $chatId: ${body.take(300)}")
            !parsed.ok -> System.err.println("sendMessage failed for chat $chatId: ${parsed.description ?: "unknown error"}")
            else -> parsed.result?.messageId?.let { lastMessageIds[chatId] = it }
        }
        return body
    }

    fun editMessage(chatId: Long, messageId: Long, message: String): String {
        val encodedText = URLEncoder.encode(message, "utf-8")
        val urlEditMessage =
            "$TELEGRAM_BASE_URL$botToken/editMessageText?chat_id=$chatId&message_id=$messageId&text=$encodedText"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlEditMessage)).build()
        val response: HttpResponse<String> =
            client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun editMessageWithKeyboard(chatId: Long, messageId: Long, text: String, replyMarkup: String): String {
        val replyMarkupJson = json.parseToJsonElement(replyMarkup)
        val requestBody = buildJsonObject {
            put("chat_id", JsonPrimitive(chatId))
            put("message_id", JsonPrimitive(messageId))
            put("text", JsonPrimitive(text))
            put("parse_mode", JsonPrimitive("HTML"))
            put("reply_markup", replyMarkupJson)
        }
        val url = "$TELEGRAM_BASE_URL$botToken/editMessageText"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(JsonElement.serializer(), requestBody)))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun updateProgress(chatId: Long, messageId: Long, percent: Int) {
        editMessage(chatId, messageId, "Обработка файла... $percent%\n[${progressBar(percent)}]")
    }

    private fun progressBar(percent: Int): String {
        val filled = (percent / 10).coerceIn(0, 10)
        return "█".repeat(filled) + "▒".repeat(10 - filled)
    }

    fun showWordStatus(chatId: Long, messageId: Long, word: String, isLearned: Boolean) {
        val status = if (isLearned) "Изучено ✅" else "Не изучено ❌"
        val escapedWord = word.replace("<", "&lt;").replace(">", "&gt;")
        val text = """
            📚 Слово: <b>$escapedWord</b>
            
            Статус: $status
        """.trimIndent()
        editMessageWithKeyboard(chatId, messageId, text, createWordMenu(isLearned, word))
    }

    private fun createWordMenu(isLearned: Boolean, word: String): String {
        val buttons = if (isLearned) {
            listOf(
                listOf(InlineKeyboard(WORD_RESET_PREFIX + word, "Отметить не выученным ❌")),
            )
        } else {
            listOf(
                listOf(InlineKeyboard(WORD_MARK_LEARNED_PREFIX + word, "Отметить выученным ✅"))
            )
        }
        return json.encodeToString(ReplyMarkup.serializer(), ReplyMarkup(buttons))
    }

    fun safeEditMessage(chatId: Long, messageId: Long, newText: String): Boolean {
        val desc = try {
            val response = editMessage(chatId, messageId, newText)
            val jsonResponse = json.decodeFromString<EditMessageResponse>(response)
            if (jsonResponse.ok) return true
            jsonResponse.description.orEmpty()
        } catch (e: Exception) {
            (e.cause?.message ?: e.message).orEmpty()
        }
        return when {
            desc.contains("message is not modified", ignoreCase = true) -> true
            desc.contains("message to edit not found", ignoreCase = true) ||
                    desc.contains("MESSAGE_EDIT_TIME_EXPIRED", ignoreCase = true) -> false

            else -> false
        }
    }

    fun sendPhoto(
        file: File,
        chatId: Long,
        hasSpoiler: Boolean = false,
        caption: String? = null,
        replyMarkup: ReplyMarkup? = null,
        parseMode: String? = null,
    ): String {
        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = file
        data["has_spoiler"] = hasSpoiler
        caption?.let { data["caption"] = it }
        parseMode?.let { data["parse_mode"] = it }
        replyMarkup?.let { data["reply_markup"] = json.encodeToString(ReplyMarkup.serializer(), it) }
        val boundary = BigInteger(35, Random()).toString(36)
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$TELEGRAM_BASE_URL$botToken/sendPhoto"))
            .postMultipartFormData(boundary, data)
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val body = response.body()
        val parsed = runCatching { json.decodeFromString<SendPhotoResponse>(body) }.getOrNull()
        when {
            parsed == null -> System.err.println("sendPhoto parse error for chat $chatId: ${body.take(300)}")
            !parsed.ok -> System.err.println("sendPhoto failed for chat $chatId: ${parsed.description ?: "unknown error"}")
            else -> parsed.result?.messageId?.let { lastMessageIds[chatId] = it }
        }
        return body
    }

    private fun sendPhotoByFileIdWithCaptionAndKeyboard(
        chatId: Long,
        fileId: String,
        caption: String,
        replyMarkup: ReplyMarkup,
        hasSpoiler: Boolean,
    ): String {
        val replyMarkupJson = json.encodeToString(ReplyMarkup.serializer(), replyMarkup)
        val body = "chat_id=$chatId&photo=${URLEncoder.encode(fileId, "UTF-8")}&caption=${
            URLEncoder.encode(
                caption,
                "UTF-8"
            )
        }&has_spoiler=$hasSpoiler&reply_markup=${URLEncoder.encode(replyMarkupJson, "UTF-8")}"
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$TELEGRAM_BASE_URL$botToken/sendPhoto"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val responseBody = response.body()
        val parsed = runCatching { json.decodeFromString<SendPhotoResponse>(responseBody) }.getOrNull()
        when {
            parsed == null -> System.err.println(
                "sendPhoto(fileId) parse error for chat $chatId: ${
                    responseBody.take(
                        300
                    )
                }"
            )

            !parsed.ok -> System.err.println("sendPhoto(fileId) failed for chat $chatId: ${parsed.description ?: "unknown error"}")
            else -> parsed.result?.messageId?.let { lastMessageIds[chatId] = it }
        }
        return responseBody
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

    private fun HttpRequest.Builder.postMultipartFormData(
        boundary: String,
        data: Map<String, Any>
    ): HttpRequest.Builder {
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
                    listOf(
                        InlineKeyboard(
                            text = "Сбросить прогресc \uD83E\uDDE9",
                            callbackData = RESET_PROGRESS_CALLBACK
                        )
                    ),
                    listOf(
                        InlineKeyboard(
                            text = "Очистить словарь \uD83D\uDDD1\uFE0F",
                            callbackData = CLEAR_DICTIONARY_CALLBACK
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
        val body = response.body()
        val parsed = runCatching { json.decodeFromString<SendMessageResponse>(body) }.getOrNull()
        when {
            parsed == null -> System.err.println("sendMenu parse error for chat $chatId: ${body.take(300)}")
            !parsed.ok -> System.err.println("sendMenu failed for chat $chatId: ${parsed.description ?: "unknown error"}")
            else -> chatId?.let { id -> parsed.result?.messageId?.let { lastMessageIds[id] = it } }
        }
        return body
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
        val body = response.body()
        val parsed = runCatching { json.decodeFromString<SendMessageResponse>(body) }.getOrNull()
        when {
            parsed == null -> System.err.println("sendQuestion parse error for chat $chatId: ${body.take(300)}")
            !parsed.ok -> System.err.println("sendQuestion failed for chat $chatId: ${parsed.description ?: "unknown error"}")
            else -> parsed.result?.messageId?.let { lastMessageIds[chatId] = it }
        }
        return body
    }

    fun formatStatisticsWithProgressBar(statistics: Statistics): String {
        val percent = statistics.percent.coerceIn(0, 100)
        return """
            📊 Ваш прогресс:
            
            ✅ Выучено слов: ${statistics.learnedWords}
            📚 Всего слов в словаре: ${statistics.totalCount}
            📈 Прогресс: $percent%
            [${progressBar(percent)}]
        """.trimIndent()
    }

    fun sendOrUpdateDynamicStats(
        chatId: Long,
        trainer: LearnWordsTrainer?,
        dynamicMessage: DynamicMessage,
        prefix: String,
    ) {
        val statistics = trainer?.getStatistics() ?: return
        val statsText = formatStatisticsWithProgressBar(statistics)
        val fullText = prefix + statsText
        val messageId = dynamicMessage.getMessageId(chatId)
        if (messageId != null) {
            dynamicMessage.updateAndPush(chatId, fullText)
        } else {
            sendMessage(chatId, fullText)
            getLastMessageId(chatId)?.let { dynamicMessage.setMessage(chatId, it, fullText) }
        }
    }

    fun checkNextQuestionAndSend(trainer: LearnWordsTrainer?, chatId: Long, imageDir: File, json: Json) {
        var totalCount = trainer?.getStatistics()?.totalCount ?: 0
        if (totalCount == 0) {
            val defaultDict = File(DEFAULT_DICTIONARY_FILE)
            if (defaultDict.exists() && defaultDict.isFile) {
                trainer?.addWordsFromFile(defaultDict.absolutePath)
                totalCount = trainer?.getStatistics()?.totalCount ?: 0
            }
        }
        val nextQuestion = trainer?.getNextQuestion()
        when {
            totalCount == 0 -> sendMessage(chatId, DICTIONARY_EMPTY, "HTML")
            nextQuestion == null -> sendMessage(chatId, ALL_WORDS_ARE_LEARNED)
            else -> {
                val questionText =
                    "\uD83C\uDDEC\uD83C\uDDE7 ${nextQuestion.correctAnswer.word}\n\nВыбери правильный ответ:"
                val replyMarkup = ReplyMarkup(
                    nextQuestion.questionWords.mapIndexed { index, word ->
                        listOf(
                            InlineKeyboard(
                                text = word.translation,
                                callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
                            )
                        )
                    } + listOf(listOf(InlineKeyboard(MENU_BUTTON, MENU_BUTTON_TEXT)))
                )
                val imageFile = findImageFile(nextQuestion.correctAnswer, imageDir)

                if (imageFile != null) {
                    try {
                        val cachedFileId = nextQuestion.correctAnswer.fileId
                        if (cachedFileId != null) {
                            sendPhotoByFileIdWithCaptionAndKeyboard(chatId, cachedFileId, questionText, replyMarkup, true)
                        } else {
                            val responseBody = sendPhoto(
                                imageFile,
                                chatId,
                                hasSpoiler = true,
                                caption = questionText,
                                replyMarkup = replyMarkup
                            )
                            val sendPhotoResponse = json.decodeFromString<SendPhotoResponse>(responseBody)
                            val fileId = sendPhotoResponse.result?.photo?.maxByOrNull { it.fileSize }?.fileId
                            if (fileId != null) {
                                nextQuestion.correctAnswer.fileId = fileId
                                trainer.save()
                            }
                        }
                    } catch (e: Exception) {
                        System.err.println("Failed to send question photo for chatId=$chatId. Fallback to text. Error: ${e.message}")
                        sendQuestion(chatId, nextQuestion)
                    }
                } else {
                    sendQuestion(chatId, nextQuestion)
                }
            }
        }
    }

    fun checkAnswer(
        chatId: Long,
        callbackData: String?,
        trainer: LearnWordsTrainer?,
        correctWord: Word?,
        dynamicMessage: DynamicMessage? = null,
    ) {
        val userAnswerIndex = callbackData?.substringAfter(CALLBACK_DATA_ANSWER_PREFIX)?.toIntOrNull()
        val isCorrect = trainer?.checkAnswer(userAnswerIndex) == true

        if (dynamicMessage != null) {
            if (userAnswerIndex != null && isCorrect) {
                sendMessage(chatId, CORRECT_ANSWER)
                sendOrUpdateDynamicStats(chatId, trainer, dynamicMessage, "")
            } else {
                val wrongText = "Неправильно! \uD83E\uDEE2\n${correctWord?.word} – это ${correctWord?.translation}"
                sendMessage(chatId, wrongText)
                getLastMessageId(chatId)?.let { dynamicMessage.setMessage(chatId, it, wrongText) }
            }

            if (correctWord != null && !isCorrect) {
                sendMessage(chatId, "📚 ${correctWord.word}")
                getLastMessageId(chatId)?.let { msgId ->
                    val learned = (trainer?.getCorrectAnswersCount(correctWord.word) ?: 0) >= CORRECT_ANSWERS
                    showWordStatus(chatId, msgId, correctWord.word, learned)
                }
            }
        } else {
            if (userAnswerIndex != null && isCorrect) sendMessage(chatId, CORRECT_ANSWER)
            else sendMessage(
                chatId,
                "Неправильно! \uD83E\uDEE2\n${correctWord?.word} – это ${correctWord?.translation}"
            )
        }
    }
}