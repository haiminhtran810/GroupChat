package com.nhoctax.groupchat.data.datasource

import com.nhoctax.groupchat.domain.crypto.CryptoManager
import com.nhoctax.groupchat.domain.crypto.KeyStoreManager
import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.domain.model.MessageStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeerMessagingDataSourceTest {

    private lateinit var peerDataSource: PeerMessagingDataSource
    private lateinit var cryptoManager: CryptoManager
    private lateinit var keyStoreManager: KeyStoreManager

    @Before
    fun setUp() {
        peerDataSource = PeerMessagingDataSource()
        cryptoManager = CryptoManager()
        keyStoreManager = KeyStoreManager()
    }

    @Test
    fun sendOverNetwork_emitsMessageToOutgoingMessagesFlow() = runTest {
        val testMsg = Message(
            id = "msg1",
            conversationId = "conv1",
            senderId = "me",
            recipientId = "alice",
            content = "Secret"
        )

        var emittedMessage: Message? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            peerDataSource.outgoingMessages.collect {
                emittedMessage = it
            }
        }

        val resultMsg = peerDataSource.sendOverNetwork(testMsg)

        assertEquals(MessageStatus.SENT, resultMsg.status)
        assertNotNull(emittedMessage)
        assertEquals("msg1", emittedMessage?.id)
    }

    @Test
    fun simulatePeerReply_encryptsResponseAndEmitsToIncomingMessagesFlow() = runTest {
        val recipientKeyPair = keyStoreManager.generateKeyPair("recipient")
        var receivedIncomingMsg: Message? = null

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            peerDataSource.incomingMessages.collect {
                receivedIncomingMsg = it
            }
        }

        peerDataSource.simulatePeerReply(
            conversationId = "conv1",
            senderId = "alice",
            recipientId = "me",
            plainTextResponse = "Secret reply from Alice",
            recipientPublicKey = recipientKeyPair.public,
            cryptoManager = cryptoManager
        )

        assertNotNull(receivedIncomingMsg)
        assertTrue(receivedIncomingMsg?.isEncrypted == true)
        assertNotNull(receivedIncomingMsg?.encryptedPayload)

        val decryptedText = cryptoManager.decrypt(
            receivedIncomingMsg!!.encryptedPayload!!,
            recipientKeyPair.private
        )
        assertEquals("Secret reply from Alice", decryptedText)
    }
}
