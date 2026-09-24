package com.nhoctax.groupchat.domain.usecase

import com.nhoctax.groupchat.domain.crypto.CryptoManager
import com.nhoctax.groupchat.domain.model.EncryptedPayload
import com.nhoctax.groupchat.domain.model.Message
import java.security.PrivateKey

class DecryptMessageUseCase(
    private val cryptoManager: CryptoManager
) {
    fun decryptPayload(payload: EncryptedPayload, privateKey: PrivateKey): String {
        return cryptoManager.decrypt(payload, privateKey)
    }

    fun decryptMessage(message: Message, recipientPrivateKey: PrivateKey): Message {
        val payload = message.encryptedPayload
            ?: return message.copy(isEncrypted = false)

        val decryptedText = cryptoManager.decrypt(payload, recipientPrivateKey)
        return message.copy(
            content = decryptedText,
            isEncrypted = false
        )
    }
}
