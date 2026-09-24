package com.nhoctax.groupchat.domain.usecase

import com.nhoctax.groupchat.domain.crypto.KeyStoreManager
import java.security.KeyPair

class GenerateKeyPairUseCase(
    private val keyStoreManager: KeyStoreManager
) {
    operator fun invoke(alias: String): KeyPair {
        return keyStoreManager.getOrCreateKeyPair(alias)
    }
}
