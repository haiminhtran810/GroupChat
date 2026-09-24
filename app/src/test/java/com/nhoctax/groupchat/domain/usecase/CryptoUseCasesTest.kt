package com.nhoctax.groupchat.domain.usecase

import com.nhoctax.groupchat.domain.crypto.CryptoManager
import com.nhoctax.groupchat.domain.crypto.KeyStoreManager
import com.nhoctax.groupchat.domain.crypto.SafetyNumberGenerator
import com.nhoctax.groupchat.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CryptoUseCasesTest {

    private lateinit var keyStoreManager: KeyStoreManager
    private lateinit var cryptoManager: CryptoManager
    private lateinit var safetyNumberGenerator: SafetyNumberGenerator

    private lateinit var generateKeyPairUseCase: GenerateKeyPairUseCase
    private lateinit var encryptMessageUseCase: EncryptMessageUseCase
    private lateinit var decryptMessageUseCase: DecryptMessageUseCase
    private lateinit var generateSafetyNumberUseCase: GenerateSafetyNumberUseCase

    @Before
    fun setUp() {
        keyStoreManager = KeyStoreManager()
        cryptoManager = CryptoManager()
        safetyNumberGenerator = SafetyNumberGenerator()

        generateKeyPairUseCase = GenerateKeyPairUseCase(keyStoreManager)
        encryptMessageUseCase = EncryptMessageUseCase(cryptoManager)
        decryptMessageUseCase = DecryptMessageUseCase(cryptoManager)
        generateSafetyNumberUseCase = GenerateSafetyNumberUseCase(safetyNumberGenerator)
    }

    @Test
    fun testEndToEndMessageEncryptionAndDecryptionFlow() {
        val aliceKeyPair = generateKeyPairUseCase("alice_key")
        val bobKeyPair = generateKeyPairUseCase("bob_key")

        val plainText = "Hello Bob, this is an encrypted group chat message!"

        // Alice encrypts a message for Bob
        val encryptedMessage = encryptMessageUseCase(
            conversationId = "conv_123",
            senderId = "alice",
            recipientId = "bob",
            plainTextContent = plainText,
            recipientPublicKey = bobKeyPair.public
        )

        assertNotNull(encryptedMessage.encryptedPayload)
        assertTrue(encryptedMessage.isEncrypted)
        assertEquals(MessageStatus.PENDING, encryptedMessage.status)

        // Bob decrypts the message
        val decryptedMessage = decryptMessageUseCase.decryptMessage(encryptedMessage, bobKeyPair.private)

        assertEquals(plainText, decryptedMessage.content)
        assertEquals(false, decryptedMessage.isEncrypted)
    }

    @Test
    fun testSafetyNumberUseCase() {
        val aliceKeyPair = generateKeyPairUseCase("alice")
        val bobKeyPair = generateKeyPairUseCase("bob")

        val safetyNumber = generateSafetyNumberUseCase(
            user1Id = "alice",
            publicKey1 = aliceKeyPair.public,
            user2Id = "bob",
            publicKey2 = bobKeyPair.public
        )

        assertNotNull(safetyNumber)
        assertEquals(6, safetyNumber.displayFingerprint.split(" ").size)
    }
}
