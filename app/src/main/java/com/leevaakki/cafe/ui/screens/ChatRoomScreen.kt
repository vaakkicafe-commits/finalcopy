package com.leevaakki.cafe.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.leevaakki.cafe.ui.components.DeliveryTrackingCard
import com.leevaakki.cafe.ui.components.MessageBubble
import com.leevaakki.cafe.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    auth: FirebaseAuth,
    threadId: String,
    chatViewModel: ChatViewModel = viewModel(),
    isAdmin: Boolean = false,
    onBack: () -> Unit
) {
    val currentUserId = auth.currentUser?.uid ?: ""
    val logicUserId = if (isAdmin) "admin" else currentUserId
    val messages = chatViewModel.messages
    var messageText by remember { mutableStateOf("") }

    val thread = chatViewModel.threads.find { it.id == threadId }

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val type = if (it.toString().contains("video")) "video" else "image"
            chatViewModel.uploadMedia(it, threadId, type) { url ->
                if (url != null && thread != null) {
                    val senderType = if (isAdmin) "admin" else "customer"
                    chatViewModel.sendMessage(
                        threadId = threadId,
                        senderId = logicUserId,
                        text = "",
                        participants = thread.participants,
                        senderType = senderType,
                        type = type,
                        mediaUrl = url
                    )
                }
            }
        }
    }

    // Typing indicator logic
    val typingUsers = chatViewModel.typingStatus.filter { it.key != logicUserId && it.value }.keys
    val typingText = if (typingUsers.isNotEmpty()) {
        if (isAdmin) "Customer is typing..." else "Admin is typing..."
    } else null

    LaunchedEffect(threadId) {
        chatViewModel.listenToMessages(threadId, logicUserId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = thread?.customerName?.ifEmpty { thread?.title } ?: "Chat",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (isAdmin && thread != null) {
                            Text(
                                text = "${thread.branchId.uppercase()} | ${thread.customerPhone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        chatViewModel.markAsRead(threadId, logicUserId)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin && thread?.orderId?.isNotEmpty() == true) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                "Order #${thread.orderId}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val listState = rememberLazyListState()

        // Auto-scroll logic for new messages
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(0)
            }
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding() // Handles keyboard overlap
        ) {
            // Delivery tracking card if order is assigned to delivery partner
            val activeOrder = chatViewModel.orders.find { it.id == thread?.orderId }
            if (activeOrder != null && activeOrder.status == "out_for_delivery") {
                DeliveryTrackingCard(order = activeOrder)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                item {
                    if (typingText != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 4.dp)
                        ) {
                            Text(
                                text = typingText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                // Reversed order because of reverseLayout = true
                items(messages.asReversed()) { message ->
                    MessageBubble(
                        threadId = threadId,
                        message = message,
                        isMe = message.senderId == logicUserId,
                        viewModel = chatViewModel,
                        onReaction = { emoji ->
                            chatViewModel.addReaction(threadId, message.id, logicUserId, emoji)
                        }
                    )
                }

                if (chatViewModel.isLoadingMoreMessages.value) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // Media Picker and Message Input
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom // For multi-line text field
                ) {
                    IconButton(
                        onClick = { mediaLauncher.launch("*/*") }, // Allow both image and video
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach Media",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            chatViewModel.setTypingStatus(threadId, logicUserId, it.isNotEmpty())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        placeholder = { Text("Type a message...") },
                        maxLines = 5,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        shape = MaterialTheme.shapes.extraLarge
                    )

                    IconButton(
                        onClick = {
                            if (messageText.trim().isNotEmpty() && thread != null) {
                                val textToSend = messageText.trim()
                                messageText = "" // Immediate UI response
                                chatViewModel.setTypingStatus(threadId, logicUserId, false)
                                
                                val senderType = if (isAdmin) "admin" else "customer"
                                chatViewModel.sendMessage(
                                    threadId = threadId,
                                    senderId = logicUserId,
                                    text = textToSend,
                                    participants = thread.participants,
                                    senderType = senderType
                                )
                            }
                        },
                        enabled = messageText.trim().isNotEmpty(),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.trim().isNotEmpty()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
