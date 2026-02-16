package org.example.additional

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val botService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()
    val lastQuestion: Question? = null

    println(botService.getMe())

    val updateIdReg: Regex = "\"update_id\":(\\d+)".toRegex()
    val messageTextReg: Regex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdReg: Regex = "\"chat\":\\{\"id\":(\\d+),".toRegex()
    val callBackQueryReg: Regex = "\"data\":\"(.+?)\"".toRegex()
    val callbackChatIdReg: Regex = "\"callback_query\":\\{.*?\"chat\":\\{\"id\":(\\d+),".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates: String = botService.getUpdates(updateId)
        println(updates)

        val updateIdString = updateIdReg.find(updates)?.groups?.get(1)?.value

        if (updateIdString != null) {
            updateId = updateIdString.toInt() + 1
        }

        val message = messageTextReg.find(updates)?.groups?.get(1)?.value

        if (message != null) {
            println(message)
        } else {
            println("Нет новых сообщений")
        }

        val chatIdString = chatIdReg.find(updates)?.groups?.get(1)?.value

        if (chatIdString != null && message?.startsWith(START_BUTTON) == true) {
            botService.sendMessage(chatIdString, HELLO_TEXT)
            botService.sendMenu(chatIdString)
        }

        val callBackQueryData = callBackQueryReg.find(updates)?.groups?.get(1)?.value
        val callbackChatId =callbackChatIdReg.find(updates)?.groups?.get(1)?.value

        if (callBackQueryData != null && callBackQueryData.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
            botService.checkAnswer(callbackChatId, callBackQueryData, trainer, botService, lastQuestion?.correctAnswer)
            botService.checkNextQuestionAndSend(trainer, botService, callBackQueryData)
        }

        if (callbackChatId != null) {
            when (callBackQueryData) {
                LEARN_WORDS_CALLBACK -> {
                    botService.checkNextQuestionAndSend(trainer, botService, callbackChatId)
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
            }
        }

    }
}