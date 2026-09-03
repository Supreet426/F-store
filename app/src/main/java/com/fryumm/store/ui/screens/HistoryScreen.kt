package com.fryumm.store.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.ui.components.AnalyticsMiniCard
import com.fryumm.store.ui.components.HistoryLogItem

@Composable
fun HistoryScreen(viewModel: StoreViewModel) {
    val historyOrders = viewModel.orders.filter { 
        it.status == OrderStatus.DELIVERED || it.status == OrderStatus.CANCELLED 
    }.sortedByDescending { it.timestamp }
    
    val totalRevenue = historyOrders.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.totalAmount }
    val avgOrderValue = if (historyOrders.isNotEmpty()) totalRevenue / historyOrders.size else 0.0

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Summary Cards for Research
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalyticsMiniCard("Total Orders", "${historyOrders.size}", Modifier.weight(1f))
            AnalyticsMiniCard("Revenue", "₹${totalRevenue.toInt()}", Modifier.weight(1f))
            AnalyticsMiniCard("Avg Value", "₹${avgOrderValue.toInt()}", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Order Logs", fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (historyOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data for analysis.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(historyOrders) { order ->
                    HistoryLogItem(order, viewModel)
                }
            }
        }
    }
}
