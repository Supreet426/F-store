package com.fryumm.store.data.model

data class SupportMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFromAdmin: Boolean = false,
    val type: String = "TEXT" // TEXT, IMAGE, ORDER_REFERENCE
)

data class SupportSession(
    val id: String = "", // Same as User UID
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userRole: String = "MERCHANT", // MERCHANT or CUSTOMER
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val isUnreadByAdmin: Boolean = true,
    val isActive: Boolean = true
)

data class SupportRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val userRole: String = "MERCHANT", // MERCHANT or CUSTOMER
    val status: String = "PENDING", // PENDING, CALLED, RESOLVED
    val type: String = "CALLBACK_REQUEST",
    val timestamp: Long = System.currentTimeMillis()
)

