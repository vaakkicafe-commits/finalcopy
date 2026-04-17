package com.leevaakki.cafe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leevaakki.cafe.BuildConfig
import com.leevaakki.cafe.models.InventoryItem
import com.leevaakki.cafe.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminInventoryScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val branchId = when (BuildConfig.BRANCH_TYPE) {
        "CAFE" -> "cafe"
        "DHABA" -> "dhaba"
        "FARM" -> "farm"
        else -> "cafe"
    }

    LaunchedEffect(Unit) {
        viewModel.listenToInventory(branchId)
    }

    val inventoryItems = viewModel.inventoryItems
    var showAdjustDialog by remember { mutableStateOf<InventoryItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (inventoryItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No inventory items found for this branch.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(inventoryItems) { item ->
                    InventoryCard(
                        item = item,
                        onAdjust = { showAdjustDialog = item }
                    )
                }
            }
        }

        if (showAdjustDialog != null) {
            AdjustStockDialog(
                item = showAdjustDialog!!,
                onDismiss = { showAdjustDialog = null },
                onConfirm = { delta, reason ->
                    viewModel.updateInventoryQuantity(
                        itemId = showAdjustDialog!!.id,
                        itemName = showAdjustDialog!!.name,
                        delta = delta,
                        reason = reason
                    )
                    showAdjustDialog = null
                }
            )
        }
    }
}

@Composable
fun InventoryCard(item: InventoryItem, onAdjust: () -> Unit) {
    val isLowStock = item.quantity <= item.minThreshold

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isLowStock) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Category: ${item.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isLowStock) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Low Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = onAdjust,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("ADJUST", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun AdjustStockDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var isAdd by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Stock: ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = isAdd,
                        onClick = { isAdd = true },
                        label = { Text("Add Stock") },
                        leadingIcon = { Icon(Icons.Default.Add, null) }
                    )
                    FilterChip(
                        selected = !isAdd,
                        onClick = { isAdd = false },
                        label = { Text("Remove Stock") },
                        leadingIcon = { Icon(Icons.Default.Remove, null) }
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Quantity (${item.unit})") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (e.g., New Delivery, Waste)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val delta = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(if (isAdd) delta else -delta, reason)
                },
                enabled = amount.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
