package com.fryumm.store.ui.screens.useractivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.*
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomerTab(
    viewModel: StoreViewModel,
    searchQuery: String,
    onCustomerClick: (CustomerProfile) -> Unit,
    onOrderClick: (Order) -> Unit,
    onRoleChangeClick: (String, String, String) -> Unit
) {
    val customerStats = viewModel.getCustomerSummaryStats()
    val customerAnalysis = viewModel.getCustomerAnalysis()
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    var selectedFilter by remember { mutableStateOf("All") }
    
    val filteredCustomers = run {
        val base = when(selectedFilter) {
            "Total Customers" -> customerAnalysis["Total Customers"] ?: emptyList()
            "New Customers" -> customerAnalysis["New Customers"] ?: emptyList()
            "Repeat Customers" -> customerAnalysis["Repeat Customers"] ?: emptyList()
            "Dormant Customers" -> customerAnalysis["Dormant Customers"] ?: emptyList()
            "Risky Customers" -> customerAnalysis["Risky Customers"] ?: emptyList()
            "VIP" -> customerAnalysis["VIP"] ?: emptyList()
            else -> customerAnalysis["Total Customers"] ?: emptyList()
        }
        base.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.mobile.contains(searchQuery, ignoreCase = true) 
        }
    }

    val marketingAnalytics = if (selectedFilter == "Marketing") viewModel.getMarketingAnalytics() else null
    val fraudAnalytics = if (selectedFilter == "Fraud Detection") viewModel.getFraudAnalytics() else null
    val businessKPIs = if (selectedFilter == "Business KPIs") viewModel.getBusinessKPIs() else null
    val orderAnalytics = if (selectedFilter == "Order Analytics") viewModel.orders else emptyList()
    val searchEvents = viewModel.searchEvents.sortedByDescending { it.timestamp }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { IntelligenceCard("All", customerStats.total.toString(), Color(0xFF2196F3), selectedFilter == "All") { selectedFilter = "All" } }
            item { IntelligenceCard("Fraud", (viewModel.getFraudAnalytics().totalFlaggedUsers).toString(), Color.Red, selectedFilter == "Fraud Detection") { selectedFilter = "Fraud Detection" } }
            item { IntelligenceCard("Business KPIs", "Stats", Color(0xFF009688), selectedFilter == "Business KPIs") { selectedFilter = "Business KPIs" } }
            item { IntelligenceCard("Marketing", "ROI 4x", Color(0xFF673AB7), selectedFilter == "Marketing") { selectedFilter = "Marketing" } }
            item { IntelligenceCard("Order Analyt.", viewModel.orders.size.toString(), Color(0xFFE91E63), selectedFilter == "Order Analytics") { selectedFilter = "Order Analytics" } }
            item { IntelligenceCard("VIP", (customerAnalysis["VIP"]?.size ?: 0).toString(), Color(0xFFFFD700), selectedFilter == "VIP") { selectedFilter = "VIP" } }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedFilter == "Fraud Detection" && fraudAnalytics != null) {
                item { FraudSummaryCard(fraudAnalytics) }
                val allIncidents = (fraudAnalytics.multipleAccounts + 
                    fraudAnalytics.referralFraud + 
                    fraudAnalytics.couponAbuse + 
                    fraudAnalytics.highReturners + 
                    fraudAnalytics.codAbuse + 
                    fraudAnalytics.gpsAnomaly + 
                    fraudAnalytics.paymentSuspicion).sortedByDescending { it.riskLevel }
                items(allIncidents) { incident -> FraudIncidentItem(incident) }
            }

            if (selectedFilter == "Business KPIs" && businessKPIs != null) {
                item { BusinessKPIsDashboard(businessKPIs) }
            }

            if (selectedFilter == "Marketing" && marketingAnalytics != null) {
                item { MarketingSummaryCard(marketingAnalytics) }
                items(marketingAnalytics.topCampaigns) { campaign -> CampaignItem(campaign) }
            }

            if (selectedFilter == "Order Analytics") {
                items(orderAnalytics) { order -> OrderAnalyticsItem(order, onClick = { onOrderClick(order) }) }
            }

            if (selectedFilter !in listOf("Fraud Detection", "Business KPIs", "Marketing", "Order Analytics")) {
                items(filteredCustomers) { customer ->
                    CustomerListItem(
                        customer = customer,
                        viewModel = viewModel,
                        onClick = { onCustomerClick(customer) },
                        onRoleChangeClick = { onRoleChangeClick(customer.id, customer.name, "CUSTOMER") }
                    )
                }
            }
            
            item {
                Spacer(Modifier.height(24.dp))
                Text("Recent Live Searches", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(searchEvents.take(15)) { event ->
                ActivityItem(
                    user = event.userId,
                    action = "Searched for \"${event.query}\"",
                    time = sdf.format(Date(event.timestamp)),
                    area = event.area,
                    results = event.resultCount
                )
            }
        }
    }
}
