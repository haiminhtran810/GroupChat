package com.nhoctax.groupchat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhoctax.groupchat.di.AppContainer
import com.nhoctax.groupchat.ui.chat.ChatScreen
import com.nhoctax.groupchat.ui.chat.ChatViewModel
import com.nhoctax.groupchat.ui.conversation.ConversationListScreen
import com.nhoctax.groupchat.ui.conversation.ConversationListViewModel
import com.nhoctax.groupchat.ui.verification.KeyVerificationScreen
import com.nhoctax.groupchat.ui.verification.KeyVerificationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainAdaptiveApp(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val conversationListViewModel: ConversationListViewModel = viewModel(
        factory = ConversationListViewModel.Factory(
            chatRepository = appContainer.chatRepository,
            contactRepository = appContainer.contactRepository
        )
    )

    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(
            chatRepository = appContainer.chatRepository,
            contactRepository = appContainer.contactRepository,
            securityRepository = appContainer.securityRepository
        )
    )

    val keyVerificationViewModel: KeyVerificationViewModel = viewModel(
        factory = KeyVerificationViewModel.Factory(
            securityRepository = appContainer.securityRepository,
            contactRepository = appContainer.contactRepository
        )
    )

    val conversationListState by conversationListViewModel.uiState.collectAsStateWithLifecycle()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val keyVerificationState by keyVerificationViewModel.uiState.collectAsStateWithLifecycle()

    val navigator = rememberListDetailPaneScaffoldNavigator<String>()

    BackHandler(enabled = navigator.canNavigateBack()) {
        coroutineScope.launch {
            navigator.navigateBack()
        }
    }

    // Auto-select first conversation if available on larger screen
    LaunchedEffect(conversationListState.conversations) {
        if (conversationListState.selectedConversationId == null && conversationListState.conversations.isNotEmpty()) {
            val firstConvId = conversationListState.conversations.first().id
            conversationListViewModel.selectConversation(firstConvId)
            chatViewModel.setConversationId(firstConvId)
        }
    }

    ListDetailPaneScaffold(
        modifier = modifier,
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                ConversationListScreen(
                    uiState = conversationListState,
                    onConversationSelected = { conversationId ->
                        conversationListViewModel.selectConversation(conversationId)
                        chatViewModel.setConversationId(conversationId)
                        coroutineScope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, conversationId)
                        }
                    },
                    onSearchQueryChanged = { query ->
                        conversationListViewModel.onSearchQueryChanged(query)
                    }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                ChatScreen(
                    uiState = chatState,
                    onBackClick = if (navigator.canNavigateBack()) {
                        {
                            coroutineScope.launch {
                                navigator.navigateBack()
                            }
                        }
                    } else null,
                    onOpenVerification = { contactId ->
                        keyVerificationViewModel.loadContactId(contactId)
                        coroutineScope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Extra, contactId)
                        }
                    },
                    onInputTextChanged = { text ->
                        chatViewModel.onInputTextChanged(text)
                    },
                    onSendMessage = {
                        chatViewModel.sendMessage()
                    },
                    onTogglePayloadView = { messageId ->
                        chatViewModel.toggleMessagePayloadView(messageId)
                    },
                    onToggleGlobalPayloadView = {
                        chatViewModel.toggleGlobalPayloadView()
                    },
                    onClearChat = {
                        chatViewModel.clearConversation()
                    },
                    onClearError = {
                        chatViewModel.clearError()
                    }
                )
            }
        },
        extraPane = {
            AnimatedPane {
                KeyVerificationScreen(
                    uiState = keyVerificationState,
                    onBackClick = {
                        coroutineScope.launch {
                            navigator.navigateBack()
                        }
                    },
                    onToggleVerification = {
                        keyVerificationViewModel.toggleVerification()
                    },
                    onClearStatusMessage = {
                        keyVerificationViewModel.clearStatusMessage()
                    }
                )
            }
        }
    )
}
