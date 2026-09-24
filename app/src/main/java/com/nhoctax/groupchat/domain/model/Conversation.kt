package com.nhoctax.groupchat.domain.model

data class Conversation(
    val id: String,
    val participant: Contact,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
