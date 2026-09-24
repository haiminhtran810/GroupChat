package com.nhoctax.groupchat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nhoctax.groupchat.domain.model.Contact
import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.domain.repository.ChatRepository
import com.nhoctax.groupchat.domain.repository.ContactRepository
import com.nhoctax.groupchat.domain.repository.SecurityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversationId: String? = null,
    val contact: Contact? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isE2eEncrypted: Boolean = true,
    val isVerifiedContact: Boolean = false,
    val showPayloadMap: Map<String, Boolean> = emptyMap(),
    val isGlobalPayloadView: Boolean = false,
    val isSending: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    @Suppress("unused") private val securityRepository: SecurityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null
    private var contactJob: Job? = null

    fun setConversationId(conversationId: String?) {
        if (conversationId == null) {
            _uiState.value = ChatUiState()
            return
        }

        if (_uiState.value.conversationId == conversationId) {
            return
        }

        _uiState.update {
            ChatUiState(
                conversationId = conversationId,
                isLoading = true
            )
        }

        messagesJob?.cancel()
        contactJob?.cancel()

        contactJob = viewModelScope.launch {
            contactRepository.getContactById(conversationId).collect { contact ->
                _uiState.update { state ->
                    state.copy(
                        contact = contact,
                        isVerifiedContact = contact?.isVerified == true
                    )
                }
            }
        }

        messagesJob = viewModelScope.launch {
            chatRepository.getMessages(conversationId).collect { messages ->
                _uiState.update { state ->
                    state.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
                if (messages.any {
                        it.senderId != SecurityRepository.CURRENT_USER_ID &&
                                it.status != com.nhoctax.groupchat.domain.model.MessageStatus.READ
                    }) {
                    chatRepository.markAsRead(conversationId)
                }
            }
        }

        viewModelScope.launch {
            chatRepository.markAsRead(conversationId)
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        if (currentState.isSending) return
        val convId = currentState.conversationId ?: return
        val text = currentState.inputText.trim()
        if (text.isEmpty()) return

        val recipientId = currentState.contact?.id ?: convId

        _uiState.update { it.copy(isSending = true, inputText = "") }

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                conversationId = convId,
                recipientId = recipientId,
                content = text
            )

            result.onFailure { error ->
                _uiState.update { state ->
                    if (state.conversationId != convId) return@update state
                    state.copy(
                        isSending = false,
                        inputText = text,
                        errorMessage = error.message ?: "Failed to send encrypted message"
                    )
                }
            }.onSuccess {
                _uiState.update { state ->
                    if (state.conversationId == convId) state.copy(isSending = false) else state
                }
            }
        }
    }

    fun toggleMessagePayloadView(messageId: String) {
        _uiState.update { state ->
            val currentMap = state.showPayloadMap.toMutableMap()
            val currentValue = currentMap[messageId] ?: false
            currentMap[messageId] = !currentValue
            state.copy(showPayloadMap = currentMap)
        }
    }

    fun toggleGlobalPayloadView() {
        _uiState.update { state ->
            state.copy(isGlobalPayloadView = !state.isGlobalPayloadView)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearConversation() {
        val convId = _uiState.value.conversationId ?: return
        viewModelScope.launch {
            chatRepository.clearConversation(convId).onFailure { error ->
                _uiState.update { state ->
                    state.copy(errorMessage = error.message ?: "Failed to clear conversation")
                }
            }
        }
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val contactRepository: ContactRepository,
        private val securityRepository: SecurityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(chatRepository, contactRepository, securityRepository) as T
        }
    }
}
