package com.nhoctax.groupchat.domain.model

import com.nhoctax.groupchat.domain.crypto.Base64Utils

data class EncryptedPayload(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val encryptedSymmetricKey: ByteArray,
    val algorithm: String = DEFAULT_ALGORITHM,
) {
    companion object {
        const val DEFAULT_ALGORITHM = "AES-256-GCM / RSA-OAEP-256"
    }

    val ciphertextBase64: String
        get() = Base64Utils.encode(ciphertext)

    val ivBase64: String
        get() = Base64Utils.encode(iv)

    val encryptedSymmetricKeyBase64: String
        get() = Base64Utils.encode(encryptedSymmetricKey)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedPayload

        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!encryptedSymmetricKey.contentEquals(other.encryptedSymmetricKey)) return false
        return algorithm == other.algorithm
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + encryptedSymmetricKey.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        return result
    }
}
