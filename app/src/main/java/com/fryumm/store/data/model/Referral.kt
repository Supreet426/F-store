package com.fryumm.store.data.model

data class ReferralRecord(
    val id: String = "",
    val referrerId: String = "",
    val referrerName: String = "",
    val refereeId: String = "",
    val refereeName: String = "",
    val isFirstOrderDone: Boolean = false,
    val isSecondOrderDone: Boolean = false,
    val firstOrderRewardPaid: Boolean = false,
    val secondOrderRewardPaid: Boolean = false,
    val isFrozenPendingVerification: Boolean = false,
    val freezeReason: String = "",
    val isVerifiedByStaff: Boolean = false,
    val verifiedByStaffId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
