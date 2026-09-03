package com.fryumm.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.ui.screens.insights.*
import com.fryumm.store.ui.theme.FStorePurple
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedInsightsScreen(viewModel: StoreViewModel) {
    val adv = viewModel.getAdvancedAnalytics()
    val kpis = viewModel.getBusinessKPIs()
    val marketing = viewModel.getMarketingAnalytics()
    val fraud = viewModel.getFraudAnalytics()
    
    // Filter States for Analytics
    var dateFilterMode by remember { mutableStateOf("All Time") }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    var dateExpanded by remember { mutableStateOf(false) }
    val dateOptions = listOf("Today", "Yesterday", "This Week", "This Month", "This Year", "Custom", "All Time")

    val dateRange = remember(dateFilterMode, dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis) {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        when (dateFilterMode) {
            "Today" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis..now
            }
            "Yesterday" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                start..cal.timeInMillis
            }
            "This Week" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis..now
            }
            "This Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis..now
            }
            "This Year" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis..now
            }
            "Custom" -> {
                val start = dateRangePickerState.selectedStartDateMillis ?: 0L
                val end = (dateRangePickerState.selectedEndDateMillis ?: dateRangePickerState.selectedStartDateMillis ?: Long.MAX_VALUE)
                start..(end + 86399999L)
            }
            else -> 0L..Long.MAX_VALUE
        }
    }

    val filteredOrders = viewModel.orders.filter { it.timestamp in dateRange }
    val filteredSearches = viewModel.searchEvents.filter { it.timestamp in dateRange }
    val filteredEvents = viewModel.customerEvents.filter { it.timestamp in dateRange }

    var showReasonDialog by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("DONE") } }
        ) {
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }

    if (showReasonDialog) {
        ReasonBreakdownDialog(
            cancelledOrders = filteredOrders.filter { it.status == OrderStatus.CANCELLED || it.status == OrderStatus.RETURNED },
            onDismiss = { showReasonDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Financial Performance Trend Card
            item {
                val totalRev = filteredOrders.sumOf { it.totalAmount }
                val totalOrdersCount = filteredOrders.size
                val avgVal = if(totalOrdersCount > 0) totalRev / totalOrdersCount else 0.0
                
                LocalRevenueTrendCard(
                    totalRevenue = totalRev,
                    totalOrders = totalOrdersCount,
                    avgOrderValue = avgVal,
                    orders = filteredOrders,
                    dateFilterMode = dateFilterMode,
                    onFilterClick = { dateExpanded = true },
                    dateExpanded = dateExpanded,
                    dateOptions = dateOptions,
                    onDateOptionClick = { option ->
                        dateFilterMode = option
                        dateExpanded = false
                        if (option == "Custom") showDatePicker = true
                    },
                    onDismissFilter = { dateExpanded = false }
                )
            }

            // 2. Unit Economics & E-Commerce Metrics (Amazon / Flipkart Benchmark)
            item {
                UnitEconomicsCard(
                    kpis = kpis,
                    retention = adv.retention
                )
            }

            // 3. Quick-Commerce 10-Min SLA & MFC Operations (Zepto / Blinkit Benchmark)
            item {
                QuickCommerceSlaCard(
                    operational = adv.operational,
                    darkStores = viewModel.darkStores,
                    outOfStockProducts = adv.productIntelligence.outOfStock
                )
            }

            // 4. Google Search Analytics & Unmet Demand Gaps
            item {
                SearchAndInventoryGapCard(
                    zeroSearches = adv.zeroResultSearches,
                    searchEvents = filteredSearches
                )
            }

            // 5. Returns & Cancellation Impact
            item {
                val cancelledOrders = filteredOrders.filter { it.status == OrderStatus.CANCELLED || it.status == OrderStatus.RETURNED }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AssignmentReturn, null, tint = Color.Red, modifier = Modifier.size(20.dp)) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Returns & Cancellations Impact", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${cancelledOrders.size} Impacted Orders (RTO)", fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }
                        }
                        TextButton(onClick = { showReasonDialog = true }) {
                            Text("VIEW REASONS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Red)
                        }
                    }
                }
            }

            // 6. Marketing Attribution & Flash-O Pass Subscription
            item {
                MarketingAndPassCard(
                    marketing = marketing,
                    ordersCount = filteredOrders.size
                )
            }

            // 7. Fraud & Risk Anomaly Detection
            item {
                FraudAndRiskCard(
                    fraud = fraud
                )
            }

            // 8. Search & Product Conversion Funnel
            item {
                LocalConversionMetricsCard(
                    totalSearches = filteredSearches.size,
                    totalOrders = filteredOrders.size,
                    events = filteredEvents,
                    searchEvents = filteredSearches
                )
            }

            // 9. Delivery Heatmap & Peak Hours
            item {
                LocalInsightSectionCard(title = "Delivery Heatmap & Hourly Load", icon = Icons.Default.LocalShipping, iconColor = Color(0xFF673AB7)) {
                    val heatMapData = filteredOrders.groupBy { 
                        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        when(cal.get(Calendar.HOUR_OF_DAY)) {
                            in 6..11 -> "Morning"
                            in 12..16 -> "Afternoon"
                            in 17..21 -> "Evening"
                            else -> "Night"
                        }
                    }.mapValues { it.value.size }
                    
                    LocalDeliveryHeatmap(heatMapData)
                    Spacer(Modifier.height(16.dp))
                    
                    val avgTime = adv.operational.avgDeliveryTimeMinutes
                    Text("Average Fulfillment SLA: ${avgTime} mins", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 10. Local AI Insight Card
            item {
                LocalAIInsightCard(orders = filteredOrders)
            }
            
            // 11. Full Funnel Drop-off Analysis
            item {
                LocalInsightSectionCard(title = "Core Funnel Performance Steps", icon = Icons.Default.AssignmentReturn, iconColor = FStorePurple) {
                    val steps = adv.funnel
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        steps.forEachIndexed { index, step ->
                            LocalFunnelStepRow(index, step, steps)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
