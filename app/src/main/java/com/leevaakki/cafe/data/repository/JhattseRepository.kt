package com.leevaakki.cafe.data.repository

import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JhattseRepository @Inject constructor(
    private val client: HttpClient
) {
    // Note: Jhattse usually provides a Partner API for Order Injection
    // The following is a template for the order sync logic
    
    suspend fun syncOrderToJhattse(
        apiKey: String,
        orderId: String,
        customerName: String,
        items: List<Pair<String, Int>>, // Name to Quantity
        totalAmount: Double
    ): Boolean {
        return try {
            val response: HttpResponse = client.post("https://api.jhattse.com/v1/business/orders/sync") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("order_id", orderId)
                    put("source", "LeeVaakkiApp")
                    put("customer_name", customerName)
                    put("total_amount", totalAmount)
                    put("items", buildJsonArray {
                        items.forEach { (name, qty) ->
                            addJsonObject {
                                put("name", name)
                                put("quantity", qty)
                            }
                        }
                    })
                })
            }
            
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                Log.d("JhattseIntegration", "Order $orderId synced successfully")
                true
            } else {
                Log.e("JhattseIntegration", "Sync failed: ${response.status}")
                false
            }
        } catch (e: Exception) {
            Log.e("JhattseIntegration", "Error syncing order: ${e.message}")
            false
        }
    }
}
