package com.fryumm.store.data.model

data class WalletTransaction(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val type: String = WalletTransactionType.CREDIT, // CREDIT, DEBIT
    val category: String = WalletCategory.REFUND, // REFUND, CASHBACK, PROMO, ORDER_PAYMENT
    val description: String = "",
    val orderId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object WalletTransactionType {
    const val CREDIT = "CREDIT"
    const val DEBIT = "DEBIT"
}

object WalletCategory {
    const val REFUND = "REFUND"
    const val CASHBACK = "CASHBACK"
    const val PROMO = "PROMO"
    const val ORDER_PAYMENT = "ORDER_PAYMENT"
    const val REFERRAL_BONUS = "REFERRAL_BONUS"
}
