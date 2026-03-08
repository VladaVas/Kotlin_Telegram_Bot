package org.example.additional

import kotlinx.serialization.json.Json
import java.io.File

fun main(args: Array<String>) {

    val botToken = args[0]
    var lastUpdateId = 0L
    val botService = TelegramBotService(botToken)
    val dynamicMessage = DynamicMessage(botService)
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
                    botService.sendMessage(chatIdString, "Обработка файла... 0%\n[▒▒▒▒▒▒▒▒▒▒]")
                    val progressMessageId = botService.getLastMessageId(chatIdString)
                    if (progressMessageId != null) {
                        botService.updateProgress(chatIdString, progressMessageId, 50)
                    }
                    botService.downloadFile(telegramFile.filePath, localFileName)
                    trainer.addWordsFromFile(localFileName)
                    if (progressMessageId != null) {
                        botService.updateProgress(chatIdString, progressMessageId, 100)
                        botService.editMessage(chatIdString, progressMessageId, "Словарь обновлён: добавлены слова из файла ${document.fileName}.")
                    } else {
                        botService.sendMessage(chatIdString, "Словарь обновлён: добавлены слова из файла ${document.fileName}.")
                    }
                    botService.sendMenu(chatIdString)
                }
            }

            if (chatIdString != null && message == UNDO_COMMAND) {
                if (dynamicMessage.undo(chatIdString)) {
                    botService.sendMessage(chatIdString, "↩ Возврат к предыдущему сообщению.")
                } else {
                    botService.sendMessage(chatIdString, "Нечего откатывать.")
                }
            }

            if (chatIdString != null && message?.startsWith(START_BUTTON) == true) {
                trainers.getOrPut(chatIdString) { LearnWordsTrainer(chatIdString) }
                val startImage = File(imageDir, START_IMAGE_FILENAME)
                if (startImage.exists() && startImage.isFile) {
                    botService.sendPhoto(startImage, chatIdString, caption = HELLO_TEXT)
                } else {
                    botService.sendMessage(chatIdString, HELLO_TEXT)
                }
                botService.sendMenu(chatIdString)
            }
            val callBackQueryData = update.callbackQuery?.data
            val callbackChatId = update.callbackQuery?.message?.chat?.id

            if (callbackChatId != null) {
                val trainer = trainers.getOrPut(callbackChatId) { LearnWordsTrainer(callbackChatId) }
                if (callBackQueryData?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
                    botService.checkAnswer(
                        callbackChatId,
                        callBackQueryData,
                        trainer,
                        trainer.question?.correctAnswer,
                        dynamicMessage,
                    )
                    botService.checkNextQuestionAndSend(trainer, callbackChatId, imageDir, json)
                }
                when (callBackQueryData) {
                    LEARN_WORDS_CALLBACK -> botService.checkNextQuestionAndSend(trainer, callbackChatId, imageDir, json)

                    STATISTICS_CALLBACK -> {
                        val statsMessageBody = botService.formatStatisticsWithProgressBar(trainer.getStatistics())
                        botService.sendMessage(callbackChatId, statsMessageBody)
                        botService.getLastMessageId(callbackChatId)?.let { messageId ->
                            dynamicMessage.setMessage(callbackChatId, messageId, statsMessageBody)
                        }
                        botService.sendMenu(callbackChatId)
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
                        val pauseImage = File(imageDir, PAUSE_IMAGE_FILENAME)
                        if (pauseImage.exists() && pauseImage.isFile) {
                            botService.sendPhoto(pauseImage, callbackChatId, caption = EXIT_TEXT)
                        } else {
                            botService.sendMessage(callbackChatId, EXIT_TEXT)
                        }
                    }

                    else -> {
                        if (callBackQueryData != null) {
                            val messageId = update.callbackQuery.message.messageId
                            when {
                                callBackQueryData.startsWith(WORD_MARK_LEARNED_PREFIX) -> {
                                    val word = callBackQueryData.removePrefix(WORD_MARK_LEARNED_PREFIX)
                                    if (trainer.markWordAsLearned(word) && messageId != null) {
                                        botService.showWordStatus(callbackChatId, messageId, word, isLearned = true)
                                    }
                                }
                                callBackQueryData.startsWith(WORD_RESET_PREFIX) -> {
                                    val word = callBackQueryData.removePrefix(WORD_RESET_PREFIX)
                                    if (trainer.resetWordProgress(word) && messageId != null) {
                                        botService.showWordStatus(callbackChatId, messageId, word, isLearned = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
