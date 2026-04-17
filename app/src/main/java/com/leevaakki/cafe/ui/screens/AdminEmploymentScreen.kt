package com.leevaakki.cafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leevaakki.cafe.BuildConfig
import com.leevaakki.cafe.models.StaffMember
import com.leevaakki.cafe.viewmodel.ChatViewModel
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEmploymentScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val branchId = when (BuildConfig.BRANCH_TYPE) {
        "CAFE" -> "cafe"
        "DHABA" -> "dhaba"
        "FARM" -> "farm"
        else -> "cafe"
    }

    var staffList by remember { mutableStateOf(listOf<StaffMember>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("staff")
            .whereEqualTo("branchId", branchId)
            .addSnapshotListener { snapshot, _ ->
                staffList = snapshot?.documents?.mapNotNull { it.toObject(StaffMember::class.java)?.copy(id = it.id) } ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employment Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Add Staff */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Staff")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (staffList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No staff records found for this branch.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(staffList) { staff ->
                    StaffCard(staff = staff)
                }
            }
        }
    }
}

@Composable
fun StaffCard(staff: StaffMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(staff.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(staff.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(staff.status, style = MaterialTheme.typography.labelSmall, color = if (staff.status == "Active") Color(0xFF4CAF50) else Color.Red)
            }
            
            IconButton(onClick = { /* Call Staff */ }) {
                Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
