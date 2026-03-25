package org.example.additional

import kotlinx.serialization.json.Json
import java.io.File

fun main(args: Array<String>) {

    val botToken = System.getenv("TELEGRAM_BOT_TOKEN") ?: args.getOrNull(0)
        ?: throw IllegalStateException(
            "Telegram bot token is not provided. Set TELEGRAM_BOT_TOKEN env var or pass it as the first program argument."
        )
    println("Starting Telegram bot long polling.")
    println("DB_URL=$DB_URL")
    println("DEFAULT_DICTIONARY_FILE=$DEFAULT_DICTIONARY_FILE")
    println("IMAGES_FOLDER=$IMAGES_FOLDER")
    val defaultDictionary = File(DEFAULT_DICTIONARY_FILE)
    if (!defaultDictionary.exists()) {
        System.err.println(
            "Default dictionary file not found: ${defaultDictionary.absolutePath}. " +
                "Users can still upload their own dictionary files."
        )
    }
    var lastUpdateId = 0L
    val botService = TelegramBotService(botToken)
    val dynamicMessage = DynamicMessage(botService)
    val trainers = HashMap<Long, LearnWordsTrainer>()
    val imageDir = File(IMAGES_FOLDER)
    if (!imageDir.exists() || !imageDir.isDirectory) {
        System.err.println("Images directory not found: ${imageDir.absolutePath}. Bot will send text without images.")
    }

    val json = Json {
        ignoreUnknownKeys = true
    }

    while (true) {
        try {
            Thread.sleep(2000)
            val responseString: String = botService.getUpdates(lastUpdateId) ?: continue
            val response: Response = json.decodeFromString(responseString)
            if (!response.ok) {
                System.err.println("Telegram getUpdates failed: ${response.description ?: "unknown error"}")
                continue
            }
            val updates = response.result

            updates.forEach { update ->
                try {
                    val updateId = update.updateId
                    lastUpdateId = updateId + 1

                    val chatIdString = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id
                    if (chatIdString != null) {
                        // Force user initialization in DB for any interaction type.
                        trainers.getOrPut(chatIdString) { LearnWordsTrainer(chatIdString) }
                    }
                    val message = update.message?.text?.let { raw ->
                        try {
                            InputValidator.validateUserText(raw)
                        } catch (e: IllegalArgumentException) {
                            SecurityLogger.logSuspiciousActivity("invalid message from chatId=$chatIdString", raw)
                            if (chatIdString != null) {
                                botService.sendMessage(
                                    chatIdString,
                                    "Не удалось обработать сообщение. Используй буквы, цифры и стандартные символы."
                                )
                            }
                            null
                        }
                    }

                    val document = update.message?.document
                    if (chatIdString != null && document != null) {
                        val trainer = trainers.getOrPut(chatIdString) { LearnWordsTrainer(chatIdString) }
                        val jsonResponse = botService.getFile(document.fileId, json)
                        val getFileResponse: GetFileResponse = json.decodeFromString(jsonResponse)
                        if (!getFileResponse.ok || getFileResponse.result == null) {
                            System.err.println("Telegram getFile failed for chat $chatIdString: ${jsonResponse.take(300)}")
                            botService.sendMessage(chatIdString, "Не удалось получить файл из Telegram. Попробуй снова.")
                        } else {
                            val telegramFile = getFileResponse.result
                            val tempFile = File.createTempFile("word_upload_${chatIdString}_", ".txt")
                            try {
                                botService.sendMessage(chatIdString, "Обработка файла... 0%\n[▒▒▒▒▒▒▒▒▒▒]")
                                val progressMessageId = botService.getLastMessageId(chatIdString)
                                if (progressMessageId != null) {
                                    botService.updateProgress(chatIdString, progressMessageId, 50)
                                }
                                val hadNoWords = trainer.getStatistics().totalCount == 0
                                if (hadNoWords) {
                                    val defaultDict = File(DEFAULT_DICTIONARY_FILE)
                                    if (defaultDict.exists() && defaultDict.isFile) {
                                        trainer.addWordsFromFile(defaultDict.absolutePath)
                                    }
                                }
                                botService.downloadFile(telegramFile.filePath, tempFile.absolutePath)
                                trainer.addWordsFromFile(tempFile.absolutePath)
                                if (progressMessageId != null) {
                                    botService.updateProgress(chatIdString, progressMessageId, 100)
                                    botService.editMessage(chatIdString, progressMessageId, "Словарь обновлён! \uD83C\uDD95\nДобавлены слова из файла \"${document.fileName}\".")
                                } else {
                                    botService.sendMessage(chatIdString, "Словарь обновлён: добавлены слова из файла \"${document.fileName}\".")
                                }
                                botService.sendMenu(chatIdString)
                            } finally {
                                tempFile.delete()
                            }
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
                        val startImage = File(imageDir, START_IMAGE_FILENAME)
                        if (startImage.exists() && startImage.isFile) {
                            botService.sendPhoto(startImage, chatIdString, caption = HELLO_TEXT, parseMode = "HTML")
                        } else {
                            botService.sendMessage(chatIdString, HELLO_TEXT, "HTML")
                        }
                        botService.sendMessage(chatIdString, LEARNED_WORD_RULE_TEXT, "HTML")
                        botService.sendMenu(chatIdString)
                    }

                    val callbackChatId = update.callbackQuery?.message?.chat?.id
                    val callBackQueryData = update.callbackQuery?.data?.let { raw ->
                        try {
                            InputValidator.validateCallbackData(raw)
                        } catch (e: IllegalArgumentException) {
                            SecurityLogger.logSuspiciousActivity("invalid callback from chatId=$callbackChatId", raw)
                            null
                        }
                    }

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
                            CLEAR_DICTIONARY_CALLBACK -> {
                                trainer.clearDictionary()
                                val defaultDict = File(DEFAULT_DICTIONARY_FILE)
                                if (defaultDict.exists() && defaultDict.isFile) {
                                    trainer.addWordsFromFile(defaultDict.absolutePath)
                                }
                                botService.sendMessage(callbackChatId, CLEAR_DICTIONARY_TEXT)
                                botService.sendMenu(callbackChatId)
                            }
                            MENU_BUTTON -> botService.sendMenu(callbackChatId)
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
                } catch (e: Exception) {
                    System.err.println("Failed to process update: ${e.message}")
                    e.printStackTrace(System.err)
                }
            }
        } catch (e: Exception) {
            System.err.println("Polling iteration failed: ${e.message}")
            e.printStackTrace(System.err)
        }
    }
}
