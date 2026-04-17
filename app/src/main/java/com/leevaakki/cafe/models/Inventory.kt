package com.leevaakki.cafe.models

import com.google.firebase.Timestamp

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val branchId: String = "", // cafe, dhaba, farm
    val quantity: Double = 0.0,
    val unit: String = "kg", // kg, ltr, pcs, box
    val minThreshold: Double = 5.0,
    val lastUpdated: Timestamp = Timestamp.now(),
    val category: String = "General"
)

data class InventoryLog(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val type: String = "IN", // IN, OUT, ADJUST
    val quantity: Double = 0.0,
    val reason: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val updatedBy: String = ""
)
