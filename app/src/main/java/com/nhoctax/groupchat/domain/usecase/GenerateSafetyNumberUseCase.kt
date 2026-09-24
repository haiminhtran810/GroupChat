package com.nhoctax.groupchat.domain.usecase

import com.nhoctax.groupchat.domain.crypto.SafetyNumberGenerator
import com.nhoctax.groupchat.domain.model.SafetyNumber
import java.security.PublicKey

class GenerateSafetyNumberUseCase(
    private val safetyNumberGenerator: SafetyNumberGenerator
) {
    operator fun invoke(
        user1Id: String,
        publicKey1: PublicKey,
        user2Id: String,
        publicKey2: PublicKey
    ): SafetyNumber {
        return safetyNumberGenerator.generateSafetyNumber(
            user1Id = user1Id,
            publicKey1 = publicKey1,
            user2Id = user2Id,
            publicKey2 = publicKey2
        )
    }

    operator fun invoke(
        user1Id: String,
        publicKey1Bytes: ByteArray,
        user2Id: String,
        publicKey2Bytes: ByteArray
    ): SafetyNumber {
        return safetyNumberGenerator.generateSafetyNumber(
            user1Id = user1Id,
            publicKey1Bytes = publicKey1Bytes,
            user2Id = user2Id,
            publicKey2Bytes = publicKey2Bytes
        )
    }
}
