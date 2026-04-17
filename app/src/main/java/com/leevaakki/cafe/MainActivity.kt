package com.leevaakki.cafe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.leevaakki.cafe.ui.MainNavigation
import com.leevaakki.cafe.ui.NotificationPermissionHandler
import com.razorpay.PaymentResultListener

class MainActivity : ComponentActivity(), PaymentResultListener {

    private var initialThreadId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        setContent {
            NotificationPermissionHandler {
                MainNavigation(
                    auth = auth,
                    db = db,
                    initialThreadId = initialThreadId,
                    onThreadIdConsumed = { initialThreadId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data != null && data.pathSegments.contains("chat")) {
                initialThreadId = data.lastPathSegment
            }
        } else {
            // Handle notification extras if any
            initialThreadId = intent?.getStringExtra("threadId")
        }
    }

    fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM", "Token updated successfully")
            }
            .addOnFailureListener {
                Log.e("FCM", "Error updating token", it)
            }
    }

    override fun onPaymentSuccess(paymentId: String?) {
        onPaymentSuccess?.invoke(paymentId ?: "")
    }

    override fun onPaymentError(code: Int, response: String?) {
        onPaymentError?.invoke(code, response ?: "")
    }

    companion object {
        var onPaymentSuccess: ((String) -> Unit)? = null
        var onPaymentError: ((Int, String) -> Unit)? = null
    }
}
