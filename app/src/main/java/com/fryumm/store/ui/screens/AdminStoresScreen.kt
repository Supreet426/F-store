package com.fryumm.store.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.fryumm.store.data.model.Product
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.ui.components.ProductCard
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.*

fun formatRemainingTime(targetTimeMs: Long?): String {
    if (targetTimeMs == null || targetTimeMs <= 0) return "No Limit"
    val diff = targetTimeMs - System.currentTimeMillis()
    if (diff <= 0) return "Expired / Ended"
    val days = diff / (1000 * 60 * 60 * 24)
    val hours = (diff / (1000 * 60 * 60)) % 24
    val minutes = (diff / (1000 * 60)) % 60
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m left"
        hours > 0 -> "${hours}h ${minutes}m left"
        else -> "${minutes}m left"
    }
}

@Composable
fun PreorderScheduleDialog(
    selectedCount: Int,
    initialEndMs: Long? = null,
    initialCapacity: Int = 100,
    onDismiss: () -> Unit,
    onConfirm: (endMs: Long, capacity: Int) -> Unit
) {
    val context = LocalContext.current
    var selectedEndMs by remember {
        mutableLongStateOf(initialEndMs?.takeIf { it > System.currentTimeMillis() } ?: (System.currentTimeMillis() + (48 * 60 * 60 * 1000L)))
    }
    var capacityText by remember { mutableStateOf(if (initialCapacity > 0) initialCapacity.toString() else "100") }

    val calendar = Calendar.getInstance().apply { timeInMillis = selectedEndMs }

    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedEndMs
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedEndMs = cal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val timePickerDialog = remember {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedEndMs
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                selectedEndMs = cal.timeInMillis
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
    }

    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedEndMs))
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(selectedEndMs))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = FStorePurple)
                Spacer(Modifier.width(8.dp))
                Text("Book Now", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Set exact countdown end date, time & capacity for $selectedCount product(s):",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                // Quick Presets
                Text("Quick Presets:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { selectedEndMs = System.currentTimeMillis() + (24 * 60 * 60 * 1000L) },
                        label = { Text("+24h", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { selectedEndMs = System.currentTimeMillis() + (48 * 60 * 60 * 1000L) },
                        label = { Text("+48h", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { selectedEndMs = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L) },
                        label = { Text("+3 Days", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { selectedEndMs = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L) },
                        label = { Text("+7 Days", fontSize = 10.sp) }
                    )
                }

                // Date & Time Picker Buttons
                Text("Select Exact Date & Time:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(dateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { timePickerDialog.show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(timeStr, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it.filter { char -> char.isDigit() } },
                    label = { Text("Max Order Capacity (Units)") },
                    placeholder = { Text("e.g. 50, 100, 500 (0 for unlimited)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = FStorePurple.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Preorder Countdown Ends On:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                        Text("$dateStr at $timeStr (${formatRemainingTime(selectedEndMs)})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cap = capacityText.toIntOrNull() ?: 0
                    onConfirm(selectedEndMs, cap)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
            ) {
                Text("SAVE DATE & TIME", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun ScheduledSaleDialog(
    selectedCount: Int,
    initialStartMs: Long? = null,
    initialEndMs: Long? = null,
    initialDiscountPercent: Double = 20.0,
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long, endMs: Long, discountPercent: Double) -> Unit
) {
    val context = LocalContext.current
    var selectedStartMs by remember {
        mutableLongStateOf(initialStartMs?.takeIf { it >= System.currentTimeMillis() } ?: System.currentTimeMillis())
    }
    var selectedEndMs by remember {
        mutableLongStateOf(initialEndMs?.takeIf { it > System.currentTimeMillis() } ?: (System.currentTimeMillis() + (24 * 60 * 60 * 1000L)))
    }
    var discountText by remember { mutableStateOf(if (initialDiscountPercent > 0) initialDiscountPercent.toString() else "20") }

    val startCalendar = Calendar.getInstance().apply { timeInMillis = selectedStartMs }
    val endCalendar = Calendar.getInstance().apply { timeInMillis = selectedEndMs }

    val startDatePicker = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedStartMs
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedStartMs = cal.timeInMillis
                if (selectedEndMs <= selectedStartMs) selectedEndMs = selectedStartMs + (24 * 60 * 60 * 1000L)
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val startTimePicker = remember {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedStartMs
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                selectedStartMs = cal.timeInMillis
                if (selectedEndMs <= selectedStartMs) selectedEndMs = selectedStartMs + (24 * 60 * 60 * 1000L)
            },
            startCalendar.get(Calendar.HOUR_OF_DAY),
            startCalendar.get(Calendar.MINUTE),
            false
        )
    }

    val endDatePicker = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedEndMs
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedEndMs = cal.timeInMillis
            },
            endCalendar.get(Calendar.YEAR),
            endCalendar.get(Calendar.MONTH),
            endCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val endTimePicker = remember {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedEndMs
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                selectedEndMs = cal.timeInMillis
            },
            endCalendar.get(Calendar.HOUR_OF_DAY),
            endCalendar.get(Calendar.MINUTE),
            false
        )
    }

    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFE65100))
                Spacer(Modifier.width(8.dp))
                Text("Create New Deal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Set discount & exact sale schedule for $selectedCount product(s):",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it },
                    label = { Text("Discount Percentage (%)") },
                    placeholder = { Text("e.g. 10, 20, 50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Sale Start Picker
                Text("Sale Starts On:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { startDatePicker.show() }, modifier = Modifier.weight(1f)) {
                        Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedStartMs)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { startTimePicker.show() }, modifier = Modifier.weight(1f)) {
                        Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(selectedStartMs)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Sale End Picker
                Text("Sale Ends On:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { endDatePicker.show() }, modifier = Modifier.weight(1f)) {
                        Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedEndMs)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { endTimePicker.show() }, modifier = Modifier.weight(1f)) {
                        Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(selectedEndMs)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Sale Duration: ${sdf.format(Date(selectedStartMs))} ➔ ${sdf.format(Date(selectedEndMs))}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val disc = discountText.toDoubleOrNull() ?: 0.0
                    onConfirm(selectedStartMs, selectedEndMs, disc)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Text("LAUNCH SCHEDULED SALE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminStoresScreen(
    viewModel: StoreViewModel, 
    navController: NavHostController,
    externalSearchQuery: String = ""
) {
    val allProducts = viewModel.products
    val currentTime = System.currentTimeMillis()

    // Auto Status & Expired Timer calculation
    val liveProducts = allProducts.map { product ->
        var updatedProduct = product

        // Auto Out Of Stock logic
        if (product.isInStock && product.stockQuantity <= 0) {
            updatedProduct = updatedProduct.copy(isInStock = false)
        }

        // Auto Preorder complete -> Active
        if (product.isPreorderEnabled && product.preorderEnd != null && product.preorderEnd < currentTime) {
            updatedProduct = updatedProduct.copy(isPreorderEnabled = false)
        }

        // Auto Flash Sale complete -> Normal Active
        if (product.flashSale && product.saleEnd != null && product.saleEnd < currentTime) {
            updatedProduct = updatedProduct.copy(flashSale = false)
        }
        
        updatedProduct
    }

    // Approved Vault = Master Repository of verified products
    val approvedProducts = liveProducts.filter { it.isVerified && (it.finalApproval || it.publishingStatus == "PUBLISHED") }
    
    // Customer-Facing Filtered Views (Requires isAvailable == true to be live on app)
    val publishedProducts = approvedProducts.filter { it.isAvailable && !it.isComingSoon && !it.isPreorderEnabled && !it.flashSale && it.isInStock }
    val comingSoonProducts = approvedProducts.filter { it.isAvailable && it.isComingSoon }
    val bookNowProducts = approvedProducts.filter { it.isAvailable && it.isPreorderEnabled && !it.isComingSoon }
    val scheduledSalesProducts = approvedProducts.filter { it.isAvailable && it.flashSale && !it.isComingSoon }
    val outOfStockProducts = approvedProducts.filter { it.isAvailable && !it.isInStock && !it.isComingSoon && !it.isPreorderEnabled && !it.flashSale }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Approved", "Published", "Coming Soon", "Book Now", "Scheduled Sales", "Out of Stock")

    val currentList = when (selectedTabIndex) {
        0 -> approvedProducts
        1 -> publishedProducts
        2 -> comingSoonProducts
        3 -> bookNowProducts
        4 -> scheduledSalesProducts
        else -> outOfStockProducts
    }

    // Selection & Bulk Schedule Dialog States
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedProductIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkActionMenu by remember { mutableStateOf(false) }
    var showPreorderDialog by remember { mutableStateOf(false) }
    var showScheduledSaleDialog by remember { mutableStateOf(false) }

    // Direct Target for Single Item Edit Schedule
    var singleTargetProductId by remember { mutableStateOf<String?>(null) }

    val searchQuery = externalSearchQuery

    val filteredList = if (searchQuery.isNotEmpty()) {
        currentList.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.brand.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    } else {
        currentList
    }

    val categoryOrder = viewModel.dynamicAppConfig.categorySequence

    // Group by Category & Sort Products inside each Category by sequenceNumber
    val groupedProducts = filteredList
        .groupBy { it.category.ifEmpty { "Uncategorized" } }
        .mapValues { (_, prods) -> prods.sortedBy { it.sequenceNumber } }
        .toList()
        .sortedBy { (category, _) -> 
            val idx = categoryOrder.indexOf(category)
            if (idx >= 0) idx else 999
        }

    // Reset selection if tab changes
    LaunchedEffect(selectedTabIndex) {
        isSelectionMode = false
        selectedProductIds = emptySet()
    }

    if (showPreorderDialog) {
        val targets = if (singleTargetProductId != null) listOf(singleTargetProductId!!) else selectedProductIds.toList()
        val targetProduct = allProducts.find { it.id == singleTargetProductId }
        PreorderScheduleDialog(
            selectedCount = targets.size,
            initialEndMs = targetProduct?.preorderEnd,
            initialCapacity = targetProduct?.maxPreorderCapacity ?: 100,
            onDismiss = {
                showPreorderDialog = false
                singleTargetProductId = null
            },
            onConfirm = { endMs, capacity ->
                val startMs = System.currentTimeMillis()
                viewModel.updateProductsStatus(
                    targets,
                    mapOf(
                        "isAvailable" to true,
                        "isPreorderEnabled" to true,
                        "isComingSoon" to false,
                        "flashSale" to false,
                        "preorderStart" to startMs,
                        "preorderEnd" to endMs,
                        "maxPreorderCapacity" to capacity,
                        "isInStock" to true
                    )
                )
                showPreorderDialog = false
                singleTargetProductId = null
                isSelectionMode = false
                selectedProductIds = emptySet()
            }
        )
    }

    if (showScheduledSaleDialog) {
        val targets = if (singleTargetProductId != null) listOf(singleTargetProductId!!) else selectedProductIds.toList()
        val targetProduct = allProducts.find { it.id == singleTargetProductId }
        ScheduledSaleDialog(
            selectedCount = targets.size,
            initialStartMs = targetProduct?.saleStart,
            initialEndMs = targetProduct?.saleEnd,
            initialDiscountPercent = targetProduct?.discountPercent ?: 20.0,
            onDismiss = {
                showScheduledSaleDialog = false
                singleTargetProductId = null
            },
            onConfirm = { startMs, endMs, discountPercent ->
                viewModel.updateProductsStatus(
                    targets,
                    mapOf(
                        "isAvailable" to true,
                        "flashSale" to true,
                        "isComingSoon" to false,
                        "isPreorderEnabled" to false,
                        "saleStart" to startMs,
                        "saleEnd" to endMs,
                        "discountPercent" to discountPercent,
                        "isInStock" to true
                    )
                )
                showScheduledSaleDialog = false
                singleTargetProductId = null
                isSelectionMode = false
                selectedProductIds = emptySet()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 3.dp
        ) {
            Column {
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FStorePurple.copy(alpha = 0.1f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${selectedProductIds.size} Selected", 
                            color = FStorePurple, 
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            TextButton(onClick = {
                                if (selectedProductIds.size == filteredList.size) {
                                    selectedProductIds = emptySet()
                                } else {
                                    selectedProductIds = filteredList.map { it.id }.toSet()
                                }
                            }) {
                                Text(if (selectedProductIds.size == filteredList.size) "Deselect All" else "Select All")
                            }
                            Box {
                                IconButton(onClick = { showBulkActionMenu = true }) {
                                    Icon(Icons.Default.MoreVert, "Bulk Actions")
                                }
                                DropdownMenu(
                                    expanded = showBulkActionMenu,
                                    onDismissRequest = { showBulkActionMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("🚀 Published") },
                                        onClick = {
                                            viewModel.updateProductsStatus(
                                                selectedProductIds.toList(),
                                                mapOf("isAvailable" to true, "isComingSoon" to false, "isPreorderEnabled" to false, "flashSale" to false, "isInStock" to true)
                                            )
                                            showBulkActionMenu = false
                                            isSelectionMode = false
                                            selectedProductIds = emptySet()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("⏳ Coming Soon") },
                                        onClick = {
                                            viewModel.updateProductsStatus(
                                                selectedProductIds.toList(),
                                                mapOf("isAvailable" to true, "isComingSoon" to true, "isPreorderEnabled" to false, "flashSale" to false)
                                            )
                                            showBulkActionMenu = false
                                            isSelectionMode = false
                                            selectedProductIds = emptySet()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("⏱️ Book Now") },
                                        onClick = {
                                            showBulkActionMenu = false
                                            showPreorderDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("⚡ Create New Deal") },
                                        onClick = {
                                            showBulkActionMenu = false
                                            showScheduledSaleDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("📦 Out of Stock") },
                                        onClick = {
                                            viewModel.updateProductsStatus(
                                                selectedProductIds.toList(),
                                                mapOf("isAvailable" to true, "isInStock" to false)
                                            )
                                            showBulkActionMenu = false
                                            isSelectionMode = false
                                            selectedProductIds = emptySet()
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Stage in Vault (Hide from App)", color = Color.Red) },
                                        onClick = {
                                            viewModel.updateProductsStatus(
                                                selectedProductIds.toList(),
                                                mapOf("isAvailable" to false)
                                            )
                                            showBulkActionMenu = false
                                            isSelectionMode = false
                                            selectedProductIds = emptySet()
                                        }
                                    )
                                }
                            }
                            TextButton(onClick = {
                                isSelectionMode = false
                                selectedProductIds = emptySet()
                            }) {
                                Text("Cancel", color = Color.Red)
                            }
                        }
                    }
                }
                
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = FStorePurple,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = FStorePurple
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = title, 
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                ) 
                            }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No products found in this section.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(filteredList, isSelectionMode) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val hitItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        offset.y >= item.offset && offset.y <= item.offset + item.size
                                    }
                                    val key = hitItem?.key as? String
                                    if (key != null && !key.startsWith("cat_")) {
                                        isSelectionMode = true
                                        if (!selectedProductIds.contains(key)) {
                                            selectedProductIds = selectedProductIds + key
                                        }
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val currentY = change.position.y
                                    val hitItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        currentY >= item.offset && currentY <= item.offset + item.size
                                    }
                                    val key = hitItem?.key as? String
                                    if (key != null && !key.startsWith("cat_")) {
                                        isSelectionMode = true
                                        if (!selectedProductIds.contains(key)) {
                                            selectedProductIds = selectedProductIds + key
                                        }
                                    }

                                    // Auto-scroll logic when dragging near top or bottom edges
                                    val containerHeight = lazyListState.layoutInfo.viewportSize.height
                                    val topThreshold = 120.dp.toPx()
                                    val bottomThreshold = containerHeight - 120.dp.toPx()

                                    if (currentY < topThreshold) {
                                        val scrollAmount = (topThreshold - currentY).coerceIn(10f, 60f)
                                        coroutineScope.launch {
                                            lazyListState.scrollBy(-scrollAmount)
                                        }
                                    } else if (currentY > bottomThreshold) {
                                        val scrollAmount = (currentY - bottomThreshold).coerceIn(10f, 60f)
                                        coroutineScope.launch {
                                            lazyListState.scrollBy(scrollAmount)
                                        }
                                    }
                                },
                                onDragEnd = {},
                                onDragCancel = {}
                            )
                        },
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedProducts.forEach { (category, products) ->
                        stickyHeader(key = "cat_$category") {
                            Surface(
                                color = FStorePurple.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = category,
                                    color = FStorePurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                        
                        itemsIndexed(products, key = { _, product -> product.id }) { index, product ->
                            val isSelected = selectedProductIds.contains(product.id)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) FStorePurple.copy(alpha = 0.12f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedProductIds = if (isSelected) {
                                                    val newSet = selectedProductIds - product.id
                                                    if (newSet.isEmpty()) isSelectionMode = false
                                                    newSet
                                                } else {
                                                    selectedProductIds + product.id
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            isSelectionMode = true
                                            selectedProductIds = if (isSelected) {
                                                val newSet = selectedProductIds - product.id
                                                if (newSet.isEmpty()) isSelectionMode = false
                                                newSet
                                            } else {
                                                selectedProductIds + product.id
                                            }
                                        }
                                    )
                            ) {
                                Column {
                                    // Status & Schedule Banner
                                    if (selectedTabIndex == 0) { // Approved Vault View
                                        Surface(
                                            color = if (!product.isAvailable) Color(0xFFEEEEEE) else when {
                                                product.flashSale -> Color(0xFFFFF3E0)
                                                product.isPreorderEnabled -> FStorePurple.copy(alpha = 0.1f)
                                                product.isComingSoon -> Color(0xFFFFF8E1)
                                                product.isInStock -> Color(0xFFE8F5E9)
                                                else -> Color(0xFFFFEBEE)
                                            },
                                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (!product.isAvailable) "● STAGED IN VAULT (HIDDEN FROM APP)" else when {
                                                        product.flashSale -> "● LIVE ON APP (FLASH SALE: ${product.discountPercent}% OFF)"
                                                        product.isPreorderEnabled -> "● LIVE ON APP (BOOK NOW PREORDER)"
                                                        product.isComingSoon -> "● LIVE ON APP (COMING SOON)"
                                                        product.isInStock -> "● LIVE ON APP (PUBLISHED)"
                                                        else -> "● LIVE ON APP (OUT OF STOCK)"
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (!product.isAvailable) Color.Gray else when {
                                                        product.flashSale -> Color(0xFFE65100)
                                                        product.isPreorderEnabled -> FStorePurple
                                                        product.isComingSoon -> Color(0xFFF57F17)
                                                        product.isInStock -> Color(0xFF2E7D32)
                                                        else -> Color.Red
                                                    }
                                                )
                                                
                                                Text(
                                                    if (product.isAvailable) "LIVE ON APP" else "STAGED IN VAULT",
                                                    fontSize = 10.sp,
                                                    color = if (product.isAvailable) Color(0xFF2E7D32) else Color.Gray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else if (product.isPreorderEnabled || product.flashSale || product.isComingSoon) {
                                        Surface(
                                            color = when {
                                                product.flashSale -> Color(0xFFFFF3E0)
                                                product.isPreorderEnabled -> FStorePurple.copy(alpha = 0.1f)
                                                else -> Color(0xFFFFF8E1)
                                            },
                                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (product.flashSale) Icons.Default.Bolt else Icons.Default.Timer,
                                                        contentDescription = null,
                                                        tint = if (product.flashSale) Color(0xFFE65100) else FStorePurple,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = when {
                                                            product.flashSale -> "FLASH SALE (${product.discountPercent}% OFF) • ${formatRemainingTime(product.saleEnd)}"
                                                            product.isPreorderEnabled -> "PREORDER (BOOK NOW) • ${formatRemainingTime(product.preorderEnd)}"
                                                            else -> "COMING SOON"
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = if (product.flashSale) Color(0xFFE65100) else FStorePurple
                                                    )
                                                }

                                                TextButton(
                                                    onClick = {
                                                        singleTargetProductId = product.id
                                                        if (product.flashSale) {
                                                            showScheduledSaleDialog = true
                                                        } else {
                                                            showPreorderDialog = true
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Edit Timer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    ProductCard(
                                        product = product,
                                        viewModel = viewModel,
                                        serialNumber = index + 1,
                                        onEditClick = {
                                            navController.navigate("edit_product/${product.id}")
                                        },
                                        onReorder = { newSerial ->
                                            val targetIndex = (newSerial - 1).coerceIn(0, products.size - 1)
                                            val newList = products.toMutableList()
                                            val item = newList.removeAt(index)
                                            newList.add(targetIndex, item)
                                            viewModel.bulkUpdateProductSequences(newList)
                                        },
                                        onSchedulePreorder = {
                                            singleTargetProductId = product.id
                                            showPreorderDialog = true
                                        },
                                        onScheduleSale = {
                                            singleTargetProductId = product.id
                                            showScheduledSaleDialog = true
                                        }
                                    )
                                }
                                
                                if (isSelected) {
                                    Surface(
                                        color = FStorePurple,
                                        shape = CircleShape,
                                        shadowElevation = 3.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.padding(3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
