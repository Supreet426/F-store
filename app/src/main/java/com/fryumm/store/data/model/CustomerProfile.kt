package com.fryumm.store.data.model

data class AddressItem(
    val id: String = "",
    val label: String = "Home", // Home, Work, Other
    val addressLine: String = "",
    val landmark: String = "",
    val pincode: String = "",
    val isDefault: Boolean = false
)

data class CustomerProfile(
    val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val location: String = "",
    val joinedDate: Long = System.currentTimeMillis(),
    val referralCode: String = "",
    val walletBalance: Double = 0.0,
    val totalOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val returnedOrders: Int = 0,
    val lifetimeSpending: Double = 0.0,
    val avgOrderValue: Double = 0.0,
    val preferredPaymentMethod: String = "",
    val favoriteCategory: String = "",
    val favoriteProducts: List<String> = emptyList(),
    val lastOrderDate: Long? = null,
    val lastLogin: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val device: String = "",
    val language: String = "English",
    val acquisitionSource: String? = null, // SMS, WhatsApp, Notification, FB_Ads, Google_Ads, Instagram, Referral
    val referredByMerchantId: String? = null,
    val isReferralVerified: Boolean = false,
    val deviceId: String = "", // Unique ID for fraud prevention
    val isBlacklisted: Boolean = false,
    
    // Referral Earnings Security & Verification Status
    val isDuplicateDeviceDetected: Boolean = false,
    val registrationDuplicateFlags: List<String> = emptyList(), // e.g. ["SHARED_DEVICE_ID", "SHARED_MOBILE", "SHARED_EMAIL"]
    val referralEarningsFrozen: Boolean = false,
    val referralEarningsFreezeReason: String = "",
    val referralVerificationStatus: String = "VERIFIED", // VERIFIED, FROZEN_PENDING_VERIFICATION, REJECTED_FRAUD
    val verifiedByStaffId: String? = null,
    val verificationTimestamp: Long? = null,
    val bankUpiId: String = "", // Duplicate payout wallet prevention

    // Pass/Subscription Details
    val isSubscribed: Boolean = false,
    val isTrialEligible: Boolean = true, // New: True for new users, false after first order activation
    val subscriptionTier: String? = null, // "basic_99" or "premium_199"
    val subscriptionExpiry: Long? = null,
    val subscriptionStartDate: Long? = null,

    // Advanced Quick-Commerce Profile Fields
    val savedAddresses: List<AddressItem> = emptyList(),
    val defaultDeliveryInstructions: String = "", // e.g. "Leave at door", "Don't ring bell"
    val totalRefundsCount: Int = 0,
    val totalRefundedAmount: Double = 0.0,
    val openSupportTicketsCount: Int = 0,
    val cartAbandonedItemsCount: Int = 0,
    val cartAbandonedTotalValue: Double = 0.0,
    val pushNotificationEnabled: Boolean = true,
    val whatsappUpdatesOptIn: Boolean = true,
    val avgCustomerRating: Double = 5.0
)
