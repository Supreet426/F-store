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

    // Franchise & Investment Vacancy Details
    val isFranchiseVacant: Boolean = false, // True if open for franchise investment/vacancy
    val franchiseStatus: String = "OPERATIONAL", // OPERATIONAL, VACANT_INVESTMENT_OPEN, UNDER_CONTRACT
    val vacancyReason: String = "", // e.g. "New Area Expansion", "Additional Hub", "Contract Renewal / Transfer"
    val investmentAmount: String = "", // e.g. "₹5,00,000 - ₹10,00,000"
    val expectedAreaSqFt: String = "", // e.g. "500 - 1000 Sq. Ft."
    val franchiseContactPhone: String = "", // Contact for franchise investment inquiry
    val franchiseContactEmail: String = "", // Email for franchise investment inquiry
    val investmentNotes: String = "", // Highlights/notes for prospective investors

    val createdAt: Long = System.currentTimeMillis()
)
