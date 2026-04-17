package com.leevaakki.cafe.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.leevaakki.cafe.BuildConfig
import com.leevaakki.cafe.MainActivity

import com.leevaakki.cafe.viewmodel.ChatViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Language

@Composable
fun LoginAndDataScreen(
    auth: FirebaseAuth, 
    db: FirebaseFirestore, 
    viewModel: ChatViewModel,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Use BuildType to determine default branch
    val defaultBranch = if (BuildConfig.BRANCH_TYPE == "CAFE") "cafe_001" else "dhaba_001"
    var branchId by remember { mutableStateOf(defaultBranch) }
    
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fastfood,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Welcome to Leevaakki™",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Experience the best food in ${BuildConfig.BRANCH_TYPE.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.trim().isEmpty() || password.trim().isEmpty()) {
                        errorMessage = "Please fill all fields"
                        return@Button
                    }
                    isLoading = true
                    auth.signInWithEmailAndPassword(email.trim(), password.trim())
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                                    (context as? MainActivity)?.saveTokenToFirestore(token)
                                }
                                onLoginSuccess()
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Login failed"
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Login")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    if (email.trim().isEmpty() || password.trim().isEmpty()) {
                        errorMessage = "Please fill all fields"
                        return@OutlinedButton
                    }
                    isLoading = true
                    auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                                    (context as? MainActivity)?.saveTokenToFirestore(token)
                                }
                                onLoginSuccess()
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Registration failed"
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                Text("Create New Account")
            }

            OutlinedButton(
                onClick = {
                    onLoginSuccess() // Bypassing for testing
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Skip Login (Guest Mode)")
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(32.dp))

            Text("Development Tools", style = MaterialTheme.typography.titleSmall)
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = branchId,
                onValueChange = { branchId = it },
                label = { Text("Branch ID (Test Order)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (auth.currentUser != null) {
                            val order = hashMapOf(
                                "item" to "Special ${BuildConfig.BRANCH_TYPE} Item",
                                "price" to 250,
                                "branchId" to branchId,
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("orders").add(order)
                                .addOnSuccessListener { Toast.makeText(context, "Test order sent!", Toast.LENGTH_SHORT).show() }
                        } else {
                            Toast.makeText(context, "Login first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Test Order", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                            Log.d("FCM_TOKEN", "Token: $token")
                            Toast.makeText(context, "Token logged to Logcat", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Log FCM Token", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.fetchMenuFromWordPress(BuildConfig.WORDPRESS_URL) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp),
                enabled = !viewModel.isRefreshingMenu.value
            ) {
                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (viewModel.isRefreshingMenu.value) "Testing WordPress..." else "Test WordPress Connection")
            }

            if (viewModel.menuItems.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "WordPress Connection Success!",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            "Found ${viewModel.menuItems.size} items from ${BuildConfig.WORDPRESS_URL}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sample: ${viewModel.menuItems.firstOrNull()?.name ?: "No name"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
