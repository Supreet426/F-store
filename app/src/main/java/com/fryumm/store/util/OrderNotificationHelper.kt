package com.fryumm.store.util

import com.fryumm.store.data.model.Order
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.data.model.UserRole

object OrderNotificationHelper {

    val MASTER_ADMIN_EMAILS = MasterAdminSecurity.MASTER_ADMIN_EMAILS

    /**
     * Business & Security Logic:
     * Determines whether an incoming order should trigger an order alert/notification/ring call
     * for a specific user.
     *
     * Rules:
     * 1. ADMIN (Master Admin or UserRole.ADMIN): Always notified for ALL incoming orders across all dark stores and merchants.
     * 2. CUSTOMER / RIDER: Never receives merchant incoming order ring calls.
     * 3. DARK STORE / HUB STAFF: Notified if the order belongs to their assigned darkStoreId or if any item in the order is stocked in their hub.
     * 4. MERCHANT (Store Owner): Notified ONLY if:
     *    - The order's primary merchantId matches the merchant's UID, OR
     *    - At least one product in order.items was created/added under this merchant's ID (product.merchantId == merchant.uid).
     *
     * If none of these match, return false so other merchants are not disturbed or confused!
     */
    fun shouldNotifyUserForOrder(userProfile: UserProfile?, order: Order): Boolean {
        if (userProfile == null) return false

        val emailLower = userProfile.email.lowercase().trim()
        val isAdmin = userProfile.userRole == UserRole.ADMIN || MASTER_ADMIN_EMAILS.contains(emailLower)
        
        // Rule 1: Admin receives notifications/calls for ALL orders
        if (isAdmin) return true

        // Rule 2: Customers and Riders do not get merchant incoming order calls
        if (userProfile.userRole == UserRole.CUSTOMER || userProfile.userRole == UserRole.RIDER) {
            return false
        }

        val currentUserId = userProfile.uid

        // Rule 3: Dark Store Hub Manager/Staff check
        val userDarkStoreId = userProfile.darkStoreId
        if (!userDarkStoreId.isNullOrBlank()) {
            if (order.merchantId == userDarkStoreId) return true
            if (order.items.any { it.product.merchantId == userDarkStoreId }) return true
        }

        // Rule 4: Merchant Product Ownership Check
        if (order.merchantId == currentUserId) return true
        if (order.items.any { it.product.merchantId == currentUserId }) return true

        // Order does not contain any product from this merchant -> DO NOT DISTURB
        return false
    }

    /**
     * Helper check using basic fields for FCM background notifications
     */
    fun shouldNotifyUser(
        userUid: String,
        userRole: UserRole,
        userEmail: String,
        userDarkStoreId: String?,
        orderMerchantId: String,
        orderItemMerchantIds: List<String>
    ): Boolean {
        val emailLower = userEmail.lowercase().trim()
        val isAdmin = userRole == UserRole.ADMIN || MASTER_ADMIN_EMAILS.contains(emailLower)
        if (isAdmin) return true

        if (userRole == UserRole.CUSTOMER || userRole == UserRole.RIDER) return false

        if (!userDarkStoreId.isNullOrBlank()) {
            if (orderMerchantId == userDarkStoreId) return true
            if (orderItemMerchantIds.contains(userDarkStoreId)) return true
        }

        if (orderMerchantId == userUid) return true
        if (orderItemMerchantIds.contains(userUid)) return true

        return false
    }
}
