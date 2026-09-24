package com.nhoctax.groupchat.domain.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CryptoManagerTest {

    private lateinit var cryptoManager: CryptoManager
    private lateinit var keyStoreManager: KeyStoreManager

    @Before
    fun setUp() {
        cryptoManager = CryptoManager()
        keyStoreManager = KeyStoreManager()
    }

    @Test
    fun testEncryptAndDecryptPayload() {
        val recipientKeyPair = keyStoreManager.generateKeyPair("recipient_key")
        val originalText = "Top secret group message containing E2EE content."

        val encryptedPayload = cryptoManager.encrypt(originalText, recipientKeyPair.public)

        assertNotNull(encryptedPayload)
        assertNotNull(encryptedPayload.ciphertext)
        assertNotNull(encryptedPayload.iv)
        assertNotNull(encryptedPayload.encryptedSymmetricKey)
        assertTrue(encryptedPayload.ciphertextBase64.isNotEmpty())
        assertTrue(encryptedPayload.ivBase64.isNotEmpty())
        assertTrue(encryptedPayload.encryptedSymmetricKeyBase64.isNotEmpty())

        val decryptedText = cryptoManager.decrypt(encryptedPayload, recipientKeyPair.private)
        assertEquals(originalText, decryptedText)
    }

    @Test(expected = Exception::class)
    fun testDecryptWithWrongPrivateKeyFails() {
        val recipientKeyPair = keyStoreManager.generateKeyPair("recipient")
        val attackerKeyPair = keyStoreManager.generateKeyPair("attacker")

        val payload = cryptoManager.encrypt("Secret", recipientKeyPair.public)

        // Attempting to decrypt recipient's payload with attacker's key must throw Exception
        cryptoManager.decrypt(payload, attackerKeyPair.private)
    }
}
