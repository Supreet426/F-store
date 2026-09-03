package com.fryumm.store.data.model

data class CampaignStats(
    val campaignName: String,
    val sentCount: Int = 0,
    val openedCount: Int = 0,
    val clickedCount: Int = 0,
    val orderCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val customerCount: Int = 0,
    val avgOrderValue: Double = 0.0
)

data class MarketingAnalytics(
    val topCampaigns: List<CampaignStats>,
    val totalCampaignRevenue: Double,
    val sourceSplit: Map<String, Int>, // Source name to count
    val overallFunnel: MarketingFunnel,
    val cohorts: List<CohortStats> = emptyList()
)

data class CohortStats(
    val period: String, // e.g. "Week 1", "Week 2"
    val totalCustomers: Int,
    val returnedPercentage: Double,
    val avgReturnDays: Int
)

data class MarketingFunnel(
    val sent: Int,
    val opened: Int,
    val clicked: Int,
    val ordered: Int
)

data class BusinessKPIs(
    val cac: Double = 0.0,
    val ltv: Double = 0.0,
    val roas: Double = 0.0,
    val avgBasketSize: Double = 0.0,
    val repeatPurchaseRate: Double = 0.0,
    val retentionRate: Double = 0.0,
    val cancellationRate: Double = 0.0,
    val returnRate: Double = 0.0,
    val refundPercentage: Double = 0.0,
    val netProfit: Double = 0.0,
    val grossProfit: Double = 0.0,
    val contributionMargin: Double = 0.0
)
