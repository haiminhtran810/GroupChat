package com.nhoctax.groupchat.domain.model

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String? = null,
    val publicKeyPem: String? = null,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val verifiedPublicKeyPem: String? = null,
    val lastSeen: Long? = null
)
