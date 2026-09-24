package com.nhoctax.groupchat.data.repository

import com.nhoctax.groupchat.data.datasource.LocalMessageDataSource
import com.nhoctax.groupchat.data.datasource.PeerMessagingDataSource
import com.nhoctax.groupchat.domain.crypto.CryptoManager
import com.nhoctax.groupchat.domain.crypto.KeyStoreManager
import com.nhoctax.groupchat.domain.model.Contact
import com.nhoctax.groupchat.domain.model.Conversation
import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.domain.model.MessageStatus
import com.nhoctax.groupchat.domain.repository.ChatRepository
import com.nhoctax.groupchat.domain.repository.ContactRepository
import com.nhoctax.groupchat.domain.repository.SecurityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepositoryImpl(
    private val localMessageDataSource: LocalMessageDataSource,
    private val peerMessagingDataSource: PeerMessagingDataSource,
    private val contactRepository: ContactRepository,
    private val cryptoManager: CryptoManager,
    private val keyStoreManager: KeyStoreManager,
    private val userAlias: String = SecurityRepository.DEFAULT_USER_ALIAS,
    private val userId: String = SecurityRepository.CURRENT_USER_ID,
    externalScope: CoroutineScope? = null
) : ChatRepository {

    init {
        externalScope?.launch {
            peerMessagingDataSource.incomingMessages.collect { incoming ->
                receiveMessage(incoming)
            }
        }
    }

    override fun getConversations(): Flow<List<Conversation>> {
        return combine(
            localMessageDataSource.allMessagesFlow,
            contactRepository.getContacts()
        ) { messages, contacts ->
            val grouped = messages.groupBy { it.conversationId }
            val contactsMap = contacts.associateBy { it.id }

            grouped.map { (conversationId, convMessages) ->
                val sortedMsgs = convMessages.sortedBy { it.timestamp }
                val rawLastMsg = sortedMsgs.lastOrNull()
                val decryptedLastMsg = rawLastMsg?.let { decryptMessageIfNeeded(it) }

                val contact = contactsMap[conversationId]
                    ?: Contact(
                        id = conversationId,
                        name = "User $conversationId"
                    )

                val unreadCount = convMessages.count { (it.senderId != userId) && (it.status != MessageStatus.READ) }

                Conversation(
                    id = conversationId,
                    participant = contact,
                    lastMessage = decryptedLastMsg,
                    unreadCount = unreadCount,
                    updatedAt = rawLastMsg?.timestamp ?: System.currentTimeMillis()
                )
            }.sortedByDescending { it.updatedAt }
        }
    }

    override fun getMessages(conversationId: String): Flow<List<Message>> {
        return localMessageDataSource.getMessagesFlow(conversationId).map { messages ->
            messages.map { decryptMessageIfNeeded(it) }
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        recipientId: String,
        content: String
    ): Result<Message> {
        return try {
            val contact = contactRepository.getContact(recipientId)
                ?: return Result.failure(IllegalArgumentException("Contact $recipientId not found"))

            val publicKeyPem = contact.publicKeyPem
                ?: return Result.failure(IllegalStateException("Contact $recipientId has no public key"))

            val recipientPublicKey = keyStoreManager.parsePublicKeyPem(publicKeyPem)

            val payload = cryptoManager.encrypt(content, recipientPublicKey)

            val message = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = userId,
                recipientId = recipientId,
                content = content,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.PENDING,
                isEncrypted = true,
                encryptedPayload = payload
            )

            localMessageDataSource.saveMessage(message)

            val networkMessage = message.copy(content = payload.ciphertextBase64)
            val sentMessage = peerMessagingDataSource.sendOverNetwork(networkMessage)

            val finalMessage = message.copy(status = sentMessage.status)
            localMessageDataSource.saveMessage(finalMessage)

            Result.success(finalMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun receiveMessage(message: Message): Result<Unit> {
        return try {
            localMessageDataSource.saveMessage(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(conversationId: String): Result<Unit> {
        return try {
            localMessageDataSource.markMessagesAsRead(conversationId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            localMessageDataSource.deleteMessage(messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearConversation(conversationId: String): Result<Unit> {
        return try {
            localMessageDataSource.clearConversation(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decryptMessageIfNeeded(message: Message): Message {
        val payload = message.encryptedPayload ?: return message

        if (!message.isEncrypted) {
            return message
        }

        // If message is sent by current user, the plain content is already available locally
        if (message.senderId == userId && message.content.isNotEmpty() && message.content != payload.ciphertextBase64) {
            return message.copy(isEncrypted = false)
        }

        return try {
            val privateKey = keyStoreManager.getPrivateKey(userAlias)
                ?: return message

            val decryptedContent = cryptoManager.decrypt(payload, privateKey)
            message.copy(
                content = decryptedContent,
                isEncrypted = false
            )
        } catch (_: Exception) {
            message
        }
    }
}
