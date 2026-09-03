package com.fryumm.store.data.model

import com.google.firebase.firestore.PropertyName

data class Product(
    val id: String = "",
    val brand: String = "",
    val name: String = "",
    val description: String = "",
    val shortDescription: String = "",
    val price: Double = 0.0, // Calculated final price stored here
    val originalPrice: Double = 0.0,
    val imageUrl: String = "",
    val images: List<String> = emptyList(),
    val category: String = "",
    val subCategory: String = "",
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val sizes: List<String> = emptyList(),
    val reviews: List<Review> = emptyList(),
    var stockCount: Int = 20,
    val sellerId: String = "seller_1",
    val sellerName: String = "Flash-O Seller",
    val unit: String = "1 unit",
    val specifications: Map<String, String> = emptyMap(),
    
    // Coordination/Warehouse fields
    val barcode: String = "",
    val aisle: String = "A1",
    val rack: String = "R1",
    val weight: String = "1kg",
    
    // Inventory fields
    val stockQuantity: Int = 0,
    val minStockLevel: Int = 5,
    val minOrderQuantity: Int = 1, // Minimum units a customer must order
    @get:PropertyName("isInStock") @set:PropertyName("isInStock") var isInStock: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val availableCoupons: List<Coupon> = emptyList(),
    
    // Pricing Components (Merchant fills these)
    val basePrice: Double = 0.0,      // Actual cost before tax/discount
    val merchantRate: Double = 0.0,   // Amount to be paid to merchant per unit (Fixed)
    val taxRate: Double = 0.0,       // Total GST % (e.g. 5.0, 12.0)
    val cgstRate: Double = 0.0,      // Central GST %
    val sgstRate: Double = 0.0,      // State GST %
    val igstRate: Double = 0.0,      // Integrated GST %
    val hsnCode: String = "",        // HSN Code for GST
    val commissionRate: Double = 0.0, // Platform Commission % (Admin sets this)
    val adminProfitPercent: Double = 0.0, // Admin Profit % (Admin sets this)
    val handlingFee: Double = 0.0,    // Packing/Platform fee per unit
    val discountValue: Double = 0.0,  // Fixed discount on base price
    @get:PropertyName("isTaxInclusive") @set:PropertyName("isTaxInclusive") var isTaxInclusive: Boolean = true, // Whether basePrice already includes tax
    
    // New fields requested
    val searchNames: List<String> = emptyList(), // Regional / Nicknames for search
    val deliveryCharge: Double = 0.0, // Manual delivery charge
    val unitType: String = "Weight",  // Weight, Volume, Unit, Length
    val measurementUnit: String = "g", // g, kg, ml, l, pc, cm, m, etc.
    val expiryDate: Long? = null,
    @get:PropertyName("isFoodProduct") @set:PropertyName("isFoodProduct") var isFoodProduct: Boolean = false,
    val deliveryChargeIncluded: Boolean = true,
    val manufacturingDate: Long? = null,
    val legalDisclaimer: String = "",
    val nutritionalInfo: String = "",
    
    // Free Delivery Thresholds
    val freeDeliveryMinAmount: Double = 0.0,    // Free delivery if product value in cart >= X
    val freeDeliveryMinQuantity: Int = 0,       // Free delivery if quantity in cart >= Y

    // Admin Verification System
    @get:PropertyName("isVerified") @set:PropertyName("isVerified") var isVerified: Boolean = false,
    val verifiedBy: String? = null,
    val verificationDate: Long? = null,
    val adminFeedback: String? = null, // Stores what admin changed or notes for merchant

    // Availability System (Merchant Control)
    @get:PropertyName("isAvailable") @set:PropertyName("isAvailable") var isAvailable: Boolean = true, // To Hide/Show product instantly without deleting
    @get:PropertyName("isComingSoon") @set:PropertyName("isComingSoon") var isComingSoon: Boolean = false, // New: To show product but disable orders

    // Ordering/Sequence for Admin and Customer App
    val sequenceNumber: Int = 999, // Lower numbers show first

    // Scheduling & Automated Management
    val dailySchedules: Map<String, DaySchedule> = emptyMap(), // Key: "Monday", "Tuesday", etc.
    val vacationStart: Long? = null,
    val vacationEnd: Long? = null,

    // SKU Variants & Out-of-stock Substitutes
    val variants: List<ProductVariant> = emptyList(),
    val substituteProductIds: List<String> = emptyList()
) {
    val discountAmount: Double
        get() = if (originalPrice > price) originalPrice - price else 0.0

    val discountPercentage: Int
        get() = if (originalPrice > 0 && originalPrice > price) {
            val percentage = (((originalPrice - price) / originalPrice) * 100).toInt()
            if (percentage > 100) 100 else percentage
        } else 0
}

