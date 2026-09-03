package com.fryumm.store.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.Order

@Composable
fun LocalDeliveryHeatmap(data: Map<String, Int>) {
    val maxVal = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        data.forEach { (period, count) ->
            val intensity = count.toFloat() / maxVal
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(36.dp).background(Color(0xFF4CAF50).copy(alpha = intensity.coerceIn(0.1f, 1f)), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(count.toString(), fontWeight = FontWeight.Black, color = if(intensity > 0.5f) Color.White else Color.Black, fontSize = 12.sp)
                }
                Text(period, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LocalAIInsightCard(orders: List<Order>) {
    if (orders.isEmpty()) return
    
    val totalRevenue = orders.sumOf { it.totalAmount }
    val premiumCount = orders.count { it.subscribed }
    val topArea = orders.groupBy { it.area }.maxByOrNull { it.value.size }?.key ?: "N/A"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("AI Business Insights", fontWeight = FontWeight.Black, color = Color(0xFF1976D2))
            }
            Spacer(Modifier.height(8.dp))
            Text("• ${if(premiumCount > orders.size/3) "Premium adoption is high! Focus on retention." else "Premium users are low. Consider a promotion in $topArea."}", fontSize = 12.sp, color = Color.DarkGray)
            Text("• ${if(totalRevenue > 5000) "High sales volume detected. Ensure operational capacity." else "Sales are stable. Focus on increasing average basket size."}", fontSize = 12.sp, color = Color.DarkGray)
        }
    }
}
