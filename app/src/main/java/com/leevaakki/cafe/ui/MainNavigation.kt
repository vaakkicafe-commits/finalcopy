package com.leevaakki.cafe.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.leevaakki.cafe.BuildConfig
import com.leevaakki.cafe.ui.screens.*
import com.leevaakki.cafe.viewmodel.ChatViewModel

@Composable
fun MainNavigation(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    initialThreadId: String? = null,
    onThreadIdConsumed: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("splash") }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var selectedThreadId by remember { mutableStateOf<String?>(null) }
    var webViewUrl by remember { mutableStateOf<String?>(null) }
    var webViewTitle by remember { mutableStateOf("") }
    val chatViewModel: ChatViewModel = viewModel()

    val branchType = try {
        when (BuildConfig.BRANCH_TYPE) {
            "CAFE" -> "cafe"
            "DHABA" -> "dhaba"
            "FARM" -> "farm"
            else -> "cafe"
        }
    } catch (e: Exception) {
        "cafe"
    }


    // Load base data
    LaunchedEffect(isLoggedIn) {
        chatViewModel.listenToMenu(branchType)
        
        // Use the flavor-specific WordPress URL from BuildConfig
        val wpUrl = BuildConfig.WORDPRESS_URL
        
        chatViewModel.fetchMenuFromWordPress(wpUrl)
        chatViewModel.fetchOffersFromWordPress(wpUrl)
        chatViewModel.fetchSettingsFromWordPress(wpUrl)
        
        if (isLoggedIn) {
            val userId = auth.currentUser?.uid ?: ""
            chatViewModel.listenToUserOrders(userId)
            chatViewModel.listenToThreads(userId, branchType)
        }
    }

    // React to deep-link thread ID changes (e.g., from notifications or SEO links)
    LaunchedEffect(initialThreadId) {
        if (initialThreadId != null) {
            when (initialThreadId) {
                "deep_link_nepolian_pizza", "deep_link_best_seller" -> {
                    currentScreen = "menu_list"
                }
                else -> {
                    selectedThreadId = initialThreadId
                }
            }
            onThreadIdConsumed()
        }
    }

    // Check if the user is an admin
    val isAdmin = auth.currentUser?.email == "admin@leevaakki.com"

    when {
        currentScreen == "splash" -> {
            SplashScreen(onFinish = {
                currentScreen = if (isLoggedIn) "thread_list" else "login"
            })
        }

        !isLoggedIn || currentScreen == "login" -> {
            LoginAndDataScreen(auth, db, chatViewModel) {
                isLoggedIn = true
                currentScreen = "thread_list"
            }
        }

        currentScreen == "menu_list" -> {
            CafeMenuScreen(
                viewModel = chatViewModel,
                onBack = { currentScreen = "thread_list" }
            )
        }

        currentScreen == "order_history" -> {
            OrderHistoryScreen(
                viewModel = chatViewModel,
                onBack = { currentScreen = "thread_list" },
                onBrowseMenu = { currentScreen = "menu_list" },
                onOpenWebsite = { url, title ->
                    webViewUrl = url
                    webViewTitle = title
                    currentScreen = "webview"
                }
            )
        }

        currentScreen == "webview" -> {
            WebViewScreen(
                url = webViewUrl ?: "",
                title = webViewTitle,
                onBack = { currentScreen = "thread_list" }
            )
        }

        currentScreen == "admin_orders" -> {
            AdminOrderDashboardScreen(
                viewModel = chatViewModel,
                onBack = { currentScreen = "thread_list" }
            )
        }

        currentScreen == "admin_management" -> {
            AdminManagementScreen(
                viewModel = chatViewModel,
                onBack = { currentScreen = "thread_list" },
                onOpenAdminOrders = { currentScreen = "admin_orders" },
                onOpenAdminOffers = { currentScreen = "admin_offers" },
                onOpenAdminSwiggy = { currentScreen = "admin_swiggy" },
                onOpenInventory = { currentScreen = "admin_inventory" },
                onOpenSalesAnalytics = { currentScreen = "admin_sales_analytics" },
                onOpenEmployment = { currentScreen = "admin_employment" }
            )
        }

        currentScreen == "admin_inventory" -> {
            AdminInventoryScreen(
                viewModel = chatViewModel,
                onBack = { currentScreen = "admin_management" }
            )
        }

        currentScreen == "admin_sales_analytics" -> {
            AdminSalesAnalyticsScreen(
                viewModel = chatViewModel,
                onBack = { currentScreen = "admin_management" }
            )
        }

        currentScreen == "admin_swiggy" -> {
            AdminSwiggyScreen(
                viewModel = chatViewModel,
                onBack = { currentScreen = "admin_management" }
            )
        }

        currentScreen == "admin_offers" -> {
            AdminOffersScreen(
                viewModel = chatViewModel,
                onBack = { currentScreen = "admin_management" }
            )
        }

        selectedThreadId == null -> {
            ChatThreadListScreen(
                auth = auth,
                chatViewModel = chatViewModel,
                isAdmin = isAdmin,
                onThreadClick = { threadId ->
                    selectedThreadId = threadId
                },
                onOpenMenu = { currentScreen = "menu_list" },
                onOpenOrderHistory = { currentScreen = "order_history" },
                onOpenAdminOrders = { currentScreen = "admin_orders" },
                onOpenAdminManagement = { currentScreen = "admin_management" },
                onOpenWebsite = { url, title ->
                    webViewUrl = url
                    webViewTitle = title
                    currentScreen = "webview"
                }
            )
        }

        else -> {
            ChatRoomScreen(
                auth = auth,
                threadId = selectedThreadId!!,
                chatViewModel = chatViewModel,
                isAdmin = isAdmin,
                onBack = {
                    selectedThreadId = null
                }
            )
        }
    }
}

@Composable
fun NotificationPermissionHandler(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val initialPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    var hasNotificationPermission by remember {
        mutableStateOf(initialPermission)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Always render content; permission affects only notifications
    content()
}
