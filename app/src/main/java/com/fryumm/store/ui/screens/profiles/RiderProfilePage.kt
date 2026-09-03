package com.fryumm.store.ui.screens.profiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.data.model.RiderPerformance
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.ui.components.StatusBadge
import com.fryumm.store.ui.theme.FStorePurple
import java.util.*

@Composable
fun RiderProfilePage(
    profile: UserProfile,
    performance: RiderPerformance?,
    viewModel: StoreViewModel,
    onBack: () -> Unit,
    onStatusChange: (Boolean) -> Unit
) {
    val p = performance ?: RiderPerformance()
    
    val assignedOrders = viewModel.orders.filter { it.deliveryAgentId == profile.uid && it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED }
    val availableOrders = viewModel.orders.filter { it.deliveryAgentId == null && (it.status == OrderStatus.READY || it.status == OrderStatus.SEALED) }

    // Delivery volume trend (dummy data for visual representation if real history not available)
    val trendData = listOf(4f, 6f, 3f, 8f, 5f, 7f, 9f)

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        ProfileHeader(
            name = profile.name,
            subTitle = profile.email,
            tag = "DELIVERY PARTNER",
            onBack = onBack,
            backgroundColor = FStorePurple,
            photoUrl = profile.photoUrl
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionTitle("Live Tracking")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = FStorePurple, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Current Location: ${if (profile.latitude != null) "Active on Map" else "Main Hub"}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Last updated: Just now", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            item {
                SectionTitle("Assigned Orders (${assignedOrders.size})")
                if (assignedOrders.isEmpty()) {
                    Text("No active orders assigned currently.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        assignedOrders.forEach { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9).copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(order.area, fontSize = 11.sp)
                                    }
                                    StatusBadge(order.status)
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("Available for Assignment")
                if (availableOrders.isEmpty()) {
                    Text("No pending orders available for dispatch.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableOrders.forEach { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(order.area, fontSize = 11.sp, color = FStorePurple)
                                    }
                                    Button(
                                        onClick = { viewModel.assignOrderToRider(order.id, profile.uid) },
                                        colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Text("Assign to Him", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("Delivery Performance")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Deliveries", p.totalDeliveries.toString(), Icons.Default.LocalShipping, FStorePurple, Modifier.weight(1f))
                    StatCard("Earnings", "₹${p.totalEarnings.toInt()}", Icons.Default.AccountBalanceWallet, Color(0xFF4CAF50), Modifier.weight(1f))
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Delivery Volume Analysis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        SimpleLineChart(data = trendData, color = FStorePurple)
                        Spacer(Modifier.height(8.dp))
                        Text("Efficiency Score: ${(p.onTimePercentage).toInt()}% On-Time Delivery", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Rating", String.format(Locale.getDefault(), "%.1f ★", p.avgRating), Icons.Default.Star, Color(0xFFFFC107), Modifier.weight(1f))
                    StatCard("Distance", "${p.totalDistanceKm.toInt()} km", Icons.Default.Route, Color(0xFF607D8B), Modifier.weight(1f))
                }
            }

            item {
                SectionTitle("Account Settings")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Login Permissions", fontWeight = FontWeight.Medium)
                            Text("Allow rider to access the app", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(checked = profile.active, onCheckedChange = { onStatusChange(it) })
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
