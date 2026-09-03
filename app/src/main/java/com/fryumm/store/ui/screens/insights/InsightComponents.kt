package com.fryumm.store.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.Order
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReasonBreakdownDialog(cancelledOrders: List<Order>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancellation Reasons", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LocalReasonBreakdownChart(cancelledOrders)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
fun LocalReasonBreakdownChart(cancelledOrders: List<Order>) {
    val reasons = cancelledOrders.groupBy { it.cancellationReason ?: "Other/Unknown" }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
    
    val total = cancelledOrders.size
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        reasons.forEach { (reason, count) ->
            val percentage = if (total > 0) (count.toFloat() / total * 100).toInt() else 0
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(reason, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("$count ($percentage%)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) count.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color.Red,
                    trackColor = Color.Red.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun LocalEmptyStateCard(message: String) {
    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun LocalSimpleLineChart(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val maxVal = data.maxOfOrNull { it }?.coerceAtLeast(1f) ?: 1f
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = if (data.size > 1) width / (data.size - 1) else 0f
            val points = data.mapIndexed { index, value -> Offset(x = index * spacing, y = height - (value / maxVal * height)) }
            
            // Draw area gradient
            val fillPath = Path().apply {
                moveTo(0f, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(width, height)
                close()
            }
            drawPath(path = fillPath, brush = Brush.verticalGradient(colors = listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.01f))))

            // Draw line
            for (i in 0 until points.size - 1) {
                drawLine(color = color, start = points[i], end = points[i + 1], strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun LocalInsightSectionCard(
    title: String, 
    icon: ImageVector, 
    iconColor: Color, 
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.SpaceBetween, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(color = iconColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (trailing != null) {
                    Spacer(Modifier.width(16.dp))
                    Box(modifier = Modifier.wrapContentSize()) {
                        trailing()
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
