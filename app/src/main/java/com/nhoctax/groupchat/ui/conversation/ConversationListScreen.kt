package com.nhoctax.groupchat.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhoctax.groupchat.domain.model.Contact
import com.nhoctax.groupchat.domain.model.Conversation
import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.ui.theme.GroupChatTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    uiState: ConversationListUiState,
    onConversationSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GroupChat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "E2E Encrypted",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "E2E Secure",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search conversations...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val conversations = uiState.filteredConversations

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.searchQuery.isEmpty()) "No conversations yet." else "No matching conversations.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            isSelected = conversation.id == uiState.selectedConversationId,
                            onClick = { onConversationSelected(conversation.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circle
            ContactAvatar(
                name = conversation.participant.name,
                isVerified = conversation.participant.isVerified
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Conversation details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.participant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (conversation.participant.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Verified Contact",
                            tint = Color(0xFF2E7D32), // Green verified shield
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val lastMsg = conversation.lastMessage
                val snippetText = when {
                    lastMsg == null -> "No messages yet"
                    lastMsg.isEncrypted -> "🔒 Encrypted message"
                    else -> lastMsg.content
                }

                Text(
                    text = snippetText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time & Unread count
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val timeString = formatTimestamp(conversation.updatedAt)
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactAvatar(
    name: String,
    isVerified: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Int = 48
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val avatarBgColor = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(avatarBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        if (isVerified) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(9.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(date)
}

@Preview(showBackground = true)
@Composable
fun ConversationListScreenPreview() {
    GroupChatTheme {
        ConversationListScreen(
            uiState = ConversationListUiState(
                conversations = listOf(
                    Conversation(
                        id = "1",
                        participant = Contact(
                            id = "1",
                            name = "Alice Smith",
                            isVerified = true
                        ),
                        lastMessage = Message(
                            id = "m1",
                            conversationId = "1",
                            senderId = "1",
                            recipientId = "me",
                            content = "Check my safety number!",
                            isEncrypted = false
                        ),
                        unreadCount = 2,
                        updatedAt = System.currentTimeMillis()
                    ),
                    Conversation(
                        id = "2",
                        participant = Contact(
                            id = "2",
                            name = "Bob Jones",
                            isVerified = false
                        ),
                        lastMessage = Message(
                            id = "m2",
                            conversationId = "2",
                            senderId = "me",
                            recipientId = "2",
                            content = "Let's setup key verification",
                            isEncrypted = true
                        ),
                        unreadCount = 0,
                        updatedAt = System.currentTimeMillis() - 3600000
                    )
                ),
                filteredConversations = listOf(
                    Conversation(
                        id = "1",
                        participant = Contact(
                            id = "1",
                            name = "Alice Smith",
                            isVerified = true
                        ),
                        lastMessage = Message(
                            id = "m1",
                            conversationId = "1",
                            senderId = "1",
                            recipientId = "me",
                            content = "Check my safety number!",
                            isEncrypted = false
                        ),
                        unreadCount = 2,
                        updatedAt = System.currentTimeMillis()
                    ),
                    Conversation(
                        id = "2",
                        participant = Contact(
                            id = "2",
                            name = "Bob Jones",
                            isVerified = false
                        ),
                        lastMessage = Message(
                            id = "m2",
                            conversationId = "2",
                            senderId = "me",
                            recipientId = "2",
                            content = "Let's setup key verification",
                            isEncrypted = true
                        ),
                        unreadCount = 0,
                        updatedAt = System.currentTimeMillis() - 3600000
                    )
                )
            ),
            onConversationSelected = {},
            onSearchQueryChanged = {}
        )
    }
}
