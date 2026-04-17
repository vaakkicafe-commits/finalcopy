package com.leevaakki.cafe.models

import com.google.firebase.Timestamp

data class ChatThread(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val updatedAt: com.google.firebase.Timestamp? = null,
    val title: String = "",
    val unreadCountByUser: Map<String, Int> = emptyMap(),
    val branchId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val orderId: String = "",
    val typingByUser: Map<String, Boolean> = emptyMap(),
    val lastReadAtByUser: Map<String, com.google.firebase.Timestamp> = emptyMap(),
    val isArchived: Boolean = false
)
