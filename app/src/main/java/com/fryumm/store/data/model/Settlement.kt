package com.fryumm.store.data.model

data class MerchantSettlement(
    val merchantId: String = "",
    val merchantName: String = "",
    val items: List<SettlementItem> = emptyList(),
    val totalPayout: Double = 0.0,
    val totalOrders: Int = 0
)

data class SettlementItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val merchantRate: Double = 0.0,
    val totalAmount: Double = 0.0,
    val orders: List<String> = emptyList() // List of Order IDs
)
