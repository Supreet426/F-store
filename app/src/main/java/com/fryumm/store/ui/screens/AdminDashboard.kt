package com.fryumm.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(viewModel: StoreViewModel, navController: NavHostController) {
    val userProfile = viewModel.currentUserProfile
    val stats = viewModel.getCEODashboardStats()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState()
    var customRevenue by remember { mutableStateOf<Double?>(null) }
    var dateRangeText by remember { mutableStateOf("Select Date Range") }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = datePickerState.selectedStartDateMillis
                    val end = datePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        // End date from picker is midnight of the selected day.
                        // We add almost 24h to include the full selected end day.
                        val fullEnd = end + (24 * 60 * 60 * 1000) - 1
                        customRevenue = viewModel.getRevenueForRange(start, fullEnd)
                        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                        dateRangeText = "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
                    }
                    showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = datePickerState,
                modifier = Modifier.height(400.dp)
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Row
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Welcome back,", fontSize = 12.sp, color = Color.Gray)
                Text(userProfile?.name ?: "Admin", fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
        // Top Row: Stores, Users, FY Revenue
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FStorePurple),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Revenue (FY 2024-25)", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text("₹${stats.totalRevenueFY}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Stores", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text("${stats.totalStores}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Total Users", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text("${stats.totalUsers}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        EcosystemStatItem("Total Orders", stats.totalOrders.toString(), FStorePurple)
                        EcosystemStatItem("Pending", stats.pendingOrders.toString(), Color(0xFFFF9800))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        EcosystemStatItem("Packed", stats.packedOrders.toString(), Color(0xFF9C27B0))
                        EcosystemStatItem("Out for Delivery", stats.outForDeliveryOrders.toString(), Color(0xFF2196F3))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        EcosystemStatItem("Delivered", stats.deliveredOrders.toString(), Color(0xFF4CAF50))
                        EcosystemStatItem("Cancelled", stats.cancelledOrders.toString(), Color.Red)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        EcosystemStatItem("Returned", stats.returnedOrders.toString(), Color.DarkGray)
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Revenue Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RevenueSmallCard("Today", "₹${stats.todayRevenue}", Color(0xFF4CAF50), Modifier.weight(1f))
                RevenueSmallCard("Yesterday", "₹${stats.yesterdayRevenue}", Color(0xFFFF9800), Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RevenueSmallCard("Weekly", "₹${stats.weeklyRevenue}", Color(0xFF2196F3), Modifier.weight(1f))
                RevenueSmallCard("Monthly", "₹${stats.monthlyRevenue}", Color(0xFF9C27B0), Modifier.weight(1f))
            }
        }

        // Custom Revenue Picker
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null, tint = FStorePurple)
                        Spacer(Modifier.width(8.dp))
                        Text("Custom Period Revenue", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(dateRangeText)
                    }
                    if (customRevenue != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Revenue: ₹$customRevenue",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = FStorePurple,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        item {
            val lowStockCount = viewModel.lowStockProducts.size
            AdminQuickAction(
                "Inventory Alerts",
                if (lowStockCount > 0) "$lowStockCount items are low on stock!" else "All stores are well-stocked",
                if (lowStockCount > 0) Icons.Default.Warning else Icons.Default.Inventory,
                onClick = { navController.navigate("inventory_alerts") }
            )
        }

        item {
            AdminQuickAction(
                "Store Network",
                "Manage Dark Store Hubs & Radius",
                Icons.Default.Hub,
                onClick = { navController.navigate("dark_stores") }
            )
        }

        item {
            AdminQuickAction(
                "Merchant Directory",
                "Manage connected merchants",
                Icons.Default.Storefront,
                onClick = { navController.navigate("stores") }
            )
        }

        item {
            AdminQuickAction(
                "Business Intelligence",
                "Analysis, User Directory & Live Searches",
                Icons.Default.Insights,
                onClick = { navController.navigate("users") }
            )
        }

        item {
            AdminQuickAction(
                "App UI Management",
                "Banners, Promoted Brands & Rankings",
                Icons.Default.SettingsSuggest,
                onClick = { navController.navigate("app_management") }
            )
        }
    }
}

@Composable
fun EcosystemStatItem(label: String, value: String, color: Color) {
    Column(modifier = Modifier.width(140.dp)) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun RevenueSmallCard(title: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun AdminStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AdminQuickAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.LightGray.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(32.dp), tint = FStorePurple)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}
