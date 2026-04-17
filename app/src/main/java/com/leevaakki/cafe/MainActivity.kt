package com.leevaakki.cafe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.leevaakki.cafe.ui.MainNavigation
import com.leevaakki.cafe.ui.NotificationPermissionHandler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentThreadId = intent?.getStringExtra("threadId") ?: 
            if (intent?.action == Intent.ACTION_VIEW) intent.data?.lastPathSegment else null

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        setContent {
            val threadIdState = remember { mutableStateOf(intentThreadId) }
            
            NotificationPermissionHandler {
                MainNavigation(
                    auth = auth,
                    db = db,
                    initialThreadId = threadIdState.value,
                    onThreadIdConsumed = { threadIdState.value = null }
                )
            }
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
}
