package com.leevaakki.cafe.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.leevaakki.cafe.models.ChatThread
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadItemRow(
    thread: ChatThread,
    isAdmin: Boolean,
    onArchive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dateString = thread.updatedAt?.toDate()?.let { sdf.format(it) } ?: ""

    var showOptions by remember { mutableStateOf(false) }
    
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val logicUserId = if (isAdmin) "admin" else currentUserId
    val unreadCount = thread.unreadCountByUser[logicUserId] ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (isAdmin) showOptions = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (unreadCount > 0) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unreadCount > 0) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thread.customerName.ifEmpty { thread.title.ifEmpty { "Customer Support" } },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (thread.branchId == "cafe") Color(0xFFE1F5FE) else Color(0xFFF1F8E9)
                        ) {
                            Text(
                                text = thread.branchId.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (thread.branchId == "cafe") Color(0xFF01579B) else Color(0xFF33691E)
                            )
                        }
                    }
                    if (thread.isArchived) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archived",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            supportingContent = {
                Column {
                    Text(
                        text = thread.lastMessage.ifEmpty { "No messages yet" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (unreadCount > 0) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdmin && thread.customerPhone.isNotEmpty()) {
                        Text(
                            text = thread.customerPhone,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unreadCount > 0) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(unreadCount.toString())
                        }
                    } else if (isAdmin && thread.isArchived) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Closed",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                    }
                }
            },
            leadingContent = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (thread.customerName.ifEmpty { thread.title }).take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        )
    }

    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("Chat Management") },
            text = { Text("Update status for ${thread.customerName}?") },
            confirmButton = {
                TextButton(onClick = {
                    onArchive(!thread.isArchived)
                    showOptions = false
                }) {
                    Text(if (thread.isArchived) "Re-activate Chat" else "Archive / Close Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDelete()
                    showOptions = false
                }) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}
