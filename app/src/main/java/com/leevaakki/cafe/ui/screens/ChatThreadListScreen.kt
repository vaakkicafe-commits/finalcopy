package com.leevaakki.cafe.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.leevaakki.cafe.BuildConfig
import com.leevaakki.cafe.R
import com.leevaakki.cafe.ui.components.OfferSlider
import com.leevaakki.cafe.ui.components.ThreadItemRow
import com.leevaakki.cafe.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadListScreen(
    auth: FirebaseAuth,
    chatViewModel: ChatViewModel = viewModel(),
    isAdmin: Boolean = false,
    onThreadClick: (String) -> Unit,
    onOpenMenu: () -> Unit = {},
    onOpenOrderHistory: () -> Unit = {},
    onOpenAdminOrders: () -> Unit = {},
    onOpenAdminManagement: () -> Unit = {},
    onOpenWebsite: (String, String) -> Unit = { _, _ -> }
) {
    val currentUserId = auth.currentUser?.uid ?: ""
    val branchType = when (BuildConfig.BRANCH_TYPE) {
        "CAFE" -> "cafe"
        "DHABA" -> "dhaba"
        "FARM" -> "farm"
        else -> "cafe"
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedBranchFilter by remember { mutableStateOf("all") }
    var showArchived by remember { mutableStateOf(false) }

    val isRefreshingOffers by chatViewModel.isRefreshingOffers
    val isRefreshingMenu by chatViewModel.isRefreshingMenu

    // Admin sees everything, regular users see only their branch
    val filterBranchId = if (isAdmin) "admin" else branchType

    val threads = chatViewModel.threads

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            chatViewModel.listenToThreads(currentUserId, filterBranchId)
        }
    }

    val filteredThreads = threads.filter { thread ->
        val matchesSearch = thread.title.contains(searchQuery, ignoreCase = true) ||
                thread.customerName.contains(searchQuery, ignoreCase = true) ||
                thread.customerPhone.contains(searchQuery, ignoreCase = true) ||
                thread.orderId.contains(searchQuery, ignoreCase = true) ||
                thread.lastMessage.contains(searchQuery, ignoreCase = true)

        val matchesBranch = if (isAdmin && selectedBranchFilter != "all") {
            thread.branchId == selectedBranchFilter
        } else {
            true
        }

        val matchesArchive = thread.isArchived == showArchived

        matchesSearch && matchesBranch && matchesArchive
    }.sortedByDescending { it.updatedAt }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Using the app launcher icon as the small logo
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher),
                                contentDescription = "Lee Vaakki Logo",
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Lee Vaakki",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isAdmin) {
                                        "Private Limited"
                                    } else {
                                        branchType.replaceFirstChar { it.uppercase() }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            chatViewModel.fetchOffersFromWordPress(BuildConfig.WORDPRESS_URL)
                            chatViewModel.fetchMenuFromWordPress(BuildConfig.WORDPRESS_URL)
                            chatViewModel.fetchSettingsFromWordPress(BuildConfig.WORDPRESS_URL)
                        }) {
                            if (isRefreshingOffers || isRefreshingMenu) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh WordPress Data")
                            }
                        }

                        if (isAdmin) {
                            IconButton(onClick = onOpenAdminManagement) {
                                Icon(Icons.Default.SettingsSuggest, contentDescription = "Management")
                            }
                            IconButton(onClick = onOpenAdminOrders) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = "Manage Orders")
                            }
                        }

                        if (!isAdmin) {
                            IconButton(onClick = onOpenOrderHistory) {
                                Icon(Icons.Default.History, contentDescription = "Order History")
                            }
                            IconButton(onClick = {
                                val url = BuildConfig.WORDPRESS_URL
                                onOpenWebsite(url, "Lee Vaakki™ Store")
                            }) {
                                Icon(Icons.Default.Language, contentDescription = "Visit Website")
                            }
                            IconButton(onClick = onOpenMenu) {
                                Icon(Icons.Default.RestaurantMenu, contentDescription = "Menu")
                            }
                        }
                        
                        var showFilterMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("All Branches") },
                                    leadingIcon = { Icon(Icons.Default.AllInclusive, null) },
                                    onClick = { selectedBranchFilter = "all"; showFilterMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cafe") },
                                    leadingIcon = { Icon(Icons.Default.Coffee, null) },
                                    onClick = { selectedBranchFilter = "cafe"; showFilterMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dhaba") },
                                    leadingIcon = { Icon(Icons.Default.DinnerDining, null) },
                                    onClick = { selectedBranchFilter = "dhaba"; showFilterMenu = false }
                                )
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = { Text(if (showArchived) "View Active" else "View Archived") },
                                leadingIcon = { 
                                    Icon(
                                        if (showArchived) Icons.Default.ChatBubbleOutline else Icons.Default.Archive, 
                                        null
                                    ) 
                                },
                                onClick = { showArchived = !showArchived; showFilterMenu = false }
                            )
                        }
                    }
                )

                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        chatViewModel.searchMessages(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search by name, phone, order ID, or messages...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; chatViewModel.searchMessages("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val user = auth.currentUser
                        chatViewModel.createThread(
                            userId = currentUserId,
                            branchId = branchType,
                            title = "${branchType.replaceFirstChar { it.uppercase() }} Support",
                            customerName = user?.displayName ?: user?.email?.split("@")?.firstOrNull() ?: "Customer",
                            customerPhone = ""
                        ) { threadId ->
                            if (threadId != null) {
                                onThreadClick(threadId)
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Chat") }
                )
            }
        }
    ) { padding ->
        val searchResults = chatViewModel.searchResults

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Only show slider if not searching and for customers
            if (!isAdmin && searchQuery.isEmpty() && chatViewModel.offerImages.isNotEmpty()) {
                item {
                    OfferSlider(images = chatViewModel.offerImages)
                }
            }

            if (searchQuery.isNotEmpty()) {
                if (searchResults.isNotEmpty()) {
                    item {
                        Text(
                            "Message Results",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(searchResults) { (thread, message) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onThreadClick(thread.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${thread.customerName} (${thread.branchId.uppercase()})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = thread.updatedAt?.let { java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(it.toDate()) } ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(16.dp))
                        Text(
                            "Thread Results",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (filteredThreads.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No results found", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (filteredThreads.isEmpty() && searchQuery.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active chats", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            items(filteredThreads) { thread ->
                ThreadItemRow(
                    thread = thread,
                    isAdmin = isAdmin,
                    onArchive = { isArchived ->
                        chatViewModel.archiveThread(thread.id, isArchived)
                    },
                    onDelete = {
                        chatViewModel.deleteThread(thread.id)
                    }
                ) {
                    onThreadClick(thread.id)
                }
            }
        }
    }
}
