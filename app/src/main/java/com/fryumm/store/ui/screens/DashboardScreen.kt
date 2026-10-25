package com.fryumm.store.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.ui.components.OrderCard
import com.fryumm.store.ui.theme.FStorePurple
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardStatItem(label: String, value: String, color: Color, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray, maxLines = 1, softWrap = false)
    }
}

@Composable
fun ImpactScoreCard(label: String, score: Double, icon: ImageVector, modifier: Modifier) {
    val color = when {
        score >= 7.0 -> Color(0xFFD32F2F)
        score >= 4.0 -> Color(0xFFF57C00)
        else -> Color(0xFF388E3C)
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, fontSize = 10.sp, color = Color.Gray)
                Text(
                    text = if (score > 0) String.format(Locale.getDefault(), "%.1f/10", score) else "Normal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDashboard(navController: NavHostController, viewModel: StoreViewModel, onStartScan: (String, String) -> Unit) {
    val userProfile = viewModel.currentUserProfile
    val isStoreOwner = userProfile?.userRole == UserRole.STORE_OWNER
    
    var selectedFilter by remember { mutableIntStateOf(0) }
    val filters = listOf("Pending", "Ongoing", "History")
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Total") }
    var selectedAreas by remember { mutableStateOf(setOf("All")) }
    var dateFilterMode by remember { mutableStateOf("All Time") }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    
    var areaExpanded by remember { mutableStateOf(false) }
    var dateExpanded by remember { mutableStateOf(false) }

    val allAreas = listOf("All") + viewModel.getUniqueAreas()
    val dateOptions = listOf("Today", "Yesterday", "This Week", "This Month", "This Year", "Custom", "All Time")

    val stats = viewModel.getStoreStats()
    
    // Date calculation logic
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

    val filteredOrders = viewModel.orders.filter { order ->
        val inDateRange = order.timestamp in dateRange
        val inArea = if (selectedAreas.contains("All")) true else selectedAreas.contains(order.area)
        val inStatus = if (selectedStatus != "Total") {
            when(selectedStatus) {
                "Pending" -> order.status == OrderStatus.PLACED
                "Packed" -> order.status == OrderStatus.READY || order.status == OrderStatus.SEALED
                "Out for Delivery" -> order.status == OrderStatus.DISPATCHED || order.status == OrderStatus.ASSIGNED
                "Delivered" -> order.status == OrderStatus.DELIVERED
                "Cancelled" -> order.status == OrderStatus.CANCELLED
                "Returned" -> order.status == OrderStatus.RETURNED
                else -> true
            }
        } else {
            when (selectedFilter) {
                0 -> order.status == OrderStatus.PLACED
                1 -> order.status in listOf(
                    OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.PICKING_ASSIGNED,
                    OrderStatus.PICKING, OrderStatus.PICKED, OrderStatus.READY, 
                    OrderStatus.ASSIGNED, OrderStatus.SEALED, OrderStatus.BAGGED, 
                    OrderStatus.VERIFYING, OrderStatus.DISPATCHED
                )
                2 -> order.status in listOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.RETURNED)
                else -> true
            }
        }
        val matchesSearch = if (searchQuery.isEmpty()) true else {
            order.id.contains(searchQuery, ignoreCase = true) || 
            (!isStoreOwner && (order.customerName.contains(searchQuery, ignoreCase = true) || order.phoneNumber.contains(searchQuery)))
        }
        inDateRange && inArea && inStatus && matchesSearch
    }.sortedByDescending { it.timestamp }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("DONE") } }
        ) {
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }

    val storeConfig = viewModel.storeConfig
    val isStoreOpen = storeConfig.isOpen

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Store Status Banner Item
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isStoreOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isStoreOpen) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isStoreOpen) "STORE IS ONLINE" else "STORE IS OFFLINE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStoreOpen) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Text(
                                text = if (isStoreOpen) "Accepting new orders" else "Taking orders paused",
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                    Switch(
                        checked = isStoreOpen,
                        onCheckedChange = { isOpen ->
                            viewModel.updateStoreConfig(storeConfig.copy(isOpen = isOpen))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4CAF50),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.Gray
                        )
                    )
                }
            }
        }

        // Impact Scores (Weather & Events)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImpactScoreCard(
                    label = "Weather Impact",
                    score = viewModel.simulatedWeatherImpact,
                    icon = Icons.Default.Cloud,
                    modifier = Modifier.weight(1f)
                )
                ImpactScoreCard(
                    label = "Event Surge",
                    score = viewModel.simulatedEventImpact,
                    icon = Icons.Default.Celebration,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column {
                // Top Search Bar & Notification Icon Row (Single Search Button & Notification Icon on right side of top title/search area)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        placeholder = { Text("Search ID, Name or Phone...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f))
                    )

                    Spacer(Modifier.width(12.dp))

                    IconButton(
                        onClick = { navController.navigate("promotion") },
                        modifier = Modifier
                            .size(40.dp)
                            .background(FStorePurple.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notifications",
                            tint = FStorePurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Compact Header Info & Batch Picking
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredOrders.size} ${filters[selectedFilter]}", 
                        fontSize = 14.sp, 
                        color = Color.Gray, 
                        fontWeight = FontWeight.Bold
                    )
                    
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { navController.navigate("batch_picking") }
                    ) {
                        Text("Batch Picking", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                // Stats Row (Always visible, acts as quick filter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardStatItem("Total", stats.totalOrders.toString(), FStorePurple) {
                        selectedStatus = "Total"
                    }
                    DashboardStatItem("Pending", stats.pendingOrders.toString(), Color(0xFFFF9800)) {
                        selectedStatus = "Pending"
                    }
                    DashboardStatItem("Packed", stats.packedOrders.toString(), Color(0xFF9C27B0)) {
                        selectedStatus = "Packed"
                    }
                    DashboardStatItem("Out for Delivery", stats.outForDeliveryOrders.toString(), Color(0xFF2196F3)) {
                        selectedStatus = "Out for Delivery"
                    }
                    DashboardStatItem("Delivered", stats.deliveredOrders.toString(), Color(0xFF4CAF50)) {
                        selectedStatus = "Delivered"
                    }
                    DashboardStatItem("Cancelled", stats.cancelledOrders.toString(), Color.Red) {
                        selectedStatus = "Cancelled"
                    }
                    DashboardStatItem("Returned", stats.returnedOrders.toString(), Color.Gray) {
                        selectedStatus = "Returned"
                    }
                }

                // Area & Date Filters Row
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { areaExpanded = true }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (selectedAreas.contains("All")) "Areas" else "Areas (${selectedAreas.size})", fontSize = 10.sp)
                            }
                            DropdownMenu(expanded = areaExpanded, onDismissRequest = { areaExpanded = false }) {
                                allAreas.forEach { area ->
                                    DropdownMenuItem(
                                        text = { Row { Checkbox(checked = selectedAreas.contains(area), onCheckedChange = null); Text(area) } },
                                        onClick = { 
                                            selectedAreas = if (area == "All") setOf("All") else {
                                                val newSet = selectedAreas.toMutableSet()
                                                if (newSet.contains("All")) newSet.remove("All")
                                                if (newSet.contains(area)) newSet.remove(area) else newSet.add(area)
                                                if (newSet.isEmpty()) setOf("All") else newSet
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { dateExpanded = true }, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.Event, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(dateFilterMode, fontSize = 10.sp)
                            }
                            DropdownMenu(expanded = dateExpanded, onDismissRequest = { dateExpanded = false }) {
                                dateOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        dateFilterMode = option
                                        dateExpanded = false
                                        if (option == "Custom") showDatePicker = true
                                    })
                                }
                            }
                        }
                    }
                }

                // Standard Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedFilter,
                    containerColor = Color.White,
                    contentColor = FStorePurple,
                    edgePadding = 16.dp,
                    divider = { HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f)) }
                ) {
                    filters.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedFilter == index,
                            onClick = { selectedFilter = index; selectedStatus = "Total" },
                            text = { 
                                Text(
                                    title, 
                                    fontWeight = if (selectedFilter == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                ) 
                            }
                        )
                    }
                }
            }
        }
        
        if (filteredOrders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.LightGray.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val isFiltered = searchQuery.isNotEmpty() || selectedAreas.size > 1 || selectedAreas.first() != "All" || selectedStatus != "Total" || dateFilterMode != "All Time"
                        Text(
                            text = if (isFiltered) "No Matching Orders Found" else "No Orders Right Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.DarkGray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isFiltered) "Try adjusting search terms or date/area filters" else "New customer orders will appear here automatically",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            if (selectedFilter == 2) {
                // History Summary Metrics
                val totalRevenue = filteredOrders.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.totalAmount }
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Delivered Revenue", fontSize = 10.sp, color = Color.Gray)
                                Text("₹${totalRevenue.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                            }
                        }
                        Card(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Success Rate", fontSize = 10.sp, color = Color.Gray)
                                val rate = if(filteredOrders.isNotEmpty()) (filteredOrders.count { it.status == OrderStatus.DELIVERED }.toFloat() / filteredOrders.size * 100).toInt() else 100
                                Text("$rate%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = FStorePurple)
                            }
                        }
                    }
                }
                
                items(filteredOrders) { order ->
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        com.fryumm.store.ui.components.HistoryLogItem(order, viewModel)
                    }
                }
            } else {
                items(filteredOrders, key = { it.id }) { order ->
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        OrderCard(
                            order = order,
                            viewModel = viewModel,
                            onStartScan = onStartScan,
                            showDetails = true,
                            onDetailClick = { navController.navigate("order_details/$it") }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
