package com.leevaakki.cafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leevaakki.cafe.BuildConfig
import com.leevaakki.cafe.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSalesAnalyticsScreen(
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
        viewModel.listenToAllOrders(branchId)
    }

    val orders = viewModel.orders
    val totalRevenue = orders.sumOf { it.totalAmount }
    val orderCount = orders.size
    val avgOrderValue = if (orderCount > 0) totalRevenue / orderCount else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Analytics") },
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
                    "Overview - ${branchId.uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Revenue",
                        value = "₹${"%.2f".format(totalRevenue)}",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    StatCard(
                        title = "Orders",
                        value = orderCount.toString(),
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }

            item {
                StatCard(
                    title = "Avg. Order Value",
                    value = "₹${"%.2f".format(avgOrderValue)}",
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            }

            item {
                Text(
                    "Recent Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Simple revenue list as a placeholder for a chart
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF4CAF50))
                            Spacer(Modifier.width(8.dp))
                            Text("Revenue by Day", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        
                        // Grouping orders by day for a simple report
                        val ordersByDay = orders.groupBy { 
                            it.createdAt?.toDate()?.let { date ->
                                SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
                            } ?: "Unknown"
                        }

                        ordersByDay.forEach { (day, dayOrders) ->
                            val dayRevenue = dayOrders.sumOf { it.totalAmount }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(day, style = MaterialTheme.typography.bodyMedium)
                                Text("₹${"%.2f".format(dayRevenue)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
