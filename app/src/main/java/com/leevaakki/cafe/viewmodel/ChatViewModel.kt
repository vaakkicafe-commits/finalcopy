package com.leevaakki.cafe.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.leevaakki.cafe.BuildConfig
import com.leevaakki.cafe.data.repository.ChatRepository
import com.leevaakki.cafe.data.repository.JhattseRepository
import com.leevaakki.cafe.data.repository.WordPressRepository
import com.leevaakki.cafe.di.NetworkModule
import com.leevaakki.cafe.MainActivity
import com.leevaakki.cafe.models.*
import com.razorpay.Checkout
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    private val wordpressRepository = WordPressRepository(NetworkModule.provideHttpClient())
    private val jhattseRepository = JhattseRepository(NetworkModule.provideHttpClient())
    
    val threads = mutableStateListOf<ChatThread>()
    val messages = mutableStateListOf<ChatMessage>()
    val orders = mutableStateListOf<Order>()
    val searchResults = mutableStateListOf<Pair<ChatThread, ChatMessage>>()
    val menuItems = mutableStateListOf<MenuItem>()
    val inventoryItems = mutableStateListOf<InventoryItem>()
    val cartItems = mutableStateMapOf<String, Int>() // itemId -> quantity
    val offerImages = mutableStateListOf<String>()
    
    val isProcessingPayment = mutableStateOf(false)
    val paymentStatus = mutableStateOf<String?>(null)
    
    val swiggyUrl = mutableStateOf(BuildConfig.SWIGGY_URL)
    val zomatoUrl = mutableStateOf(BuildConfig.ZOMATO_URL)
    val mapsUrl = mutableStateOf(BuildConfig.MAPS_URL)
    
    val isLoadingThreads = mutableStateOf(false)
    val isLoadingMoreMessages = mutableStateOf(false)
    val isEndOfMessages = mutableStateOf(false)
    val isRefreshingMenu = mutableStateOf(false)
    val isRefreshingOffers = mutableStateOf(false)
    val isRefreshingThreads = mutableStateOf(false)
    val isLoadingOrders = mutableStateOf(false)

    fun refreshOrders() {
        isLoadingOrders.value = true
        // Assuming you have a method to load orders from Firestore
        // loadOrders() or fetchOrders()
        // For now, we simulate a delay and reset loading
        viewModelScope.launch {
            delay(1500)
            isLoadingOrders.value = false
        }
    }

    val typingStatus = mutableStateMapOf<String, Boolean>()
    val lastReadTimestamps = mutableStateMapOf<String, com.google.firebase.Timestamp>()

    private var threadsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var threadDetailListener: ListenerRegistration? = null
    private var lastVisibleMessage: DocumentSnapshot? = null
    private val PAGE_SIZE = 30L

    fun listenToMenu(branchId: String) {
        chatRepository.getMenuCollection(branchId)
            .addSnapshotListener { snapshot, _ ->
                menuItems.clear()
                snapshot?.documents?.forEach { doc ->
                    val item = doc.toObject(MenuItem::class.java)?.copy(id = doc.id)
                    if (item != null) menuItems.add(item)
                }
            }
    }

    fun listenToThreads(userId: String, branchId: String) {
        threadsListener?.remove()
        isLoadingThreads.value = true
        threadsListener = chatRepository.getThreadsQuery(userId, branchId)
            .addSnapshotListener { snapshot, e ->
                isLoadingThreads.value = false
                if (e != null) {
                    Log.e("ChatViewModel", "Threads listen failed", e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    threads.clear()
                    for (doc in snapshot.documents) {
                        val thread = doc.toObject(ChatThread::class.java)?.copy(id = doc.id)
                        if (thread != null) threads.add(thread)
                    }
                }
            }
    }

    fun listenToMessages(threadId: String, userId: String) {
        messagesListener?.remove()
        threadDetailListener?.remove()
        messages.clear()
        typingStatus.clear()
        lastReadTimestamps.clear()
        lastVisibleMessage = null
        isEndOfMessages.value = false
        
        chatRepository.updateLastRead(threadId, userId)
        
        threadDetailListener = chatRepository.getThreadDocument(threadId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val typingMap = snapshot.get("typingByUser") as? Map<String, Boolean> ?: emptyMap()
                typingMap.forEach { (uid, isTyping) -> typingStatus[uid] = isTyping }
                val readMap = snapshot.get("lastReadAtByUser") as? Map<String, com.google.firebase.Timestamp> ?: emptyMap()
                readMap.forEach { (uid, ts) -> lastReadTimestamps[uid] = ts }
            }

        messagesListener = chatRepository.getMessagesQuery(threadId, PAGE_SIZE)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                
                if (lastVisibleMessage == null && snapshot.documents.isNotEmpty()) {
                    lastVisibleMessage = snapshot.documents.last()
                    if (snapshot.documents.size < PAGE_SIZE) isEndOfMessages.value = true
                }

                val newMessages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }

                val historical = messages.filter { msg -> 
                    newMessages.none { newMsg -> newMsg.id == msg.id } && 
                    (msg.createdAt?.seconds ?: 0) < (newMessages.lastOrNull()?.createdAt?.seconds ?: Long.MAX_VALUE)
                }
                
                messages.clear()
                messages.addAll(newMessages.reversed())
                messages.addAll(0, historical)
            }
    }

    fun setTypingStatus(threadId: String, userId: String, isTyping: Boolean) {
        chatRepository.updateTypingStatus(threadId, userId, isTyping)
    }

    fun markAsRead(threadId: String, userId: String) {
        chatRepository.updateLastRead(threadId, userId)
    }

    fun listenToUserOrders(userId: String) {
        FirebaseFirestore.getInstance().collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                val fetchedOrders = snapshot?.documents?.mapNotNull { it.toObject(Order::class.java)?.copy(id = it.id) } ?: emptyList()
                
                // Keep the mock order if it exists and we're adding it
                val mockOrder = if (orders.none { it.status == "out_for_delivery" } && fetchedOrders.none { it.status == "out_for_delivery" }) {
                    Order(
                        id = "mock_order_123",
                        userId = userId,
                        userName = "Mock User",
                        items = listOf(OrderItem(name = "Classic Coffee (Mock)", quantity = 1, price = 150.0)),
                        totalAmount = 150.0,
                        status = "out_for_delivery",
                        deliveryPartnerName = "Test Rider (Mock)",
                        deliveryPartnerPhone = "+919876543210",
                        deliveryPartnerLat = 28.6139,
                        deliveryPartnerLng = 77.2090,
                        createdAt = com.google.firebase.Timestamp.now(),
                        branchId = "cafe"
                    )
                } else null

                orders.clear()
                orders.addAll(fetchedOrders)
                mockOrder?.let { orders.add(0, it) }

                // Simulate movement for the mock rider if added
                if (mockOrder != null) {
                    viewModelScope.launch {
                        var currentLat = 28.6139
                        var currentLng = 77.2090
                        while (isActive && orders.any { it.id == "mock_order_123" }) {
                            delay(3000)
                            currentLat += 0.0005
                            currentLng += 0.0005
                            val index = orders.indexOfFirst { it.id == "mock_order_123" }
                            if (index != -1) {
                                orders[index] = orders[index].copy(
                                    deliveryPartnerLat = currentLat,
                                    deliveryPartnerLng = currentLng
                                )
                            }
                        }
                    }
                }
            }
    }

    fun listenToAllOrders(branchId: String) {
        FirebaseFirestore.getInstance().collection("orders")
            .whereEqualTo("branchId", branchId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Error listening to all orders", e)
                    return@addSnapshotListener
                }
                orders.clear()
                snapshot?.documents?.mapNotNull { it.toObject(Order::class.java)?.copy(id = it.id) }?.let {
                    orders.addAll(it)
                }
            }
    }

    fun listenToInventory(branchId: String) {
        FirebaseFirestore.getInstance().collection("inventory")
            .whereEqualTo("branchId", branchId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                inventoryItems.clear()
                snapshot?.documents?.mapNotNull { it.toObject(InventoryItem::class.java)?.copy(id = it.id) }?.let {
                    inventoryItems.addAll(it)
                }
            }
    }

    fun updateInventoryQuantity(itemId: String, itemName: String, delta: Double, reason: String, type: String = "ADJUST") {
        val db = FirebaseFirestore.getInstance()
        val itemRef = db.collection("inventory").document(itemId)
        
        db.runTransaction { transaction ->
            val snapshot = transaction.get(itemRef)
            val currentQty = snapshot.getDouble("quantity") ?: 0.0
            val newQty = currentQty + delta
            
            transaction.update(itemRef, "quantity", newQty)
            transaction.update(itemRef, "lastUpdated", com.google.firebase.Timestamp.now())
            
            val logRef = db.collection("inventory_logs").document()
            val log = InventoryLog(
                id = logRef.id,
                itemId = itemId,
                itemName = itemName,
                type = type,
                quantity = delta,
                reason = reason,
                updatedBy = FirebaseAuth.getInstance().currentUser?.email ?: "admin"
            )
            transaction.set(logRef, log)
        }.addOnFailureListener { e ->
            Log.e("ChatViewModel", "Inventory update failed", e)
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String, deliveryPartner: String? = null) {
        val updates = mutableMapOf<String, Any>("status" to newStatus)
        deliveryPartner?.let { updates["deliveryPartnerName"] = it }
        
        FirebaseFirestore.getInstance().collection("orders").document(orderId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("ChatViewModel", "Order status updated to $newStatus")
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Failed to update order status", e)
            }
    }

    fun updateOrderLocation(orderId: String, lat: Double, lng: Double) {
        FirebaseFirestore.getInstance().collection("orders").document(orderId)
            .update(
                mapOf(
                    "deliveryPartnerLat" to lat,
                    "deliveryPartnerLng" to lng
                )
            )
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Failed to update location", e)
            }
    }

    fun createThread(
        userId: String,
        branchId: String,
        title: String,
        customerName: String,
        customerPhone: String,
        onResult: (String?) -> Unit
    ) {
        chatRepository.createThread(userId, branchId, title, customerName, customerPhone, onResult)
    }

    fun archiveThread(threadId: String, isArchived: Boolean) {
        chatRepository.archiveThread(threadId, isArchived)
    }

    fun deleteThread(threadId: String) {
        chatRepository.deleteThread(threadId)
    }

    fun searchMessages(query: String) {
        if (query.isBlank()) {
            searchResults.clear()
            return
        }
        
        // Local filtering across currently loaded threads and messages
        // In a production app, this would ideally hit a search index (like Algolia or AppSearch)
        searchResults.clear()
        threads.forEach { thread ->
            if (thread.lastMessage.contains(query, ignoreCase = true)) {
                // For now, if the last message matches, we add it to results
                // We'd need to search all messages in the thread for a thorough search
                // but that requires fetching them first.
                val dummyMsg = ChatMessage(text = thread.lastMessage, senderId = "system")
                searchResults.add(thread to dummyMsg)
            }
        }
    }

    fun loadMoreMessages(threadId: String) {
        val lastDoc = lastVisibleMessage ?: return
        if (isEndOfMessages.value || isLoadingMoreMessages.value) return

        isLoadingMoreMessages.value = true
        
        FirebaseFirestore.getInstance().collection("threads").document(threadId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .startAfter(lastDoc)
            .limit(PAGE_SIZE)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isEmpty()) {
                    isEndOfMessages.value = true
                } else {
                    lastVisibleMessage = snapshot.documents.last()
                    if (snapshot.documents.size < PAGE_SIZE) isEndOfMessages.value = true
                    val oldMessages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                    }
                    messages.addAll(0, oldMessages.reversed())
                }
                isLoadingMoreMessages.value = false
            }
            .addOnFailureListener {
                isLoadingMoreMessages.value = false
                Log.e("ChatViewModel", "Load more failed", it)
            }
    }

    fun uploadMedia(uri: Uri, threadId: String, type: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val url = chatRepository.uploadMedia(uri, threadId, type)
            onResult(url)
        }
    }

    fun sendMessage(
        threadId: String, 
        senderId: String, 
        text: String, 
        participants: List<String>, 
        senderType: String = "customer",
        type: String = "text",
        mediaUrl: String? = null
    ) {
        viewModelScope.launch {
            chatRepository.sendMessage(threadId, senderId, text, participants, senderType, type, mediaUrl)
        }
    }

    fun addReaction(threadId: String, messageId: String, userId: String, emoji: String) {
        chatRepository.addReaction(threadId, messageId, userId, emoji)
    }

    fun fetchMenuFromWordPress(baseUrl: String = "https://leevaakki.com") {
        viewModelScope.launch {
            isRefreshingMenu.value = true
            val items = wordpressRepository.fetchMenu(baseUrl)
            if (items.isNotEmpty()) {
                menuItems.clear()
                menuItems.addAll(items)
            }
            isRefreshingMenu.value = false
        }
    }

    fun fetchOffersFromWordPress(baseUrl: String = "https://leevaakki.com") {
        viewModelScope.launch {
            isRefreshingOffers.value = true
            val images = wordpressRepository.fetchOffers(baseUrl)
            if (images.isNotEmpty()) {
                offerImages.clear()
                offerImages.addAll(images)
            }
            isRefreshingOffers.value = false
        }
    }

    fun fetchSettingsFromWordPress(baseUrl: String = "https://leevaakki.com") {
        viewModelScope.launch {
            val settings = wordpressRepository.fetchSettings(baseUrl)
            settings["swiggy_url"]?.let { swiggyUrl.value = it }
            settings["zomato_url"]?.let { zomatoUrl.value = it }
            settings["maps_url"]?.let { mapsUrl.value = it }
        }
    }

    fun toggleCartItem(itemId: String) {
        if (cartItems.containsKey(itemId)) {
            cartItems.remove(itemId)
        } else {
            cartItems[itemId] = 1
        }
    }

    fun updateCartQuantity(itemId: String, quantity: Int) {
        if (quantity <= 0) {
            cartItems.remove(itemId)
        } else {
            cartItems[itemId] = quantity
        }
    }

    fun startCheckout(activity: android.app.Activity, userEmail: String?, userPhone: String?) {
        val total = cartItems.entries.sumOf { (id, qty) ->
            val item = menuItems.find { it.id == id }
            (item?.price ?: 0.0) * qty
        }
        if (total <= 0) return

        isProcessingPayment.value = true
        paymentStatus.value = "Initiating payment..."

        val checkout = Checkout()
        checkout.setKeyID(BuildConfig.RAZORPAY_KEY)

        try {
            val options = JSONObject()
            options.put("name", "Lee Vaakki™")
            options.put("description", "Food Order")
            options.put("image", "https://leevaakki.com/wp-content/uploads/2024/01/logo.png")
            options.put("theme.color", "#3399cc")
            options.put("currency", "INR")
            options.put("amount", (total * 100).toInt()) // amount in paisa
            options.put("prefill.email", userEmail ?: "customer@example.com")
            options.put("prefill.contact", userPhone ?: "")

            MainActivity.onPaymentSuccess = { paymentId ->
                placeOrder(paymentId, total)
            }
            MainActivity.onPaymentError = { code, response ->
                isProcessingPayment.value = false
                paymentStatus.value = "Payment failed: $response"
            }

            checkout.open(activity, options)
        } catch (e: Exception) {
            Log.e("Razorpay", "Error in starting Razorpay Checkout", e)
            isProcessingPayment.value = false
            paymentStatus.value = "Error: ${e.message}"
        }
    }

    private fun placeOrder(paymentId: String, total: Double) {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: "anonymous"
        val userEmail = user?.email ?: "customer@leevaakki.com"
        val branchId = when (BuildConfig.BRANCH_TYPE) {
            "CAFE" -> "cafe"
            "DHABA" -> "dhaba"
            "FARM" -> "farm"
            else -> "cafe"
        }

        val orderItems = cartItems.mapNotNull { (id, qty) ->
            menuItems.find { it.id == id }?.let { item ->
                OrderItem(itemId = id, name = item.name, price = item.price, quantity = qty)
            }
        }

        val order = Order(
            userId = userId,
            userName = user?.displayName ?: "Guest Customer",
            userPhone = user?.phoneNumber ?: "",
            items = orderItems,
            totalAmount = total,
            status = "paid",
            paymentId = paymentId,
            createdAt = com.google.firebase.Timestamp.now(),
            branchId = branchId
        )

        paymentStatus.value = "Placing order..."
        
        FirebaseFirestore.getInstance().collection("orders")
            .add(order)
            .addOnSuccessListener { orderDoc ->
                cartItems.clear()
                isProcessingPayment.value = false
                paymentStatus.value = "Order placed successfully! ID: ${orderDoc.id}"
                
                // Automatically send confirmation message
                sendOrderConfirmationMessage(orderDoc.id, total, orderItems, userId, branchId, userEmail)
            }
            .addOnFailureListener { e ->
                isProcessingPayment.value = false
                paymentStatus.value = "Order placement failed: ${e.message}. Payment ID: $paymentId"
            }
    }

    private fun sendOrderConfirmationMessage(
        orderId: String, 
        total: Double, 
        items: List<OrderItem>, 
        userId: String, 
        branchId: String,
        userEmail: String
    ) {
        val itemListText = items.joinToString("\n") { "- ${it.name} x ${it.quantity}" }
        val confirmationText = """
            ✅ *Order Confirmed!*
            Order ID: $orderId
            Email: $userEmail
            
            *Items:*
            $itemListText
            
            *Total Paid:* ₹$total
            
            Thank you for ordering from Lee Vaakki™ $branchId! We are preparing your food.
        """.trimIndent()

        // 1. Check if a thread for this user and branch already exists
        chatRepository.getThreadsQuery(userId, branchId).get()
            .addOnSuccessListener { snapshot ->
                val existingThread = snapshot.documents.firstOrNull()?.id
                if (existingThread != null) {
                    // 2a. Send to existing thread
                    sendMessage(
                        threadId = existingThread,
                        senderId = "system",
                        text = confirmationText,
                        participants = listOf(userId, "admin"),
                        senderType = "system"
                    )
                } else {
                    // 2b. Create new thread and send
                    createThread(userId, branchId, "Order #$orderId", "Customer", "") { newThreadId ->
                        if (newThreadId != null) {
                            sendMessage(
                                threadId = newThreadId,
                                senderId = "system",
                                text = confirmationText,
                                participants = listOf(userId, "admin"),
                                senderType = "system"
                            )
                        }
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        threadsListener?.remove()
        messagesListener?.remove()
        threadDetailListener?.remove()
    }
}
