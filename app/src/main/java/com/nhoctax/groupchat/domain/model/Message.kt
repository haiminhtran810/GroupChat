package com.nhoctax.groupchat.domain.model

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.PENDING,
    val isEncrypted: Boolean = true,
    val encryptedPayload: EncryptedPayload? = null
)
