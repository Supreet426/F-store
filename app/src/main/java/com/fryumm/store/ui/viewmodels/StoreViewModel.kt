package com.fryumm.store.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fryumm.store.data.model.*
import com.fryumm.store.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    application: Application,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val storageRepository: StorageRepository,
    private val notificationRepository: NotificationRepository,
    private val walletRepository: WalletRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AndroidViewModel(application) {

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowSuccess(val message: String) : UiEvent()
    }

    val masterAdmins = com.fryumm.store.util.MasterAdminSecurity.MASTER_ADMIN_EMAILS

    var currentUserProfile by mutableStateOf<UserProfile?>(null)
        private set

    val isMasterAdmin: Boolean
        get() = com.fryumm.store.util.MasterAdminSecurity.isMasterAdminEmail(currentUserProfile?.email)

    var isAuthLoading by mutableStateOf(true)
        private set

    var orders by mutableStateOf<List<Order>>(emptyList())
        private set
    var products by mutableStateOf<List<Product>>(emptyList())
        private set
    var allUsers by mutableStateOf<List<UserProfile>>(emptyList())
        private set
    var allCustomers by mutableStateOf<List<CustomerProfile>>(emptyList())
        private set
    var searchEvents by mutableStateOf<List<SearchEvent>>(emptyList())
        private set
    var customerEvents by mutableStateOf<List<CustomerActivityEvent>>(emptyList())
        private set
    var appUpdates by mutableStateOf<List<AppUpdateInfo>>(emptyList())
        private set
    var serviceRequests by mutableStateOf<List<ServiceRequest>>(emptyList())
        private set

    var customerBehaviorAnalytics by mutableStateOf(CustomerBehaviorAnalytics())
        private set
    var storeConfig by mutableStateOf(StoreConfig())
        private set
    var dynamicAppConfig by mutableStateOf(DynamicAppConfig())
        private set
    var darkStores by mutableStateOf<List<DarkStore>>(emptyList())
        private set

    var userSegmentationReport by mutableStateOf(UserSegmentationReport())
        private set

    var lowStockProducts by mutableStateOf<List<Product>>(emptyList())
        private set

    private val notifiedLowStockIds = mutableSetOf<String>()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    var pickers by mutableStateOf<List<Picker>>(emptyList())
    var riders by mutableStateOf<List<Rider>>(emptyList())

    private var rawOrders = emptyList<Order>()
    private var rawProducts = emptyList<Product>()
    private var rawSearchEvents = emptyList<SearchEvent>()
    private var rawCustomerEvents = emptyList<CustomerActivityEvent>()

    // Simulation
    var simulatedWeatherImpact by mutableStateOf(0.0)
    var simulatedEventImpact by mutableStateOf(0.0)

    init {
        setupSession()
        startDataObservers()
    }

    fun setupSession() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            isAuthLoading = false
            return
        }

        viewModelScope.launch {
            try {
                val collections = listOf("users", "sellers", "riders", "staff", "customers")
                var doc: com.google.firebase.firestore.DocumentSnapshot? = null
                for (coll in collections) {
                    try {
                        val d = firestore.collection(coll).document(currentUser.uid).get().await()
                        if (d.exists()) {
                            doc = d
                            break
                        }
                    } catch (e: Exception) {
                        Log.e("StoreViewModel", "Error checking collection $coll", e)
                    }
                }

                if (doc != null && doc.exists()) {
                    val profile = try { doc.toObject(UserProfile::class.java) } catch (e: Exception) { null }
                    currentUserProfile = profile?.let {
                        it.copy(
                            email = it.email.ifEmpty { currentUser.email ?: "" },
                            name = it.name.ifEmpty { currentUser.displayName ?: "User" },
                            photoUrl = it.photoUrl.ifEmpty { currentUser.photoUrl?.toString() ?: "" }
                        )
                    } ?: UserProfile(
                        uid = currentUser.uid,
                        name = currentUser.displayName ?: "User",
                        email = currentUser.email ?: "",
                        photoUrl = currentUser.photoUrl?.toString() ?: ""
                    )
                } else {
                    val displayName = currentUser.displayName.takeIf { !it.isNullOrBlank() }
                        ?: currentUser.email?.substringBefore("@")
                        ?: "Partner User"
                    val newProfile = UserProfile(
                        uid = currentUser.uid,
                        email = currentUser.email ?: "",
                        name = displayName,
                        photoUrl = currentUser.photoUrl?.toString() ?: "",
                        role = if (currentUser.email?.lowercase() in masterAdmins) "ADMIN" else "STORE_OWNER"
                    )
                    try {
                        val targetCollection = if (newProfile.role == "ADMIN") "users" else "sellers"
                        firestore.collection(targetCollection).document(currentUser.uid).set(newProfile).await()
                    } catch (e: Exception) {
                        Log.e("StoreViewModel", "Error saving new profile", e)
                    }
                    currentUserProfile = newProfile
                }

                currentUserProfile?.let { profile ->
                    if (com.fryumm.store.util.MasterAdminSecurity.isMasterAdminEmail(profile.email)) {
                        val context = getApplication<Application>().applicationContext
                        val activeDeviceId = com.fryumm.store.util.MasterAdminSecurity.getDeviceId(context)
                        val verification = com.fryumm.store.util.MasterAdminSecurity.verifyAdminSession(
                            context = context,
                            profile = profile,
                            currentEmail = currentUser.email ?: profile.email,
                            currentPhone = profile.phoneNumber,
                            currentDeviceId = activeDeviceId
                        )
                        if (!verification.isVerified) {
                            auth.signOut()
                            currentUserProfile = null
                            _uiEvent.emit(UiEvent.ShowError(verification.message))
                            isAuthLoading = false
                            return@launch
                        }
                        if (profile.deviceId.isBlank() && activeDeviceId.isNotBlank()) {
                            try {
                                firestore.collection("users").document(profile.uid).update("deviceId", activeDeviceId).await()
                                currentUserProfile = profile.copy(deviceId = activeDeviceId)
                            } catch (e: Exception) {
                                Log.e("StoreViewModel", "Failed to update admin deviceId", e)
                            }
                        }
                    }

                    if (profile.uid.isNotEmpty()) {
                        val tokenRole = when (profile.userRole) {
                            UserRole.ADMIN -> "admin"
                            UserRole.RIDER -> "delivery"
                            else -> "seller"
                        }
                        notificationRepository.updateToken(profile.uid, tokenRole)
                        toggleServicesBasedOnRole(profile)
                    }
                }
                filterOrders()
                filterProducts()
            } catch (e: Exception) {
                Log.e("StoreViewModel", "Auth System Error", e)
                if (currentUserProfile == null) {
                    currentUserProfile = UserProfile(
                        uid = currentUser.uid,
                        email = currentUser.email ?: "",
                        name = currentUser.displayName ?: "User",
                        photoUrl = currentUser.photoUrl?.toString() ?: "",
                        role = if (currentUser.email?.lowercase() in masterAdmins) "ADMIN" else "STORE_OWNER"
                    )
                }
            } finally {
                isAuthLoading = false
            }
        }
    }

    private fun toggleServicesBasedOnRole(profile: UserProfile) {
        val context = getApplication<Application>().applicationContext
        
        // Rider Tracking Service
        val locationIntent = Intent(context, com.fryumm.store.RiderLocationService::class.java)
        if (profile.userRole == UserRole.RIDER && profile.active) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(locationIntent)
            } else {
                context.startService(locationIntent)
            }
        } else {
            context.stopService(locationIntent)
        }
    }

    private fun startDataObservers() {
        observeOrders()
        observeProducts()
        observeAllUsers()
        observeDarkStores()
        observeDynamicConfig()
        observeStoreConfig()
        observeCustomers()
        observeServiceRequests()
        observeEvents()
        observeAppUpdates()
    }

    private fun observeOrders() {
        viewModelScope.launch {
            orderRepository.getNewOrders().collect { newList ->
                rawOrders = newList
                filterOrders()
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.getProducts().collect { result ->
                rawProducts = result.getOrDefault(emptyList())
                filterProducts()
            }
        }
    }

    private fun observeDarkStores() {
        viewModelScope.launch {
            firestore.collection("dark_stores").addSnapshotListener { snapshot, _ ->
                snapshot?.let { darkStores = it.toObjects(DarkStore::class.java) }
            }
        }
    }

    private fun observeServiceRequests() {
        viewModelScope.launch {
            firestore.collection("service_requests")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.let { serviceRequests = it.toObjects(ServiceRequest::class.java) }
                }
        }
    }

    private fun observeAppUpdates() {
        viewModelScope.launch {
            firestore.collection("app_updates").addSnapshotListener { snapshot, _ ->
                snapshot?.let { appUpdates = it.toObjects(AppUpdateInfo::class.java) }
            }
        }
    }

    private fun observeDynamicConfig() {
        viewModelScope.launch {
            firestore.collection("app_config").document("config_app")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        snapshot.toObject(DynamicAppConfig::class.java)?.let { dynamicAppConfig = it }
                    } else {
                        // Fallback to legacy config document
                        firestore.collection("app_config").document("config")
                            .addSnapshotListener { legacySnapshot, _ ->
                                legacySnapshot?.toObject(DynamicAppConfig::class.java)?.let { dynamicAppConfig = it }
                            }
                    }
                }
        }
    }

    private fun observeStoreConfig() {
        viewModelScope.launch {
            firestore.collection("settings").document("store_config")
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.toObject(StoreConfig::class.java)?.let { storeConfig = it }
                }
        }
    }

    private fun observeCustomers() {
        viewModelScope.launch {
            firestore.collection("customers").addSnapshotListener { snapshot, _ ->
                snapshot?.let { 
                    allCustomers = it.toObjects(CustomerProfile::class.java) 
                    generateUserSegmentationReport()
                }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            firestore.collection("search_events").addSnapshotListener { snapshot, _ ->
                snapshot?.let { searchEvents = it.toObjects(SearchEvent::class.java) }
            }
            firestore.collection("customer_activity_events").addSnapshotListener { snapshot, _ ->
                snapshot?.let { customerEvents = it.toObjects(CustomerActivityEvent::class.java) }
            }
        }
    }

    private fun observeAllUsers() {
        val collections = listOf("users", "sellers", "riders", "customers", "staff")
        val snapshotListeners = mutableMapOf<String, List<UserProfile>>()

        collections.forEach { collectionName ->
            firestore.collection(collectionName).addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val users = snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(UserProfile::class.java)?.copy(uid = doc.id) } catch (e: Exception) { null }
                } ?: emptyList()

                snapshotListeners[collectionName] = users
                val combined = snapshotListeners.values.flatten()
                    .distinctBy { if (it.email.isNotBlank()) it.email.lowercase().trim() else it.uid }
                allUsers = combined

                val activeRiders = combined.filter { it.userRole == UserRole.RIDER && it.active }
                riders = activeRiders.map { Rider(it.uid, it.name) }
                pickers = activeRiders.map { Picker(it.uid, it.name) }
            }
        }
    }

    private fun filterOrders() {
        val profile = currentUserProfile ?: return
        val isAdmin = profile.userRole == UserRole.ADMIN || profile.email.lowercase().trim() in masterAdmins
        orders = if (isAdmin) {
            rawOrders
        } else {
            when (profile.userRole) {
                UserRole.RIDER -> rawOrders.filter { it.deliveryAgentId == profile.uid }
                UserRole.CUSTOMER -> emptyList()
                else -> {
                    // MERCHANT or DARK STORE
                    rawOrders.filter { order ->
                        order.merchantId == profile.uid ||
                        (!profile.darkStoreId.isNullOrEmpty() && order.merchantId == profile.darkStoreId) ||
                        order.items.any { item ->
                            item.product.merchantId == profile.uid ||
                            (!profile.darkStoreId.isNullOrEmpty() && item.product.merchantId == profile.darkStoreId)
                        }
                    }
                }
            }
        }.sortedByDescending { it.timestamp }
    }

    private fun filterProducts() {
        val profile = currentUserProfile ?: return
        val isAdmin = profile.userRole == UserRole.ADMIN || profile.email.lowercase().trim() in masterAdmins
        products = if (isAdmin) {
            rawProducts
        } else {
            rawProducts.filter { it.merchantId == profile.uid }
        }.sortedBy { it.sequenceNumber }

        // Update low stock list
        lowStockProducts = products.filter { it.stockQuantity <= it.minStockLevel }

        // Trigger notifications if Admin
        if (isAdmin) {
            checkAndNotifyLowStock()
        }
    }

    private fun checkAndNotifyLowStock() {
        viewModelScope.launch {
            lowStockProducts.forEach { product ->
                if (!notifiedLowStockIds.contains(product.id) && product.stockQuantity <= product.minStockLevel) {
                    val title = "Low Stock Alert: ${product.name}"
                    val body = "${product.name} is running low (${product.stockQuantity} remaining at ${product.merchantName})."
                    notificationRepository.sendNotification(
                        userId = currentUserProfile?.uid ?: "",
                        title = title,
                        body = body,
                        actionUrl = "inventory_alerts"
                    )
                    notifiedLowStockIds.add(product.id)
                }
            }
        }
    }

    fun remindMerchantAboutStock(product: Product) {
        viewModelScope.launch {
            notificationRepository.sendNotification(
                userId = product.merchantId,
                title = "Restock Request: ${product.name}",
                body = "Admin has noticed that ${product.name} is low on stock (${product.stockQuantity} units left). Please restock soon.",
                actionUrl = "inventory"
            )
            _uiEvent.emit(UiEvent.ShowSuccess("Reminder sent to ${product.merchantName}"))
        }
    }

    // AUTH
    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Please enter email and password")) }
            return
        }
        isAuthLoading = true
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                setupSession()
                _uiEvent.emit(UiEvent.ShowSuccess("Login Successful"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Login Failed: ${e.message}"))
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun kloginWithGoogle(credential: com.google.firebase.auth.AuthCredential) {
        isAuthLoading = true
        viewModelScope.launch {
            try {
                auth.signInWithCredential(credential).await()
                setupSession()
                _uiEvent.emit(UiEvent.ShowSuccess("Google Sign-In Successful"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Google Sign-In Failed: ${e.message}"))
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun logout() {
        auth.signOut()
        currentUserProfile = null
        orders = emptyList()
        products = emptyList()
        viewModelScope.launch { _uiEvent.emit(UiEvent.ShowSuccess("Logged Out")) }
    }

    fun register(email: String, pass: String, name: String, phone: String, customRole: UserRole? = null, staffDepartment: String? = null) {
        if (customRole == UserRole.ADMIN) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("SECURITY ALERT: Admin role cannot be assigned to new accounts.")) }
            return
        }
        if (phone.length < 10) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Invalid Phone Number")) }
            return
        }
        isAuthLoading = true
        viewModelScope.launch {
            try {
                val cleanPhone = phone.trim()
                val cleanEmail = email.trim().lowercase()
                val roleStr = customRole?.name ?: "STORE_OWNER"

                if (roleStr == "CUSTOMER") {
                    val existingPhone1 = firestore.collection("customers").whereEqualTo("phoneNumber", cleanPhone).get().await()
                    val existingPhone2 = firestore.collection("customers").whereEqualTo("mobile", cleanPhone).get().await()
                    val existingCustomerDoc = existingPhone1.documents.firstOrNull() ?: existingPhone2.documents.firstOrNull()

                    if (existingCustomerDoc != null) {
                        val existingEmail = existingCustomerDoc.getString("email")?.takeIf { it.isNotBlank() } ?: cleanEmail
                        try {
                            if (existingEmail.isNotEmpty()) {
                                auth.signInWithEmailAndPassword(existingEmail, pass).await()
                                setupSession()
                                _uiEvent.emit(UiEvent.ShowSuccess("Existing account found for Mobile $cleanPhone. Logged in successfully!"))
                                return@launch
                            }
                        } catch (loginEx: Exception) {
                            Log.e("StoreViewModel", "Auto-login to existing customer failed", loginEx)
                            _uiEvent.emit(UiEvent.ShowError("An account with Mobile $cleanPhone already exists ($existingEmail). Please check your password or log in."))
                            isAuthLoading = false
                            return@launch
                        }
                    }
                }

                val result = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
                val user = result.user
                if (user != null) {
                    val targetCollection = when (roleStr) {
                        "ADMIN" -> "users"
                        "RIDER" -> "riders"
                        "OTHER" -> "staff"
                        "CUSTOMER" -> "customers"
                        else -> "sellers"
                    }
                    val newProfile = UserProfile(
                        uid = user.uid,
                        email = cleanEmail,
                        name = name,
                        role = roleStr,
                        phoneNumber = cleanPhone,
                        staffDepartment = staffDepartment ?: if (roleStr == "OTHER") "General Operations" else ""
                    )
                    firestore.collection(targetCollection).document(user.uid).set(newProfile).await()
                    currentUserProfile = newProfile
                    setupSession()
                    _uiEvent.emit(UiEvent.ShowSuccess("Account Created for $cleanPhone"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Registration Failed: ${e.message}"))
            } finally {
                isAuthLoading = false
            }
        }
    }

    // ORDER ACTIONS
    fun acceptOrder(orderId: String) {
        viewModelScope.launch { orderRepository.updateOrderFields(orderId, mapOf("status" to OrderStatus.ACCEPTED)) }
    }

    fun markReady(orderId: String) {
        viewModelScope.launch {
            orderRepository.updateOrderFields(orderId, mapOf(
                "status" to OrderStatus.READY,
                "readyAt" to System.currentTimeMillis(),
                "merchantLatitude" to currentUserProfile?.latitude,
                "merchantLongitude" to currentUserProfile?.longitude
            ))
        }
    }

    fun cancelOrder(orderId: String, reason: String? = null) {
        viewModelScope.launch {
            val updates = mapOf<String, Any?>(
                "status" to OrderStatus.CANCELLED,
                "cancelledBy" to (currentUserProfile?.name ?: "System"),
                "cancellationReason" to (reason ?: "Cancelled by store")
            )
            orderRepository.updateOrderFields(orderId, updates)
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch { orderRepository.deleteOrder(orderId) }
    }

    fun markDispatched(orderId: String) {
        viewModelScope.launch { orderRepository.updateOrderFields(orderId, mapOf("status" to OrderStatus.DISPATCHED)) }
    }

    fun assignOrderToRider(orderId: String, riderId: String) {
        viewModelScope.launch {
            orderRepository.updateOrderFields(orderId, mapOf(
                "deliveryAgentId" to riderId,
                "status" to OrderStatus.ASSIGNED,
                "assignedAt" to System.currentTimeMillis()
            ))
        }
    }

    fun startPicking(orderId: String) {
        viewModelScope.launch { orderRepository.updateOrderFields(orderId, mapOf("status" to OrderStatus.PICKING)) }
    }

    fun requestSubstitute(orderId: String, substituteProduct: Product) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "substituteRequested" to true,
                    "substituteProductId" to substituteProduct.id,
                    "substituteProductName" to substituteProduct.name,
                    "substituteStatus" to SubstituteStatus.PENDING,
                    "stockReservedUntil" to System.currentTimeMillis() + (15 * 60 * 1000)
                )
                orderRepository.updateOrderFields(orderId, updates).onSuccess {
                    _uiEvent.emit(UiEvent.ShowSuccess("Substitute requested for Order #${orderId.takeLast(6)}"))
                }.onFailure {
                    _uiEvent.emit(UiEvent.ShowError("Failed to update order for substitute"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error requesting substitute"))
            }
        }
    }

    fun printInvoice(order: Order) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val pdfDocument = PdfDocument()
                val paint = Paint()
                val titlePaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 18f
                }
                val textPaint = Paint().apply {
                    textSize = 12f
                }

                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                var y = 40f
                canvas.drawText("FLASH-O STORE INVOICE", 200f, y, titlePaint)
                y += 40f
                canvas.drawText("Order ID: ${order.id}", 50f, y, textPaint)
                y += 20f
                canvas.drawText("Customer: ${order.customerName}", 50f, y, textPaint)
                y += 20f
                canvas.drawText("Phone: ${order.phoneNumber}", 50f, y, textPaint)
                y += 20f
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                canvas.drawText("Date: ${sdf.format(Date(order.timestamp))}", 50f, y, textPaint)
                y += 40f

                canvas.drawText("Items:", 50f, y, titlePaint)
                y += 25f
                
                order.items.forEach { item ->
                    val text = "${item.product.name} x ${item.quantity} = ₹${item.product.price * item.quantity}"
                    canvas.drawText(text, 70f, y, textPaint)
                    y += 20f
                }

                y += 20f
                canvas.drawText("--------------------------------------------------", 50f, y, textPaint)
                y += 20f
                canvas.drawText("Subtotal: ₹${order.totalPrice}", 50f, y, textPaint)
                y += 20f
                canvas.drawText("Delivery: ₹${order.deliveryCharge}", 50f, y, textPaint)
                y += 20f
                canvas.drawText("Platform Fee: ₹${order.handlingFee}", 50f, y, textPaint)
                y += 20f
                canvas.drawText("Discount: -₹${order.discountAmount}", 50f, y, textPaint)
                y += 25f
                canvas.drawText("TOTAL AMOUNT: ₹${order.totalAmount}", 50f, y, titlePaint)

                pdfDocument.finishPage(page)

                val file = File(context.cacheDir, "invoice_${order.id}.pdf")
                file.outputStream().use { 
                    pdfDocument.writeTo(it)
                }
                pdfDocument.close()

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                
            } catch (e: Exception) {
                Log.e("StoreViewModel", "PDF Generation Error", e)
                viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Print Failed: ${e.message}")) }
            }
        }
    }

    fun getNearbyRiders(merchantLat: Double?, merchantLon: Double?): List<Pair<Rider, Int>> {
        return riders.map { rider ->
            val riderProfile = allUsers.find { it.uid == rider.id }
            val distance = if (merchantLat != null && merchantLon != null && riderProfile?.latitude != null && riderProfile.longitude != null) {
                calculateDistance(merchantLat, merchantLon, riderProfile.latitude!!, riderProfile.longitude!!)
            } else {
                (100..1000).random()
            }
            rider to distance
        }.sortedBy { it.second }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (r * c * 1000).toInt()
    }

    fun calculateOrderEconomics(order: Order): OrderEconomics {
        val passConfig = dynamicAppConfig.flashOPass
        val customer = allCustomers.find { it.mobile == order.phoneNumber }
        val isFirstOrderTrial = customer?.totalOrders == 0 && customer.isTrialEligible

        val activeTier = if (order.subscribed || isFirstOrderTrial) {
            val tierId = if (isFirstOrderTrial) "premium_199" else order.subscriptionTier
            passConfig?.tiers?.find { it.id == tierId }
        } else null

        val couponCode = order.couponCode?.trim()?.uppercase()

        val itemDetails = order.items.map { item ->
            val p = item.product
            val rawItemValue = p.price * item.quantity

            val appliedCoupon = p.availableCoupons.find { it.code.trim().uppercase() == couponCode }
            val itemDiscount = (appliedCoupon?.discount ?: 0.0) * item.quantity
            val itemValue = (rawItemValue - itemDiscount).coerceAtLeast(0.0)

            val meetsAmount = p.freeDeliveryMinAmount > 0 && itemValue >= p.freeDeliveryMinAmount
            val meetsQty = p.freeDeliveryMinQuantity > 0 && item.quantity >= p.freeDeliveryMinQuantity
            val meetsPassThreshold = activeTier != null && order.totalPrice >= activeTier.minOrderForFreeDelivery

            val effectiveDelivery = if (meetsAmount || meetsQty || meetsPassThreshold) 0.0 else p.deliveryCharge

            val commission = itemValue * (p.commissionRate / 100.0)
            val adminProfit = itemValue * (p.adminProfitPercent / 100.0)
            val tax = itemValue * (p.taxRate / 100.0)
            val merchantPayout = p.merchantRate * item.quantity

            ItemEconomics(
                productId = p.id,
                name = p.name,
                total = itemValue,
                commission = commission,
                adminProfit = adminProfit,
                tax = tax,
                merchantPayout = merchantPayout,
                itemDeliveryCharge = effectiveDelivery,
                discount = itemDiscount
            )
        }

        val totalCommission = itemDetails.sumOf { it.commission }
        val totalBaseProfit = itemDetails.sumOf { it.adminProfit }
        val totalTax = itemDetails.sumOf { it.tax }
        val totalMerchantPayout = itemDetails.sumOf { it.merchantPayout }
        val totalDiscount = itemDetails.sumOf { it.discount }
        val totalDeliveryCharge = itemDetails.maxOfOrNull { it.itemDeliveryCharge } ?: 0.0

        return OrderEconomics(
            itemEconomics = itemDetails,
            totalCommission = totalCommission,
            totalAdminProfit = totalBaseProfit,
            totalTax = totalTax,
            totalMerchantPayout = totalMerchantPayout,
            deliveryCharge = totalDeliveryCharge,
            platformFee = storeConfig.platformFee,
            totalEstimatedProfit = totalCommission + totalBaseProfit + storeConfig.platformFee,
            totalDiscount = totalDiscount
        )
    }

    fun finalizeOrderBilling(order: Order) {
        val economics = calculateOrderEconomics(order)
        val discountToApply = if (economics.totalDiscount > 0) economics.totalDiscount else order.discountAmount
        val newTotalAmount = order.totalPrice + economics.deliveryCharge + order.handlingFee - discountToApply + order.roundingOff

        viewModelScope.launch {
            try {
                orderRepository.updateOrderFields(order.id, mapOf(
                    "deliveryCharge" to economics.deliveryCharge,
                    "totalAmount" to newTotalAmount,
                    "discountAmount" to discountToApply,
                    "estimatedProfit" to economics.totalEstimatedProfit
                ))
                _uiEvent.emit(UiEvent.ShowSuccess("Billing Finalized by Admin!"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to finalize billing: ${e.message}"))
            }
        }
    }

    data class ItemEconomics(
        val productId: String,
        val name: String,
        val total: Double,
        val commission: Double,
        val adminProfit: Double,
        val tax: Double,
        val merchantPayout: Double,
        val itemDeliveryCharge: Double,
        val discount: Double = 0.0
    )

    data class OrderEconomics(
        val itemEconomics: List<ItemEconomics>,
        val totalCommission: Double,
        val totalAdminProfit: Double,
        val totalTax: Double,
        val totalMerchantPayout: Double,
        val deliveryCharge: Double,
        val platformFee: Double,
        val totalEstimatedProfit: Double,
        val totalDiscount: Double = 0.0
    )

    // PRODUCT & CATEGORY ACTIONS
    fun addProduct(product: Product) {
        viewModelScope.launch { productRepository.addProduct(product) }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch { productRepository.updateProduct(product) }
    }

    fun updateProductByAdmin(product: Product, feedback: String? = null) {
        viewModelScope.launch {
            try {
                val updatedProduct = product.copy(adminFeedback = feedback ?: product.adminFeedback)
                productRepository.updateProduct(updatedProduct)
                _uiEvent.emit(UiEvent.ShowSuccess("Product Updated by Admin"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Update Failed"))
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch { productRepository.deleteProduct(productId) }
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            try {
                val currentSeq = dynamicAppConfig.categorySequence.toMutableList()
                if (!currentSeq.contains(categoryName)) {
                    currentSeq.add(categoryName)
                    val newConfig = dynamicAppConfig.copy(categorySequence = currentSeq)
                    firestore.collection("settings").document("dynamic_app_config").set(newConfig).await()
                    dynamicAppConfig = newConfig
                    _uiEvent.emit(UiEvent.ShowSuccess("Category '$categoryName' Created"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Creation Failed: ${e.message}"))
            }
        }
    }

    fun updateCategorySequence(newSequence: List<String>) {
        updateDynamicConfig(dynamicAppConfig.copy(categorySequence = newSequence))
    }

    fun toggleCategoryComingSoon(categoryName: String) {
        val currentList = dynamicAppConfig.comingSoonCategories.toMutableList()
        if (currentList.contains(categoryName)) currentList.remove(categoryName) else currentList.add(categoryName)
        updateDynamicConfig(dynamicAppConfig.copy(comingSoonCategories = currentList))
    }

    fun updateHomeSectionOrder(newOrder: List<String>) {
        updateDynamicConfig(dynamicAppConfig.copy(homeSectionOrder = newOrder))
    }

    fun deleteCategory(categoryName: String) {
        viewModelScope.launch {
            try {
                val newCatSeq = dynamicAppConfig.categorySequence.filter { it != categoryName }
                val newHomeSeq = dynamicAppConfig.homeSectionOrder.filter { it != categoryName }
                val newConfig = dynamicAppConfig.copy(categorySequence = newCatSeq, homeSectionOrder = newHomeSeq)
                firestore.collection("settings").document("dynamic_app_config").set(newConfig).await()
                dynamicAppConfig = newConfig
                _uiEvent.emit(UiEvent.ShowSuccess("Category '$categoryName' removed"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Delete failed: ${e.message}"))
            }
        }
    }

    fun updateProductSequence(productId: String, newSequence: Int) {
        viewModelScope.launch {
            try {
                firestore.collection("products").document(productId).update("sequenceNumber", newSequence).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Sequence updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to update sequence"))
            }
        }
    }

    fun bulkUpdateProductSequences(updatedProducts: List<Product>) {
        viewModelScope.launch {
            try {
                val batch = firestore.batch()
                updatedProducts.forEach { product ->
                    val docRef = firestore.collection("products").document(product.id)
                    batch.update(docRef, "sequenceNumber", product.sequenceNumber)
                }
                batch.commit().await()
                _uiEvent.emit(UiEvent.ShowSuccess("Sequences saved"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to update sequences"))
            }
        }
    }

    fun approveProduct(productId: String, commission: Double, profitPercent: Double, deliveryCharge: Double, gstRate: Double) {
        viewModelScope.launch {
            try {
                firestore.collection("products").document(productId).update(
                    mapOf(
                        "isVerified" to true,
                        "isAvailable" to true,
                        "verifiedBy" to (auth.currentUser?.uid ?: "admin"),
                        "verificationDate" to System.currentTimeMillis(),
                        "commissionRate" to commission,
                        "adminProfitPercent" to profitPercent,
                        "deliveryCharge" to deliveryCharge,
                        "taxRate" to gstRate,
                        "cgstRate" to gstRate / 2.0,
                        "sgstRate" to gstRate / 2.0
                    )
                ).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Product Approved"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Approval Failed"))
            }
        }
    }

    fun approveAllProducts() {
        viewModelScope.launch {
            try {
                val validProducts = rawProducts.filter { it.id.isNotBlank() }
                if (validProducts.isEmpty()) return@launch
                val batch = firestore.batch()
                validProducts.forEach { product ->
                    val docRef = firestore.collection("products").document(product.id)
                    batch.set(docRef, mapOf("isVerified" to true, "isAvailable" to true), com.google.firebase.firestore.SetOptions.merge())
                }
                batch.commit().await()
                _uiEvent.emit(UiEvent.ShowSuccess("Approved all products"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Bulk Approval Failed"))
            }
        }
    }

    suspend fun uploadProductImage(uri: Uri): String? {
        return storageRepository.uploadImage(uri, "products/${UUID.randomUUID()}")
    }

    suspend fun uploadApk(uri: Uri, appId: String): String? {
        return storageRepository.uploadImage(uri, "apks/$appId/${UUID.randomUUID()}.apk")
    }

    fun updateAppRelease(info: AppUpdateInfo) {
        viewModelScope.launch {
            try {
                firestore.collection("app_updates").document(info.appId).set(info).await()
                _uiEvent.emit(UiEvent.ShowSuccess("New Update Released: ${info.appName} v${info.versionName}"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Update Failed"))
            }
        }
    }

    fun updateDynamicConfig(config: DynamicAppConfig) {
        viewModelScope.launch {
            try {
                firestore.collection("settings").document("dynamic_app_config").set(config).await()
                dynamicAppConfig = config
                _uiEvent.emit(UiEvent.ShowSuccess("App Config Updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Update Failed"))
            }
        }
    }

    fun sendBroadcastPushNotification(title: String, body: String, imageUrl: String? = null, actionUrl: String? = null) {
        viewModelScope.launch {
            try {
                notificationRepository.sendBroadcastNotification(title, body, imageUrl, actionUrl, "ALL_CUSTOMERS")
                _uiEvent.emit(UiEvent.ShowSuccess("Broadcast Queued"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Broadcast Failed"))
            }
        }
    }

    fun sendNotification(title: String, body: String, imageUrl: String? = null, targetTopic: String = "all_users") {
        viewModelScope.launch {
            try {
                val target = if (targetTopic == "store_partners") "ALL_RIDERS" else "ALL_CUSTOMERS"
                notificationRepository.sendBroadcastNotification(title, body, imageUrl, null, target)
                _uiEvent.emit(UiEvent.ShowSuccess("Broadcast Sent to $targetTopic"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to Send Message"))
            }
        }
    }

    fun toggleProductInDeal(productId: String) {
        val currentFlashSale = dynamicAppConfig.flashSale ?: FlashSale()
        val currentProductIds = currentFlashSale.productIds.toMutableList()
        val isInDeal = currentProductIds.contains(productId)
        if (isInDeal) currentProductIds.remove(productId) else currentProductIds.add(productId)
        val updatedConfig = dynamicAppConfig.copy(flashSale = currentFlashSale.copy(productIds = currentProductIds))
        updateDynamicConfig(updatedConfig)
        viewModelScope.launch { _uiEvent.emit(UiEvent.ShowSuccess(if (isInDeal) "Removed from Deals" else "Added to Deals")) }
    }

    fun updateStoreConfig(config: StoreConfig) {
        viewModelScope.launch {
            try {
                firestore.collection("settings").document("store_config").set(config).await()
                storeConfig = config
                _uiEvent.emit(UiEvent.ShowSuccess("Store Settings Updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Update Failed"))
            }
        }
    }

    fun updateDarkStore(darkStore: DarkStore) {
        viewModelScope.launch {
            try {
                val id = darkStore.id.ifEmpty { firestore.collection("dark_stores").document().id }
                val finalStore = darkStore.copy(id = id)
                firestore.collection("dark_stores").document(id).set(finalStore).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Dark Store '${finalStore.name}' Updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to update Dark Store"))
            }
        }
    }

    fun deleteDarkStore(id: String) {
        viewModelScope.launch {
            try {
                // Safe unassign attached merchants and riders
                allUsers.filter { it.darkStoreId == id }.forEach { user ->
                    val col = when (user.userRole) {
                        UserRole.MERCHANT, UserRole.STORE_OWNER -> "sellers"
                        UserRole.RIDER -> "riders"
                        else -> "users"
                    }
                    try {
                        firestore.collection(col).document(user.uid).update("darkStoreId", null).await()
                    } catch (_: Exception) {}
                    try {
                        firestore.collection("users").document(user.uid).update("darkStoreId", null).await()
                    } catch (_: Exception) {}
                }

                firestore.collection("dark_stores").document(id).delete().await()
                _uiEvent.emit(UiEvent.ShowSuccess("Dark Store Removed & Attached Users Safely Unassigned"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Delete Failed"))
            }
        }
    }

    fun assignMerchantToDarkStore(merchantId: String, darkStoreId: String?) {
        viewModelScope.launch {
            try {
                firestore.collection("sellers").document(merchantId).update("darkStoreId", darkStoreId).await()

                // Sync assignedMerchantIds across dark stores
                darkStores.forEach { ds ->
                    val newList = ds.assignedMerchantIds.toMutableList()
                    if (ds.id == darkStoreId) {
                        if (!newList.contains(merchantId)) newList.add(merchantId)
                    } else {
                        newList.remove(merchantId)
                    }
                    if (newList != ds.assignedMerchantIds) {
                        firestore.collection("dark_stores").document(ds.id).update("assignedMerchantIds", newList).await()
                    }
                }

                _uiEvent.emit(UiEvent.ShowSuccess("Merchant Assignment Updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Assignment Failed"))
            }
        }
    }

    fun assignRiderToDarkStore(riderId: String, darkStoreId: String?) {
        viewModelScope.launch {
            try {
                val storeName = darkStores.find { it.id == darkStoreId }?.name ?: ""
                val updates = mutableMapOf<String, Any?>(
                    "darkStoreId" to darkStoreId,
                    "darkStoreArea" to storeName,
                    "darkStore" to storeName
                )

                try {
                    firestore.collection("riders").document(riderId).update(updates).await()
                } catch (_: Exception) {}
                try {
                    firestore.collection("users").document(riderId).update(updates).await()
                } catch (_: Exception) {}

                // Sync assignedRiderIds across dark stores
                darkStores.forEach { ds ->
                    val newList = ds.assignedRiderIds.toMutableList()
                    if (ds.id == darkStoreId) {
                        if (!newList.contains(riderId)) newList.add(riderId)
                    } else {
                        newList.remove(riderId)
                    }
                    if (newList != ds.assignedRiderIds) {
                        firestore.collection("dark_stores").document(ds.id).update("assignedRiderIds", newList).await()
                    }
                }

                _uiEvent.emit(UiEvent.ShowSuccess("Rider Hub Assignment Updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Rider Assignment Failed"))
            }
        }
    }

    // ANALYTICS & PREDICTIONS
    fun getStoreStats(): StoreStats {
        return StoreStats(
            totalOrders = orders.size,
            pendingOrders = orders.count { it.status == OrderStatus.PLACED },
            packedOrders = orders.count { it.status == OrderStatus.READY || it.status == OrderStatus.SEALED },
            outForDeliveryOrders = orders.count { it.status == OrderStatus.DISPATCHED || it.status == OrderStatus.ASSIGNED },
            deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED },
            cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED },
            returnedOrders = orders.count { it.status == OrderStatus.RETURNED },
            totalRevenue = orders.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.totalAmount }
        )
    }

    fun executeAutoOptimization() {
        viewModelScope.launch { _uiEvent.emit(UiEvent.ShowSuccess("Auto Optimization Executed")) }
    }

    fun setSimulatedEnvironment(weather: Double, event: Double) {
        simulatedWeatherImpact = weather
        simulatedEventImpact = event

        // Sync to Dynamic App Config for Customers
        val updatedConfig = dynamicAppConfig.copy(
            weatherImpact = weather,
            eventImpact = event,
            lastUpdated = System.currentTimeMillis()
        )
        updateDynamicConfig(updatedConfig)

        // Update analytics based on simulation
        val currentAnalytics = customerBehaviorAnalytics
        val surge = if (weather > 7.0 || event > 7.0) "High Surge" else if (weather > 3.0 || event > 3.0) "Rising" else "Normal"
        val loadFactor = 1.0 + (weather * 0.05) + (event * 0.1)
        
        customerBehaviorAnalytics = currentAnalytics.copy(
            weatherImpactScore = weather,
            eventImpactScore = event,
            surgeStatus = surge,
            activitySurgePercentage = (weather + event) * 5.0,
            preparationPlan = currentAnalytics.preparationPlan.copy(
                estimatedLoadFactor = loadFactor,
                suggestedRiders = (loadFactor * 5).toInt(),
                suggestedPickers = (loadFactor * 3).toInt()
            )
        )
    }

    fun rapidInventoryPrep() {
        viewModelScope.launch { _uiEvent.emit(UiEvent.ShowSuccess("Inventory Team Alerted")) }
    }

    fun getPredictedStockOutItems(): List<Product> = products.filter { it.stockQuantity <= it.minStockLevel }
    fun getPopularSearches(): List<PopularSearch> = emptyList()
    fun getUniqueAreas(): List<String> = orders.map { it.area }.distinct().filter { it.isNotEmpty() }
    fun getDeliveryHeatmapData(filter: String = "All"): Map<String, Int> {
        return orders.filter { if (filter == "All") true else it.area == filter }
            .groupBy { it.area }
            .mapValues { it.value.size }
    }

    fun getRevenueForRange(start: Long, end: Long): Double {
        return orders.filter { it.status == OrderStatus.DELIVERED && it.timestamp in start..end }.sumOf { it.totalAmount }
    }

    fun updateUserStatus(uid: String, active: Boolean, reason: String? = null) {
        viewModelScope.launch {
            try {
                // Determine collection based on existing role
                val user = allUsers.find { it.uid == uid } ?: return@launch
                if (com.fryumm.store.util.MasterAdminSecurity.isMasterAdminEmail(user.email) || user.userRole == UserRole.ADMIN) {
                    if (!active) {
                        _uiEvent.emit(UiEvent.ShowError("SECURITY PROTECTION: Master Admin accounts cannot be blocked or deactivated!"))
                        return@launch
                    }
                }
                val collection = when(user.userRole) {
                    UserRole.ADMIN -> "users"
                    UserRole.RIDER -> "riders"
                    UserRole.OTHER -> "staff"
                    UserRole.CUSTOMER -> "customers"
                    else -> "sellers"
                }
                
                val updates = mutableMapOf<String, Any>(
                    "active" to active,
                    "blockReason" to (reason ?: "")
                )
                
                firestore.collection(collection).document(uid).update(updates).await()
                _uiEvent.emit(UiEvent.ShowSuccess(if (active) "User Unblocked" else "User Blocked"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Update Failed: ${e.message}"))
            }
        }
    }

    fun getLastOrderDateForUser(uid: String, email: String, phone: String): Long? {
        val userOrders = orders.filter { 
            it.merchantId == uid || it.deliveryAgentId == uid ||
            (phone.isNotBlank() && it.phoneNumber == phone)
        }
        return userOrders.maxOfOrNull { it.timestamp }
    }

    fun changeUserRole(uid: String, newRole: String) {
        viewModelScope.launch {
            try {
                val user = allUsers.find { it.uid == uid }
                if (user != null) {
                    val oldCollection = when(user.userRole) {
                        UserRole.ADMIN -> "users"
                        UserRole.RIDER -> "riders"
                        UserRole.OTHER -> "staff"
                        UserRole.CUSTOMER -> "customers"
                        else -> "sellers"
                    }
                    val parsedRole = try { UserRole.valueOf(newRole.uppercase()) } catch(e: Exception) { UserRole.STORE_OWNER }
                    val newCollection = when(parsedRole) {
                        UserRole.ADMIN -> "users"
                        UserRole.RIDER -> "riders"
                        UserRole.OTHER -> "staff"
                        UserRole.CUSTOMER -> "customers"
                        else -> "sellers"
                    }

                    val updates = mapOf(
                        "role" to newRole,
                        "active" to true,
                        "isActive" to true
                    )

                    if (oldCollection == newCollection) {
                        firestore.collection(oldCollection).document(uid).update(updates).await()
                    } else {
                        val doc = firestore.collection(oldCollection).document(uid).get().await()
                        if (doc.exists()) {
                            val data = doc.data?.toMutableMap() ?: mutableMapOf()
                            data.putAll(updates)
                            firestore.collection(newCollection).document(uid).set(data).await()
                            firestore.collection(oldCollection).document(uid).delete().await()
                        }
                    }
                    _uiEvent.emit(UiEvent.ShowSuccess("Role changed to $newRole successfully"))
                } else {
                    val custDoc = firestore.collection("customers").document(uid).get().await()
                    if (custDoc.exists()) {
                        val parsedRole = try { UserRole.valueOf(newRole.uppercase()) } catch(e: Exception) { UserRole.STORE_OWNER }
                        val newCollection = when(parsedRole) {
                            UserRole.ADMIN -> "users"
                            UserRole.RIDER -> "riders"
                            UserRole.OTHER -> "staff"
                            else -> "sellers"
                        }
                        val data = custDoc.data?.toMutableMap() ?: mutableMapOf()
                        data["role"] = newRole
                        data["name"] = data["name"] ?: ""
                        data["email"] = data["email"] ?: ""
                        data["phoneNumber"] = data["mobile"] ?: ""
                        data["photoUrl"] = data["photoUrl"] ?: ""
                        
                        firestore.collection(newCollection).document(uid).set(data).await()
                        firestore.collection("customers").document(uid).delete().await()
                        _uiEvent.emit(UiEvent.ShowSuccess("Customer role changed to $newRole"))
                    }
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Role change failed: ${e.message}"))
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Reset Email Sent to $email"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed: ${e.message}"))
            }
        }
    }

    fun getAdvancedAnalytics(
        targetOrders: List<Order> = orders,
        targetSearches: List<SearchEvent> = searchEvents,
        targetEvents: List<CustomerActivityEvent> = customerEvents,
        targetCustomers: List<CustomerProfile> = allCustomers,
        targetProducts: List<Product> = products
    ): AdvancedAnalytics {
        val totalOrdersCount = targetOrders.size
        val totalSearches = targetSearches.size
        val productViews = targetEvents.count { 
            it.eventType == CustomerEventType.VIEW_PRODUCT || it.eventType == "VIEW_PRODUCT" 
        }
        val addToCarts = targetEvents.count { 
            it.eventType == CustomerEventType.ADD_TO_CART || it.eventType == "ADD_TO_CART" 
        }
        val checkouts = targetEvents.count { 
            it.eventType == CustomerEventType.CHECKOUT_STARTED || it.eventType == "CHECKOUT_INITIATED" || it.eventType == "CHECKOUT" 
        }
        val deliveredOrders = targetOrders.count { it.status == OrderStatus.DELIVERED }

        val baseFunnelCount = totalSearches.coerceAtLeast(productViews).coerceAtLeast(totalOrdersCount)

        val funnel = listOf(
            FunnelStep("Product Search", totalSearches, if (baseFunnelCount > 0) (totalSearches.toDouble() / baseFunnelCount * 100).coerceAtMost(100.0) else 0.0),
            FunnelStep("Product Clicks", productViews, if (baseFunnelCount > 0) (productViews.toDouble() / baseFunnelCount * 100).coerceAtMost(100.0) else 0.0),
            FunnelStep("Add to Cart", addToCarts, if (productViews > 0) (addToCarts.toDouble() / productViews * 100).coerceAtMost(100.0) else if (baseFunnelCount > 0) (addToCarts.toDouble() / baseFunnelCount * 100) else 0.0),
            FunnelStep("Checkout Initiated", checkouts, if (addToCarts > 0) (checkouts.toDouble() / addToCarts * 100).coerceAtMost(100.0) else 0.0),
            FunnelStep("Orders Placed", totalOrdersCount, if (checkouts > 0) (totalOrdersCount.toDouble() / checkouts * 100).coerceAtMost(100.0) else 0.0),
            FunnelStep("Delivered", deliveredOrders, if (totalOrdersCount > 0) (deliveredOrders.toDouble() / totalOrdersCount * 100) else 0.0)
        )

        val zeroSearches = targetSearches.filter { it.resultCount == 0 }
            .groupBy { it.query.lowercase().trim() }
            .map { (q, evs) -> ZeroResultSearch(q, evs.size) }
            .sortedByDescending { it.count }
            .take(10)

        val oosProducts = targetProducts.filter { it.stockQuantity <= 0 || !it.isInStock }
        val lowStockProducts = targetProducts.filter { it.stockQuantity in 1..(it.minStockLevel.coerceAtLeast(5)) }
        val deadStockProducts = targetProducts.filter { p -> p.stockQuantity > 20 && targetOrders.flatMap { it.items }.none { it.product.id == p.id } }
        val expiredProducts = targetProducts.filter { it.expiryDate != null && it.expiryDate!! < System.currentTimeMillis() }

        // Best & Worst Sellers from targetOrders items
        val soldItemsMap = mutableMapOf<String, Pair<String, Pair<Int, Double>>>()
        targetOrders.flatMap { it.items }.forEach { item ->
            val pId = item.product.id.ifEmpty { item.product.name }
            val current = soldItemsMap[pId] ?: Pair(item.product.name, Pair(0, 0.0))
            val newQty = current.second.first + item.quantity
            val newRev = current.second.second + (item.product.price * item.quantity)
            soldItemsMap[pId] = Pair(item.product.name.ifEmpty { current.first }, Pair(newQty, newRev))
        }

        val bestSellersList = soldItemsMap.values
            .map { (name, qtyRev) -> ProductAnalysis(productName = name, totalSold = qtyRev.first, revenue = qtyRev.second) }
            .sortedByDescending { it.revenue }
            .take(5)

        val worstSellersList = targetProducts
            .filter { it.stockQuantity > 0 }
            .map { p ->
                val sold = soldItemsMap[p.id]?.second?.first ?: 0
                val rev = soldItemsMap[p.id]?.second?.second ?: 0.0
                ProductAnalysis(productName = p.name, totalSold = sold, revenue = rev)
            }
            .sortedBy { it.totalSold }
            .take(5)

        // Co-Purchased Bundle Suggestions
        val pairCounts = mutableMapOf<Pair<String, String>, Int>()
        targetOrders.forEach { order ->
            val itemNames = order.items.map { it.product.name.ifEmpty { it.product.id } }.distinct()
            if (itemNames.size >= 2) {
                for (i in itemNames.indices) {
                    for (j in i + 1 until itemNames.size) {
                        val pair = if (itemNames[i] < itemNames[j]) Pair(itemNames[i], itemNames[j]) else Pair(itemNames[j], itemNames[i])
                        pairCounts[pair] = (pairCounts[pair] ?: 0) + 1
                    }
                }
            }
        }
        val bundleSuggestions = pairCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { BundleSuggestion(products = listOf(it.key.first, it.key.second), support = it.value) }

        val deliveredList = targetOrders.filter { it.status == OrderStatus.DELIVERED }
        val avgDeliveryTime = if (deliveredList.isNotEmpty()) {
            deliveredList.mapNotNull { order ->
                order.deliveryTime?.minus(order.timestamp)?.div(60000)
            }.let {
                if (it.isNotEmpty()) it.average().toInt().coerceIn(5, 60) else 12
            }
        } else 12

        val lateDeliveries = targetOrders.count { order ->
            val totalMins = (order.deliveryTime ?: System.currentTimeMillis()) - order.timestamp
            totalMins > 15 * 60 * 1000L
        }

        val qcFailures = targetOrders.count { it.status == OrderStatus.RETURNED || it.substituteRequested }
        val qcFailureRate = if (targetOrders.isNotEmpty()) (qcFailures.toFloat() / targetOrders.size * 100) else 0f

        val paymentSplit = targetOrders.groupBy { it.paymentMethod.ifEmpty { "COD" } }
            .mapValues { it.value.size }

        val paymentFailures = targetEvents.count { it.eventType == CustomerEventType.PAYMENT_FAILED }
        val paymentStarts = targetEvents.count { it.eventType == CustomerEventType.PAYMENT_STARTED || it.eventType == CustomerEventType.CHECKOUT_STARTED }.coerceAtLeast(1)
        val paymentFailureRate = (paymentFailures.toFloat() / paymentStarts * 100).coerceIn(0f, 100f)

        val repeatCount = targetCustomers.count { it.totalOrders > 1 }
        val repeatRate = if (targetCustomers.isNotEmpty()) (repeatCount.toFloat() / targetCustomers.size * 100) else 0f
        val avgLtv = if (targetCustomers.isNotEmpty()) targetCustomers.map { it.lifetimeSpending }.average() else 0.0

        val demandPredictions = targetProducts.take(10).map { product ->
            val soldUnits = soldItemsMap[product.id]?.second?.first ?: 0
            val predictedDemand = (soldUnits * 1.5).toInt().coerceAtLeast(5)
            val recommendation = when {
                product.stockQuantity <= 0 -> "Restock Immediately (OOS)"
                product.stockQuantity < product.minStockLevel -> "Low Stock - Order Soon"
                product.stockQuantity > product.minStockLevel * 5 -> "Overstocked"
                else -> "Healthy Stock"
            }
            DemandPrediction(
                productName = product.name,
                productId = product.id,
                currentStock = product.stockQuantity,
                predictedDemand = predictedDemand,
                confidence = 0.85f,
                recommendation = recommendation
            )
        }

        return AdvancedAnalytics(
            funnel = funnel,
            zeroResultSearches = zeroSearches,
            abandonmentRate = if (addToCarts > 0) ((addToCarts - totalOrdersCount).toFloat() / addToCarts * 100).coerceAtLeast(0f) else 0f,
            bundleSuggestions = bundleSuggestions,
            operational = OperationalStats(
                avgPickingTimeMinutes = 3,
                avgPackingTimeMinutes = 2,
                qcFailureRate = qcFailureRate,
                avgRiderWaitTimeMinutes = 2,
                avgDeliveryTimeMinutes = avgDeliveryTime,
                lateDeliveries = lateDeliveries
            ),
            productIntelligence = ProductIntelligence(
                bestSellers = bestSellersList,
                worstSellers = worstSellersList,
                outOfStock = oosProducts,
                lowStock = lowStockProducts,
                deadStock = deadStockProducts,
                expiredProducts = expiredProducts
            ),
            demandPredictions = demandPredictions,
            retention = RetentionStats(
                repeatPurchaseRate = repeatRate,
                averageLTV = avgLtv,
                inactiveUsersCount = targetCustomers.count { it.totalOrders == 0 }
            ),
            payment = PaymentStats(
                methodSplit = paymentSplit,
                failureRate = paymentFailureRate
            )
        )
    }

    fun getCustomerSummaryStats(): CustomerSummaryStats {
        return CustomerSummaryStats(
            total = allCustomers.size,
            activeToday = allCustomers.count { it.totalOrders > 0 },
            newThisWeek = allCustomers.count { it.totalOrders <= 1 },
            repeat = allCustomers.count { it.totalOrders > 1 },
            dormant = allCustomers.count { it.totalOrders > 0 && it.lifetimeSpending < 100 },
            vipCount = allCustomers.count { it.lifetimeSpending > 5000 },
            riskyCount = allCustomers.count { it.totalOrders > 5 && it.lifetimeSpending < 500 }
        )
    }

    fun getCustomerAnalysis(): Map<String, List<CustomerProfile>> {
        return mapOf(
            "Total Customers" to allCustomers,
            "New Customers" to allCustomers.filter { it.totalOrders <= 1 },
            "Repeat Customers" to allCustomers.filter { it.totalOrders > 1 },
            "VIP" to allCustomers.filter { it.lifetimeSpending > 5000 },
            "Risky Customers" to allCustomers.filter { it.totalOrders > 5 && it.lifetimeSpending < 500 }
        )
    }

    fun getMarketingAnalytics(
        targetOrders: List<Order> = orders,
        targetEvents: List<CustomerActivityEvent> = customerEvents,
        targetCustomers: List<CustomerProfile> = allCustomers
    ): MarketingAnalytics {
        val deliveredOrders = targetOrders.filter { it.status == OrderStatus.DELIVERED }
        val couponOrdersMap = targetOrders.filter { !it.couponCode.isNullOrEmpty() }
            .groupBy { it.couponCode!! }

        val campaignsList = couponOrdersMap.map { (code, oList) ->
            val oCount = oList.size
            val oRev = oList.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.totalAmount }
            val promoEvents = targetEvents.filter { it.details.contains(code, ignoreCase = true) }
            val sent = promoEvents.count { it.eventType == CustomerEventType.CAMPAIGN_SENT || it.eventType == CustomerEventType.NOTIFICATION_RECEIVED }.coerceAtLeast(oCount * 4)
            val opened = promoEvents.count { it.eventType == CustomerEventType.CAMPAIGN_OPENED || it.eventType == CustomerEventType.NOTIFICATION_CLICKED }.coerceAtLeast((oCount * 2))
            val clicked = promoEvents.count { it.eventType == CustomerEventType.CAMPAIGN_CLICKED || it.eventType == CustomerEventType.COUPON_APPLIED }.coerceAtLeast(oCount)

            CampaignStats(
                campaignName = "Promo $code",
                sentCount = sent,
                openedCount = opened,
                clickedCount = clicked,
                orderCount = oCount,
                totalRevenue = oRev
            )
        }.sortedByDescending { it.totalRevenue }

        val totalCampaignRev = targetOrders.filter { !it.couponCode.isNullOrEmpty() && it.status == OrderStatus.DELIVERED }
            .sumOf { it.totalAmount }

        val sourceCounts = targetCustomers
            .map { it.acquisitionSource?.ifEmpty { "Organic / Direct" } ?: "Organic / Direct" }
            .groupBy { it }
            .mapValues { it.value.size }

        val totalAcquired = targetCustomers.size.coerceAtLeast(1)
        val sourceSplit = sourceCounts.mapValues { ((it.value.toDouble() / totalAcquired) * 100).toInt() }

        val sentEvents = targetEvents.count { it.eventType == CustomerEventType.CAMPAIGN_SENT || it.eventType == CustomerEventType.NOTIFICATION_RECEIVED }.coerceAtLeast(targetCustomers.size)
        val openedEvents = targetEvents.count { it.eventType == CustomerEventType.CAMPAIGN_OPENED || it.eventType == CustomerEventType.NOTIFICATION_CLICKED }
        val clickedEvents = targetEvents.count { it.eventType == CustomerEventType.CAMPAIGN_CLICKED || it.eventType == CustomerEventType.BANNER_CLICKED || it.eventType == CustomerEventType.COUPON_APPLIED }
        val promoOrdersCount = targetOrders.count { !it.couponCode.isNullOrEmpty() }

        return MarketingAnalytics(
            topCampaigns = campaignsList.ifEmpty {
                listOf(
                    CampaignStats(
                        campaignName = "Organic & General Direct Sales",
                        sentCount = sentEvents,
                        openedCount = openedEvents,
                        clickedCount = clickedEvents,
                        orderCount = promoOrdersCount,
                        totalRevenue = totalCampaignRev
                    )
                )
            },
            totalCampaignRevenue = totalCampaignRev,
            sourceSplit = sourceSplit.ifEmpty { mapOf("Organic / Direct" to 100) },
            overallFunnel = MarketingFunnel(
                sent = sentEvents,
                opened = openedEvents,
                clicked = clickedEvents,
                ordered = promoOrdersCount
            )
        )
    }

    fun generateUserSegmentationReport(): UserSegmentationReport {
        val currentTime = System.currentTimeMillis()
        val segmentsMap = mutableMapOf<UserSegment, MutableList<CustomerProfile>>()
        UserSegment.entries.forEach { segmentsMap[it] = mutableListOf() }

        allCustomers.forEach { customer ->
            val segment = when {
                customer.isBlacklisted || customer.cancelledOrders > 5 || customer.returnedOrders > 3 -> UserSegment.RISKY
                customer.totalOrders > 15 || (customer.lifetimeSpending > 5000 && customer.isSubscribed) -> UserSegment.LOYAL
                customer.avgOrderValue > 800 || customer.lifetimeSpending > 10000 -> UserSegment.VIP
                customer.cartAbandonedItemsCount > 2 -> UserSegment.ABANDONERS
                customer.joinedDate > currentTime - (7 * 24 * 60 * 60 * 1000L) -> UserSegment.NEW
                customer.lastOrderDate != null && customer.lastOrderDate!! < currentTime - (60 * 24 * 60 * 60 * 1000L) -> UserSegment.DORMANT
                customer.lastLogin < currentTime - (30 * 24 * 60 * 60 * 1000L) -> UserSegment.INACTIVE
                customer.totalOrders in 1..5 && customer.avgCustomerRating >= 4.5 -> UserSegment.POTENTIAL
                else -> UserSegment.UNSEGMENTED
            }
            segmentsMap[segment]?.add(customer)
        }

        val counts = segmentsMap.mapValues { it.value.size }
        val topSegment = counts.maxByOrNull { it.value }?.key ?: UserSegment.UNSEGMENTED

        val report = UserSegmentationReport(
            segments = segmentsMap,
            counts = counts,
            totalCustomers = allCustomers.size,
            topSegment = topSegment
        )
        userSegmentationReport = report
        return report
    }

    fun getMarketingInsights(): List<SegmentMarketingInsight> {
        return listOf(
            SegmentMarketingInsight(UserSegment.LOYAL, "Offer early access to new launches", "LOYALTY10", 2),
            SegmentMarketingInsight(UserSegment.INACTIVE, "Win back with heavy discount", "COMEBACK50", 1),
            SegmentMarketingInsight(UserSegment.ABANDONERS, "Reminder with free delivery", "FINISHCART", 1),
            SegmentMarketingInsight(UserSegment.NEW, "Welcome kit and trial pass", "WELCOMEFLASH", 3),
            SegmentMarketingInsight(UserSegment.VIP, "Personalized concierge service", null, 2),
            SegmentMarketingInsight(UserSegment.POTENTIAL, "Upsell with Flash-O Pass", "PASSTRIAL", 4)
        )
    }

    fun sendSegmentedCampaign(segment: UserSegment, title: String, message: String) {
        val targetUsers = userSegmentationReport.segments[segment]?.map { it.id } ?: emptyList()
        if (targetUsers.isNotEmpty()) {
            sendTargetedCampaign(targetUsers, title, message)
        } else {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("No users found in segment ${segment.displayName}")) }
        }
    }

    fun getTargetingListForSegment(segment: UserSegment): List<Pair<String, String>> {
        return userSegmentationReport.segments[segment]?.map { 
            it.name to (it.mobile.ifEmpty { it.email }) 
        } ?: emptyList()
    }

    fun exportSegmentToCsv(segment: UserSegment): String {
        val customers = userSegmentationReport.segments[segment] ?: return ""
        val header = "Name,Mobile,Email,TotalOrders,LifetimeSpending,LastOrderDate\n"
        val body = customers.joinToString("\n") { 
            "${it.name},${it.mobile},${it.email},${it.totalOrders},${it.lifetimeSpending},${it.lastOrderDate ?: 0}"
        }
        return header + body
    }

    fun getFraudAnalytics(
        targetOrders: List<Order> = orders,
        targetCustomers: List<CustomerProfile> = allCustomers
    ): FraudAnalytics {
        val multipleAccountsIncidents = mutableListOf<FraudIncident>()
        val couponAbuseIncidents = mutableListOf<FraudIncident>()
        val highReturnersIncidents = mutableListOf<FraudIncident>()
        val codAbuseIncidents = mutableListOf<FraudIncident>()
        val referralFraudIncidents = mutableListOf<FraudIncident>()
        val gpsAnomalyIncidents = mutableListOf<FraudIncident>()
        val paymentSuspicionIncidents = mutableListOf<FraudIncident>()

        targetCustomers.forEach { customer ->
            if (customer.isDuplicateDeviceDetected || customer.registrationDuplicateFlags.isNotEmpty()) {
                multipleAccountsIncidents.add(
                    FraudIncident(
                        customerId = customer.id,
                        customerName = customer.name.ifEmpty { "Customer #${customer.id.take(6)}" },
                        reason = "Duplicate Device / Account Detected",
                        riskLevel = RiskLevel.CRITICAL,
                        evidence = customer.registrationDuplicateFlags.joinToString(", ").ifEmpty { "Multiple accounts linked to device ID" }
                    )
                )
            }

            if (customer.referralEarningsFrozen || customer.referralVerificationStatus == "REJECTED_FRAUD") {
                referralFraudIncidents.add(
                    FraudIncident(
                        customerId = customer.id,
                        customerName = customer.name.ifEmpty { "Customer #${customer.id.take(6)}" },
                        reason = "Frozen / Suspicious Referral Claims",
                        riskLevel = RiskLevel.HIGH,
                        evidence = customer.referralEarningsFreezeReason.ifEmpty { "Referral verification failed for suspicious self-referrals" }
                    )
                )
            }

            if (customer.returnedOrders >= 2 || (customer.totalOrders > 3 && (customer.returnedOrders.toDouble() / customer.totalOrders) > 0.3)) {
                highReturnersIncidents.add(
                    FraudIncident(
                        customerId = customer.id,
                        customerName = customer.name.ifEmpty { "Customer #${customer.id.take(6)}" },
                        reason = "High Return / Refund Rate",
                        riskLevel = RiskLevel.MEDIUM,
                        evidence = "${customer.returnedOrders} returned orders out of ${customer.totalOrders} total purchases"
                    )
                )
            }

            if (customer.cancelledOrders >= 3 || (customer.totalOrders > 3 && customer.lifetimeSpending < 100)) {
                couponAbuseIncidents.add(
                    FraudIncident(
                        customerId = customer.id,
                        customerName = customer.name.ifEmpty { "Customer #${customer.id.take(6)}" },
                        reason = "Frequent Cancellations / Coupon Abuse",
                        riskLevel = RiskLevel.HIGH,
                        evidence = "${customer.cancelledOrders} cancelled orders with low order fulfillment spending"
                    )
                )
            }

            if (customer.isBlacklisted) {
                paymentSuspicionIncidents.add(
                    FraudIncident(
                        customerId = customer.id,
                        customerName = customer.name.ifEmpty { "Customer #${customer.id.take(6)}" },
                        reason = "Blacklisted Account",
                        riskLevel = RiskLevel.CRITICAL,
                        evidence = "Account blacklisted due to security policy violations"
                    )
                )
            }
        }

        val codCancelledUserIds = targetOrders
            .filter { it.paymentMethod == "COD" && it.status == OrderStatus.CANCELLED }
            .map { it.customerName }
            .groupBy { it }
            .filter { it.value.size >= 2 }

        codCancelledUserIds.forEach { (cName, list) ->
            codAbuseIncidents.add(
                FraudIncident(
                    customerId = cName,
                    customerName = cName,
                    reason = "Repeated COD Order Cancellations",
                    riskLevel = RiskLevel.HIGH,
                    evidence = "${list.size} COD orders cancelled after dispatch/placed"
                )
            )
        }

        val allIncidents = (multipleAccountsIncidents + couponAbuseIncidents + highReturnersIncidents +
                codAbuseIncidents + referralFraudIncidents + gpsAnomalyIncidents + paymentSuspicionIncidents).distinctBy { it.customerId + it.reason }

        val totalFlagged = allIncidents.size
        val totalRisk = targetCustomers.filter { c -> allIncidents.any { it.customerId == c.id } }
            .sumOf { it.lifetimeSpending }.coerceAtLeast(totalFlagged * 200.0)

        return FraudAnalytics(
            totalFlaggedUsers = totalFlagged,
            totalRiskAmount = totalRisk,
            multipleAccounts = multipleAccountsIncidents,
            referralFraud = referralFraudIncidents,
            couponAbuse = couponAbuseIncidents,
            highReturners = highReturnersIncidents,
            codAbuse = codAbuseIncidents,
            gpsAnomaly = gpsAnomalyIncidents,
            paymentSuspicion = paymentSuspicionIncidents
        )
    }

    fun getBusinessKPIs(
        targetOrders: List<Order> = orders,
        targetCustomers: List<CustomerProfile> = allCustomers,
        targetProducts: List<Product> = products
    ): BusinessKPIs {
        val delivered = targetOrders.filter { it.status == OrderStatus.DELIVERED }
        val gmv = delivered.sumOf { it.totalAmount }
        val totalDeliveredCount = delivered.size.coerceAtLeast(1)
        val aov = if (delivered.isNotEmpty()) gmv / totalDeliveredCount else 0.0

        val totalCustomersCount = targetCustomers.size.coerceAtLeast(1)
        val repeatCustomers = targetCustomers.count { it.totalOrders > 1 }
        val repeatRate = (repeatCustomers.toDouble() / totalCustomersCount) * 100

        val cancelCount = targetOrders.count { it.status == OrderStatus.CANCELLED }
        val returnCount = targetOrders.count { it.status == OrderStatus.RETURNED }
        val totalAllOrders = targetOrders.size.coerceAtLeast(1)

        val grossProfit = delivered.sumOf { order ->
            if (order.estimatedProfit > 0) order.estimatedProfit else order.totalAmount * 0.18
        }

        val totalDiscounts = delivered.sumOf { it.discountAmount }
        val totalFees = delivered.sumOf { it.deliveryCharge + it.handlingFee }
        val contributionMargin = grossProfit + totalFees - totalDiscounts

        val netProfit = contributionMargin - (delivered.size * 20.0)

        val totalPromoDiscounts = targetOrders.sumOf { it.discountAmount }
        val acquiredCustomersWithOrders = targetCustomers.count { it.totalOrders >= 1 }.coerceAtLeast(1)
        val cac = totalPromoDiscounts / acquiredCustomersWithOrders

        val ltv = if (targetCustomers.isNotEmpty()) gmv / totalCustomersCount else 0.0

        val campaignRev = targetOrders.filter { !it.couponCode.isNullOrEmpty() && it.status == OrderStatus.DELIVERED }.sumOf { it.totalAmount }
        val roas = if (totalPromoDiscounts > 0) campaignRev / totalPromoDiscounts else 0.0

        return BusinessKPIs(
            cac = cac,
            ltv = ltv,
            roas = roas,
            avgBasketSize = aov,
            repeatPurchaseRate = repeatRate,
            retentionRate = repeatRate,
            cancellationRate = (cancelCount.toDouble() / totalAllOrders) * 100,
            returnRate = (returnCount.toDouble() / totalAllOrders) * 100,
            grossProfit = grossProfit,
            netProfit = netProfit,
            contributionMargin = contributionMargin
        )
    }

    fun sendTargetedCampaign(userIds: List<String>, title: String, message: String) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSuccess("Campaign Sent to ${userIds.size} Users"))
        }
    }

    fun deleteUser(uid: String) {
        viewModelScope.launch {
            try {
                val user = allUsers.find { it.uid == uid } ?: return@launch
                if (com.fryumm.store.util.MasterAdminSecurity.isMasterAdminEmail(user.email) || user.userRole == UserRole.ADMIN) {
                    _uiEvent.emit(UiEvent.ShowError("SECURITY PROTECTION: Master Admin accounts cannot be deleted or removed under any circumstances."))
                    return@launch
                }
                val collection = when(user.userRole) {
                    UserRole.ADMIN -> "users"
                    UserRole.RIDER -> "riders"
                    UserRole.OTHER -> "staff"
                    UserRole.CUSTOMER -> "customers"
                    else -> "sellers"
                }
                firestore.collection(collection).document(uid).delete().await()
                _uiEvent.emit(UiEvent.ShowSuccess("User Deleted"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Delete Failed: ${e.message}"))
            }
        }
    }

    fun activateCustomerPass(customerId: String, tierId: String) {
        viewModelScope.launch {
            try {
                val expiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
                firestore.collection("customers").document(customerId).update(
                    mapOf(
                        "isSubscribed" to true,
                        "subscriptionTier" to tierId,
                        "subscriptionExpiry" to expiry
                    )
                ).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Flash-O Pass Activated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Activation Failed"))
            }
        }
    }

    fun verifyCustomerReferralEarnings(customerId: String, approve: Boolean, staffId: String, reason: String = "") {
        viewModelScope.launch {
            try {
                if (approve) {
                    firestore.collection("customers").document(customerId).update(
                        mapOf(
                            "referralEarningsFrozen" to false,
                            "referralVerificationStatus" to "VERIFIED",
                            "verifiedByStaffId" to staffId,
                            "verificationTimestamp" to System.currentTimeMillis()
                        )
                    ).await()

                    // Unfreeze referral records
                    val refs = firestore.collection("referrals").whereEqualTo("refereeId", customerId).get().await()
                    for (doc in refs.documents) {
                        doc.reference.update(mapOf(
                            "isFrozenPendingVerification" to false,
                            "isVerifiedByStaff" to true,
                            "verifiedByStaffId" to staffId
                        )).await()
                    }

                    _uiEvent.emit(UiEvent.ShowSuccess("Referral Earnings UNLOCKED and Approved by Staff!"))
                } else {
                    firestore.collection("customers").document(customerId).update(
                        mapOf(
                            "referralEarningsFrozen" to true,
                            "referralVerificationStatus" to "REJECTED_FRAUD",
                            "verifiedByStaffId" to staffId,
                            "verificationTimestamp" to System.currentTimeMillis(),
                            "referralEarningsFreezeReason" to reason.ifEmpty { "Multi-account fraud confirmed by Staff/Admin." },
                            "isBlacklisted" to true
                        )
                    ).await()

                    _uiEvent.emit(UiEvent.ShowError("Referral Earnings Permanently REVOKED. Account flagged as FRAUD."))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Verification Action Failed: ${e.message}"))
            }
        }
    }

    fun getTargetedProductSuggestions(customerId: String): List<Product> {
        return products.take(3)
    }

    fun repairEcosystem() {
        viewModelScope.launch {
            try {
                val defaultCatSeq = listOf("Grocery", "Fruits", "Vegetables", "Dairy", "Bakery", "Beverages", "Snacks", "Meat", "Personal Care", "Household")
                val defaultHomeSeq = listOf("Flash Sale", "Priority Brands", "Grocery", "Fruits", "Dairy", "Promoted Sellers")
                val newConfig = dynamicAppConfig.copy(
                    categorySequence = defaultCatSeq,
                    homeSectionOrder = defaultHomeSeq,
                    lastUpdated = System.currentTimeMillis()
                )
                firestore.collection("settings").document("dynamic_app_config").set(newConfig).await()
                dynamicAppConfig = newConfig
                _uiEvent.emit(UiEvent.ShowSuccess("Home Ecosystem Repaired: Default categories and sections restored."))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Repair Failed: ${e.message}"))
            }
        }
    }

    fun addMarketTrendProducts() {
        viewModelScope.launch {
            try {
                val trending = listOf(
                    Product(name = "Amul Taaza Milk", category = "Dairy", price = 28.0, unit = "500ml", stockQuantity = 100, isVerified = true, isAvailable = true, merchantId = currentUserProfile?.uid ?: "admin"),
                    Product(name = "Harvest Gold Bread", category = "Bakery", price = 45.0, unit = "400g", stockQuantity = 50, isVerified = true, isAvailable = true, merchantId = currentUserProfile?.uid ?: "admin"),
                    Product(name = "Farm Fresh Eggs", category = "Dairy", price = 84.0, unit = "12 pcs", stockQuantity = 30, isVerified = true, isAvailable = true, merchantId = currentUserProfile?.uid ?: "admin"),
                    Product(name = "Basmati Rice", category = "Grocery", price = 120.0, unit = "1kg", stockQuantity = 200, isVerified = true, isAvailable = true, merchantId = currentUserProfile?.uid ?: "admin"),
                    Product(name = "Maggi Noodles", category = "Snacks", price = 14.0, unit = "70g", stockQuantity = 500, isVerified = true, isAvailable = true, merchantId = currentUserProfile?.uid ?: "admin"),
                    Product(name = "Fortune Soya Oil", category = "Grocery", price = 155.0, unit = "1L", stockQuantity = 150, isVerified = true, isAvailable = true, merchantId = currentUserProfile?.uid ?: "admin"),
                    Product(name = "Coca-Cola", category = "Beverages", price = 40.0, unit = "750ml", stockQuantity = 100, isVerified = true, isAvailable = true, merchantId = currentUserProfile?.uid ?: "admin"),
                    Product(name = "Tata Salt", category = "Grocery", price = 25.0, unit = "1kg", stockQuantity = 300, isVerified = true, isAvailable = true, merchantId = currentUserProfile?.uid ?: "admin"),
                    Product(name = "Red Label Tea", category = "Beverages", price = 145.0, unit = "250g", stockQuantity = 80, isVerified = true, isAvailable = true, merchantId = currentUserProfile?.uid ?: "admin"),
                    Product(name = "Surf Excel Bar", category = "Household", price = 10.0, unit = "1 unit", stockQuantity = 400, isVerified = true, isAvailable = true, merchantId = currentUserProfile?.uid ?: "admin")
                )
                
                val batch = firestore.batch()
                trending.forEach { product ->
                    val docRef = firestore.collection("products").document()
                    batch.set(docRef, product.copy(id = docRef.id))
                }
                batch.commit().await()
                _uiEvent.emit(UiEvent.ShowSuccess("10 Trending Products Added to Store"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Population Failed: ${e.message}"))
            }
        }
    }

    fun resetAndRepopulateStore() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("products").get().await()
                val batchDelete = firestore.batch()
                snapshot.documents.forEach { batchDelete.delete(it.reference) }
                batchDelete.commit().await()
                
                addMarketTrendProducts()
                _uiEvent.emit(UiEvent.ShowSuccess("Store Reset Successful"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Reset Failed: ${e.message}"))
            }
        }
    }

    fun clearAllOrders() {
        viewModelScope.launch {
            try {
                val batch = firestore.batch()
                orders.forEach { order ->
                    batch.delete(firestore.collection("orders").document(order.id))
                }
                batch.commit().await()
                orders = emptyList()
                _uiEvent.emit(UiEvent.ShowSuccess("All Orders Cleared"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to clear orders: ${e.message}"))
            }
        }
    }

    fun approveMerchant(uid: String) {
        viewModelScope.launch {
            try {
                firestore.collection("sellers").document(uid).update(
                    mapOf(
                        "active" to true,
                        "isActive" to true,
                        "blockReason" to ""
                    )
                ).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Merchant Approved & Activated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Approval Failed: ${e.message}"))
            }
        }
    }

    fun requestRoleChange(uid: String, requestedRole: String, reason: String) {
        viewModelScope.launch {
            try {
                val user = allUsers.find { it.uid == uid } ?: return@launch
                val collection = when(user.userRole) {
                    UserRole.ADMIN -> "users"
                    UserRole.RIDER -> "riders"
                    UserRole.OTHER -> "staff"
                    UserRole.CUSTOMER -> "customers"
                    else -> "sellers"
                }
                firestore.collection(collection).document(uid).update(
                    mapOf(
                        "requestedRole" to requestedRole,
                        "roleChangeReason" to reason,
                        "roleChangeStatus" to "PENDING"
                    )
                ).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Role Change Requested Successfully"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Request Failed: ${e.message}"))
            }
        }
    }

    fun approveRoleChange(uid: String, newRole: String) {
        viewModelScope.launch {
            try {
                val user = allUsers.find { it.uid == uid } ?: return@launch
                val oldCollection = when(user.userRole) {
                    UserRole.ADMIN -> "users"
                    UserRole.RIDER -> "riders"
                    UserRole.OTHER -> "staff"
                    UserRole.CUSTOMER -> "customers"
                    else -> "sellers"
                }
                
                val parsedRole = try { UserRole.valueOf(newRole.uppercase()) } catch(e: Exception) { UserRole.STORE_OWNER }
                val newCollection = when(parsedRole) {
                    UserRole.ADMIN -> "users"
                    UserRole.RIDER -> "riders"
                    UserRole.OTHER -> "staff"
                    UserRole.CUSTOMER -> "customers"
                    else -> "sellers"
                }

                val updates = mapOf(
                    "role" to newRole,
                    "requestedRole" to "",
                    "roleChangeReason" to "",
                    "roleChangeStatus" to "APPROVED",
                    "active" to true,
                    "isActive" to true
                )

                if (oldCollection == newCollection) {
                    firestore.collection(oldCollection).document(uid).update(updates).await()
                } else {
                    // Move to new collection
                    val userDoc = firestore.collection(oldCollection).document(uid).get().await()
                    if (userDoc.exists()) {
                        val data = userDoc.data?.toMutableMap() ?: mutableMapOf()
                        data.putAll(updates)
                        firestore.collection(newCollection).document(uid).set(data).await()
                        firestore.collection(oldCollection).document(uid).delete().await()
                    }
                }
                _uiEvent.emit(UiEvent.ShowSuccess("Role Change Approved! User is now $newRole"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Role Approval Failed: ${e.message}"))
            }
        }
    }

    fun rejectRoleChange(uid: String, reason: String = "") {
        viewModelScope.launch {
            try {
                val user = allUsers.find { it.uid == uid } ?: return@launch
                val collection = when(user.userRole) {
                    UserRole.ADMIN -> "users"
                    UserRole.RIDER -> "riders"
                    UserRole.OTHER -> "staff"
                    UserRole.CUSTOMER -> "customers"
                    else -> "sellers"
                }
                firestore.collection(collection).document(uid).update(
                    mapOf(
                        "roleChangeStatus" to "REJECTED",
                        "roleChangeReason" to reason
                    )
                ).await()
                _uiEvent.emit(UiEvent.ShowError("Role Change Request Rejected"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Action Failed: ${e.message}"))
            }
        }
    }

    fun approveRider(uid: String, darkStoreId: String? = null) {
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "active" to true,
                    "isActive" to true,
                    "blockReason" to ""
                )
                if (!darkStoreId.isNullOrEmpty()) {
                    updates["darkStoreId"] = darkStoreId
                }
                firestore.collection("riders").document(uid).update(updates).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Rider Approved & Onboarded"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Rider Approval Failed: ${e.message}"))
            }
        }
    }

    fun getCEODashboardStats(): CEODashboardStats {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Start of Today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        // Yesterday
        val calendarYesterday = Calendar.getInstance()
        calendarYesterday.timeInMillis = todayStart
        calendarYesterday.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStart = calendarYesterday.timeInMillis
        val yesterdayEnd = todayStart - 1

        // Week (Start of current week)
        val calendarWeek = Calendar.getInstance()
        calendarWeek.set(Calendar.DAY_OF_WEEK, calendarWeek.firstDayOfWeek)
        calendarWeek.set(Calendar.HOUR_OF_DAY, 0)
        calendarWeek.set(Calendar.MINUTE, 0)
        calendarWeek.set(Calendar.SECOND, 0)
        calendarWeek.set(Calendar.MILLISECOND, 0)
        val weekStart = calendarWeek.timeInMillis

        // Month
        val calendarMonth = Calendar.getInstance()
        calendarMonth.set(Calendar.DAY_OF_MONTH, 1)
        calendarMonth.set(Calendar.HOUR_OF_DAY, 0)
        calendarMonth.set(Calendar.MINUTE, 0)
        calendarMonth.set(Calendar.SECOND, 0)
        calendarMonth.set(Calendar.MILLISECOND, 0)
        val monthStart = calendarMonth.timeInMillis

        val deliveredOrdersList = orders.filter { it.status == OrderStatus.DELIVERED }
        
        val totalRev = deliveredOrdersList.sumOf { it.totalAmount }
        val totalProfit = deliveredOrdersList.sumOf { it.estimatedProfit }
        
        val todayOrders = deliveredOrdersList.filter { it.timestamp >= todayStart }
        val todayRev = todayOrders.sumOf { it.totalAmount }
        val todayProfitValue = todayOrders.sumOf { it.estimatedProfit }

        val yesterdayRev = deliveredOrdersList.filter { it.timestamp in yesterdayStart..yesterdayEnd }.sumOf { it.totalAmount }
        val weeklyRev = deliveredOrdersList.filter { it.timestamp >= weekStart }.sumOf { it.totalAmount }
        val monthlyRev = deliveredOrdersList.filter { it.timestamp >= monthStart }.sumOf { it.totalAmount }

        return CEODashboardStats(
            totalStores = darkStores.size,
            totalUsers = allUsers.size,
            totalRevenueFY = totalRev,
            totalProfitFY = totalProfit,
            todayRevenue = todayRev,
            todayProfit = todayProfitValue,
            yesterdayRevenue = yesterdayRev,
            weeklyRevenue = weeklyRev,
            monthlyRevenue = monthlyRev,
            totalOrders = orders.size,
            pendingOrders = orders.count { it.status == OrderStatus.PLACED },
            packedOrders = orders.count { it.status == OrderStatus.READY },
            outForDeliveryOrders = orders.count { it.status == OrderStatus.DISPATCHED },
            deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED },
            cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED }
        )
    }

    fun importProductsFromCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val content = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val lines = content.lines().filter { it.isNotBlank() }
                if (lines.size > 1) {
                    val newProducts = mutableListOf<Product>()
                    for (line in lines.drop(1)) {
                        val tokens = line.split(",")
                        if (tokens.size >= 3) {
                            newProducts.add(
                                Product(
                                    id = UUID.randomUUID().toString(),
                                    name = tokens[0].trim(),
                                    category = tokens[1].trim(),
                                    price = tokens[2].trim().toDoubleOrNull() ?: 0.0,
                                    merchantId = currentUserProfile?.uid ?: ""
                                )
                            )
                        }
                    }
                    newProducts.forEach { productRepository.addProduct(it) }
                    _uiEvent.emit(UiEvent.ShowSuccess("Imported ${newProducts.size} products from CSV"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("CSV Import Failed: ${e.message}"))
            }
        }
    }

    fun importProductsFromJson(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: "[]"
                val jsonArray = JSONArray(jsonString)
                
                val newProducts = mutableListOf<Product>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    newProducts.add(Product(
                        id = UUID.randomUUID().toString(),
                        name = obj.optString("name", "Unknown Product"),
                        category = obj.optString("category", "General"),
                        price = obj.optDouble("price", 0.0),
                        unit = obj.optString("unit", "1 unit"),
                        stockQuantity = obj.optInt("stockQuantity", 0),
                        merchantId = currentUserProfile?.uid ?: "admin",
                        isVerified = true,
                        isAvailable = true
                    ))
                }
                
                if (newProducts.isNotEmpty()) {
                    val batch = firestore.batch()
                    newProducts.forEach { p ->
                        val docRef = firestore.collection("products").document(p.id)
                        batch.set(docRef, p)
                    }
                    batch.commit().await()
                    _uiEvent.emit(UiEvent.ShowSuccess("Successfully imported ${newProducts.size} products from JSON"))
                } else {
                    _uiEvent.emit(UiEvent.ShowError("JSON was empty or invalid"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("JSON Import Failed: ${e.message}"))
            }
        }
    }

    fun clearAllServiceRequests() {
        viewModelScope.launch {
            try {
                val batch = firestore.batch()
                serviceRequests.forEach { req ->
                    batch.delete(firestore.collection("service_requests").document(req.id))
                }
                batch.commit().await()
                serviceRequests = emptyList()
                _uiEvent.emit(UiEvent.ShowSuccess("All Service Requests Cleared"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to clear service requests"))
            }
        }
    }

    fun updateUserStatusForReferral(profile: UserProfile) {
        viewModelScope.launch {
            try {
                firestore.collection("sellers").document(profile.uid).set(profile, com.google.firebase.firestore.SetOptions.merge()).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Referral target updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Update Failed"))
            }
        }
    }

    fun updateProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            try {
                val url = storageRepository.uploadImage(uri, "profiles/${UUID.randomUUID()}")
                if (url != null) {
                    val profile = currentUserProfile?.copy(photoUrl = url) ?: return@launch
                    val collection = if (profile.userRole == UserRole.ADMIN) "users" else "sellers"
                    firestore.collection(collection).document(profile.uid).update("photoUrl", url).await()
                    currentUserProfile = profile
                    _uiEvent.emit(UiEvent.ShowSuccess("Profile photo updated"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Photo upload failed"))
            }
        }
    }

    fun updateUserProfileDetails(profile: UserProfile) {
        viewModelScope.launch {
            try {
                val collection = when (profile.userRole) {
                    UserRole.ADMIN -> "users"
                    UserRole.RIDER -> "riders"
                    UserRole.OTHER -> "staff"
                    UserRole.CUSTOMER -> "customers"
                    else -> "sellers"
                }
                val updates = mutableMapOf<String, Any?>(
                    "name" to profile.name,
                    "displayName" to profile.name,
                    "fullName" to profile.name,
                    "phoneNumber" to profile.phoneNumber,
                    "mobile" to profile.phoneNumber,
                    "phone" to profile.phoneNumber,
                    "email" to profile.email,
                    "userEmail" to profile.email,
                    "role" to profile.role,
                    "staffDepartment" to profile.staffDepartment,
                    "isActive" to profile.active,
                    "active" to profile.active,
                    "blockReason" to profile.blockReason,
                    
                    // Merchant & Business Details
                    "tradeName" to profile.tradeName,
                    "legalName" to profile.legalName,
                    "businessType" to profile.businessType,
                    "contactPerson" to profile.contactPerson,
                    "gstin" to profile.gstin,
                    "fssaiNumber" to profile.fssaiNumber,
                    "fssaiExpiryDate" to profile.fssaiExpiryDate,
                    "panNumber" to profile.panNumber,
                    "averagePreparationTime" to profile.averagePreparationTime,
                    
                    // Bank & UPI Details
                    "bankName" to profile.bankName,
                    "bankAccountName" to profile.bankAccountName,
                    "bankAccountNumber" to profile.bankAccountNumber,
                    "bankIfscCode" to profile.bankIfscCode,
                    "upiId" to profile.upiId,
                    
                    // Address Details
                    "storeAddress" to profile.storeAddress,
                    "city" to profile.city,
                    "state" to profile.state,
                    "pincode" to profile.pincode,
                    "platformFeeDiscount" to profile.platformFeeDiscount
                )
                firestore.collection(collection).document(profile.uid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                if (currentUserProfile?.uid == profile.uid) {
                    currentUserProfile = profile
                }
                observeAllUsers()
                _uiEvent.emit(UiEvent.ShowSuccess("Profile Details Saved Successfully"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to save profile details"))
            }
        }
    }

    fun updateMerchantSettings(schedules: Map<String, DaySchedule>, vacationStart: Long?, vacationEnd: Long?) {
        val profile = currentUserProfile ?: return
        viewModelScope.launch {
            try {
                val updated = profile.copy(
                    dailySchedules = schedules,
                    vacationStart = vacationStart,
                    vacationEnd = vacationEnd
                )
                firestore.collection("sellers").document(profile.uid).set(updated, com.google.firebase.firestore.SetOptions.merge()).await()
                currentUserProfile = updated
                _uiEvent.emit(UiEvent.ShowSuccess("Availability settings saved"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to save settings"))
            }
        }
    }

    fun referCustomer(name: String, phone: String) {
        viewModelScope.launch {
            try {
                val id = UUID.randomUUID().toString()
                val referral = mapOf(
                    "id" to id,
                    "name" to name,
                    "phone" to phone,
                    "referredBy" to (currentUserProfile?.uid ?: ""),
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("referrals").document(id).set(referral).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Customer Referred Successfully"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Referral Failed: ${e.message}"))
            }
        }
    }

    fun getMerchantSettlements(): List<MerchantSettlement> {
        val deliveredOrders = orders.filter { it.status == OrderStatus.DELIVERED }
        if (deliveredOrders.isEmpty()) return emptyList()

        val groupedByMerchant = deliveredOrders.groupBy { it.merchantId }
        return groupedByMerchant.map { (merchantId, merchantOrders) ->
            val merchantName = allUsers.find { it.uid == merchantId }?.name ?: "Store #$merchantId"
            val settlementItems = mutableListOf<SettlementItem>()

            merchantOrders.flatMap { it.items }.groupBy { it.product.id }.forEach { (productId, items) ->
                val first = items.first()
                val totalQty = items.sumOf { it.quantity }
                val rate = first.product.merchantRate
                settlementItems.add(
                    SettlementItem(
                        productId = productId,
                        productName = first.product.name,
                        quantity = totalQty,
                        merchantRate = rate,
                        totalAmount = totalQty * rate,
                        orders = items.map { it.product.id }
                    )
                )
            }

            MerchantSettlement(
                merchantId = merchantId,
                merchantName = merchantName,
                items = settlementItems,
                totalPayout = settlementItems.sumOf { it.totalAmount },
                totalOrders = merchantOrders.size
            )
        }
    }

    // --- FLASH-O CARE SUPPORT FUNCTIONS ---
    
    var supportMessages by mutableStateOf<List<SupportMessage>>(emptyList())
    private var chatListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var supportRequestsListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun submitCallRequest(guestName: String? = null, guestPhone: String? = null) {
        val profile = currentUserProfile
        if (profile == null && guestName == null) return
        
        viewModelScope.launch {
            try {
                val id = "CALL_" + UUID.randomUUID().toString().take(8).uppercase()
                val request = ServiceRequest(
                    id = id,
                    userId = profile?.uid ?: "GUEST_${id.takeLast(4)}",
                    userName = profile?.name ?: guestName ?: "Guest Merchant",
                    userPhone = profile?.phoneNumber ?: guestPhone ?: "",
                    userEmail = profile?.email ?: "",
                    userRole = if (profile != null) "MERCHANT" else "GUEST_MERCHANT",
                    status = "PENDING",
                    type = "CALLBACK_REQUEST",
                    timestamp = System.currentTimeMillis()
                )
                firestore.collection("service_requests").document(id).set(request).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Callback requested! Our team will contact you shortly."))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Request failed. Please try again later."))
            }
        }
    }

    private fun observeSupportRequests() {
        val profile = currentUserProfile ?: return
        supportRequestsListener?.remove()
        supportRequestsListener = firestore.collection("service_requests")
            .whereEqualTo("userId", profile.uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    serviceRequests = it.toObjects(ServiceRequest::class.java)
                }
            }
    }

    private var guestChatId: String? = null

    fun startSupportChat(customGuestId: String? = null) {
        val profile = currentUserProfile
        val sessionId = profile?.uid ?: customGuestId ?: guestChatId ?: run {
            val newId = "GUEST_CHAT_${UUID.randomUUID().toString().take(6).uppercase()}"
            guestChatId = newId
            newId
        }
        
        chatListener?.remove()
        chatListener = firestore.collection("support_chats")
            .document(sessionId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    supportMessages = it.toObjects(SupportMessage::class.java)
                }
            }
        
        // Update session info
        val session = SupportSession(
            id = sessionId,
            userId = sessionId,
            userName = profile?.name ?: "Guest Merchant",
            userEmail = profile?.email ?: "",
            userRole = if (profile != null) "MERCHANT" else "GUEST_MERCHANT",
            isActive = true
        )
        firestore.collection("support_chats").document(sessionId).set(session, com.google.firebase.firestore.SetOptions.merge())
    }

    fun sendSupportMessage(text: String, isAdmin: Boolean = false, targetUserId: String? = null) {
        val profile = currentUserProfile
        if (profile == null && !isAdmin && guestChatId == null) return
        if (text.isBlank()) return
        
        val sessionId = if (isAdmin && targetUserId != null) targetUserId else (profile?.uid ?: guestChatId!!)
        
        viewModelScope.launch {
            try {
                val msgId = UUID.randomUUID().toString()
                val message = SupportMessage(
                    id = msgId,
                    senderId = profile?.uid ?: guestChatId!!,
                    senderName = profile?.name ?: "Guest Merchant",
                    message = text,
                    isFromAdmin = isAdmin
                )
                firestore.collection("support_chats")
                    .document(sessionId)
                    .collection("messages")
                    .document(msgId)
                    .set(message)
                
                // Update session's last message
                val updates = mutableMapOf<String, Any>(
                    "lastMessage" to text,
                    "lastTimestamp" to System.currentTimeMillis()
                )
                if (isAdmin) {
                    updates["isUnreadByUser"] = true
                } else {
                    updates["isUnreadByAdmin"] = true
                }
                
                firestore.collection("support_chats").document(sessionId).update(updates)
            } catch (e: Exception) {}
        }
    }

    fun assignSupportToAdmin(sessionId: String, adminId: String, adminName: String) {
        viewModelScope.launch {
            try {
                firestore.collection("support_chats").document(sessionId).update(
                    "assignedAdminId", adminId,
                    "assignedAdminName", adminName
                ).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Chat assigned to $adminName"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Assignment failed"))
            }
        }
    }

    fun stopSupportChat() {
        chatListener?.remove()
    }
}
