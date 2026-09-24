package com.nhoctax.groupchat.data.datasource

import com.nhoctax.groupchat.domain.crypto.CryptoManager
import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.domain.model.MessageStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.security.PublicKey
import java.util.UUID

class PeerMessagingDataSource {

    private val incomingChannel = Channel<Message>(capacity = Channel.BUFFERED)
    val incomingMessages: Flow<Message> = incomingChannel.receiveAsFlow()

    private val outgoingChannel = Channel<Message>(capacity = Channel.BUFFERED)
    val outgoingMessages: Flow<Message> = outgoingChannel.receiveAsFlow()

    suspend fun sendOverNetwork(message: Message): Message {
        val updatedMessage = message.copy(status = MessageStatus.SENT)
        outgoingChannel.send(updatedMessage)
        return updatedMessage
    }

    suspend fun simulateIncomingMessage(message: Message) {
        incomingChannel.send(message)
    }

    suspend fun simulatePeerReply(
        conversationId: String,
        senderId: String,
        recipientId: String,
        plainTextResponse: String,
        recipientPublicKey: PublicKey,
        cryptoManager: CryptoManager,
        timestamp: Long = System.currentTimeMillis()
    ): Message {
        val payload = cryptoManager.encrypt(plainTextResponse, recipientPublicKey)
        val incomingMessage = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = senderId,
            recipientId = recipientId,
            content = payload.ciphertextBase64,
            timestamp = timestamp,
            status = MessageStatus.DELIVERED,
            isEncrypted = true,
            encryptedPayload = payload
        )
        incomingChannel.send(incomingMessage)
        return incomingMessage
    }
}
