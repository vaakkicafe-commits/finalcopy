package com.leevaakki.cafe.models

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "pending", // pending, paid, preparing, out_for_delivery, delivered, cancelled
    val paymentId: String = "",
    val createdAt: Timestamp? = null,
    val branchId: String = "", // cafe, dhaba, farm
    val outletType: String = "dining", // dining, cloud_kitchen
    val location: String = "Delhi", // Delhi, Bangalore, etc.
    val deliveryPartnerName: String = "",
    val deliveryPartnerPhone: String = "",
    val deliveryPartnerLat: Double? = null,
    val deliveryPartnerLng: Double? = null,
    val trackingUrl: String = ""
)

data class OrderItem(
    val itemId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
)
