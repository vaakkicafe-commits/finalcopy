package com.leevaakki.cafe.data.repository

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Query.Direction
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.leevaakki.cafe.models.ChatMessage
import com.leevaakki.cafe.models.ChatThread
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun getMenuCollection(branchId: String) = 
        db.collection("menus").document(branchId).collection("items")
            .whereEqualTo("isAvailable", true)

    fun getOrdersCollection(userId: String, branchId: String): Query {
        return if (branchId == "admin") {
            db.collection("orders").orderBy("createdAt")
        } else {
            db.collection("orders").whereEqualTo("userId", userId).orderBy("createdAt")
        }
    }

    fun getBranchOrdersCollection(branchId: String): Query {
        return db.collection("orders").whereEqualTo("branchId", branchId).orderBy("createdAt")
    }

    fun getThreadsQuery(userId: String, branchId: String): Query {
        val q = if (branchId == "admin") {
            db.collection("threads").whereArrayContains("participants", "admin")
        } else {
            db.collection("threads").whereArrayContains("participants", userId).whereEqualTo("branchId", branchId)
        }
        return q.orderBy("updatedAt")
    }

    fun getMessagesQuery(threadId: String, limit: Long): Query {
        return db.collection("threads").document(threadId).collection("messages")
            .orderBy("createdAt")
            .limit(limit)
    }

    fun getThreadDocument(threadId: String) = db.collection("threads").document(threadId)

    fun createThread(
        userId: String,
        branchId: String,
        title: String,
        customerName: String,
        customerPhone: String,
        onResult: (String?) -> Unit
    ) {
        val thread = hashMapOf(
            "title" to title,
            "customerName" to customerName,
            "customerPhone" to customerPhone,
            "branchId" to branchId,
            "participants" to listOf(userId, "admin"),
            "lastMessage" to "New support request",
            "updatedAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp(),
            "isArchived" to false,
            "orderId" to "",
            "unreadCountByUser" to mapOf(userId to 0, "admin" to 1)
        )

        db.collection("threads").add(thread)
            .addOnSuccessListener { onResult(it.id) }
            .addOnFailureListener { onResult(null) }
    }

    fun archiveThread(threadId: String, isArchived: Boolean) {
        db.collection("threads").document(threadId).update("isArchived", isArchived)
    }

    fun deleteThread(threadId: String) {
        db.collection("threads").document(threadId).delete()
    }

    suspend fun searchMessages(query: String): List<Pair<ChatThread, ChatMessage>> {
        if (query.isBlank()) return emptyList()
        // Simple search: finding messages with text matching query. 
        // Note: Real search should probably use Algolia or similar, but for now we do a simple Firestore query.
        // Actually, Firestore doesn't support full-text search. 
        // We'll just search for threads with exact matches in certain fields if needed, 
        // or fetch recent messages and filter locally if it's small.
        // For now, let's just return empty as a placeholder or implement local filter in ViewModel.
        return emptyList() 
    }

    fun updateTypingStatus(threadId: String, userId: String, isTyping: Boolean) {
        db.collection("threads").document(threadId).update("typingByUser.$userId", isTyping)
    }

    fun updateLastRead(threadId: String, userId: String) {
        db.collection("threads").document(threadId).update(
            "lastReadAtByUser.$userId", FieldValue.serverTimestamp(),
            "unreadCountByUser.$userId", 0
        )
    }

    fun addReaction(threadId: String, messageId: String, userId: String, emoji: String) {
        db.collection("threads").document(threadId)
            .collection("messages").document(messageId)
            .update("reactions.$userId", emoji)
    }

    suspend fun uploadMedia(uri: Uri, threadId: String, type: String): String? {
        val extension = if (type == "video") "mp4" else "jpg"
        val contentType = if (type == "video") "video/mp4" else "image/jpeg"
        val storageRef = storage.reference.child("chat_media/$threadId/${type}s/${System.currentTimeMillis()}.$extension")
        val metadata = StorageMetadata.Builder().setContentType(contentType).build()

        return try {
            storageRef.putFile(uri, metadata).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendMessage(
        threadId: String, 
        senderId: String, 
        text: String, 
        participants: List<String>, 
        senderType: String,
        type: String,
        mediaUrl: String?
    ) {
        val threadRef = db.collection("threads").document(threadId)
        val messageRef = threadRef.collection("messages").document()
        
        val batch = db.batch()
        val message = hashMapOf(
            "text" to text,
            "senderId" to senderId,
            "senderType" to senderType,
            "type" to type,
            "mediaUrl" to mediaUrl,
            "imageUrl" to mediaUrl,
            "createdAt" to FieldValue.serverTimestamp(),
            "read" to false,
            "delivered" to true
        )
        batch.set(messageRef, message)

        val lastMsgPreview = when(type) {
            "image" -> "📷 Image"
            "video" -> "🎥 Video"
            else -> text
        }

        val updates = mutableMapOf<String, Any>(
            "lastMessage" to lastMsgPreview,
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastReadAtByUser.$senderId" to FieldValue.serverTimestamp()
        )
        participants.forEach { uid ->
            updates["unreadCountByUser.$uid"] = if (uid == senderId) 0 else FieldValue.increment(1)
        }
        
        batch.update(threadRef, updates)
        batch.commit().await()
    }
}
