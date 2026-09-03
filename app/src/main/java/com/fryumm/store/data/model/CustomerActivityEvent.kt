package com.fryumm.store.data.model

data class CustomerActivityEvent(
    val id: String = "",
    val customerId: String = "",
    val eventType: String = "", // APP_OPEN, SEARCH, VIEW_PRODUCT, ADD_TO_CART, REMOVE_FROM_CART, CHECKOUT, PAYMENT_SUCCESS, ORDER_CONFIRMED
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object CustomerEventType {
    const val APP_OPEN = "App Open"
    const val LOGIN = "Login"
    const val SIGNUP = "Signup"
    const val LOCATION_SELECTED = "Location Selected"
    const val SEARCH = "Searched"
    const val VIEW_PRODUCT = "Viewed Product"
    const val VIEW_CATEGORY = "Viewed Category"
    const val ADD_TO_CART = "Added to Cart"
    const val REMOVE_FROM_CART = "Removed Product"
    const val CHECKOUT_STARTED = "Checkout Started"
    const val PAYMENT_STARTED = "Payment Started"
    const val PAYMENT_SUCCESS = "Payment Success"
    const val PAYMENT_FAILED = "Payment Failed"
    const val ORDER_CREATED = "Order Created"
    const val ORDER_CANCELLED = "Order Cancelled"
    const val ORDER_DELIVERED = "Order Delivered"
    const val REFUND_REQUESTED = "Refund Requested"
    const val NOTIFICATION_RECEIVED = "Notification Received"
    const val NOTIFICATION_CLICKED = "Notification Clicked"
    const val BANNER_CLICKED = "Banner Clicked"
    const val COUPON_APPLIED = "Coupon Applied"
    const val COUPON_REMOVED = "Coupon Removed"
    const val SHARE_APP = "Share App"
    const val REFER_FRIEND = "Refer Friend"
    
    // Marketing Funnel Events
    const val CAMPAIGN_SENT = "Campaign Sent"
    const val CAMPAIGN_OPENED = "Campaign Opened"
    const val CAMPAIGN_CLICKED = "Campaign Clicked"
}

