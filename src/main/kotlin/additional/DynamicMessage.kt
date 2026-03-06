package org.example.additional

class DynamicMessage(private val botService: TelegramBotService) {

    private val editors = mutableMapOf<Long, MessageEditor>()

    private fun getOrPutEditor(chatId: Long): MessageEditor =
        editors.getOrPut(chatId) { MessageEditor(chatId, botService) }

    fun getMessageId(chatId: Long): Long? = editors[chatId]?.getMessageId()

    fun setMessage(chatId: Long, messageId: Long, initialText: String) {
        getOrPutEditor(chatId).setCurrentMessage(messageId, initialText)
    }

    fun updateAndPush(chatId: Long, newText: String): Boolean {
        val editor = editors[chatId] ?: return false
        if (editor.getMessageId() == null) return false
        editor.updateMessage(newText)
        return true
    }

    fun undo(chatId: Long): Boolean = editors[chatId]?.rollbackToPrevious() ?: false
}
