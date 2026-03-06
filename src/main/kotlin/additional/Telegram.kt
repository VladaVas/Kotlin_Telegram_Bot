package org.example.additional

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>,
)

@Serializable
data class Document(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat,
    @SerialName("document")
    val document: Document? = null,
)

@Serializable
data class GetFileRequest(
    @SerialName("file_id")
    val fileId: String,
)

@Serializable
data class GetFileResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: TelegramFile? = null,
)

@Serializable
data class TelegramFile(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
    @SerialName("file_path")
    val filePath: String,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String,
    @SerialName("message")
    val message: Message,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long?,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("callback_data")
    val callbackData: String,
    @SerialName("text")
    val text: String,
)

@Serializable
data class PhotoSize(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_size")
    val fileSize: Long = 0,
)

@Serializable
data class SendPhotoResult(
    @SerialName("message_id")
    val messageId: Long,
    @SerialName("photo")
    val photo: List<PhotoSize>? = null,
)

@Serializable
data class SendPhotoResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: SendPhotoResult? = null,
)

fun main(args: Array<String>) {

    val botToken = args[0]
    var lastUpdateId = 0L
    val botService = TelegramBotService(botToken)
    val trainers = HashMap<Long, LearnWordsTrainer>()
    val imageDir = File(IMAGES_FOLDER)

    val json = Json {
        ignoreUnknownKeys = true
    }

    println(botService.getMe())

    while (true) {
        Thread.sleep(2000)
        val responseString: String = botService.getUpdates(lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        val updates = response.result

        updates.forEach { update ->
            val updateId = update.updateId
            lastUpdateId = updateId + 1

            val message = update.message?.text
            val chatIdString = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id

            if (message != null) println(message)

            val document = update.message?.document
            if (chatIdString != null && document != null) {
                val trainer = trainers.getOrPut(chatIdString) { LearnWordsTrainer(chatIdString) }
                val jsonResponse = botService.getFile(document.fileId, json)
                println(jsonResponse)
                val getFileResponse: GetFileResponse = json.decodeFromString(jsonResponse)
                getFileResponse.result?.let { telegramFile ->
                    val localFileName = "word_upload_$chatIdString.txt"
                    botService.downloadFile(telegramFile.filePath, localFileName)
                    trainer.addWordsFromFile(localFileName)
                    botService.sendMessage(chatIdString, "Словарь обновлён: добавлены слова из файла ${document.fileName}.")
                }
            }

            if (chatIdString != null && message?.startsWith(START_BUTTON) == true) {
                trainers.getOrPut(chatIdString) { LearnWordsTrainer(chatIdString) }
                botService.sendMessage(chatIdString, HELLO_TEXT)
                botService.sendMenu(chatIdString)
            }
            val callBackQueryData = update.callbackQuery?.data
            val callbackChatId = update.callbackQuery?.message?.chat?.id

            if (callbackChatId != null && callBackQueryData?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
                val trainer = trainers.getOrPut(callbackChatId) { LearnWordsTrainer(callbackChatId) }
                botService.checkAnswer(
                    callbackChatId,
                    callBackQueryData,
                    trainer,
                    botService,
                    trainer.question?.correctAnswer
                )
                botService.checkNextQuestionAndSend(trainer, botService, callbackChatId, imageDir, json)
            }

            if (callbackChatId != null) {
                val trainer = trainers.getOrPut(callbackChatId) { LearnWordsTrainer(callbackChatId) }
                when (callBackQueryData) {
                    LEARN_WORDS_CALLBACK -> {
                        botService.checkNextQuestionAndSend(trainer, botService, callbackChatId, imageDir, json)
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
                        botService.sendMenu(callbackChatId)
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
}
