package com.nhoctax.groupchat.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nhoctax.groupchat.domain.model.Conversation
import com.nhoctax.groupchat.domain.repository.ChatRepository
import com.nhoctax.groupchat.domain.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationListUiState(
    val conversations: List<Conversation> = emptyList(),
    val filteredConversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedConversationId: String? = null,
    val errorMessage: String? = null
)

class ConversationListViewModel(
    private val chatRepository: ChatRepository,
    @Suppress("unused") private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState(isLoading = true))
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.getConversations().collect { conversations ->
                _uiState.update { state ->
                    val query = state.searchQuery.trim().lowercase()
                    val filtered = if (query.isEmpty()) {
                        conversations
                    } else {
                        conversations.filter { conv ->
                            conv.participant.name.lowercase().contains(query) ||
                                    (conv.lastMessage?.content?.lowercase()?.contains(query) == true)
                        }
                    }
                    state.copy(
                        conversations = conversations,
                        filteredConversations = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val trimmed = query.lowercase().trim()
            val filtered = if (trimmed.isEmpty()) {
                state.conversations
            } else {
                state.conversations.filter { conv ->
                    conv.participant.name.lowercase().contains(trimmed) ||
                            (conv.lastMessage?.content?.lowercase()?.contains(trimmed) == true)
                }
            }
            state.copy(
                searchQuery = query,
                filteredConversations = filtered
            )
        }
    }

    fun selectConversation(conversationId: String) {
        _uiState.update { it.copy(selectedConversationId = conversationId) }
        viewModelScope.launch {
            chatRepository.markAsRead(conversationId)
        }
    }

    fun clearConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.clearConversation(conversationId)
        }
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val contactRepository: ContactRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConversationListViewModel(chatRepository, contactRepository) as T
        }
    }
}
