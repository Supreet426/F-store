package com.fryumm.store.ui.viewmodels

import android.app.Application
import android.media.RingtoneManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fryumm.store.data.model.*
import com.fryumm.store.data.repository.NotificationRepository
import com.fryumm.store.data.repository.OrderRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    application: Application,
    private val orderRepository: OrderRepository,
    private val notificationRepository: NotificationRepository,
    private val firestore: FirebaseFirestore
) : AndroidViewModel(application) {

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowSuccess(val message: String) : UiEvent()
    }

    var orders by mutableStateOf<List<Order>>(emptyList())
        private set

    var riders by mutableStateOf<List<Rider>>(emptyList())

    private var rawOrders = emptyList<Order>()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        observeOrders()
        startAutoAssignmentMonitor()
    }

    private fun observeOrders() {
        viewModelScope.launch {
            orderRepository.getNewOrders().collect { newList ->
                if (rawOrders.isNotEmpty() && newList.size > rawOrders.size) {
                    val newOrders = newList.filter { newOrder -> rawOrders.none { it.id == newOrder.id } }
                    if (newOrders.any { it.status == OrderStatus.PLACED }) {
                        playOrderSound()
                        _uiEvent.emit(UiEvent.ShowSuccess("NEW ORDER RECEIVED!"))
                    }
                }

                newList.forEach { newOrder ->
                    val oldOrder = rawOrders.find { it.id == newOrder.id }
                    if (oldOrder != null && oldOrder.status != OrderStatus.DELIVERED && newOrder.status == OrderStatus.DELIVERED) {
                        handleReferralOnDelivery(newOrder)
                    }
                }

                rawOrders = newList
                filterOrders(null)
            }
        }
    }

    fun playOrderSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(getApplication(), notification)
            r.play()
        } catch (e: Exception) {
            Log.e("OrderViewModel", "Error playing sound", e)
        }
    }

    private fun handleReferralOnDelivery(order: Order) {
        viewModelScope.launch {
            try {
                val phone = order.phoneNumber.takeLast(10)
                val customerSnap = firestore.collection("customers")
                    .whereEqualTo("mobile", phone)
                    .get().await()

                val customerDoc = customerSnap.documents.firstOrNull() 
                    ?: firestore.collection("customers").whereEqualTo("phoneNumber", phone).get().await().documents.firstOrNull()
                val customerId = customerDoc?.id
                val userEmail = customerDoc?.getString("email") ?: ""
                val deviceId = customerDoc?.getString("deviceId") ?: ""

                val userSnapshot = firestore.collection("users")
                    .whereEqualTo("phoneNumber", phone)
                    .get().await()

                val userDoc = userSnapshot.documents.firstOrNull() ?: return@launch
                val userId = userDoc.id
                val referredBy = userDoc.getString("referredBy") ?: return@launch

                // STRICT SECURITY CHECK 3: Check for duplicate Device ID, Mobile Number, or Email across other customer accounts
                var isFraudDetected = false
                var freezeReason = ""

                if (deviceId.isNotEmpty()) {
                    val duplicateDevice = firestore.collection("customers")
                        .whereEqualTo("deviceId", deviceId)
                        .get().await()
                    if (duplicateDevice.size() > 1) {
                        isFraudDetected = true
                        freezeReason = "Duplicate Device ID ($deviceId) detected across multiple customer profiles."
                    }
                }

                if (!isFraudDetected && userEmail.isNotEmpty()) {
                    val duplicateEmail = firestore.collection("customers")
                        .whereEqualTo("email", userEmail)
                        .get().await()
                    if (duplicateEmail.size() > 1) {
                        isFraudDetected = true
                        freezeReason = "Duplicate Email ID ($userEmail) detected across multiple customer profiles."
                    }
                }

                if (!isFraudDetected) {
                    val duplicateMobile = firestore.collection("customers")
                        .whereEqualTo("mobile", phone)
                        .get().await()
                    if (duplicateMobile.size() > 1) {
                        isFraudDetected = true
                        freezeReason = "Duplicate Mobile Number ($phone) detected across multiple customer profiles."
                    }
                }

                val referralSnapshot = firestore.collection("referrals")
                    .whereEqualTo("referrerId", referredBy)
                    .whereEqualTo("refereeId", userId)
                    .get().await()

                val referralDoc = referralSnapshot.documents.firstOrNull() ?: return@launch
                val referralRecord = referralDoc.toObject(ReferralRecord::class.java)!!

                val previousOrders = firestore.collection("orders")
                    .whereEqualTo("phoneNumber", order.phoneNumber)
                    .whereEqualTo("status", OrderStatus.DELIVERED)
                    .get().await()

                val updates = mutableMapOf<String, Any>()
                var rewardToPay = 0.0

                if (previousOrders.size() == 1 && !referralRecord.isFirstOrderDone) {
                    updates["isFirstOrderDone"] = true
                    rewardToPay = 50.0
                } else if (previousOrders.size() == 2 && !referralRecord.isSecondOrderDone) {
                    updates["isSecondOrderDone"] = true
                    rewardToPay = 50.0
                }

                if (isFraudDetected) {
                    // FREEZE REFERRAL REWARDS pending Staff/Admin verification
                    updates["isFrozenPendingVerification"] = true
                    updates["freezeReason"] = freezeReason
                    referralDoc.reference.update(updates).await()

                    if (customerId != null) {
                        firestore.collection("customers").document(customerId).update(
                            mapOf(
                                "referralEarningsFrozen" to true,
                                "referralVerificationStatus" to "FROZEN_PENDING_VERIFICATION",
                                "referralEarningsFreezeReason" to "MULTI-ACCOUNT SUSPECTED: $freezeReason Earnings locked pending Staff/Admin manual verification."
                            )
                        ).await()
                    }

                    // Log incident in FraudAnalytics
                    val incidentMap = mapOf(
                        "id" to "FRAUD_${System.currentTimeMillis()}",
                        "userId" to userId,
                        "userName" to (order.customerName.ifEmpty { "Customer" }),
                        "userRole" to "CUSTOMER",
                        "incidentType" to "REFERRAL_MULTI_ACCOUNT_FRAUD",
                        "riskLevel" to "HIGH",
                        "description" to "1st/2nd Order Referral Frozen: $freezeReason",
                        "deviceId" to deviceId,
                        "timestamp" to System.currentTimeMillis(),
                        "isResolved" to false
                    )
                    firestore.collection("fraud_incidents").document(incidentMap["id"] as String).set(incidentMap).await()

                    _uiEvent.emit(UiEvent.ShowError("Referral Reward Frozen: Multi-account match detected on 1st/2nd order. Pending Staff/Admin verification."))
                    return@launch
                }

                // If NOT fraud and verified, proceed to pay reward
                if (rewardToPay > 0) {
                    if (previousOrders.size() == 1) updates["firstOrderRewardPaid"] = true
                    if (previousOrders.size() == 2) updates["secondOrderRewardPaid"] = true
                    referralDoc.reference.update(updates).await()

                    val referrerRef = firestore.collection("users").document(referredBy)
                    firestore.runTransaction { transaction ->
                        val referrerSnapshot = transaction.get(referrerRef)
                        val currentBalance = referrerSnapshot.getDouble("fryummCoins") ?: 0.0
                        val currentTotalEarnings = referrerSnapshot.getDouble("totalReferralEarnings") ?: 0.0

                        transaction.update(referrerRef, mapOf(
                            "fryummCoins" to (currentBalance + rewardToPay),
                            "totalReferralEarnings" to (currentTotalEarnings + rewardToPay)
                        ))
                    }.await()
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Referral processing failed", e)
            }
        }
    }

    fun filterOrders(currentUserProfile: UserProfile?, masterAdmins: List<String> = emptyList()) {
        val profile = currentUserProfile
        if (profile == null) {
            orders = rawOrders
            return
        }
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

    fun acceptOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.updateOrderFields(orderId, mapOf("status" to OrderStatus.ACCEPTED))
        }
    }

    fun markReady(orderId: String, currentProfile: UserProfile?) {
        viewModelScope.launch {
            orderRepository.updateOrderFields(orderId, mapOf(
                "status" to OrderStatus.READY,
                "readyAt" to System.currentTimeMillis(),
                "sellerLatitude" to currentProfile?.latitude,
                "sellerLongitude" to currentProfile?.longitude
            ))
        }
    }

    fun cancelOrder(orderId: String, currentProfile: UserProfile?, reason: String? = null) {
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any?>(
                "status" to OrderStatus.CANCELLED,
                "cancelledBy" to (currentProfile?.name ?: "System"),
                "cancellationReason" to (reason ?: "Cancelled by store")
            )
            orderRepository.updateOrderFields(orderId, updates)
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.deleteOrder(orderId)
        }
    }

    fun markDispatched(orderId: String) {
        viewModelScope.launch {
            orderRepository.updateOrderFields(orderId, mapOf("status" to OrderStatus.DISPATCHED))
        }
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
        viewModelScope.launch {
            orderRepository.updateOrderFields(orderId, mapOf("status" to OrderStatus.PICKING))
        }
    }

    private fun startAutoAssignmentMonitor() {
        viewModelScope.launch {
            while (true) {
                delay(30000)
                val now = System.currentTimeMillis()
                val fiveMinutes = 5 * 60 * 1000L

                orders.filter {
                    (it.status == OrderStatus.READY || it.status == OrderStatus.SEALED) &&
                    it.deliveryAgentId == null &&
                    it.readyAt != null &&
                    (now - it.readyAt!!) > fiveMinutes
                }.forEach { order ->
                    val nearestRider = getNearbyRiders(order.sellerLatitude, order.sellerLongitude, emptyList()).firstOrNull { it.second <= 1000 }?.first
                    if (nearestRider != null) {
                        assignOrderToRider(order.id, nearestRider.id)
                        Log.d("AutoAssign", "SYSTEM: Order ${order.id} auto-assigned to ${nearestRider.name} (5 min timeout)")
                    }
                }
            }
        }
    }

    fun getNearbyRiders(merchantLat: Double?, merchantLon: Double?, allUsers: List<UserProfile>): List<Pair<Rider, Int>> {
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

    fun printInvoice(order: Order) {
        Log.d("OrderViewModel", "Printing invoice for order: ${order.id}")
    }

    fun finalizeOrderBilling(order: Order, economics: OrderEconomics) {
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
}
