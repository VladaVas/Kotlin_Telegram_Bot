package org.example.additional

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val botService = TelegramBotService(botToken)

    println(botService.getMe())

    val updateIdReg: Regex = "\"update_id\":(\\d+)".toRegex()
    val messageTextReg: Regex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdReg: Regex = "\"chat_id\":\"(.+?)\"".toRegex()

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
        val text = textGroups?.get(1)?.value

        if (text != null) {
            println(text)
        } else {
            println("Нет новых сообщений")
        }

        val chatIdMatchResult: MatchResult? = chatIdReg.find(updates)
        val chatIdGroups = chatIdMatchResult?.groups
        val chatIdString = chatIdGroups?.get(1)?.value

        if (chatIdString != null && text == "Hello") {
            botService.sendMessage(chatIdString, "Hello" )
        }
    }
}