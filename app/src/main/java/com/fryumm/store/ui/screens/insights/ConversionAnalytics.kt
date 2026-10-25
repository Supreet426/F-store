package com.fryumm.store.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.CustomerActivityEvent
import com.fryumm.store.data.model.FunnelStep
import com.fryumm.store.data.model.SearchEvent
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LocalConversionMetricsCard(
    totalSearches: Int, 
    totalOrders: Int, 
    events: List<CustomerActivityEvent>,
    searchEvents: List<SearchEvent>
) {
    val productViews = events.filter { it.eventType == "VIEW_PRODUCT" }
    val addToCarts = events.filter { it.eventType == "ADD_TO_CART" }
    
    val searchToOrderRate = if(totalSearches > 0) (totalOrders.toFloat() / totalSearches * 100).toInt() else 0

    var showSearchDetails by remember { mutableStateOf(false) }
    var showClickDetails by remember { mutableStateOf(false) }
    var showCartDetails by remember { mutableStateOf(false) }

    if (showSearchDetails) {
        SearchDetailsDialog(searchEvents) { showSearchDetails = false }
    }
    if (showClickDetails) {
        EventDetailsDialog("Product Clicks", productViews) { showClickDetails = false }
    }
    if (showCartDetails) {
        EventDetailsDialog("Cart Activity", addToCarts) { showCartDetails = false }
    }

    LocalInsightSectionCard(title = "Conversion & Engagement", icon = Icons.Default.Transform, iconColor = Color(0xFFE91E63)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f).clickable { showSearchDetails = true }) {
                Text("Search Volume", fontSize = 11.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$totalSearches", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(12.dp).padding(start = 4.dp), tint = Color.Gray)
                }
                Text("Total Searches", fontSize = 9.sp, color = Color.Gray)
            }
            Column(modifier = Modifier.weight(1f).clickable { showClickDetails = true }) {
                Text("Product Clicks", fontSize = 11.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${productViews.size}", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(12.dp).padding(start = 4.dp), tint = Color.Gray)
                }
                Text("Product Views", fontSize = 9.sp, color = Color.Gray)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f).clickable { showCartDetails = true }) {
                Text("Add to Cart", fontSize = 11.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${addToCarts.size}", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(12.dp).padding(start = 4.dp), tint = Color.Gray)
                }
                Text("Cart Activity", fontSize = 9.sp, color = Color.Gray)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Conversion", fontSize = 11.sp, color = Color.Gray)
                Text("$searchToOrderRate%", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF4CAF50))
                Text("Search to Order", fontSize = 9.sp, color = Color.Gray)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LocalMiniFunnelStep("Product Discovery (Search)", totalSearches, totalSearches.coerceAtLeast(1))
            LocalMiniFunnelStep("Interest (Product Clicks)", productViews.size, totalSearches.coerceAtLeast(1))
            LocalMiniFunnelStep("Intent (Add to Cart)", addToCarts.size, totalSearches.coerceAtLeast(1))
            LocalMiniFunnelStep("Success (Orders)", totalOrders, totalSearches.coerceAtLeast(1))
        }
    }
}

@Composable
fun SearchDetailsDialog(searches: List<SearchEvent>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Queries", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (searches.isEmpty()) {
                    Text("No search history available", color = Color.Gray)
                } else {
                    LazyColumn {
                        items(searches.sortedByDescending { it.timestamp }) { search ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(search.query, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(search.area, fontSize = 10.sp, color = Color.Gray)
                                }
                                Text(
                                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(search.timestamp)),
                                    fontSize = 11.sp, 
                                    color = Color.Gray
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}

@Composable
fun EventDetailsDialog(title: String, events: List<CustomerActivityEvent>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (events.isEmpty()) {
                    Text("No data available", color = Color.Gray)
                } else {
                    LazyColumn {
                        items(events.sortedByDescending { it.timestamp }) { event ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(event.details, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("User: ${event.customerId.takeLast(6)}", fontSize = 10.sp, color = Color.Gray)
                                }
                                Text(
                                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(event.timestamp)),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}

@Composable
fun LocalMiniFunnelStep(label: String, count: Int, total: Int) {
    val percentage = (count.toFloat() / total * 100).toInt().coerceIn(0, 100)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = Color(0xFFE91E63),
            trackColor = Color(0xFFE91E63).copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun LocalFunnelStepRow(index: Int, step: FunnelStep, allSteps: List<FunnelStep>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(28.dp).background(FStorePurple.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(step.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${step.count}", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                LinearProgressIndicator(
                    progress = { (step.percentage / 100.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (index == allSteps.size - 1) Color(0xFF4CAF50) else FStorePurple,
                    trackColor = Color.LightGray.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
        if (index < allSteps.size - 1) {
            val nextStep = allSteps[index + 1]
            val dropOff = 100 - (nextStep.count.toFloat() / step.count.coerceAtLeast(1) * 100).toInt()
            Text(" ↓ $dropOff% drop-off", fontSize = 9.sp, color = Color.Red.copy(alpha = 0.6f), modifier = Modifier.padding(start = 40.dp))
        }
    }
}
