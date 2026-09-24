package com.nhoctax.groupchat.di

import com.nhoctax.groupchat.data.datasource.LocalContactDataSource
import com.nhoctax.groupchat.data.datasource.LocalMessageDataSource
import com.nhoctax.groupchat.data.datasource.PeerMessagingDataSource
import com.nhoctax.groupchat.data.repository.ChatRepositoryImpl
import com.nhoctax.groupchat.data.repository.ContactRepositoryImpl
import com.nhoctax.groupchat.data.repository.SecurityRepositoryImpl
import com.nhoctax.groupchat.domain.crypto.CryptoManager
import com.nhoctax.groupchat.domain.crypto.KeyStoreManager
import com.nhoctax.groupchat.domain.crypto.SafetyNumberGenerator
import com.nhoctax.groupchat.domain.model.Contact
import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.domain.model.MessageStatus
import com.nhoctax.groupchat.domain.repository.ChatRepository
import com.nhoctax.groupchat.domain.repository.ContactRepository
import com.nhoctax.groupchat.domain.repository.SecurityRepository
import com.nhoctax.groupchat.domain.usecase.DecryptMessageUseCase
import com.nhoctax.groupchat.domain.usecase.EncryptMessageUseCase
import com.nhoctax.groupchat.domain.usecase.GenerateKeyPairUseCase
import com.nhoctax.groupchat.domain.usecase.GenerateSafetyNumberUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class AppContainer {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val keyStoreManager: KeyStoreManager by lazy { KeyStoreManager() }
    val cryptoManager: CryptoManager by lazy { CryptoManager() }
    val safetyNumberGenerator: SafetyNumberGenerator by lazy { SafetyNumberGenerator() }

    val localContactDataSource: LocalContactDataSource by lazy { LocalContactDataSource() }
    val localMessageDataSource: LocalMessageDataSource by lazy { LocalMessageDataSource() }
    val peerMessagingDataSource: PeerMessagingDataSource by lazy { PeerMessagingDataSource() }

    val contactRepository: ContactRepository by lazy {
        ContactRepositoryImpl(localContactDataSource)
    }

    val securityRepository: SecurityRepository by lazy {
        SecurityRepositoryImpl(
            keyStoreManager = keyStoreManager,
            safetyNumberGenerator = safetyNumberGenerator,
            contactRepository = contactRepository
        )
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(
            localMessageDataSource = localMessageDataSource,
            peerMessagingDataSource = peerMessagingDataSource,
            contactRepository = contactRepository,
            cryptoManager = cryptoManager,
            keyStoreManager = keyStoreManager,
            externalScope = applicationScope
        )
    }

    // UseCases
    val encryptMessageUseCase: EncryptMessageUseCase by lazy { EncryptMessageUseCase(cryptoManager) }
    val decryptMessageUseCase: DecryptMessageUseCase by lazy { DecryptMessageUseCase(cryptoManager) }
    val generateKeyPairUseCase: GenerateKeyPairUseCase by lazy { GenerateKeyPairUseCase(keyStoreManager) }
    val generateSafetyNumberUseCase: GenerateSafetyNumberUseCase by lazy { GenerateSafetyNumberUseCase(safetyNumberGenerator) }

    init {
        seedInitialData()
        startSimulatedPeerReplies()
    }

    private fun startSimulatedPeerReplies() {
        // Demo transport: every outgoing encrypted payload receives an independently
        // encrypted peer response, exercising the same receive/decrypt flow as a socket.
        chatRepository
        applicationScope.launch {
            peerMessagingDataSource.outgoingMessages.collect { outgoing ->
                delay(800)
                val userPublicKey = keyStoreManager
                    .getOrCreateKeyPair(SecurityRepository.DEFAULT_USER_ALIAS)
                    .public
                peerMessagingDataSource.simulatePeerReply(
                    conversationId = outgoing.conversationId,
                    senderId = outgoing.recipientId,
                    recipientId = SecurityRepository.CURRENT_USER_ID,
                    plainTextResponse = "Secure automatic reply received.",
                    recipientPublicKey = userPublicKey,
                    cryptoManager = cryptoManager
                )
            }
        }
    }

    private fun seedInitialData() {
        applicationScope.launch {
            try {
                val userKeyPair = keyStoreManager.getOrCreateKeyPair(SecurityRepository.DEFAULT_USER_ALIAS)
                val userPublicKeyPem = keyStoreManager.exportPublicKeyPem(userKeyPair.public)

                // Seed Contact 1: Alice (Verified)
                val aliceKeyPair = keyStoreManager.getOrCreateKeyPair("contact_alice_key")
                val alicePublicPem = keyStoreManager.exportPublicKeyPem(aliceKeyPair.public)
                val alice = Contact(
                    id = "contact_alice",
                    name = "Alice Smith",
                    phoneNumber = "+1 555-0101",
                    publicKeyPem = alicePublicPem,
                    isVerified = true,
                    lastSeen = System.currentTimeMillis() - 120000
                )

                // Seed Contact 2: Bob (Unverified)
                val bobKeyPair = keyStoreManager.getOrCreateKeyPair("contact_bob_key")
                val bobPublicPem = keyStoreManager.exportPublicKeyPem(bobKeyPair.public)
                val bob = Contact(
                    id = "contact_bob",
                    name = "Bob Jones",
                    phoneNumber = "+1 555-0102",
                    publicKeyPem = bobPublicPem,
                    isVerified = false,
                    lastSeen = System.currentTimeMillis() - 3600000
                )

                // Seed Contact 3: Charlie (Verified)
                val charlieKeyPair = keyStoreManager.getOrCreateKeyPair("contact_charlie_key")
                val charliePublicPem = keyStoreManager.exportPublicKeyPem(charlieKeyPair.public)
                val charlie = Contact(
                    id = "contact_charlie",
                    name = "Charlie Brown",
                    phoneNumber = "+1 555-0103",
                    publicKeyPem = charliePublicPem,
                    isVerified = true,
                    lastSeen = System.currentTimeMillis() - 86400000
                )

                localContactDataSource.saveContact(alice)
                localContactDataSource.saveContact(bob)
                localContactDataSource.saveContact(charlie)
                localContactDataSource.updateVerificationStatus(alice.id, true)
                localContactDataSource.updateVerificationStatus(charlie.id, true)

                // Seed messages with Alice
                val now = System.currentTimeMillis()
                val aliceMsg1Text = "Hey there! Is our channel secure?"
                val alicePayload1 = cryptoManager.encrypt(aliceMsg1Text, userKeyPair.public)
                val msg1 = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = "contact_alice",
                    senderId = "contact_alice",
                    recipientId = SecurityRepository.CURRENT_USER_ID,
                    content = alicePayload1.ciphertextBase64,
                    timestamp = now - 180000,
                    status = MessageStatus.READ,
                    isEncrypted = true,
                    encryptedPayload = alicePayload1
                )

                val userMsg1Text = "Yes, end-to-end encrypted with RSA-OAEP & AES-GCM!"
                val userPayload1 = cryptoManager.encrypt(userMsg1Text, aliceKeyPair.public)
                val msg2 = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = "contact_alice",
                    senderId = SecurityRepository.CURRENT_USER_ID,
                    recipientId = "contact_alice",
                    content = userMsg1Text,
                    timestamp = now - 120000,
                    status = MessageStatus.READ,
                    isEncrypted = true,
                    encryptedPayload = userPayload1
                )

                val aliceMsg2Text = "Great! Check my safety number to verify key identity."
                val alicePayload2 = cryptoManager.encrypt(aliceMsg2Text, userKeyPair.public)
                val msg3 = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = "contact_alice",
                    senderId = "contact_alice",
                    recipientId = SecurityRepository.CURRENT_USER_ID,
                    content = alicePayload2.ciphertextBase64,
                    timestamp = now - 60000,
                    status = MessageStatus.DELIVERED,
                    isEncrypted = true,
                    encryptedPayload = alicePayload2
                )

                localMessageDataSource.saveMessages(listOf(msg1, msg2, msg3))

                // Seed messages with Bob
                val bobMsg1Text = "Hey, let's setup key verification."
                val bobPayload1 = cryptoManager.encrypt(bobMsg1Text, userKeyPair.public)
                val msgBob1 = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = "contact_bob",
                    senderId = "contact_bob",
                    recipientId = SecurityRepository.CURRENT_USER_ID,
                    content = bobPayload1.ciphertextBase64,
                    timestamp = now - 360000,
                    status = MessageStatus.DELIVERED,
                    isEncrypted = true,
                    encryptedPayload = bobPayload1
                )
                localMessageDataSource.saveMessages(listOf(msgBob1))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
