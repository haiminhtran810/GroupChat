package com.nhoctax.groupchat.ui.chat

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhoctax.groupchat.domain.model.Contact
import com.nhoctax.groupchat.domain.model.EncryptedPayload
import com.nhoctax.groupchat.domain.model.Message
import com.nhoctax.groupchat.domain.model.MessageStatus
import com.nhoctax.groupchat.domain.repository.SecurityRepository
import com.nhoctax.groupchat.ui.conversation.ContactAvatar
import com.nhoctax.groupchat.ui.theme.GroupChatTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onBackClick: (() -> Unit)?,
    onOpenVerification: (String) -> Unit,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onTogglePayloadView: (String) -> Unit,
    onToggleGlobalPayloadView: () -> Unit,
    onClearChat: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onClearError()
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    if (uiState.conversationId == null) {
        EmptyChatPlaceholder(modifier = modifier)
        return
    }

    val contactName = uiState.contact?.name ?: "Conversation"
    val isVerified = uiState.isVerifiedContact

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            uiState.conversationId.let { onOpenVerification(it) }
                        }
                    ) {
                        ContactAvatar(
                            name = contactName,
                            isVerified = isVerified,
                            sizeDp = 38
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = contactName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // E2E Status Indicator Badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock",
                                    tint = if (isVerified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isVerified) "Verified E2E Secured" else "End-to-End Encrypted",
                                    fontSize = 11.sp,
                                    color = if (isVerified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Toggle Encrypted Payload View button
                    IconButton(onClick = onToggleGlobalPayloadView) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Toggle Encrypted Payload View",
                            tint = if (uiState.isGlobalPayloadView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Open Safety Number Verification
                    IconButton(onClick = { onOpenVerification(uiState.conversationId) }) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Verify Safety Number",
                            tint = if (isVerified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Safety Number Verification") },
                                onClick = {
                                    showMenu = false
                                    onOpenVerification(uiState.conversationId)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Toggle Encrypted Payload View") },
                                onClick = {
                                    showMenu = false
                                    onToggleGlobalPayloadView()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Code, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Chat Messages") },
                                onClick = {
                                    showMenu = false
                                    onClearChat()
                                }
                            )
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
            // Security Card Notice at Top of Chat
            E2eSecurityCard(
                isVerified = isVerified,
                onCardClick = { onOpenVerification(uiState.conversationId) }
            )

            // Messages list
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        val isSentByMe = message.senderId == SecurityRepository.CURRENT_USER_ID
                        val isPayloadExpanded = uiState.isGlobalPayloadView || (uiState.showPayloadMap[message.id] == true)

                        MessageBubble(
                            message = message,
                            isSentByMe = isSentByMe,
                            showRawPayload = isPayloadExpanded,
                            onTogglePayload = { onTogglePayloadView(message.id) }
                        )
                    }
                }
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            // Chat Input Bar
            ChatInputBar(
                inputText = uiState.inputText,
                onInputTextChanged = onInputTextChanged,
                onSendMessage = onSendMessage,
                isSending = uiState.isSending
            )
        }
    }
}

@Composable
fun EmptyChatPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "End-to-End Encrypted Chat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select a contact from the list to start a private, secure conversation protected by RSA-OAEP & AES-GCM.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun E2eSecurityCard(
    isVerified: Boolean,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isVerified) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isVerified) Icons.Default.VerifiedUser else Icons.Default.Lock,
                contentDescription = "Security Status",
                tint = if (isVerified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isVerified) "Verified End-to-End Encryption" else "End-to-End Encrypted Chat",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isVerified) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = if (isVerified)
                        "Safety number is verified for this contact. Tap to view key fingerprints."
                    else
                        "Messages are encrypted with RSA & AES. Tap to verify Safety Number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isVerified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isSentByMe: Boolean,
    showRawPayload: Boolean,
    onTogglePayload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleShape = if (isSentByMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val containerColor = if (isSentByMe) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isSentByMe) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = containerColor,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (showRawPayload && message.encryptedPayload != null) {
                    // Raw Encrypted Payload View
                    EncryptedPayloadCard(
                        payload = message.encryptedPayload
                    )
                } else {
                    // Decrypted / Plain Content View
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted Message",
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 3.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Footer Row: Toggle Payload button + Timestamp + Delivery Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Payload Button
                    Text(
                        text = if (showRawPayload) "Hide Ciphertext" else "Show Ciphertext",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onTogglePayload)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )

                        if (isSentByMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            MessageStatusIcon(status = message.status, tint = textColor.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EncryptedPayloadCard(
    payload: EncryptedPayload,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RAW ENCRYPTED PAYLOAD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            PayloadField("Algorithm", payload.algorithm)
            PayloadField("Ciphertext (Base64)", payload.ciphertextBase64)
            PayloadField("IV (Base64)", payload.ivBase64)
            PayloadField("Encrypted Symmetric Key", payload.encryptedSymmetricKeyBase64)
        }
    }
}

