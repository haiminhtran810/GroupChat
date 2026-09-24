package com.nhoctax.groupchat.domain.repository

import com.nhoctax.groupchat.domain.model.Conversation
import com.nhoctax.groupchat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversations(): Flow<List<Conversation>>
    fun getMessages(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(conversationId: String, recipientId: String, content: String): Result<Message>
    suspend fun receiveMessage(message: Message): Result<Unit>
    suspend fun markAsRead(conversationId: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun clearConversation(conversationId: String): Result<Unit>
}
