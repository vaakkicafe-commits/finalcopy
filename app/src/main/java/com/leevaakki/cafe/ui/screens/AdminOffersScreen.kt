package com.leevaakki.cafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.leevaakki.cafe.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOffersScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    var offerTitle by remember { mutableStateOf("") }
    var offerMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Send Offers to Customers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Broadcast a new offer or announcement to all active customer chat threads.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = offerTitle,
                onValueChange = { offerTitle = it },
                label = { Text("Offer Title (e.g. Weekend Special)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = offerMessage,
                onValueChange = { offerMessage = it },
                label = { Text("Offer Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    if (offerTitle.isBlank() || offerMessage.isBlank()) return@Button
                    isSending = true
                    
                    val broadcastText = "📢 *${offerTitle.trim()}*\n\n${offerMessage.trim()}"
                    
                    // Fetch all threads to broadcast
                    FirebaseFirestore.getInstance().collection("threads")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val threadIds = snapshot.documents.map { it.id }
                            var sentCount = 0
                            
                            if (threadIds.isEmpty()) {
                                isSending = false
                                return@addOnSuccessListener
                            }

                            threadIds.forEach { threadId ->
                                viewModel.sendMessage(
                                    threadId = threadId,
                                    senderId = "admin",
                                    text = broadcastText,
                                    participants = listOf("admin"), // Participants will be updated by repo logic usually
                                    senderType = "admin"
                                )
                                sentCount++
                            }
                            
                            isSending = false
                            offerTitle = ""
                            offerMessage = ""
                            
                            // Show success
                            // viewModelScope/GlobalScope is needed for snackbar usually, but for simplicity:
                        }
                        .addOnFailureListener {
                            isSending = false
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending && offerTitle.isNotBlank() && offerMessage.isNotBlank()
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Broadcast to All Customers")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Active Offers from WordPress",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Preview current offers being shown to users
            if (viewModel.offerImages.isEmpty()) {
                Text("No active WordPress offers found.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.offerImages.size) { index ->
                        Card {
                            Text(
                                "Offer Image URL: ${viewModel.offerImages[index].take(50)}...",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
