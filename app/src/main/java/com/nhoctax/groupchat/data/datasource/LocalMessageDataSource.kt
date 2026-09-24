package com.nhoctax.groupchat.data.datasource

import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class LocalMessageDataSource {

    private val messagesMap = ConcurrentHashMap<String, Message>()
    private val _messagesFlow = MutableStateFlow<List<Message>>(emptyList())

    val allMessagesFlow: Flow<List<Message>> = _messagesFlow

    fun getMessagesFlow(conversationId: String): Flow<List<Message>> {
        return _messagesFlow.map { list ->
            list.filter { it.conversationId == conversationId }
                .sortedBy { it.timestamp }
        }
    }

    fun getMessageById(messageId: String): Message? {
        return messagesMap[messageId]
    }

    fun saveMessage(message: Message) {
        messagesMap[message.id] = message
        updateFlow()
    }

    fun saveMessages(messages: List<Message>) {
        messages.forEach { messagesMap[it.id] = it }
        updateFlow()
    }

    fun updateMessageStatus(messageId: String, status: MessageStatus) {
        val existing = messagesMap[messageId] ?: return
        messagesMap[messageId] = existing.copy(status = status)
        updateFlow()
    }

    fun markMessagesAsRead(conversationId: String, currentUserId: String) {
        var updated = false
        messagesMap.values
            .filter { (it.conversationId == conversationId) && (it.senderId != currentUserId) && (it.status != MessageStatus.READ) }
            .forEach { msg ->
                messagesMap[msg.id] = msg.copy(status = MessageStatus.READ)
                updated = true
            }
        if (updated) {
            updateFlow()
        }
    }

    fun deleteMessage(messageId: String) {
        messagesMap.remove(messageId)
        updateFlow()
    }

    fun clearConversation(conversationId: String) {
        val toRemove = messagesMap.values.filter { it.conversationId == conversationId }
        toRemove.forEach { messagesMap.remove(it.id) }
        updateFlow()
    }

    fun clearAll() {
        messagesMap.clear()
        updateFlow()
    }

    private fun updateFlow() {
        _messagesFlow.value = messagesMap.values.sortedBy { it.timestamp }
    }
}