data class ProductVariant(
    val id: String = "",
    val title: String = "", // e.g., "500g", "1kg", "500ml", "Pack of 2"
    val measurementUnit: String = "g", // g, kg, ml, l, pc
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val basePrice: Double = 0.0,
    val stockQuantity: Int = 0,
    val barcode: String = "",
    val aisle: String = "A1",
    val rack: String = "R1",
    val isAvailable: Boolean = true
)

data class DaySchedule(
    val isEnabled: Boolean = false,
    val timeRanges: List<TimeRange> = listOf(TimeRange())
)

data class TimeRange(
    val startTime: String = "00:00", // HH:mm
    val endTime: String = "23:59"    // HH:mm
)

data class Coupon(
    val code: String = "",
    val discount: Double = 0.0,
    val description: String = ""
)

data class Review(
    val id: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val date: String = "",
    var sellerReply: String? = null
)

data class StoreStats(
    val totalRevenue: Double = 0.0,
    val totalOrders: Int = 0,
    val pendingOrders: Int = 0,
    val packedOrders: Int = 0,
    val outForDeliveryOrders: Int = 0,
    val deliveredOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val returnedOrders: Int = 0,
    val inventoryHealth: Float = 0.0f,
    val averageOrderValue: Double = 0.0
)

data class ProductAnalysis(
    val productName: String = "",
    val totalSold: Int = 0,
    val revenue: Double = 0.0
)

data class CustomerInsight(
    val name: String = "",
    val totalSpent: Double = 0.0,
    val orderCount: Int = 0
)

data class AreaDemand(
    val area: String = "",
    val orderCount: Int = 0,
    val revenue: Double = 0.0
)

data class HourlyDemand(
    val hour: Int = 0,
    val orderCount: Int = 0
)

data class DayDemand(
    val day: String = "",
    val orderCount: Int = 0
)

data class CustomerBehaviorAnalytics(
    val hourlyActivity: List<HourlyDemand> = emptyList(),
    val dailyActivity: List<DayDemand> = emptyList(),
    val favoriteCategories: List<CategoryAnalysis> = emptyList(),
    val topSearches: List<PopularSearch> = emptyList(),
    val failedSearches: List<ZeroResultSearch> = emptyList(),
    val predictedPeakHour: Int = 0,
    val predictedPeakDay: String = "",
    val activitySurgePercentage: Double = 0.0,
    val surgeStatus: String = "Normal", // "Normal", "Rising", "High Surge"
    val insights: List<String> = emptyList(),
    
    // AI Predictive Extensions
    val forecastedOrdersNext24h: Int = 0,
    val weatherImpactScore: Double = 0.0, // 0 to 10 (10 = severe delay)
    val eventImpactScore: Double = 0.0,
    val preparationPlan: PreparationPlan? = null
)

data class PreparationPlan(
    val suggestedPickers: Int = 0,
    val suggestedRiders: Int = 0,
    val criticalStockItems: List<String> = emptyList(),
    val priorityZones: List<String> = emptyList(),
    val estimatedLoadFactor: Double = 0.0, // 1.0 = normal
    val systemAutopilotEnabled: Boolean = false
)

