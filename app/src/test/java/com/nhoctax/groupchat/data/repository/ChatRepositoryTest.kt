package com.nhoctax.groupchat.data.repository

import com.nhoctax.groupchat.data.datasource.LocalContactDataSource
import com.nhoctax.groupchat.data.datasource.LocalMessageDataSource
import com.nhoctax.groupchat.data.datasource.PeerMessagingDataSource
import com.nhoctax.groupchat.domain.crypto.CryptoManager
import com.nhoctax.groupchat.domain.crypto.KeyStoreManager
import com.nhoctax.groupchat.domain.model.Contact
import com.nhoctax.groupchat.domain.model.MessageStatus
import com.nhoctax.groupchat.domain.repository.SecurityRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    private lateinit var localMessageDataSource: LocalMessageDataSource
    private lateinit var peerMessagingDataSource: PeerMessagingDataSource
    private lateinit var localContactDataSource: LocalContactDataSource
    private lateinit var contactRepository: ContactRepositoryImpl
    private lateinit var cryptoManager: CryptoManager
    private lateinit var keyStoreManager: KeyStoreManager
    private lateinit var chatRepository: ChatRepositoryImpl

    private val userAlias = "user_me_test"
    private val userId = SecurityRepository.CURRENT_USER_ID

    @Before
    fun setUp() {
        localMessageDataSource = LocalMessageDataSource()
        peerMessagingDataSource = PeerMessagingDataSource()
        localContactDataSource = LocalContactDataSource()
        contactRepository = ContactRepositoryImpl(localContactDataSource)
        cryptoManager = CryptoManager()
        keyStoreManager = KeyStoreManager()

        // Generate key pair for test user
        keyStoreManager.generateKeyPair(userAlias)

        chatRepository = ChatRepositoryImpl(
            localMessageDataSource = localMessageDataSource,
            peerMessagingDataSource = peerMessagingDataSource,
            contactRepository = contactRepository,
            cryptoManager = cryptoManager,
            keyStoreManager = keyStoreManager,
            userAlias = userAlias,
            userId = userId,
            externalScope = TestScope(UnconfinedTestDispatcher())
        )
    }

    @Test
    fun sendMessage_encryptsMessageWithRecipientPublicKey() = runTest {
        val aliceKeyPair = keyStoreManager.generateKeyPair("alice_key")
        val alicePem = keyStoreManager.exportPublicKeyPem(aliceKeyPair.public)

        val alice = Contact(
            id = "alice",
            name = "Alice",
            publicKeyPem = alicePem
        )
        contactRepository.addContact(alice)

        val sendResult = chatRepository.sendMessage(
            conversationId = "alice",
            recipientId = "alice",
            content = "Top Secret Message"
        )

        assertTrue(sendResult.isSuccess)
        val sentMessage = sendResult.getOrThrow()

        assertEquals("alice", sentMessage.conversationId)
        assertEquals(MessageStatus.SENT, sentMessage.status)
        assertTrue(sentMessage.isEncrypted)
        assertNotNull(sentMessage.encryptedPayload)

        // Decrypt with Alice's private key to verify payload validity
        val decryptedText = cryptoManager.decrypt(
            sentMessage.encryptedPayload!!,
            aliceKeyPair.private
        )
        assertEquals("Top Secret Message", decryptedText)
    }

    @Test
    fun receiveMessage_decryptsIncomingEncryptedMessage() = runTest {
        val aliceKeyPair = keyStoreManager.generateKeyPair("alice_key")
        val alicePem = keyStoreManager.exportPublicKeyPem(aliceKeyPair.public)
        val alice = Contact(id = "alice", name = "Alice", publicKeyPem = alicePem)
        contactRepository.addContact(alice)

        val userPublicKey = keyStoreManager.getPublicKey(userAlias)!!

        // Alice sends encrypted message to user
        peerMessagingDataSource.simulatePeerReply(
            conversationId = "alice",
            senderId = "alice",
            recipientId = userId,
            plainTextResponse = "Hello from Alice!",
            recipientPublicKey = userPublicKey,
            cryptoManager = cryptoManager
        )

        val messages = chatRepository.getMessages("alice").first()

        assertEquals(1, messages.size)
        val receivedMessage = messages[0]
        assertEquals("Hello from Alice!", receivedMessage.content)
        assertEquals(false, receivedMessage.isEncrypted) // Decrypted for display
    }

    @Test
    fun getConversations_aggregatesUnreadCountAndLastMessage() = runTest {
        val bobKeyPair = keyStoreManager.generateKeyPair("bob_key")
        val bobPem = keyStoreManager.exportPublicKeyPem(bobKeyPair.public)
        val bob = Contact(id = "bob", name = "Bob", publicKeyPem = bobPem)
        contactRepository.addContact(bob)

        val userPublicKey = keyStoreManager.getPublicKey(userAlias)!!

        // Bob sends 2 encrypted messages with distinct timestamps
        peerMessagingDataSource.simulatePeerReply(
            conversationId = "bob",
            senderId = "bob",
            recipientId = userId,
            plainTextResponse = "Message 1",
            recipientPublicKey = userPublicKey,
            cryptoManager = cryptoManager,
            timestamp = 1000L
        )
        peerMessagingDataSource.simulatePeerReply(
            conversationId = "bob",
            senderId = "bob",
            recipientId = userId,
            plainTextResponse = "Message 2",
            recipientPublicKey = userPublicKey,
            cryptoManager = cryptoManager,
            timestamp = 2000L
        )

        val conversations = chatRepository.getConversations().first()

        assertEquals(1, conversations.size)
        val conv = conversations[0]
        assertEquals("bob", conv.id)
        assertEquals("Bob", conv.participant.name)
        assertEquals("Message 2", conv.lastMessage?.content)
        assertEquals(2, conv.unreadCount)
    }

    @Test
    fun markAsRead_resetsUnreadCountInConversations() = runTest {
        val bobKeyPair = keyStoreManager.generateKeyPair("bob_key")
        val bobPem = keyStoreManager.exportPublicKeyPem(bobKeyPair.public)
        val bob = Contact(id = "bob", name = "Bob", publicKeyPem = bobPem)
        contactRepository.addContact(bob)

        val userPublicKey = keyStoreManager.getPublicKey(userAlias)!!

        peerMessagingDataSource.simulatePeerReply(
            conversationId = "bob",
            senderId = "bob",
            recipientId = userId,
            plainTextResponse = "Hey there",
            recipientPublicKey = userPublicKey,
            cryptoManager = cryptoManager
        )

        val convsBefore = chatRepository.getConversations().first()
        assertEquals(1, convsBefore[0].unreadCount)

        chatRepository.markAsRead("bob")

        val convsAfter = chatRepository.getConversations().first()
        assertEquals(0, convsAfter[0].unreadCount)
    }
}
