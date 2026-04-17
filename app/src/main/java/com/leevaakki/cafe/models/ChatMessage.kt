package com.leevaakki.cafe.models

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val type: String = "text", // "text", "image", "video"
    val mediaUrl: String? = null,
    val createdAt: com.google.firebase.Timestamp? = null,
    val senderType: String = "customer",
    val read: Boolean = false,
    val delivered: Boolean = false,
    val reactions: Map<String, List<String>> = emptyMap(),
    val imageUrl: String? = null // Keeping for backward compatibility
)
