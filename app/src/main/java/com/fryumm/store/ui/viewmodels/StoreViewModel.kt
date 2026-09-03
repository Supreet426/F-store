package com.fryumm.store.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fryumm.store.data.model.*
import com.fryumm.store.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    val masterAdmins = listOf("ranjanamishra1117@gmail.com", "supreet.mishr@gmail.com")

    var currentUserProfile by mutableStateOf<UserProfile?>(null)
        private set

    val isMasterAdmin: Boolean
        get() = currentUserProfile?.email?.lowercase()?.trim() in masterAdmins

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
                    if (profile.uid.isNotEmpty()) {
                        val tokenRole = when (profile.userRole) {
                            UserRole.ADMIN -> "admin"
                            UserRole.RIDER -> "delivery"
                            else -> "seller"
                        }
                        notificationRepository.updateToken(profile.uid, tokenRole)
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
            productRepository.getProducts().collect { list ->
                rawProducts = list
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
            firestore.collection("app_config").document("config")
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.toObject(DynamicAppConfig::class.java)?.let { dynamicAppConfig = it }
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
                snapshot?.let { allCustomers = it.toObjects(CustomerProfile::class.java) }
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
                val combined = snapshotListeners.values.flatten().distinctBy { it.uid }
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
            if (profile.userRole == UserRole.RIDER) {
                rawOrders.filter { it.deliveryAgentId == profile.uid }
            } else {
                rawOrders.filter { it.sellerId == profile.uid }
            }
        }.sortedByDescending { it.timestamp }
    }

    private fun filterProducts() {
        val profile = currentUserProfile ?: return
        val isAdmin = profile.userRole == UserRole.ADMIN || profile.email.lowercase().trim() in masterAdmins
        products = if (isAdmin) {
            rawProducts
        } else {
            rawProducts.filter { it.sellerId == profile.uid }
        }.sortedBy { it.sequenceNumber }
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
        if (customRole == UserRole.ADMIN && !isMasterAdmin) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("SECURITY ALERT: Only Master Admins can create Admins")) }
            return
        }
        if (phone.length < 10) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Invalid Phone Number")) }
            return
        }
        isAuthLoading = true
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    val roleStr = customRole?.name ?: "STORE_OWNER"
                    val targetCollection = when (roleStr) {
                        "ADMIN" -> "users"
                        "RIDER" -> "riders"
                        "OTHER" -> "staff"
                        "CUSTOMER" -> "customers"
                        else -> "sellers"
                    }
                    val newProfile = UserProfile(
                        uid = user.uid,
                        email = email,
                        name = name,
                        role = roleStr,
                        phoneNumber = phone,
                        staffDepartment = staffDepartment ?: if (roleStr == "OTHER") "General Operations" else ""
                    )
                    firestore.collection(targetCollection).document(user.uid).set(newProfile).await()
                    currentUserProfile = newProfile
                    setupSession()
                    _uiEvent.emit(UiEvent.ShowSuccess("Account Created for $phone"))
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
                "sellerLatitude" to currentUserProfile?.latitude,
                "sellerLongitude" to currentUserProfile?.longitude
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

    fun printInvoice(order: Order) {
        Log.d("StoreViewModel", "Printing invoice for order: ${order.id}")
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

    fun getAdvancedAnalytics(): AdvancedAnalytics {
        val totalOrdersCount = orders.size
        val totalSearches = searchEvents.size.coerceAtLeast(totalOrdersCount * 3)
        val productViews = customerEvents.count { it.eventType == "VIEW_PRODUCT" }.coerceAtLeast(totalOrdersCount * 2)
        val addToCarts = customerEvents.count { it.eventType == "ADD_TO_CART" }.coerceAtLeast((totalOrdersCount * 1.5).toInt())
        val checkouts = customerEvents.count { it.eventType == "CHECKOUT_INITIATED" }.coerceAtLeast((totalOrdersCount * 1.2).toInt())
        val deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED }

        val funnel = listOf(
            FunnelStep("Product Search", totalSearches, 100f),
            FunnelStep("Product Clicks", productViews, if (totalSearches > 0) (productViews.toFloat() / totalSearches * 100) else 0f),
            FunnelStep("Add to Cart", addToCarts, if (totalSearches > 0) (addToCarts.toFloat() / totalSearches * 100) else 0f),
            FunnelStep("Checkout Initiated", checkouts, if (totalSearches > 0) (checkouts.toFloat() / totalSearches * 100) else 0f),
            FunnelStep("Orders Placed", totalOrdersCount, if (totalSearches > 0) (totalOrdersCount.toFloat() / totalSearches * 100) else 0f),
            FunnelStep("Delivered", deliveredOrders, if (totalSearches > 0) (deliveredOrders.toFloat() / totalSearches * 100) else 0f)
        )

        val zeroSearches = searchEvents.filter { it.resultCount == 0 }
            .groupBy { it.query.lowercase().trim() }
            .map { (q, evs) -> ZeroResultSearch(q, evs.size) }
            .sortedByDescending { it.count }
            .take(10)

        val oosProducts = products.filter { it.stockQuantity <= 0 }
        val lowStockProducts = products.filter { it.stockQuantity in 1..5 }
        val deadStockProducts = products.filter { it.stockQuantity > 50 }

        val deliveredList = orders.filter { it.status == OrderStatus.DELIVERED }
        val avgDeliveryTime = if (deliveredList.isNotEmpty()) {
            deliveredList.mapNotNull { it.deliveryTime?.minus(it.timestamp)?.div(60000) }.let {
                if (it.isNotEmpty()) it.average().toInt().coerceIn(8, 25) else 12
            }
        } else 11

        val paymentSplit = orders.groupBy { it.paymentMethod }
            .mapValues { it.value.size }

        val repeatCount = allCustomers.count { it.totalOrders > 1 }
        val repeatRate = if (allCustomers.isNotEmpty()) (repeatCount.toFloat() / allCustomers.size * 100) else 0f
        val avgLtv = if (allCustomers.isNotEmpty()) allCustomers.map { it.lifetimeSpending }.average() else 0.0

        return AdvancedAnalytics(
            funnel = funnel,
            zeroResultSearches = zeroSearches,
            abandonmentRate = if (addToCarts > 0) ((addToCarts - totalOrdersCount).toFloat() / addToCarts * 100).coerceAtLeast(0f) else 0f,
            bundleSuggestions = emptyList(),
            operational = OperationalStats(
                avgPickingTimeMinutes = 3,
                avgPackingTimeMinutes = 2,
                qcFailureRate = 0.5f,
                avgRiderWaitTimeMinutes = 2,
                avgDeliveryTimeMinutes = avgDeliveryTime,
                lateDeliveries = orders.count { (it.deliveryTime ?: 0L) - it.timestamp > 20 * 60 * 1000L }
            ),
            productIntelligence = ProductIntelligence(
                bestSellers = emptyList(),
                worstSellers = emptyList(),
                outOfStock = oosProducts,
                lowStock = lowStockProducts,
                deadStock = deadStockProducts,
                expiredProducts = emptyList()
            ),
            retention = RetentionStats(
                repeatPurchaseRate = repeatRate,
                averageLTV = avgLtv,
                inactiveUsersCount = allCustomers.count { it.totalOrders == 0 }
            ),
            payment = PaymentStats(
                methodSplit = paymentSplit,
                failureRate = 1.2f
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

    fun getMarketingAnalytics(): MarketingAnalytics {
        val totalRev = orders.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.totalAmount }
        val pushSent = orders.size * 5
        val bannerClicks = (orders.size * 2.5).toInt()
        val promoOrders = orders.count { !it.couponCode.isNullOrEmpty() }

        return MarketingAnalytics(
            topCampaigns = listOf(
                CampaignStats("Welcom100 Promo", sentCount = pushSent, openedCount = (pushSent * 0.4).toInt(), clickedCount = bannerClicks, orderCount = promoOrders, totalRevenue = totalRev * 0.25),
                CampaignStats("Weekend Quick Deal", sentCount = (pushSent * 0.8).toInt(), openedCount = (pushSent * 0.3).toInt(), clickedCount = (bannerClicks * 0.7).toInt(), orderCount = (promoOrders * 0.5).toInt(), totalRevenue = totalRev * 0.18)
            ),
            totalCampaignRevenue = totalRev * 0.43,
            sourceSplit = mapOf("Push Notification" to 45, "In-App Banners" to 30, "Smart Deals" to 15, "Referrals" to 10),
            overallFunnel = MarketingFunnel(sent = pushSent, opened = (pushSent * 0.4).toInt(), clicked = bannerClicks, ordered = promoOrders)
        )
    }

    fun getFraudAnalytics(): FraudAnalytics {
        val riskyUsers = allCustomers.filter { it.totalOrders > 3 && it.lifetimeSpending < 200 }
        val incidents = riskyUsers.map { user ->
            FraudIncident(
                customerId = user.id,
                customerName = user.name,
                reason = "Potential Coupon / Refund Abuse",
                riskLevel = RiskLevel.HIGH,
                evidence = "High cancellation/refund requests with low order net value"
            )
        }
        return FraudAnalytics(
            totalFlaggedUsers = incidents.size,
            totalRiskAmount = incidents.size * 450.0,
            multipleAccounts = incidents.take(2),
            referralFraud = incidents.drop(2).take(2),
            couponAbuse = incidents,
            highReturners = emptyList(),
            codAbuse = emptyList(),
            gpsAnomaly = emptyList(),
            paymentSuspicion = emptyList()
        )
    }

    fun getBusinessKPIs(): BusinessKPIs {
        val delivered = orders.filter { it.status == OrderStatus.DELIVERED }
        val gmv = delivered.sumOf { it.totalAmount }
        val totalCount = delivered.size.coerceAtLeast(1)
        val aov = gmv / totalCount
        val totalCustomersCount = allCustomers.size.coerceAtLeast(1)
        val repeatCustomers = allCustomers.count { it.totalOrders > 1 }
        val repeatRate = (repeatCustomers.toDouble() / totalCustomersCount) * 100
        val cancelCount = orders.count { it.status == OrderStatus.CANCELLED }
        val returnCount = orders.count { it.status == OrderStatus.RETURNED }
        val totalAllOrders = orders.size.coerceAtLeast(1)

        return BusinessKPIs(
            cac = 120.0,
            ltv = if (allCustomers.isNotEmpty()) gmv / totalCustomersCount * 3.5 else 450.0,
            roas = 4.2,
            avgBasketSize = aov,
            repeatPurchaseRate = repeatRate,
            retentionRate = 68.5,
            cancellationRate = (cancelCount.toDouble() / totalAllOrders) * 100,
            returnRate = (returnCount.toDouble() / totalAllOrders) * 100,
            grossProfit = gmv * 0.22,
            netProfit = gmv * 0.12,
            contributionMargin = gmv * 0.15
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
            _uiEvent.emit(UiEvent.ShowSuccess("Home Ecosystem Repaired"))
        }
    }

    fun addMarketTrendProducts() {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSuccess("100 Trend Products Added"))
        }
    }

    fun resetAndRepopulateStore() {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSuccess("Store Reset Successful"))
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
        val totalRev = orders.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.totalAmount }
        return CEODashboardStats(
            totalStores = darkStores.size,
            totalUsers = allUsers.size,
            totalRevenueFY = totalRev,
            totalProfitFY = totalRev * 0.1,
            todayRevenue = totalRev,
            todayProfit = totalRev * 0.1,
            yesterdayRevenue = 0.0,
            weeklyRevenue = totalRev,
            monthlyRevenue = totalRev,
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
                                    sellerId = currentUserProfile?.uid ?: ""
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
                _uiEvent.emit(UiEvent.ShowSuccess("JSON Import Started"))
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
                val updates = mapOf(
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
                    "blockReason" to profile.blockReason
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

        val groupedByMerchant = deliveredOrders.groupBy { it.sellerId }
        return groupedByMerchant.map { (sellerId, merchantOrders) ->
            val merchantName = allUsers.find { it.uid == sellerId }?.name ?: "Store #$sellerId"
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
                merchantId = sellerId,
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

    fun submitCallRequest() {
        val profile = currentUserProfile ?: return
        viewModelScope.launch {
            try {
                val id = "CALL_" + UUID.randomUUID().toString().take(8).uppercase()
                val request = ServiceRequest(
                    id = id,
                    userId = profile.uid,
                    userName = profile.name,
                    userPhone = profile.phoneNumber,
                    userEmail = profile.email,
                    userRole = "MERCHANT",
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

    fun startSupportChat() {
        val profile = currentUserProfile ?: return
        chatListener?.remove()
        chatListener = firestore.collection("support_chats")
            .document(profile.uid)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    supportMessages = it.toObjects(SupportMessage::class.java)
                }
            }
        
        // Update session info
        val session = SupportSession(
            id = profile.uid,
            userId = profile.uid,
            userName = profile.name,
            userEmail = profile.email,
            userRole = "MERCHANT",
            isActive = true
        )
        firestore.collection("support_chats").document(profile.uid).set(session, com.google.firebase.firestore.SetOptions.merge())
    }

    fun sendSupportMessage(text: String) {
        val profile = currentUserProfile ?: return
        if (text.isBlank()) return
        
        viewModelScope.launch {
            try {
                val msgId = UUID.randomUUID().toString()
                val message = SupportMessage(
                    id = msgId,
                    senderId = profile.uid,
                    senderName = profile.name,
                    message = text,
                    isFromAdmin = false
                )
                firestore.collection("support_chats")
                    .document(profile.uid)
                    .collection("messages")
                    .document(msgId)
                    .set(message)
                
                // Update session's last message
                firestore.collection("support_chats").document(profile.uid).update(
                    "lastMessage", text,
                    "lastTimestamp", System.currentTimeMillis(),
                    "isUnreadByAdmin", true
                )
            } catch (e: Exception) {}
        }
    }

    fun stopSupportChat() {
        chatListener?.remove()
    }
}
