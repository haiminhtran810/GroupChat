package com.nhoctax.groupchat.ui

import com.nhoctax.groupchat.domain.model.Contact
import com.nhoctax.groupchat.domain.model.Conversation
import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.domain.model.SafetyNumber
import com.nhoctax.groupchat.domain.repository.ChatRepository
import com.nhoctax.groupchat.domain.repository.ContactRepository
import com.nhoctax.groupchat.domain.repository.SecurityRepository
import com.nhoctax.groupchat.ui.chat.ChatViewModel
import com.nhoctax.groupchat.ui.conversation.ConversationListViewModel
import com.nhoctax.groupchat.ui.verification.KeyVerificationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPair

@OptIn(ExperimentalCoroutinesApi::class)
class PresentationViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun conversationList_filtersAndSelectsConversation() = runTest(dispatcher) {
        val alice = Contact(id = "alice", name = "Alice")
        val bob = Contact(id = "bob", name = "Bob")
        val chatRepository = FakeChatRepository(
            initialConversations = listOf(
                Conversation(id = alice.id, participant = alice),
                Conversation(id = bob.id, participant = bob)
            )
        )
        val viewModel = ConversationListViewModel(chatRepository, FakeContactRepository(alice, bob))

        advanceUntilIdle()
        viewModel.onSearchQueryChanged("ali")
        viewModel.selectConversation("alice")
        advanceUntilIdle()

        assertEquals(listOf("alice"), viewModel.uiState.value.filteredConversations.map { it.id })
        assertEquals("alice", viewModel.uiState.value.selectedConversationId)
        assertEquals("alice", chatRepository.lastMarkedRead)
    }

    @Test
    fun chat_failedSend_restoresDraftAndShowsError() = runTest(dispatcher) {
        val alice = Contact(id = "alice", name = "Alice")
        val chatRepository = FakeChatRepository(sendFailure = IllegalStateException("offline"))
        val viewModel = ChatViewModel(
            chatRepository = chatRepository,
            contactRepository = FakeContactRepository(alice),
            securityRepository = FakeSecurityRepository()
        )

        viewModel.setConversationId("alice")
        advanceUntilIdle()
        viewModel.onInputTextChanged(" secret message ")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals("secret message", viewModel.uiState.value.inputText)
        assertEquals("offline", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun verification_toggle_updatesRepositoryAndUi() = runTest(dispatcher) {
        val alice = Contact(id = "alice", name = "Alice", publicKeyPem = "public-key")
        val contacts = FakeContactRepository(alice)
        val security = FakeSecurityRepository(
            safetyNumber = SafetyNumber(
                user1Id = SecurityRepository.CURRENT_USER_ID,
                user2Id = alice.id,
                displayFingerprint = "12345 67890 12345 67890 12345 67890",
                rawHash = byteArrayOf(1)
            )
        )
        val viewModel = KeyVerificationViewModel(security, contacts)

        viewModel.loadContactId("alice")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isVerified)

        viewModel.toggleVerification()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isVerified)
        assertEquals("alice" to true, security.lastVerification)
        assertTrue(viewModel.uiState.value.statusMessage!!.contains("verified"))
    }
}

private class FakeChatRepository(
    initialConversations: List<Conversation> = emptyList(),
    private val sendFailure: Throwable? = null
) : ChatRepository {
    private val conversations = MutableStateFlow(initialConversations)
    private val messages = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    var lastMarkedRead: String? = null

    override fun getConversations(): Flow<List<Conversation>> = conversations

    override fun getMessages(conversationId: String): Flow<List<Message>> =
        messages.getOrPut(conversationId) { MutableStateFlow(emptyList()) }

    override suspend fun sendMessage(
        conversationId: String,
        recipientId: String,
        content: String
    ): Result<Message> = sendFailure?.let { Result.failure(it) }
        ?: Result.success(
            Message(
                id = "sent",
                conversationId = conversationId,
                senderId = SecurityRepository.CURRENT_USER_ID,
                recipientId = recipientId,
                content = content
            )
        )

    override suspend fun receiveMessage(message: Message): Result<Unit> = Result.success(Unit)

    override suspend fun markAsRead(conversationId: String): Result<Unit> {
        lastMarkedRead = conversationId
        return Result.success(Unit)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = Result.success(Unit)
    override suspend fun clearConversation(conversationId: String): Result<Unit> = Result.success(Unit)
}

private class FakeContactRepository(vararg initialContacts: Contact) : ContactRepository {
    private val contacts = MutableStateFlow(initialContacts.toList())

    override fun getContacts(): Flow<List<Contact>> = contacts
    override fun getContactById(contactId: String): Flow<Contact?> =
        contacts.map { values -> values.find { it.id == contactId } }

    override suspend fun getContact(contactId: String): Contact? = contacts.value.find { it.id == contactId }

    override suspend fun addContact(contact: Contact): Result<Unit> {
        contacts.value = contacts.value.filterNot { it.id == contact.id } + contact
        return Result.success(Unit)
    }

    override suspend fun updateContact(contact: Contact): Result<Unit> = addContact(contact)

    override suspend fun verifyContact(contactId: String, isVerified: Boolean): Result<Unit> {
        contacts.value = contacts.value.map {
            if (it.id == contactId) it.copy(isVerified = isVerified) else it
        }
        return Result.success(Unit)
    }
}

private class FakeSecurityRepository(
    private val safetyNumber: SafetyNumber? = null
) : SecurityRepository {
    var lastVerification: Pair<String, Boolean>? = null

    override suspend fun getOrCreateUserKeyPair(alias: String): KeyPair =
        error("Not needed by presentation tests")

    override suspend fun getUserPublicKeyPem(alias: String): String = "user-public-key"

    override suspend fun generateSafetyNumber(contactId: String): Result<SafetyNumber> =
        safetyNumber?.let { Result.success(it) } ?: Result.failure(IllegalStateException("Missing"))

    override fun getSafetyNumber(contactId: String): Flow<SafetyNumber?> = flowOf(safetyNumber)

    override suspend fun verifySafetyNumber(contactId: String, isVerified: Boolean): Result<Unit> {
        lastVerification = contactId to isVerified
        return Result.success(Unit)
    }
}
