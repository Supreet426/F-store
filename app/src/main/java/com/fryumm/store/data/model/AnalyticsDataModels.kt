package com.fryumm.store.data.model

data class TimeRange(
    val startTime: String = "09:00",
    val endTime: String = "21:00"
)

data class DaySchedule(
    val isEnabled: Boolean = true,
    val timeRanges: List<TimeRange> = listOf(TimeRange())
)

data class StoreStats(
    val totalOrders: Int = 0,
    val pendingOrders: Int = 0,
    val packedOrders: Int = 0,
    val outForDeliveryOrders: Int = 0,
    val deliveredOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val returnedOrders: Int = 0,
    val totalRevenue: Double = 0.0
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

data class RiderPerformance(
    val riderId: String = "",
    val totalDeliveries: Int = 0,
    val totalEarnings: Double = 0.0,
    val onTimePercentage: Double = 100.0,
    val avgRating: Double = 5.0,
    val totalDistanceKm: Double = 0.0
)

data class FunnelStep(
    val stepName: String = "",
    val count: Int = 0,
    val percentage: Double = 0.0,
    val name: String = stepName
)

data class OperationalStats(
    val avgPickingTimeMinutes: Int = 0,
    val avgPackingTimeMinutes: Int = 0,
    val qcFailureRate: Float = 0f,
    val avgRiderWaitTimeMinutes: Int = 0,
    val avgDeliveryTimeMinutes: Int = 0,
    val lateDeliveries: Int = 0
)

data class ProductIntelligence(
    val bestSellers: List<ProductAnalysis> = emptyList(),
    val worstSellers: List<ProductAnalysis> = emptyList(),
    val outOfStock: List<Product> = emptyList(),
    val lowStock: List<Product> = emptyList(),
    val deadStock: List<Product> = emptyList(),
    val expiredProducts: List<Product> = emptyList()
)

data class ProductAnalysis(
    val productName: String = "",
    val productId: String = "",
    val totalSold: Int = 0,
    val revenue: Double = 0.0
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

data class ZeroResultSearch(
    val query: String = "",
    val count: Int = 0
)


data class HourlyDemand(
    val hour: Int = 0,
    val count: Int = 0
)

data class DayDemand(
    val day: String = "",
    val count: Int = 0
)

data class CategoryAnalysis(
    val category: String = "",
    val totalSold: Int = 0,
    val revenue: Double = 0.0
)

data class BundleSuggestion(
    val primaryProduct: String = "",
    val suggestedProduct: String = "",
    val products: List<String> = listOf(primaryProduct, suggestedProduct),
    val support: Int = 0,
    val coOccurrenceCount: Int = 0,
    val confidence: Float = 0f
)

data class DemandPrediction(
    val productName: String = "",
    val productId: String = "",
    val currentStock: Int = 0,
    val predictedDemand: Int = 0,
    val confidence: Float = 0.85f,
    val recommendation: String = ""
)

data class CustomerBehaviorAnalytics(
    val hourlyActivity: List<HourlyDemand> = emptyList(),
    val dailyActivity: List<DayDemand> = emptyList(),
    val favoriteCategories: List<CategoryAnalysis> = emptyList(),
    val categoryStats: List<CategoryAnalysis> = emptyList(),
    val topSearches: List<PopularSearch> = emptyList(),
    val failedSearches: List<ZeroResultSearch> = emptyList(),
    val predictedPeakHour: Int = 12,
    val predictedPeakDay: String = "Mon",
    val activitySurgePercentage: Double = 0.0,
    val surgeStatus: String = "Normal",
    val insights: List<String> = emptyList(),
    val forecastedOrdersNext24h: Int = 0,
    val weatherImpactScore: Double = 0.0,
    val eventImpactScore: Double = 0.0,
    val preparationPlan: PreparationPlan = PreparationPlan()
)

data class PreparationPlan(
    val suggestedPickers: Int = 2,
    val suggestedRiders: Int = 3,
    val criticalStockItems: List<String> = emptyList(),
    val priorityZones: List<String> = emptyList(),
    val estimatedLoadFactor: Double = 1.0,
    val systemAutopilotEnabled: Boolean = false
)

data class AdvancedAnalytics(
    val funnel: List<FunnelStep> = emptyList(),
    val zeroResultSearches: List<ZeroResultSearch> = emptyList(),
    val abandonmentRate: Float = 0f,
    val bundleSuggestions: List<BundleSuggestion> = emptyList(),
    val operational: OperationalStats = OperationalStats(),
    val productIntelligence: ProductIntelligence = ProductIntelligence(),
    val demandPredictions: List<DemandPrediction> = emptyList(),
    val retention: RetentionStats = RetentionStats(),
    val payment: PaymentStats = PaymentStats(),
    val riderPerformance: List<RiderPerformance> = emptyList()
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
