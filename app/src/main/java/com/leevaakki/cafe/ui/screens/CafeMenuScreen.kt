package com.leevaakki.cafe.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leevaakki.cafe.BuildConfig
import androidx.compose.material.icons.filled.ShoppingCart
import com.leevaakki.cafe.MainActivity
import com.leevaakki.cafe.ui.components.MenuItemCard
import com.leevaakki.cafe.ui.components.OrderSummaryDialog
import com.leevaakki.cafe.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeMenuScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isRefreshing by viewModel.isRefreshingMenu
    val cartItems = viewModel.cartItems
    val paymentStatus by viewModel.paymentStatus

    val branchName = when (BuildConfig.BRANCH_TYPE) {
        "CAFE" -> "Cafe"
        "DHABA" -> "Dhaba"
        "FARM" -> "Farm"
        else -> "Cafe"
    }

    // TOGGLE: Set this to true when you want to enable the Farm menu inside the app
    val showFarmMenuInApp = false 

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vaakki $branchName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (BuildConfig.BRANCH_TYPE != "FARM" || showFarmMenuInApp) {
                        IconButton(onClick = {
                            viewModel.fetchMenuFromWordPress(BuildConfig.WORDPRESS_URL)
                        }) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(
                                    Icons.Default.Fastfood,
                                    contentDescription = "Refresh Menu"
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (BuildConfig.BRANCH_TYPE == "FARM" && !showFarmMenuInApp) {
            // Website Redirect View (Current Plan)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Fresh from the Farm",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Our fresh poultry and farm products are currently available for purchase exclusively through our website. In-app ordering coming soon!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://farm.leevaakki.com"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                        Spacer(Modifier.width(8.dp))
                        Text("VISIT FARM WEBSITE")
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:0,0?q=Lee+Vaakki+Cafe+Dhaba+Farm")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.Map, null)
                        Spacer(Modifier.width(8.dp))
                        Text("VIEW ON GOOGLE MAPS")
                    }

                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBack) {
                        Text("Explore Cafe or Dhaba")
                    }
                }
            }
        } else {
            // Full Menu View (For Cafe, Dhaba, and Future Farm App)
            Column(modifier = Modifier.padding(padding)) {
                if (paymentStatus != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            paymentStatus!!,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (viewModel.menuItems.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator()
                        } else {
                            Text("No items found in menu.")
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.menuItems) { item ->
                            MenuItemCard(
                                item = item,
                                quantity = cartItems[item.id] ?: 0,
                                onAddToCart = { viewModel.toggleCartItem(item.id) },
                                onUpdateQuantity = { qty -> viewModel.updateCartQuantity(item.id, qty) }
                            )
                        }
                    }
                }
            }
        }
    }
}