@Composable
fun PayloadField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus, tint: Color) {
    when (status) {
        MessageStatus.PENDING -> Text("...", fontSize = 10.sp, color = tint)
        MessageStatus.SENT -> Icon(Icons.Default.Done, contentDescription = "Sent", modifier = Modifier.size(12.dp), tint = tint)
        MessageStatus.DELIVERED -> Icon(Icons.Default.DoneAll, contentDescription = "Delivered", modifier = Modifier.size(12.dp), tint = tint)
        MessageStatus.READ -> Icon(Icons.Default.DoneAll, contentDescription = "Read", modifier = Modifier.size(12.dp), tint = Color(0xFF1976D2))
        MessageStatus.FAILED -> Text("!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = { Text("Type an encrypted message...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted input",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                maxLines = 4,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendMessage,
                enabled = inputText.trim().isNotEmpty() && !isSending,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.trim().isNotEmpty() && !isSending)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = if (inputText.trim().isNotEmpty() && !isSending)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
fun ChatScreenPreview() {
    GroupChatTheme {
        ChatScreen(
            uiState = ChatUiState(
                conversationId = "contact_alice",
                contact = Contact(
                    id = "contact_alice",
                    name = "Alice Smith",
                    isVerified = true
                ),
                messages = listOf(
                    Message(
                        id = "1",
                        conversationId = "contact_alice",
                        senderId = "contact_alice",
                        recipientId = "me",
                        content = "Hey! Is our chat E2E encrypted?",
                        timestamp = System.currentTimeMillis() - 120000,
                        status = MessageStatus.READ,
                        isEncrypted = true,
                        encryptedPayload = EncryptedPayload(
                            ciphertext = "EncryptedTextBytes".toByteArray(),
                            iv = "123456789012".toByteArray(),
                            encryptedSymmetricKey = "SymKeyBytes".toByteArray()
                        )
                    ),
                    Message(
                        id = "2",
                        conversationId = "contact_alice",
                        senderId = "me",
                        recipientId = "contact_alice",
                        content = "Yes! Secured with RSA-OAEP and AES-GCM.",
                        timestamp = System.currentTimeMillis() - 60000,
                        status = MessageStatus.DELIVERED,
                        isEncrypted = true,
                        encryptedPayload = EncryptedPayload(
                            ciphertext = "EncryptedResponseBytes".toByteArray(),
                            iv = "123456789012".toByteArray(),
                            encryptedSymmetricKey = "SymKeyBytes2".toByteArray()
                        )
                    )
                ),
                isVerifiedContact = true
            ),
            onBackClick = {},
            onOpenVerification = {},
            onInputTextChanged = {},
            onSendMessage = {},
            onTogglePayloadView = {},
            onToggleGlobalPayloadView = {},
            onClearChat = {},
            onClearError = {}
        )
    }
}
