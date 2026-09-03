package com.fryumm.store.ui.screens.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.Order
import java.util.Calendar

@Composable
fun LocalRevenueTrendCard(
    totalRevenue: Double,
    totalOrders: Int,
    avgOrderValue: Double,
    orders: List<Order>,
    dateFilterMode: String,
    onFilterClick: () -> Unit,
    dateExpanded: Boolean,
    dateOptions: List<String>,
    onDateOptionClick: (String) -> Unit,
    onDismissFilter: () -> Unit
) {
    LocalInsightSectionCard(
        title = "Financial Performance", 
        icon = Icons.AutoMirrored.Filled.TrendingUp, 
        iconColor = Color(0xFF4CAF50),
        trailing = {
            Box {
                OutlinedButton(
                    onClick = onFilterClick, 
                    shape = RoundedCornerShape(8.dp), 
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Event, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(dateFilterMode, fontSize = 11.sp)
                }
                DropdownMenu(expanded = dateExpanded, onDismissRequest = onDismissFilter) {
                    dateOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { onDateOptionClick(option) })
                    }
                }
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total Revenue", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("₹${totalRevenue.toInt()}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF2E7D32))
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Total Orders", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("$totalOrders", fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Text("Avg. Order Value: ₹${avgOrderValue.toInt()}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        
        Spacer(Modifier.height(16.dp))
        
        val trendData = remember(orders, dateFilterMode) {
            val cal = Calendar.getInstance()
            when (dateFilterMode) {
                "Today", "Yesterday" -> {
                    val hours = MutableList(24) { 0.0 }
                    orders.forEach { 
                        cal.timeInMillis = it.timestamp
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        hours[hour] += it.totalAmount
                    }
                    hours.map { it.toFloat() }
                }
                "This Week" -> {
                    val days = MutableList(7) { 0.0 }
                    orders.forEach {
                        cal.timeInMillis = it.timestamp
                        val day = cal.get(Calendar.DAY_OF_WEEK)
                        val index = (day - cal.firstDayOfWeek + 7) % 7
                        days[index] += it.totalAmount
                    }
                    days.map { it.toFloat() }
                }
                else -> {
                    if (orders.isEmpty()) List(10) { 0f }
                    else {
                        val sorted = orders.sortedBy { it.timestamp }
                        val min = sorted.first().timestamp
                        val max = sorted.last().timestamp
                        val step = (max - min) / 9.coerceAtLeast(1)
                        if (step == 0L) List(10) { sorted.first().totalAmount.toFloat() }
                        else {
                            val points = MutableList(10) { 0.0 }
                            orders.forEach {
                                val idx = ((it.timestamp - min) / step).toInt().coerceIn(0, 9)
                                points[idx] += it.totalAmount
                            }
                            points.map { it.toFloat() }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            LocalSimpleLineChart(data = trendData, color = Color(0xFF4CAF50), modifier = Modifier.fillMaxSize())
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Trend Overview ($dateFilterMode)", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            if (trendData.isNotEmpty()) {
                val growth = if (trendData.first() > 0) ((trendData.last() - trendData.first()) / trendData.first() * 100).toInt() else 0
                Text(
                    text = "${if(growth >= 0) "+" else ""}$growth% period shift", 
                    fontSize = 10.sp, 
                    color = if(growth >= 0) Color(0xFF2E7D32) else Color.Red,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
