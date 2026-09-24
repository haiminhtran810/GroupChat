package com.nhoctax.groupchat.data.datasource

import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.domain.model.MessageStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LocalMessageDataSourceTest {

    private lateinit var dataSource: LocalMessageDataSource

    @Before
    fun setUp() {
        dataSource = LocalMessageDataSource()
    }

    @Test
    fun saveMessageAndGetMessagesFlow_returnsSavedMessagesSortedByTimestamp() = runTest {
        val msg1 = Message(
            id = "msg1",
            conversationId = "conv1",
            senderId = "user1",
            recipientId = "user2",
            content = "Hello",
            timestamp = 1000L
        )
        val msg2 = Message(
            id = "msg2",
            conversationId = "conv1",
            senderId = "user2",
            recipientId = "user1",
            content = "Hi",
            timestamp = 2000L
        )

        dataSource.saveMessage(msg2)
        dataSource.saveMessage(msg1)

        val messages = dataSource.getMessagesFlow("conv1").first()

        assertEquals(2, messages.size)
        assertEquals("msg1", messages[0].id)
        assertEquals("msg2", messages[1].id)
    }

    @Test
    fun updateMessageStatus_updatesStatusCorrectly() = runTest {
        val msg = Message(
            id = "msg1",
            conversationId = "conv1",
            senderId = "user1",
            recipientId = "user2",
            content = "Hello",
            status = MessageStatus.PENDING
        )
        dataSource.saveMessage(msg)

        dataSource.updateMessageStatus("msg1", MessageStatus.DELIVERED)

        val updated = dataSource.getMessageById("msg1")
        assertEquals(MessageStatus.DELIVERED, updated?.status)
    }

    @Test
    fun deleteMessage_removesMessageFromDataSource() = runTest {
        val msg = Message(
            id = "msg1",
            conversationId = "conv1",
            senderId = "user1",
            recipientId = "user2",
            content = "Hello"
        )
        dataSource.saveMessage(msg)

        dataSource.deleteMessage("msg1")

        assertNull(dataSource.getMessageById("msg1"))
    }

    @Test
    fun clearConversation_removesAllMessagesForThatConversation() = runTest {
        val msg1 = Message("msg1", "conv1", "u1", "u2", "Hello")
        val msg2 = Message("msg2", "conv2", "u1", "u3", "Hey")
        dataSource.saveMessage(msg1)
        dataSource.saveMessage(msg2)

        dataSource.clearConversation("conv1")

        val conv1Messages = dataSource.getMessagesFlow("conv1").first()
        val conv2Messages = dataSource.getMessagesFlow("conv2").first()

        assertEquals(0, conv1Messages.size)
        assertEquals(1, conv2Messages.size)
    }
}
