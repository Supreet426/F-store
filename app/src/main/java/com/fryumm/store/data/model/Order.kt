package com.fryumm.store.data.model

import com.google.firebase.firestore.PropertyName

data class Order(
    val id: String = "",
    val sellerId: String = "seller_1",
    val sellerName: String = "Flash-O Seller",
    val items: List<CartItem> = emptyList(),
    val totalPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val date: String = "",
    var status: String = OrderStatus.PLACED,
    val customerName: String = "Anonymous",
    val shippingAddress: String = "",
    val phoneNumber: String = "",
    val paymentMethod: String = "COD",
    val paymentStatus: String = PaymentStatus.PENDING,
    val area: String = "Main Zone",
    val customerAddress: String = "",
    val pickerId: String? = null,
    val deliveryAgentId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val collectedProductIds: List<String> = emptyList(),
    val verifiedProductIds: List<String> = emptyList(),
    val bagQrCode: String? = null,
    val shelfSlot: String? = "A1",
    val deliveryOtp: String? = null,
    val deliveryTime: Long? = null,
    val couponCode: String? = null,
    val discountAmount: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    @get:PropertyName("isSubscribed") val subscribed: Boolean = false,
    val subscriptionTier: String? = null, // "basic_99" or "premium_199"
    val cancelledBy: String? = null,
    val cancellationReason: String? = null,
    val readyAt: Long? = null,
    val sellerLatitude: Double? = null,
    val sellerLongitude: Double? = null,
    val liveLat: Double? = null,
    val liveLng: Double? = null,
    val deliveryCharge: Double = 0.0,
    val handlingFee: Double = 0.0,
    val roundingOff: Double = 0.0,
    val distanceKm: Double = 0.0,
    var trackingHistory: List<TrackingStep> = emptyList(),
    
    // Cashfree Payments
    val cashfreeOrderId: String? = null,
    val cashfreePaymentId: String? = null,

    // Substitute Product Flow & Stock Reservation Locking
    val substituteRequested: Boolean = false,
    val substituteProductId: String? = null,
    val substituteProductName: String? = null,
    val substituteStatus: String = SubstituteStatus.NONE, // NONE, PENDING, APPROVED, REJECTED
    val stockReservedUntil: Long? = null
)

object SubstituteStatus {
    const val NONE = "NONE"
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

object OrderStatus {
    const val PLACED = "PLACED"
    const val ACCEPTED = "ACCEPTED"
    const val PREPARING = "PREPARING"
    const val PICKING_ASSIGNED = "PICKING_ASSIGNED"
    const val PICKING = "PICKING"
    const val PICKED = "PICKED"
    const val BAGGED = "BAGGED"
    const val VERIFYING = "VERIFYING"
    const val SEALED = "SEALED"
    const val READY = "READY"
    const val ASSIGNED = "ASSIGNED"
    const val DISPATCHED = "DISPATCHED"
    const val DELIVERED = "DELIVERED"
    const val CANCELLED = "CANCELLED"
    const val RETURNED = "RETURNED"
}

object PaymentStatus {
    const val PENDING = "PENDING"
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
}

data class TrackingStep(
    val title: String = "",
    val time: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = ""
)

data class Picker(
    val id: String = "",
    val name: String = ""
)

data class Rider(
    val id: String = "",
    val name: String = ""
)
