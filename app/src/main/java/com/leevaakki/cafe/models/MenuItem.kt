package com.leevaakki.cafe.models

data class MenuItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "", // e.g., "Beverages", "Snacks"
    val isAvailable: Boolean = true
)
