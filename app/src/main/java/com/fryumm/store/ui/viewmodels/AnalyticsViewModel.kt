package com.fryumm.store.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fryumm.store.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    application: Application,
    private val firestore: FirebaseFirestore
) : AndroidViewModel(application) {

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowSuccess(val message: String) : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    var searchEvents by mutableStateOf<List<SearchEvent>>(emptyList())
        private set

    var customerEvents by mutableStateOf<List<CustomerActivityEvent>>(emptyList())
        private set

    var customerBehaviorAnalytics by mutableStateOf(CustomerBehaviorAnalytics())
        private set

    // AI Simulation Controls
    var simulatedWeatherImpact by mutableStateOf(0.0) // 0 (Clear) to 10 (Storm/Flood)
    var simulatedEventImpact by mutableStateOf(0.0) // 0 to 10 (Mega Festival)

    private var rawSearchEvents = emptyList<SearchEvent>()
    private var rawCustomerEvents = emptyList<CustomerActivityEvent>()

    init {
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            try {
                firestore.collection("search_events")
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            rawSearchEvents = snapshot.toObjects(SearchEvent::class.java)
                            searchEvents = rawSearchEvents
                        }
                    }

                firestore.collection("customer_activity_events")
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            rawCustomerEvents = snapshot.toObjects(CustomerActivityEvent::class.java)
                            customerEvents = rawCustomerEvents
                        }
                    }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun getStoreStats(orders: List<Order>): StoreStats {
        return StoreStats(
            totalOrders = orders.size,
            pendingOrders = orders.count { it.status == OrderStatus.PLACED },
            packedOrders = orders.count { it.status == OrderStatus.READY || it.status == OrderStatus.SEALED },
            outForDeliveryOrders = orders.count { it.status == OrderStatus.DISPATCHED || it.status == OrderStatus.ASSIGNED },
            deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED },
            cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED },
            returnedOrders = orders.count { it.status == OrderStatus.RETURNED },
            totalRevenue = orders.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.totalAmount }
        )
    }

    fun updateAnalytics(orders: List<Order>, products: List<Product>) {
        customerBehaviorAnalytics = calculateCustomerBehaviorAnalytics(orders)
    }

    fun calculateCustomerBehaviorAnalytics(orders: List<Order>): CustomerBehaviorAnalytics {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // 1. Hourly Activity (Weighted towards last 7 days)
        val hourlyWeights = DoubleArray(24) { 0.0 }
        orders.forEach {
            calendar.timeInMillis = it.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val weight = if (it.timestamp > now - (7 * 24 * 60 * 60 * 1000L)) 2.0 else 1.0
            hourlyWeights[hour] += weight
        }
        val hourlyActivity = hourlyWeights.mapIndexed { index, count -> HourlyDemand(index, count.toInt()) }

        // 2. Daily Activity
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dailyWeights = DoubleArray(7) { 0.0 }
        orders.forEach {
            calendar.timeInMillis = it.timestamp
            val day = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val weight = if (it.timestamp > now - (14 * 24 * 60 * 60 * 1000L)) 1.5 else 1.0
            dailyWeights[day] += weight
        }
        val dailyActivity = dailyWeights.mapIndexed { index, count -> DayDemand(dayNames[index], count.toInt()) }

        // 3. Category Analysis
        val categoryStats = orders.flatMap { it.items }
            .groupBy { it.product.category }
            .map { (cat, items) ->
                CategoryAnalysis(
                    category = cat,
                    totalSold = items.sumOf { it.quantity },
                    revenue = items.sumOf { it.product.price * it.quantity }
                )
            }.sortedByDescending { it.revenue }

        // 4. Search & Intent Analytics
        val topSearches = rawSearchEvents.groupBy { it.query.lowercase().trim() }
            .map { (q, events) -> PopularSearch(q, events.size) }
            .sortedByDescending { it.count }.take(10)

        val failedSearches = rawSearchEvents.filter { it.resultCount == 0 }
            .groupBy { it.query.lowercase().trim() }
            .map { (q, events) -> ZeroResultSearch(q, events.size) }
            .sortedByDescending { it.count }.take(10)

        // Predict Peak Hour & Day
        val predictedPeakHour = hourlyWeights.indices.maxByOrNull { hourlyWeights[it] } ?: 0
        val predictedPeakDay = dailyWeights.indices.maxByOrNull { dailyWeights[it] }?.let { dayNames[it] } ?: "Mon"

        val eventImpact = calculateEventImpact(calendar)
        val weatherImpact = simulatedWeatherImpact

        val baselineDailyAvg = if (orders.isNotEmpty()) orders.size.toDouble() / ((now - orders.minOf { it.timestamp }) / (24 * 3600000.0)).coerceAtLeast(1.0) else 0.0
        val forecastedOrders = (baselineDailyAvg * (1.0 + (eventImpact / 10.0)) * (1.0 - (weatherImpact / 20.0))).toInt()

        val recentWindow = 6 * 60 * 60 * 1000L
        val currentActivity = orders.count { it.timestamp > now - recentWindow }
        val baselineActivity = baselineDailyAvg / 4
        val surgePct = if (baselineActivity > 0) ((currentActivity - baselineActivity) / baselineActivity) * 100 else 0.0

        val surgeStatus = when {
            surgePct > 40 || weatherImpact > 7 -> "High Surge / Crisis"
            surgePct > 15 || eventImpact > 5 -> "Rising Demand"
            surgePct < -30 -> "Sudden Fall"
            else -> "Normal"
        }

        val plan = PreparationPlan(
            suggestedPickers = (forecastedOrders / 20).coerceAtLeast(2),
            suggestedRiders = (forecastedOrders / 15 * (1.0 + weatherImpact / 10.0)).toInt().coerceAtLeast(3),
            criticalStockItems = categoryStats.take(3).map { it.category },
            priorityZones = getDeliveryHeatmapData(orders).entries.sortedByDescending { it.value }.take(2).map { it.key },
            estimatedLoadFactor = 1.0 + (surgePct / 100.0) + (eventImpact / 10.0),
            systemAutopilotEnabled = surgeStatus == "High Surge / Crisis"
        )

        val insights = mutableListOf<String>()
        if (weatherImpact > 5) insights.add("🌧️ WEATHER ALERT: High weather impact ($weatherImpact). Delivery times may increase by ${(weatherImpact * 5).toInt()} mins.")
        if (eventImpact > 5) insights.add("🎉 FESTIVAL MODE: Event proximity detected. Expecting ${surgePct.toInt()}% more traffic.")

        insights.add("🤖 AI Forecast: Tomorrow's expected load is $forecastedOrders orders. Suggested staff: ${plan.suggestedPickers} Pickers, ${plan.suggestedRiders} Riders.")

        failedSearches.firstOrNull()?.let {
            insights.add("💡 Inventory Gap: High demand for '${it.query}'. Stocking this could add ₹${(it.count * 150)} to daily revenue.")
        }

        return CustomerBehaviorAnalytics(
            hourlyActivity = hourlyActivity,
            dailyActivity = dailyActivity,
            favoriteCategories = categoryStats,
            topSearches = topSearches,
            failedSearches = failedSearches,
            predictedPeakHour = predictedPeakHour,
            predictedPeakDay = predictedPeakDay,
            activitySurgePercentage = surgePct,
            surgeStatus = surgeStatus,
            insights = insights,
            forecastedOrdersNext24h = forecastedOrders,
            weatherImpactScore = weatherImpact,
            eventImpactScore = eventImpact,
            preparationPlan = plan
        )
    }

    private fun calculateEventImpact(calendar: Calendar): Double {
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return when {
            month == Calendar.OCTOBER || month == Calendar.NOVEMBER -> 8.5
            month == Calendar.MARCH -> 7.0
            month == Calendar.DECEMBER && day > 20 -> 9.0
            else -> simulatedEventImpact
        }
    }

    fun setSimulatedEnvironment(orders: List<Order>, weather: Double, event: Double) {
        simulatedWeatherImpact = weather
        simulatedEventImpact = event
        customerBehaviorAnalytics = calculateCustomerBehaviorAnalytics(orders)
    }

    fun getDeliveryHeatmapData(orders: List<Order>, filter: String = "All"): Map<String, Int> {
        return orders.filter { if (filter == "All") true else it.area == filter }
            .groupBy { it.area }
            .mapValues { it.value.size }
    }

    fun getRevenueForRange(orders: List<Order>, start: Long, end: Long): Double {
        return orders.filter { it.status == OrderStatus.DELIVERED && it.timestamp in start..end }.sumOf { it.totalAmount }
    }
}
