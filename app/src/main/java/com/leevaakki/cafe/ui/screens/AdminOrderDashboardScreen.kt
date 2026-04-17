package com.leevaakki.cafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leevaakki.cafe.BuildConfig
import com.leevaakki.cafe.models.Order
import com.leevaakki.cafe.ui.components.LocationTracker
import com.leevaakki.cafe.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderDashboardScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locationTracker = remember { LocationTracker(context, viewModel) }
    var activeTrackingOrderId by remember { mutableStateOf<String?>(null) }
    
    val branchId = when (BuildConfig.BRANCH_TYPE) {
        "CAFE" -> "cafe"
        "DHABA" -> "dhaba"
        "FARM" -> "farm"
        else -> "cafe"
    }

    LaunchedEffect(Unit) {
        viewModel.listenToAllOrders(branchId)
    }

    val orders = viewModel.orders

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin: $branchId Orders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No orders found for this branch.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { order ->
                    AdminOrderCard(
                        order = order,
                        isTracking = activeTrackingOrderId == order.id,
                        onUpdateStatus = { newStatus, partner ->
                            viewModel.updateOrderStatus(order.id, newStatus, partner)
                        },
                        onToggleTracking = {
                            if (activeTrackingOrderId == order.id) {
                                locationTracker.stopTracking()
                                activeTrackingOrderId = null
                            } else {
                                activeTrackingOrderId?.let { locationTracker.stopTracking() }
                                locationTracker.startTracking(order.id)
                                activeTrackingOrderId = order.id
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Cleanup tracking when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            locationTracker.stopTracking()
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order, 
    isTracking: Boolean,
    onUpdateStatus: (String, String?) -> Unit,
    onToggleTracking: () -> Unit
) {
    var showStatusDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateString = order.createdAt?.toDate()?.let { dateFormat.format(it) } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Order #${order.id.takeLast(6).uppercase()}", fontWeight = FontWeight.Bold)
                    Text(text = dateString, style = MaterialTheme.typography.bodySmall)
                }
                StatusChip(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Customer: ${order.userName}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Phone: ${order.userPhone}", style = MaterialTheme.typography.bodySmall)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            order.items.forEach { item ->
                Text(text = "• ${item.name} x ${item.quantity}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Total: ₹${order.totalAmount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showStatusDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Status")
                }

                if (order.status == "out_for_delivery" || order.status == "preparing") {
                    Button(
                        onClick = onToggleTracking,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTracking) Color.Red else Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            if (isTracking) Icons.Default.Stop else Icons.Default.LocationOn, 
                            contentDescription = null, 
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isTracking) "Stop GPS" else "Start GPS")
                    }
                }
            }
        }
    }

    if (showStatusDialog) {
        var partnerName by remember { mutableStateOf(order.deliveryPartnerName) }
        val statuses = listOf("paid", "preparing", "out_for_delivery", "delivered", "cancelled")

        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Order Status") },
            text = {
                Column {
                    Text("Select new status:")
                    statuses.forEach { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (order.status == status),
                                onClick = { 
                                    onUpdateStatus(status, if (status == "out_for_delivery") partnerName else null)
                                    showStatusDialog = false
                                }
                            )
                            Text(text = status.replace("_", " ").uppercase(), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    
                    if (order.status == "preparing" || order.status == "out_for_delivery") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = partnerName,
                            onValueChange = { partnerName = it },
                            label = { Text("Delivery Partner Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) { Text("Close") }
            }
        )
    }
}