data class CategoryAnalysis(
    val category: String = "",
    val totalSold: Int = 0,
    val revenue: Double = 0.0
)

data class FunnelStep(
    val name: String = "",
    val count: Int = 0,
    val percentage: Float = 0f
)

data class ZeroResultSearch(
    val query: String = "",
    val count: Int = 0
)

data class BundleSuggestion(
    val products: List<String> = emptyList(),
    val support: Int = 0
)

data class OperationalStats(
    val avgPickingTimeMinutes: Int = 0,
    val avgPackingTimeMinutes: Int = 0,
    val qcFailureRate: Float = 0f,
    val avgRiderWaitTimeMinutes: Int = 0,
    val avgDeliveryTimeMinutes: Int = 0,
    val lateDeliveries: Int = 0
)

data class RiderPerformance(
    val riderId: String = "",
    val riderName: String = "",
    val totalDeliveries: Int = 0,
    val avgDeliveryTimeMinutes: Int = 0,
    val onTimePercentage: Float = 0f,
    val lateDeliveryPercentage: Float = 0f,
    val avgRating: Double = 0.0,
    val totalDistanceKm: Double = 0.0,
    val totalEarnings: Double = 0.0
)

data class RetentionStats(
    val repeatPurchaseRate: Float = 0f,
    val averageLTV: Double = 0.0,
    val inactiveUsersCount: Int = 0
)

data class PaymentStats(
    val methodSplit: Map<String, Int> = emptyMap(),
    val failureRate: Float = 0f
)

data class CustomerSummaryStats(
    val total: Int = 0,
    val activeToday: Int = 0,
    val newThisWeek: Int = 0,
    val repeat: Int = 0,
    val dormant: Int = 0,
    val vipCount: Int = 0,
    val riskyCount: Int = 0
)

data class CEODashboardStats(
    val totalStores: Int = 0,
    val totalUsers: Int = 0,
    val totalRevenueFY: Double = 0.0,
    val totalProfitFY: Double = 0.0,
    val todayRevenue: Double = 0.0,
    val todayProfit: Double = 0.0,
    val yesterdayRevenue: Double = 0.0,
    val weeklyRevenue: Double = 0.0,
    val monthlyRevenue: Double = 0.0,
    val totalOrders: Int = 0,
    val pendingOrders: Int = 0,
    val packedOrders: Int = 0,
    val outForDeliveryOrders: Int = 0,
    val deliveredOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val returnedOrders: Int = 0
)

data class ProductIntelligence(
    val bestSellers: List<ProductAnalysis> = emptyList(),
    val worstSellers: List<ProductAnalysis> = emptyList(),
    val outOfStock: List<Product> = emptyList(),
    val lowStock: List<Product> = emptyList(),
    val deadStock: List<Product> = emptyList(),
    val expiredProducts: List<Product> = emptyList()
)

data class DemandPrediction(
    val productName: String,
    val productId: String,
    val currentStock: Int,
    val predictedDemand: Int,
    val confidence: Float, // 0.0 to 1.0
    val recommendation: String // "Restock Immediately", "Healthy", "Overstocked"
)

data class AdvancedAnalytics(
    val funnel: List<FunnelStep> = emptyList(),
    val zeroResultSearches: List<ZeroResultSearch> = emptyList(),
    val abandonmentRate: Float = 0f,
    val bundleSuggestions: List<BundleSuggestion> = emptyList(),
    val operational: OperationalStats = OperationalStats(),
    val riderPerformance: List<RiderPerformance> = emptyList(),
    val productIntelligence: ProductIntelligence = ProductIntelligence(),
    val demandPredictions: List<DemandPrediction> = emptyList(),
    val retention: RetentionStats = RetentionStats(),
    val payment: PaymentStats = PaymentStats()
)

