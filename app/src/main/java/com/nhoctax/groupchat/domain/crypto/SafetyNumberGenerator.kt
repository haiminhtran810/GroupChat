package com.nhoctax.groupchat.domain.crypto

import com.nhoctax.groupchat.domain.model.SafetyNumber
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.PublicKey
import java.util.Locale

class SafetyNumberGenerator {

    companion object {
        private const val HASH_ALGORITHM = "SHA-256"
        private const val BLOCK_COUNT = 6
        private const val DIGITS_PER_BLOCK = 5
        private const val MODULO = 100000
    }

    fun generateSafetyNumber(
        user1Id: String,
        publicKey1: PublicKey,
        user2Id: String,
        publicKey2: PublicKey,
    ): SafetyNumber {
        return generateSafetyNumber(
            user1Id = user1Id,
            publicKey1Bytes = publicKey1.encoded,
            user2Id = user2Id,
            publicKey2Bytes = publicKey2.encoded,
        )
    }

    fun generateSafetyNumber(
        user1Id: String,
        publicKey1Bytes: ByteArray,
        user2Id: String,
        publicKey2Bytes: ByteArray,
    ): SafetyNumber {
        val (firstUser, firstKey, secondUser, secondKey) = if (user1Id <= user2Id) {
            Quadruple(user1Id, publicKey1Bytes, user2Id, publicKey2Bytes)
        } else {
            Quadruple(user2Id, publicKey2Bytes, user1Id, publicKey1Bytes)
        }

        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        digest.update(firstUser.toByteArray(Charsets.UTF_8))
        digest.update(firstKey)
        digest.update(secondUser.toByteArray(Charsets.UTF_8))
        digest.update(secondKey)

        val rawHash = digest.digest()
        val displayFingerprint = formatFingerprint(rawHash)

        return SafetyNumber(
            user1Id = user1Id,
            user2Id = user2Id,
            displayFingerprint = displayFingerprint,
            rawHash = rawHash,
            isVerified = false,
        )
    }

    fun formatFingerprint(hashBytes: ByteArray): String {
        require(hashBytes.size >= 24) { "Hash bytes must be at least 24 bytes long" }
        val buffer = ByteBuffer.wrap(hashBytes)
        val blocks = MutableList(BLOCK_COUNT) {
            val chunk = buffer.int and 0x7FFFFFFF
            val value = chunk % MODULO
            String.format(Locale.US, "%0${DIGITS_PER_BLOCK}d", value)
        }
        return blocks.joinToString(" ")
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )
}
