package org.example.additional

class MessageEditor(
    private val chatId: Long,
    private val botService: TelegramBotService,
) {
    private var lastMessageId: Long = 0
    private val messageHistory = mutableListOf<String>()

    fun getMessageId(): Long? = if (lastMessageId != 0L) lastMessageId else null

    fun setCurrentMessage(messageId: Long, initialText: String? = null) {
        lastMessageId = messageId
        if (initialText != null) messageHistory.add(initialText)
    }

    fun updateMessage(newText: String) {
        if (lastMessageId != 0L && botService.safeEditMessage(chatId, lastMessageId, newText)) {
            messageHistory.add(newText)
        }
    }

    fun rollbackToPrevious(): Boolean {
        if (messageHistory.size <= 1) return false
        messageHistory.removeLastOrNull()
        val previousText = messageHistory.lastOrNull() ?: return false
        return botService.safeEditMessage(chatId, lastMessageId, previousText)
    }
}
