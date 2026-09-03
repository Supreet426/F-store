package com.fryumm.store.data.model

data class StoreConfig(
    val isOpen: Boolean = true,
    val autoAcceptOrders: Boolean = true, // Added for Load Balancing
    val deliveryThreshold: Double = 0.0, // Disabled in favor of per-product thresholds
    val deliveryCharge: Double = 0.0,    // Disabled in favor of per-product thresholds
    val platformFee: Double = 5.0,
    val gstPercentage: Double = 5.0,
    val currentBannerUrl: String = "",
    val storeMessage: String = "",

    val upiId: String = "9235900175@yapl", // Added to sync with Flash-O App
    val upiName: String = "Ranjna Devi",

    // Location Based Delivery Settings
    val storeLatitude: Double = 28.6273,
    val storeLongitude: Double = 77.3725,
    val deliveryRadiusKm: Double = 5.0,
    val deliveryChargePerKm: Double = 0.0, // Additional charge per km beyond base
    val freeDeliveryThresholdKm: Double = 0.0, // Distance within which delivery is free if applicable

    // Specific allowed points outside radius (Exceptions)
    val allowedExceptions: List<LocationPoint> = emptyList(),

    // Specific blocked points inside radius (Hard to reach areas)
    val blockedAreas: List<LocationPoint> = emptyList(),

    // Alert Settings
    val notificationSoundUri: String? = null,
    val isLoudAlertEnabled: Boolean = true
)

data class LocationPoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val name: String = "",
    val radiusMeters: Double = 500.0
)

data class ServiceRequest(
    val id: String = "",
    val userId: String = "", // Unified ID
    val userName: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val userRole: String = "MERCHANT", // MERCHANT or CUSTOMER
    val status: String = "PENDING",
    val type: String = "SUPPORT", // SUPPORT, CALLBACK, CUSTOMER_SERVICE
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val address: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
