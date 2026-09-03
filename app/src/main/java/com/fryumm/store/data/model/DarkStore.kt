package com.fryumm.store.data.model

data class DarkStore(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val centerLatitude: Double = 0.0,
    val centerLongitude: Double = 0.0,
    val deliveryRadiusKm: Double = 5.0,
    val visibilityRadiusKm: Double = 5.0, // Operational Radius for visibility
    val isActive: Boolean = true,

    // Manager Details
    val managerName: String = "",
    val managerMobile: String = "",
    val aadharNumber: String = "",

    // Bank Details
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankIfsc: String = "",
    
    // Allowed points outside radius (Exceptions) with their own custom radius
    val allowedExceptions: List<LocationPoint> = emptyList(),

    // Blocked points inside radius
    val blockedAreas: List<LocationPoint> = emptyList(),
    
    // List of merchants assigned to this dark store hub
    val assignedMerchantIds: List<String> = emptyList(),

    // List of riders assigned to this dark store hub
    val assignedRiderIds: List<String> = emptyList(),
    
    val createdAt: Long = System.currentTimeMillis()
)
