package com.leevaakki.cafe.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leevaakki.cafe.models.MenuItem
import com.leevaakki.cafe.viewmodel.ChatViewModel

@Composable
fun OrderSummaryDialog(
    viewModel: ChatViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cartItems = viewModel.cartItems
    val menuItems = viewModel.menuItems
    
    val totalAmount = cartItems.entries.sumOf { (id, qty) ->
        val item = menuItems.find { it.id == id }
        (item?.price ?: 0.0) * qty
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order Summary") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp)
                ) {
                    items(cartItems.toList()) { (id, qty) ->
                        val item = menuItems.find { it.id == id }
                        if (item != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.name} x $qty", modifier = Modifier.weight(1f))
                                Text("₹${item.price * qty}")
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", fontWeight = FontWeight.Bold)
                    Text("₹$totalAmount", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm & Pay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
