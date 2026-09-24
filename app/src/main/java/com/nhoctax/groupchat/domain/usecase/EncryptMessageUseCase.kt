package com.nhoctax.groupchat.domain.usecase

import com.nhoctax.groupchat.domain.crypto.CryptoManager
import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.domain.model.MessageStatus
import java.security.PublicKey
import java.util.UUID

class EncryptMessageUseCase(
    private val cryptoManager: CryptoManager
) {
    operator fun invoke(
        conversationId: String,
        senderId: String,
        recipientId: String,
        plainTextContent: String,
        recipientPublicKey: PublicKey
    ): Message {
        val payload = cryptoManager.encrypt(plainTextContent, recipientPublicKey)
        val messageId = UUID.randomUUID().toString()

        return Message(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            recipientId = recipientId,
            content = plainTextContent,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING,
            isEncrypted = true,
            encryptedPayload = payload
        )
    }
}
