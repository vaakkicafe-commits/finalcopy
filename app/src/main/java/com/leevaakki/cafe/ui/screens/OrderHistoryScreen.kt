package com.leevaakki.cafe.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.leevaakki.cafe.BuildConfig
import com.leevaakki.cafe.models.Order
import com.leevaakki.cafe.models.OrderItem
import com.leevaakki.cafe.ui.theme.LeevaakkipvtltdTheme
import com.leevaakki.cafe.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onBrowseMenu: () -> Unit = {},
    onOpenWebsite: (String, String) -> Unit = { _, _ -> }
) {
    val orders = viewModel.orders
    val isLoading by viewModel.isLoadingOrders

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading && orders.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) { ShimmerOrderCard() }
                }
            } else if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No orders found in app",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        
                        val websiteUrl = remember {
                            val urls = mapOf(
                                "CAFE" to "https://leevaakkicafe.com/my-account/orders/",
                                "DHABA" to "https://leevaakkidhaba.com/my-account/orders/",
                                "FARM" to "https://leevaakkipvtld.com/my-account/orders/"
                            )
                            urls[BuildConfig.BRANCH_TYPE] ?: urls["CAFE"]!!
                        }
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = {
                            onOpenWebsite(websiteUrl, "Website Orders")
                        }) {
                            Text("Check Website Orders")
                        }

                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onBrowseMenu) {
                            val buttonText = remember {
                                val labels = mapOf(
                                    "CAFE" to "Visit Cafe Store",
                                    "DHABA" to "Visit Dhaba Store",
                                    "FARM" to "Visit Farm Store"
                                )
                                labels[BuildConfig.BRANCH_TYPE] ?: labels["CAFE"]!!
                            }
                            Text(buttonText)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { order ->
                        val animatedProgress = remember { Animatable(0f) }
                        LaunchedEffect(order.id) {
                            animatedProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
                            )
                        }

                        Box(modifier = Modifier.graphicsLayer {
                            alpha = animatedProgress.value
                            translationY = 40f * (1f - animatedProgress.value)
                        }) {
                            OrderCard(order = order, onOpenWebsite = onOpenWebsite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerOrderCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColor = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize().background(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = shimmerColor,
                start = androidx.compose.ui.geometry.Offset(translateAnim - 300f, translateAnim - 300f),
                end = androidx.compose.ui.geometry.Offset(translateAnim, translateAnim)
            )
        ))
    }
}

@Composable
fun OrderCard(order: Order, onOpenWebsite: (String, String) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateString = order.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Unknown date"

    val branchIcon = when (order.branchId.uppercase()) {
        "CAFE" -> Icons.Default.Coffee
        "DHABA" -> Icons.Default.Restaurant
        "FARM" -> Icons.Default.Agriculture
        else -> Icons.AutoMirrored.Filled.ReceiptLong
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = branchIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Order #${order.id.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val shareUrl = when (order.branchId.uppercase()) {
                                "CAFE" -> "https://leevaakkicafe.com/order-tracking/"
                                "DHABA" -> "https://leevaakkidhaba.com/order-tracking/"
                                else -> "https://leevaakkipvtld.com/order-tracking/"
                            }
                            val shareText = "Check out my Lee Vaakki™ order #${order.id.takeLast(6).uppercase()}!\n\n" +
                                    "Status: ${order.status.uppercase()}\n" +
                                    "Total: ₹${order.totalAmount.toInt()}\n\n" +
                                    "Track it here: $shareUrl"
                            
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Order",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        StatusChip(status = order.status)
                        if (order.branchId.isNotEmpty()) {
                            val outletInfo = buildString {
                                append(order.branchId.uppercase())
                                if (order.outletType.isNotEmpty()) append(" (${order.outletType.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }})")
                                if (order.location.isNotEmpty()) append(" - ${order.location}")
                            }
                            Text(
                                text = outletInfo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.name} x ${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "₹${(item.price * item.quantity).toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "₹${order.totalAmount.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                OutlinedButton(
                    onClick = {
                        val url = when (order.branchId.uppercase()) {
                            "CAFE" -> "https://leevaakkicafe.com/my-account/orders/"
                            "DHABA" -> "https://leevaakkidhaba.com/my-account/orders/"
                            "FARM" -> "https://leevaakkipvtld.com/my-account/orders/"
                            else -> "https://leevaakkipvtld.com"
                        }
                        onOpenWebsite(url, "Checkout / Buy Again")
                    },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Buy Again", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (order.deliveryPartnerName.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (order.status == "out_for_delivery") {
                            PulseAnimation()
                            Spacer(Modifier.width(8.dp))
                        }
                        Column {
                            Text(
                                text = "Delivery by: ${order.deliveryPartnerName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (order.status == "out_for_delivery") Color(0xFFF44336) else MaterialTheme.colorScheme.secondary
                            )
                            if (order.deliveryPartnerPhone.isNotEmpty() && order.status == "out_for_delivery") {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, "tel:${order.deliveryPartnerPhone}".toUri())
                                        context.startActivity(intent)
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Call Partner", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    
                    if (order.status == "out_for_delivery" && order.deliveryPartnerLat != null && order.deliveryPartnerLng != null) {
                        Button(
                            onClick = {
                                val gmmIntentUri = "google.navigation:q=${order.deliveryPartnerLat},${order.deliveryPartnerLng}".toUri()
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {
                                    val webIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com/maps/dir/?api=1&destination=${order.deliveryPartnerLat},${order.deliveryPartnerLng}".toUri())
                                    context.startActivity(webIntent)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                        ) {
                            Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Track Live", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (color, icon) = when (status.lowercase()) {
        "paid", "delivered" -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
        "preparing" -> Color(0xFFFF9800) to Icons.Default.Timer
        "out_for_delivery" -> Color(0xFF2196F3) to Icons.Default.DeliveryDining
        "cancelled" -> Color(0xFFF44336) to Icons.Default.Cancel
        else -> MaterialTheme.colorScheme.outline to Icons.Default.Info
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = status.uppercase().replace("_", " "),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .graphicsLayer(alpha = alpha)
            .background(Color(0xFFF44336), shape = CircleShape)
    )
}

@Preview(showBackground = true)
@Composable
fun OrderCardPreview() {
    LeevaakkipvtltdTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            OrderCard(
                order = Order(
                    id = "order_123456789",
                    status = "out_for_delivery",
                    totalAmount = 450.0,
                    branchId = "CAFE",
                    items = listOf(
                        OrderItem(name = "Classic Coffee", quantity = 2, price = 150.0),
                        OrderItem(name = "Paneer Tikka", quantity = 1, price = 150.0)
                    ),
                    deliveryPartnerName = "Rahul Sharma",
                    deliveryPartnerLat = 28.6139,
                    deliveryPartnerLng = 77.2090,
                    createdAt = com.google.firebase.Timestamp.now()
                )
            )
        }
    }
}
