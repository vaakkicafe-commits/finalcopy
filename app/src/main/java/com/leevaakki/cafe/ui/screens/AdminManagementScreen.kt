package com.leevaakki.cafe.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leevaakki.cafe.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenAdminOrders: () -> Unit,
    onOpenAdminOffers: () -> Unit,
    onOpenAdminSwiggy: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSalesAnalytics: () -> Unit,
    onOpenEmployment: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Operations",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AdminMenuCard(
                    title = "Order Management",
                    description = "View and manage all active orders, update status and track delivery.",
                    icon = Icons.Default.ReceiptLong,
                    onClick = onOpenAdminOrders
                )
            }

            item {
                AdminMenuCard(
                    title = "Broadcast Offers",
                    description = "Send marketing offers and announcements to all customers.",
                    icon = Icons.Default.Campaign,
                    onClick = onOpenAdminOffers
                )
            }

            item {
                AdminMenuCard(
                    title = "Swiggy Integration",
                    description = "Manage Swiggy API connections, menu sync, and order webhooks.",
                    icon = Icons.Default.CloudSync,
                    onClick = { /* TODO: Implement Swiggy Integration Screen */ }
                )
            }

            item {
                Text(
                    "Business Insights",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                AdminMenuCard(
                    title = "Units & Inventory",
                    description = "Manage stock levels across Cafe, Dhaba, and Farm units.",
                    icon = Icons.Default.Inventory,
                    onClick = onOpenInventory
                )
            }

            item {
                AdminMenuCard(
                    title = "Sales Analytics",
                    description = "View daily, weekly, and monthly sales reports and revenue data.",
                    icon = Icons.Default.BarChart,
                    onClick = onOpenSalesAnalytics
                )
            }

            item {
                Text(
                    "Human Resources",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                AdminMenuCard(
                    title = "Employment Details",
                    description = "Manage staff profiles, attendance, and payroll information.",
                    icon = Icons.Default.Badge,
                    onClick = onOpenEmployment
                )
            }
        }
    }
}

@Composable
fun AdminMenuCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
