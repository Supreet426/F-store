package com.fryumm.store.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

enum class UserRole {
    CUSTOMER,
    ADMIN,
    MERCHANT,
    STORE_OWNER, // Maintained for backwards compatibility with MERCHANT
    RIDER,
    OTHER // Technical Staff: Helpline, Cancellation Management, Website Management, Advertisements, Operations
}

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    @get:PropertyName("role") @set:PropertyName("role") var role: String = "STORE_OWNER",
    val storeId: String? = null,
    @get:PropertyName("isActive") @set:PropertyName("isActive") var active: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val lastLogin: Long = System.currentTimeMillis(),
    val darkStoreId: String? = null, // Assigned Hub
    val latitude: Double? = null,
    val longitude: Double? = null,
    val blockReason: String? = null,

    // Store-wide Scheduling & Availability
    val dailySchedules: Map<String, DaySchedule> = emptyMap(),
    val vacationStart: Long? = null,
    val vacationEnd: Long? = null,

    // New Merchant Details
    val tradeName: String = "",
    val legalName: String = "",
    val phoneNumber: String = "",
    val alternatePhoneNumber: String = "",
    val businessType: String = "",
    val contactPerson: String = "",
    val gstin: String = "",
    val fssaiNumber: String = "",
    val fssaiExpiryDate: String = "",
    val panNumber: String = "",
    val averagePreparationTime: Int = 15, // in minutes
    
    // Bank Details
    val bankName: String = "",
    val bankAccountName: String = "",
    val bankAccountNumber: String = "",
    val bankIfscCode: String = "",
    
    // Detailed Address
    val storeAddress: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    
    // Referral System
    val merchantReferralCode: String = "",
    val referralCount: Int = 0, // Total Leads
    val referralVerifiedCount: Int = 0, // Successful Orders
    val referralPoints: Int = 0,
    @get:PropertyName("fryummCoins") @set:PropertyName("fryummCoins") var flashOCoins: Double = 0.0,
    val merchantBadge: String = "BRONZE", // BRONZE, SILVER, GOLD, PLATINUM
    val currentReferralTarget: Int = 10,
    val platformFeeDiscount: Double = 0.0, // Applied for Gold/Platinum

    // Staff / Other Technical Worker Details (Helpline, Cancellations, Web Admin, Advertisements, Operations)
    val staffDepartment: String = "", // e.g., "Helpline & Support", "Cancellation Management", "Website Management", "Advertisement & Marketing", "Technical Operations"
    val staffPermissions: List<String> = emptyList(),

    // Role Change Request System
    val requestedRole: String = "",
    val roleChangeReason: String = "",
    val roleChangeStatus: String = "" // "NONE", "PENDING", "APPROVED", "REJECTED"
) {
    @get:Exclude
    val userRole: UserRole
        get() {
            val emailLower = email.lowercase().trim()
            val masterAdmins = listOf("ranjanamishra1117@gmail.com", "supreet.mishr@gmail.com")
            if (masterAdmins.contains(emailLower)) {
                return UserRole.ADMIN
            }
            val r = role.lowercase().trim()
            val parsed = when {
                r.contains("customer") || r.contains("user") -> UserRole.CUSTOMER
                r.contains("rider") || r.contains("delivery") || r.contains("partner") -> UserRole.RIDER
                r.contains("other") || r.contains("staff") || r.contains("worker") || r.contains("helpline") || r.contains("support") || r.contains("cancel") || r.contains("web") || r.contains("ad") -> UserRole.OTHER
                r.contains("seller") || r.contains("owner") || r.contains("merchant") || r.contains("store") -> UserRole.MERCHANT
                else -> try {
                    UserRole.valueOf(role.uppercase().trim())
                } catch (e: Exception) {
                    UserRole.STORE_OWNER
                }
            }
            // Strict Security Enforcement: Absolutely no new user can yield ADMIN role under any condition!
            return if (parsed == UserRole.ADMIN) UserRole.OTHER else parsed
        }
}
