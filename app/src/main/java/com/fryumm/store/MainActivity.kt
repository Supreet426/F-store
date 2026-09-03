package com.fryumm.store

import com.fryumm.store.ui.screens.SupportScreen
import com.fryumm.store.ui.viewmodels.StoreViewModel

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.ui.components.AppUpdateChecker
import com.fryumm.store.ui.screens.*
import com.fryumm.store.ui.theme.FStoreTheme
import com.fryumm.store.ui.theme.FStorePurple
import com.fryumm.store.ui.viewmodels.AuthViewModel
import com.fryumm.store.ui.viewmodels.OrderViewModel
import com.fryumm.store.ui.viewmodels.ProductViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start Order Sync Service
        val serviceIntent = Intent(this, NotificationSyncService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: StoreViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val orderViewModel: OrderViewModel = hiltViewModel()
            val productViewModel: ProductViewModel = hiltViewModel()
            FStoreTheme {
                MainScreen(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    orderViewModel = orderViewModel,
                    productViewModel = productViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: StoreViewModel,
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel()
) {
    val userProfile = authViewModel.currentUserProfile ?: viewModel.currentUserProfile
    val isAuthLoading = authViewModel.isAuthLoading
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Request Notification Permission for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var authError by remember { mutableStateOf<String?>(null) }
    var authSuccess by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is StoreViewModel.UiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is StoreViewModel.UiEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.uiEvent.collect { event ->
            when (event) {
                is AuthViewModel.UiEvent.ShowError -> {
                    authError = event.message
                    authSuccess = null
                    if (userProfile != null) {
                        scope.launch { snackbarHostState.showSnackbar(event.message) }
                    }
                }
                is AuthViewModel.UiEvent.ShowSuccess -> {
                    authError = null
                    authSuccess = event.message
                    if (userProfile != null) {
                        scope.launch { snackbarHostState.showSnackbar(event.message) }
                    }
                }
            }
        }
    }

    var initialAuthChecked by remember { mutableStateOf(false) }

    LaunchedEffect(isAuthLoading) {
        if (!isAuthLoading) {
            initialAuthChecked = true
        }
    }

    if (!initialAuthChecked && isAuthLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FStorePurple)
        }
        return
    }

    // Google Sign In Setup
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrEmpty()) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                authViewModel.loginWithGoogle(credential) {
                    viewModel.setupSession()
                }
            } else {
                authError = "Google Sign-In failed: No ID Token received"
                authSuccess = null
            }
        } catch (e: ApiException) {
            val errorMsg = when (e.statusCode) {
                10 -> "Developer Error (SHA-1 / Web Client ID mismatch). Check Firebase console."
                12500 -> "Google Sign-In failed on device. Check Google Play Services."
                12501 -> null // User cancelled picker, do not show error
                else -> "Google Sign-In Error (${e.statusCode}): ${e.message}"
            }
            if (errorMsg != null) {
                authError = errorMsg
                authSuccess = null
            }
        } catch (e: Exception) {
            val msg = "Google Sign-In Error: ${e.localizedMessage ?: e.message}"
            authError = msg
            authSuccess = null
        }
    }

    if (userProfile == null) {
        LoginScreen(
            isLoading = isAuthLoading,
            authError = authError,
            authSuccess = authSuccess,
            onClearError = {
                authError = null
                authSuccess = null
            },
            onContactSupport = {
                val supportNumber = "9235900175"
                val url = "https://api.whatsapp.com/send?phone=91$supportNumber&text=I need help with login to F-Store"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(url)
                }
                context.startActivity(intent)
            },
            onGoogleLogin = {
                authError = null
                authSuccess = null
                try {
                    val webClientId = try {
                        context.getString(R.string.default_web_client_id)
                    } catch (e: Exception) {
                        "216011846150-c3gne9t3c3t2angcqftb39vck8vm50nf.apps.googleusercontent.com"
                    }
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                } catch (e: Exception) {
                    val msg = "Could not start Google Sign-In: ${e.localizedMessage}"
                    authError = msg
                }
            }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var dynamicTitle by remember { mutableStateOf("") }
    var globalSearchQuery by remember { mutableStateOf("") }
    var isGlobalSearchActive by remember { mutableStateOf(false) }

    // Deep link handling from Notifications
    LaunchedEffect(Unit) {
        val action = (context as? android.app.Activity)?.intent?.getStringExtra("actionUrl")
        if (!action.isNullOrBlank()) {
            navController.navigate(action)
            // Clear the intent so it doesn't navigate again on recomposition/config change
            (context as? android.app.Activity)?.intent?.removeExtra("actionUrl")
        }
    }

    // Master Admin Emails
    val masterAdmins = listOf("ranjanamishra1117@gmail.com", "supreet.mishr@gmail.com")

    // Admin Detection (STRICT: Whitelisted Master Admins Only)
    val isAdmin = masterAdmins.contains(userProfile.email.lowercase().trim()) || userProfile.userRole == UserRole.ADMIN

    val currentTitle = when {
        currentRoute == "orders" -> "Live Orders Board"
        currentRoute == "stores" -> "Product Catalog & Inventory"
        currentRoute == "users" -> if (dynamicTitle.isNotEmpty()) dynamicTitle else "User Management Portal"
        currentRoute == "promotion" -> "Coupons & Promo Offers"
        currentRoute == "insights" -> "Overview"
        currentRoute == "app_management" -> "Ecosystem Controls"
        currentRoute == "inventory" -> "Stock Inventory"
        currentRoute == "dispatch" -> "Realtime Operations Live Map"
        currentRoute == "dark_stores" -> "Dark Store Fulfillment Hubs"
        currentRoute == "add_product" -> "Inventory Catalog"
        currentRoute == "settlements" -> "Settlements"
        currentRoute?.startsWith("edit_product") == true -> "Edit Product"
        else -> "F-Store"
    }

    val isFullScreen = currentRoute?.let { 
        it == "add_product" || it.startsWith("edit_product") || it == "location_settings" || it == "profile" || it == "app_update" || it == "settlements" || it == "referral" || it == "admin_referrals" || it == "pending_approvals" || it == "support"
    } ?: false
    val hasCustomTopBar = currentRoute == "promotion" || currentRoute == "dark_stores"

    // Check for updates
    AppUpdateChecker(viewModel)

    // Global Popup Notification
    val popup = viewModel.dynamicAppConfig.popupNotification
    if (popup.isActive && popup.title.isNotBlank()) {
        var showPopup by remember(viewModel.dynamicAppConfig.lastUpdated) { mutableStateOf(true) }
        if (showPopup) {
            AlertDialog(
                onDismissRequest = { showPopup = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.padding(horizontal = 24.dp),
                content = {
                    Card(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (popup.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = popup.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(popup.title, fontWeight = FontWeight.Black, fontSize = 22.sp, color = FStorePurple, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(Modifier.height(12.dp))
                                Text(popup.message, fontSize = 14.sp, color = Color.DarkGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        if (popup.actionUrl.isNotBlank()) {
                                            try { navController.navigate(popup.actionUrl) } catch (e: Exception) {}
                                        }
                                        showPopup = false
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
                                ) {
                                    Text("VIEW DETAILS", fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { showPopup = false }, modifier = Modifier.padding(top = 4.dp)) {
                                    Text("Dismiss", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isFullScreen,
            drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 0.dp
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if(isAdmin) Color(0xFF1A237E) else MaterialTheme.colorScheme.primary)
                        .padding(24.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        if (userProfile.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(userProfile.photoUrl).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.clip(CircleShape)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(userProfile.name.take(1).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(userProfile.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(userProfile.email, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }

                Spacer(Modifier.height(12.dp))

                // Drawer Items
                NavigationDrawerItem(
                    label = { Text("My Profile", fontWeight = FontWeight.Bold) },
                    selected = currentRoute == "profile",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("profile")
                    },
                    icon = { Icon(Icons.Default.AccountCircle, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Refer Customers", fontWeight = FontWeight.Bold) },
                    selected = currentRoute == "referral",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("referral")
                    },
                    icon = { Icon(Icons.Default.Stars, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Settlements", fontWeight = FontWeight.Bold) },
                    selected = currentRoute == "settlements",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settlements")
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                if (userProfile.userRole == UserRole.STORE_OWNER) {
                    NavigationDrawerItem(
                        label = { Text("Store Operating Hours", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("profile") 
                        },
                        icon = { Icon(Icons.Default.Schedule, null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                NavigationDrawerItem(
                    label = { Text("Change Password", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.sendPasswordResetEmail(userProfile.email)
                    },
                    icon = { Icon(Icons.Default.LockReset, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Support & Help", fontWeight = FontWeight.Bold) },
                    selected = currentRoute == "support",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("support")
                    },
                    icon = { Icon(Icons.Default.SupportAgent, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                if (isAdmin) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
                    
                    NavigationDrawerItem(
                        label = { Text("Pending Approvals", fontWeight = FontWeight.Bold) },
                        selected = currentRoute == "pending_approvals",
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("pending_approvals")
                        },
                        icon = { Icon(Icons.Default.HourglassTop, null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Manage Referrals", fontWeight = FontWeight.Bold) },
                        selected = currentRoute == "admin_referrals",
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("admin_referrals")
                        },
                        icon = { Icon(Icons.Default.TrackChanges, null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                NavigationDrawerItem(
                    label = { Text("Logout", fontWeight = FontWeight.Bold, color = Color.Red) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        authViewModel.logout { viewModel.logout() }
                    },
                    icon = { Icon(Icons.Default.Logout, null, tint = Color.Red) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                Spacer(Modifier.weight(1f))
                Text(
                    "F-Store v1.2.0", 
                    modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!isFullScreen && !hasCustomTopBar) {
                    TopAppBar(
                        navigationIcon = {
                            if (isGlobalSearchActive && currentRoute == "stores") {
                                IconButton(onClick = { 
                                    isGlobalSearchActive = false
                                    globalSearchQuery = ""
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                            } else {
                                IconButton(onClick = {
                                    if (navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    } else {
                                        scope.launch { drawerState.open() }
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                            }
                        },
                        title = { 
                            if (isGlobalSearchActive && currentRoute == "stores") {
                                TextField(
                                    value = globalSearchQuery,
                                    onValueChange = { globalSearchQuery = it },
                                    placeholder = { Text("Search products...", color = Color.White.copy(alpha = 0.7f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        cursorColor = Color.White,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true
                                )
                            } else {
                                Text(currentTitle, fontWeight = FontWeight.Black)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if(isAdmin) Color(0xFF1A237E) else MaterialTheme.colorScheme.primary, 
                            titleContentColor = Color.White
                        ),
                        actions = {
                            if (currentRoute == "stores") {
                                if (!isGlobalSearchActive) {
                                    IconButton(onClick = { isGlobalSearchActive = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search Catalog", tint = Color.White)
                                    }
                                } else if (globalSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { globalSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear Search", tint = Color.White)
                                    }
                                }
                                IconButton(onClick = { navController.navigate("add_product") }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Product", tint = Color.White)
                                }
                            }
                            if (currentRoute == "orders") {
                                IconButton(onClick = { navController.navigate("batch_picking") }) {
                                    Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = "Batch Picking", tint = Color.White)
                                }
                                if (isAdmin) {
                                    var showClearConfirm by remember { mutableStateOf(false) }
                                    if (showClearConfirm) {
                                        AlertDialog(
                                            onDismissRequest = { showClearConfirm = false },
                                            title = { Text("Clear All Orders?") },
                                            text = { Text("This will permanently delete all order history from Firestore. This action cannot be undone.") },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.clearAllOrders()
                                                        showClearConfirm = false
                                                    },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                                ) { Text("CLEAR ALL") }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showClearConfirm = false }) { Text("CANCEL") }
                                            }
                                        )
                                    }
                                    IconButton(onClick = { showClearConfirm = true }) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Orders", tint = Color.White)
                                    }
                                }
                            }
                            if (currentRoute == "users") {
                                IconButton(onClick = { navController.navigate("pending_approvals") }) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = "Pending Approvals", tint = Color.White)
                                }
                            }
                            if (currentRoute == "dispatch") {
                                IconButton(onClick = { /* trigger scan callback */ }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Handover Scan", tint = Color.White)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    )
                }
            },
            bottomBar = {
                if (!isFullScreen) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        Row(
                            modifier = if (isAdmin) {
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isAdmin) Arrangement.Start else Arrangement.SpaceEvenly
                        ) {
                            val itemModifier = if (isAdmin) Modifier.padding(horizontal = 12.dp) else Modifier.weight(1f)
                            if (isAdmin) {
                                NavTabItem(selected = currentRoute == "orders", onClick = { navController.navigate("orders") }, icon = Icons.Default.ListAlt, label = "Orders", isAdmin = true, modifier = itemModifier)
                                NavTabItem(selected = currentRoute == "stores", onClick = { navController.navigate("stores") }, icon = Icons.Default.Store, label = "Stores", isAdmin = true, modifier = itemModifier)
                                NavTabItem(selected = currentRoute == "users", onClick = { navController.navigate("users") }, icon = Icons.Default.People, label = "Users", isAdmin = true, modifier = itemModifier)
                                NavTabItem(selected = currentRoute == "promotion", onClick = { navController.navigate("promotion") }, icon = Icons.Default.AdsClick, label = "Promotion", isAdmin = true, modifier = itemModifier)
                                NavTabItem(selected = currentRoute == "insights", onClick = { navController.navigate("insights") }, icon = Icons.Default.Insights, label = "Insights", isAdmin = true, modifier = itemModifier)
                                NavTabItem(selected = currentRoute == "app_management", onClick = { navController.navigate("app_management") }, icon = Icons.Default.Settings, label = "Control", isAdmin = true, modifier = itemModifier)
                            } else {
                                NavTabItem(selected = currentRoute == "orders", onClick = { navController.navigate("orders") }, icon = Icons.Default.ListAlt, label = "Orders", isAdmin = false, modifier = itemModifier)
                                NavTabItem(selected = currentRoute == "inventory", onClick = { navController.navigate("inventory") }, icon = Icons.Default.Inventory2, label = "Stock", isAdmin = false, modifier = itemModifier)
                                NavTabItem(selected = currentRoute == "dispatch", onClick = { navController.navigate("dispatch") }, icon = Icons.Default.FlashOn, label = "Dispatch", isAdmin = false, modifier = itemModifier)
                                NavTabItem(selected = currentRoute == "insights", onClick = { navController.navigate("insights") }, icon = Icons.Default.QueryStats, label = "Insights", isAdmin = false, modifier = itemModifier)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(navController = navController, startDestination = "orders", modifier = Modifier.padding(if(isFullScreen) PaddingValues(0.dp) else padding)) {
                composable("orders") { MerchantDashboard(navController, viewModel, onStartScan = { _, _ -> }) }
                composable("stores") { 
                    AdminStoresScreen(
                        viewModel = viewModel, 
                        navController = navController,
                        externalSearchQuery = if (isGlobalSearchActive) globalSearchQuery else ""
                    ) 
                }
                composable("users") { UserActivityScreen(viewModel, navController, onTitleChange = { dynamicTitle = it }) }
                composable("promotion") { PromotionScreen(viewModel, navController) }
                composable("insights") { UnifiedInsightsScreen(viewModel) }
                composable("inventory") { InventoryScreen(navController, viewModel) }
                composable("dispatch") { 
                    DispatchScreen(
                        viewModel = viewModel, 
                        navController = navController,
                        onStartScan = { id, type -> /* Implement scan logic if needed */ }
                    )
                }
                composable("add_product") { backStackEntry ->
                    val category = backStackEntry.arguments?.getString("category")
                    AddProductScreen(navController, viewModel, null, prefilledCategory = category) 
                }
                composable("edit_product/{productId}") { backStackEntry ->
                    val productId = backStackEntry.arguments?.getString("productId")
                    val product = viewModel.products.find { it.id == productId }
                    AddProductScreen(navController, viewModel, product)
                }
                composable("order_details/{orderId}") { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                    OrderDetailsScreen(orderId, viewModel, onBack = { navController.popBackStack() })
                }
                composable("settlements") { SettlementScreen(viewModel, navController) }
                composable("app_management") { AppManagementScreen(viewModel, navController) }
                composable("app_update") { AppUpdateScreen(viewModel, onBack = { navController.popBackStack() }) }
                composable("location_settings") { LocationSettingsScreen(viewModel, onBack = { navController.popBackStack() }) }
                composable("dark_stores") { DarkStoreManagementScreen(viewModel, onBack = { navController.popBackStack() }) }
                composable("profile") { 
                    ProfileScreen(
                        viewModel = viewModel, 
                        onBack = { navController.popBackStack() },
                        onNavigateToUpdate = { navController.navigate("app_update") },
                        onNavigateToLocation = { navController.navigate("location_settings") }
                    ) 
                }
                composable("referral") { 
                    ReferralScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) 
                }
                composable("support") {
                    SupportScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable("admin_referrals") {
                    MerchantReferralAdminScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable("pending_approvals") {
                    PendingApprovalsScreen(viewModel = viewModel, navController = navController, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
}

@Composable
fun ProfileIcon(profile: UserProfile, onClick: () -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    ) {
        if (profile.photoUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(profile.photoUrl)
                    .crossfade(true)
                    .error(R.drawable.ic_launcher_foreground) // Fallback
                    .build(),
                contentDescription = "Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(CircleShape)
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.background(Color.White)) {
                val initial = if (profile.email.isNotEmpty()) profile.email.take(1).uppercase() else profile.name.take(1).uppercase()
                Text(
                    text = initial,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if(profile.userRole == UserRole.ADMIN) Color(0xFF1A237E) else FStorePurple
                )
            }
        }
    }
}

@Composable
fun NavTabItem(selected: Boolean, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isAdmin: Boolean, modifier: Modifier = Modifier) {
    val contentColor = if (selected) (if(isAdmin) Color(0xFF1A237E) else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
