package com.fryumm.store.data.model

enum class UserSegment(val displayName: String, val description: String) {
    LOYAL("Loyal Customers", "Frequent buyers with high lifetime value"),
    VIP("VIP / High Spenders", "Customers with high average order value"),
    INACTIVE("Inactive / Churned", "Customers who haven't ordered in a while"),
    DORMANT("Dormant", "Previous customers who have stopped ordering"),
    NEW("New Joinees", "Recently registered users"),
    RISKY("Risky / Fraudulent", "Users with high cancellation or return rates"),
    ABANDONERS("Cart Abandoners", "Users who leave items in their cart"),
    POTENTIAL("Potential Growth", "Active users with low spending but high engagement"),
    UNSEGMENTED("General", "Users who don't fit other specific categories")
}

data class UserSegmentationReport(
    val segments: Map<UserSegment, List<CustomerProfile>> = emptyMap(),
    val counts: Map<UserSegment, Int> = emptyMap(),
    val totalCustomers: Int = 0,
    val topSegment: UserSegment = UserSegment.UNSEGMENTED,
    val timestamp: Long = System.currentTimeMillis()
)

data class SegmentMarketingInsight(
    val segment: UserSegment,
    val recommendedAction: String,
    val suggestedCoupon: String?,
    val priority: Int // 1 (High) to 5 (Low)
)
