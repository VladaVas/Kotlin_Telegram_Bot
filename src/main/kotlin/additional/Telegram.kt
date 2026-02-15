package org.example.additional

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val botService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()

    println(botService.getMe())

    val updateIdReg: Regex = "\"update_id\":(\\d+)".toRegex()
    val messageTextReg: Regex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdReg: Regex = "\"chat\":\\{\"id\":(\\d+),".toRegex()
    val callBackQueryReg: Regex = "\"callback_query\":\\{\"data\":\"(.+?)\"".toRegex()
    val callbackChatIdReg: Regex = "\"chat\":\\{\"id\":(\\d+),".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates: String = botService.getUpdates(updateId)
        println(updates)

        val updateIdMatchResult: MatchResult? = updateIdReg.find(updates)
        val updateIdGroups = updateIdMatchResult?.groups
        val updateIdString = updateIdGroups?.get(1)?.value

        if (updateIdString != null) {
            updateId = updateIdString.toInt() + 1
        }

        val textMatchResult: MatchResult? = messageTextReg.find(updates)
        val textGroups = textMatchResult?.groups
        val message = textGroups?.get(1)?.value

        if (message != null) {
            println(message)
        } else {
            println("Нет новых сообщений")
        }

        val chatIdMatchResult: MatchResult? = chatIdReg.find(updates)
        val chatIdGroups = chatIdMatchResult?.groups
        val chatIdString = chatIdGroups?.get(1)?.value

        if (chatIdString != null && message?.lowercase() == MENU_BUTTON) {
            botService.sendMessage(chatIdString, HELLO_TEXT)
        }

        if (chatIdString != null && message?.startsWith(MENU_BUTTON) == true) {
            botService.sendMenu(chatIdString)
        }

        val callBackQueryResult: MatchResult? = callBackQueryReg.find(updates)
        val callBackQueryGroups = callBackQueryResult?.groups
        val callBackQueryData = callBackQueryGroups?.get(1)?.value

        val callbackChatIdResult: MatchResult? = callbackChatIdReg.find(updates)
        val callbackChatIdGroups = callbackChatIdResult?.groups
        val callbackChatId = callbackChatIdGroups?.get(1)?.value

        if (callbackChatId != null) {
            when (callBackQueryData) {
                "learn_word_click" -> {
                    botService.sendMessage(callbackChatId, TRAINING_MODE)
                }

                "statistics_click" -> {
                    botService.sendMessage(callbackChatId, SHOW_STATISTICS)
                }
            }
        }

    }
}