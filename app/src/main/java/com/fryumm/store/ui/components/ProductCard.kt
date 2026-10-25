package com.fryumm.store.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.Product
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProductDetailDialog(
    product: Product,
    viewModel: StoreViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(product.addedAt))
    val expiryStr = product.expiryDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "Category: ${product.category}${if (product.subCategory.isNotBlank()) " > ${product.subCategory}" else ""}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product Image Banner
                if (product.imageUrl.isNotBlank() || product.images.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = product.imageUrl.ifEmpty { product.images.firstOrNull() ?: "" },
                            contentDescription = product.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Status Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (product.isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (product.isAvailable) "● LIVE" else "● HIDDEN",
                            color = if (product.isAvailable) Color(0xFF2E7D32) else Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = if (product.isVerified) Color(0xFFE3F2FD) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (product.isVerified) "VERIFIED" else "PENDING APPROVAL",
                            color = if (product.isVerified) Color(0xFF1565C0) else Color(0xFFE65100),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (product.isFoodProduct) {
                        Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "FOOD ITEM",
                                color = Color(0xFFE65100),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (product.isComingSoon) {
                        Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "COMING SOON",
                                color = Color(0xFFF57F17),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Description
                if (product.shortDescription.isNotBlank()) {
                    Text(
                        text = product.shortDescription,
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (product.description.isNotBlank()) {
                    Surface(
                        color = Color(0xFFFAFAFA),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Text(product.description, fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }

                if (product.specifications.isNotEmpty()) {
                    Text("Specifications", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FStorePurple)
                    ProductSpecificationsTable(product.specifications)
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                // Pricing Details
                Text("Pricing & Economics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FStorePurple)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Selling Price", fontSize = 11.sp, color = Color.Gray)
                        Text("₹${product.price}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = FStorePurple)
                    }
                    if (product.originalPrice > product.price) {
                        Column {
                            Text("MRP", fontSize = 11.sp, color = Color.Gray)
                            Text("₹${product.originalPrice}", fontWeight = FontWeight.Bold, fontSize = 15.sp, style = TextStyle(textDecoration = TextDecoration.LineThrough), color = Color.Gray)
                        }
                    }
                    Column {
                        Text("Base Price", fontSize = 11.sp, color = Color.Gray)
                        Text("₹${product.basePrice}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Merchant Rate", fontSize = 11.sp, color = Color.Gray)
                        Text("₹${product.merchantRate}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFE65100))
                    }
                }

                // Tax & GST Details
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GST Tax Rate: ${product.taxRate}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            if (product.hsnCode.isNotBlank()) {
                                Text("HSN: ${product.hsnCode}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            }
                        }
                        if (product.taxRate > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text("CGST: ${product.cgstRate.takeIf { it > 0 } ?: (product.taxRate / 2)}% | SGST: ${product.sgstRate.takeIf { it > 0 } ?: (product.taxRate / 2)}%", fontSize = 10.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                // Inventory & Logistics
                Text("Stock & Warehouse Location", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF5D4037))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("In Stock", fontSize = 11.sp, color = Color.Gray)
                        Text("${product.stockQuantity} units", fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (product.stockQuantity < product.minStockLevel) Color.Red else Color(0xFF2E7D32))
                    }
                    Column {
                        Text("Min Order Qty", fontSize = 11.sp, color = Color.Gray)
                        Text("${product.minOrderQuantity} units", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Aisle / Rack", fontSize = 11.sp, color = Color.Gray)
                        Text("${product.aisle} - ${product.rack}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF5D4037))
                    }
                }

                if (product.barcode.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, null, tint = FStorePurple, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Barcode / SKU: ", fontSize = 11.sp, color = Color.Gray)
                        Text(product.barcode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                    }
                }

                if (expiryStr != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Expiry Date: ", fontSize = 11.sp, color = Color.Gray)
                        Text(expiryStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Added: $dateStr", fontSize = 11.sp, color = Color.Gray)
                }

                if (product.searchNames.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Search Keywords:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(product.searchNames.joinToString(", "), fontSize = 11.sp, color = Color.DarkGray)
                }

                if (!product.adminFeedback.isNullOrBlank()) {
                    Surface(color = Color(0xFFFFFDE7), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Admin Notes / Changes:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                            Spacer(Modifier.height(4.dp))
                            Text(product.adminFeedback!!, fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onEdit()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("EDIT PRODUCT", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("CLOSE")
            }
        }
    )
}

@Composable
fun ProductVerificationDialog(product: Product, viewModel: StoreViewModel, onDismiss: () -> Unit, onApprove: (Double, Double, Double, Double) -> Unit) {
    // Pre-fill with existing product data or system defaults
    var commission by remember { mutableStateOf(if (product.commissionRate > 0) product.commissionRate.toString() else "5.0") }
    var profitPercent by remember { mutableStateOf(if (product.adminProfitPercent > 0) product.adminProfitPercent.toString() else "10.0") }
    var deliveryCharge by remember { mutableStateOf(if (product.deliveryCharge > 0) product.deliveryCharge.toString() else viewModel.storeConfig.deliveryCharge.toString()) }
    var gstRate by remember { mutableStateOf(if (product.taxRate > 0) product.taxRate.toString() else viewModel.storeConfig.gstPercentage.toString()) }

    val checks = listOf(
        "Quality: Photos are clear, correctly categorized.",
        "Safety: FSSAI, Expiry, Veg/Non-Veg marks correct.",
        "Pricing: MRP/GST/Fees verified as per policy.",
        "Logistics: Weight, Volume, Barcode data accurate.",
        "Legal: No illegal items, fake brands, or copyright issues."
    )
    val checkStates = remember { mutableStateListOf(*Array(checks.size) { false }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quality Check & Approval", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Verify these points for ${product.name}:", fontSize = 12.sp, color = Color.Gray)
                checks.forEachIndexed { index, check ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checkStates[index], onCheckedChange = { checkStates[index] = it })
                        Text(check, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Set Economics & Margins:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = commission,
                        onValueChange = { commission = it },
                        label = { Text("Comm %") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = profitPercent,
                        onValueChange = { profitPercent = it },
                        label = { Text("Profit %") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gstRate,
                        onValueChange = { gstRate = it },
                        label = { Text("Total GST %") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = deliveryCharge,
                        onValueChange = { deliveryCharge = it },
                        label = { Text("Del ₹") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                val g = gstRate.toDoubleOrNull() ?: 0.0
                if (g > 0) {
                    Text("Tax Split: CGST ${g/2}% | SGST ${g/2}%", fontSize = 10.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApprove(
                        commission.toDoubleOrNull() ?: 0.0,
                        profitPercent.toDoubleOrNull() ?: 0.0,
                        deliveryCharge.toDoubleOrNull() ?: 0.0,
                        gstRate.toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = checkStates.all { it },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("APPROVE & GO LIVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun ProductCard(
    product: Product, 
    viewModel: StoreViewModel, 
    onEditClick: () -> Unit,
    serialNumber: Int? = null,
    onReorder: ((Int) -> Unit)? = null
) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val addedDate = sdf.format(Date(product.addedAt))
    val isAdmin = viewModel.currentUserProfile?.userRole == UserRole.ADMIN || viewModel.isMasterAdmin
    var showDetailDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var showSequenceDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    
    // Find merchant name
    val merchant = if (isAdmin) viewModel.allUsers.find { it.uid == product.merchantId } else null

    if (showDetailDialog) {
        ProductDetailDialog(
            product = product,
            viewModel = viewModel,
            onDismiss = { showDetailDialog = false },
            onEdit = {
                showDetailDialog = false
                onEditClick()
            }
        )
    }

    if (showVerifyDialog) {
        ProductVerificationDialog(
            product = product,
            viewModel = viewModel,
            onDismiss = { showVerifyDialog = false },
            onApprove = { comm, profit, del, gst -> 
                viewModel.approveProduct(product.id, comm, profit, del, gst)
                showVerifyDialog = false
            }
        )
    }

    if (showSequenceDialog) {
        EditSequenceDialog(
            currentSeq = serialNumber ?: product.sequenceNumber,
            onDismiss = { showSequenceDialog = false },
            onConfirm = { 
                if (onReorder != null) onReorder(it)
                else viewModel.updateProductSequence(product.id, it)
                showSequenceDialog = false
            }
        )
    }

    if (showScheduleDialog) {
        AvailabilitySchedulingDialog(
            title = "Product Availability: ${product.name}",
            initialDailySchedules = product.dailySchedules,
            initialVacationStart = product.vacationStart,
            initialVacationEnd = product.vacationEnd,
            onDismiss = { showScheduleDialog = false },
            onSave = { schedules, start, end ->
                viewModel.updateProduct(product.copy(
                    dailySchedules = schedules,
                    vacationStart = start,
                    vacationEnd = end
                ))
                showScheduleDialog = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetailDialog = true }, 
        shape = RoundedCornerShape(12.dp), 
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Admin controls for sequence and merchant info
            if (isAdmin) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (merchant != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, null, tint = FStorePurple, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Merchant: ", fontSize = 11.sp, color = Color.Gray)
                            Text(merchant.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                        }
                    }
                    // Sequence Badge (Clickable for Admin)
                    Surface(
                        color = FStorePurple.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable { showSequenceDialog = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Icon(Icons.Default.Sort, null, tint = FStorePurple, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (serialNumber != null) "SERIAL: $serialNumber" else "ORDER: ${product.sequenceNumber}", 
                                color = FStorePurple, 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Verification Badge
            if (!product.isVerified) {
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Info, null, tint = Color.Red, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PENDING ADMIN APPROVAL - NOT LIVE", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (serialNumber != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FStorePurple.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp).clickable { showSequenceDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(serialNumber.toString(), fontWeight = FontWeight.Bold, color = FStorePurple)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                    }

                    if (product.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (product.shortDescription.isNotBlank()) {
                            Text(product.shortDescription, fontSize = 11.sp, color = Color.Gray, maxLines = 2)
                        }
                        Text("₹${product.price} (Base: ₹${product.basePrice}) • ${product.weight}", fontSize = 12.sp, color = Color.Gray)
                        
                        if (product.isFoodProduct) {
                            Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                                Text(" FOOD PRODUCT ", color = Color(0xFFE65100), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = product.isAvailable,
                        onCheckedChange = { 
                            viewModel.updateProduct(product.copy(isAvailable = it))
                        }, 
                        modifier = Modifier.scale(0.8f)
                    )
                    Text(
                        if (product.isComingSoon) "COMING SOON"
                        else if (!product.isVerified) "UNVERIFIED"
                        else if (product.isAvailable) "LIVE" 
                        else "HIDDEN", 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Black, 
                        color = when {
                            product.isComingSoon -> Color(0xFFFF9800)
                            !product.isVerified -> Color.Gray
                            product.isAvailable -> Color(0xFF2E7D32)
                            else -> Color.Red
                        }
                    )
                }
            }
            
            if (product.isVerified && !product.isAvailable) {
                Surface(
                    color = Color.Gray.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        "Currently Hidden from Customers. Toggle 'LIVE' to show again.",
                        modifier = Modifier.padding(8.dp),
                        fontSize = 10.sp,
                        color = Color.DarkGray
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BARCODE: ${product.barcode}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = FStorePurple)
                    if (product.hsnCode.isNotBlank()) {
                        Text("HSN: ${product.hsnCode}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                    }
                    Text("LOCATION: ${product.aisle}-${product.rack}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF5D4037))
                    Text("Added: $addedDate", fontSize = 10.sp, color = Color.Gray)
                    if (product.expiryDate != null) {
                        Text("EXPIRY: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(product.expiryDate))}", 
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    }
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Delivery: ${if (product.deliveryChargeIncluded) "Included" else "Extra"}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (product.freeDeliveryMinAmount > 0) {
                        Text("Free > ₹${product.freeDeliveryMinAmount}", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                    if (product.freeDeliveryMinQuantity > 0) {
                        Text("Free > ${product.freeDeliveryMinQuantity} pcs", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                    Text("Stock: ${product.stockQuantity}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (product.stockQuantity < product.minStockLevel) Color.Red else Color(0xFF2E7D32))
                    if (product.minOrderQuantity > 1) {
                        Text("Min Order: ${product.minOrderQuantity} units", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFE91E63))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))

            if (product.availableCoupons.isNotEmpty()) {
                Text("COUPONS: ${product.availableCoupons.joinToString { it.code }}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Spacer(Modifier.height(8.dp))
            }

            if (!product.adminFeedback.isNullOrBlank()) {
                Surface(
                    color = Color.Yellow.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, Color.Yellow.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditNote, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("ADMIN CHANGES/NOTES:", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B8000))
                            Text(product.adminFeedback!!, fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (product.originalPrice > product.price) {
                        Text("₹${product.originalPrice}", fontSize = 12.sp, color = Color.Gray, style = TextStyle(textDecoration = TextDecoration.LineThrough))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("₹${product.price}", fontSize = 18.sp, color = FStorePurple, fontWeight = FontWeight.Black)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // View details button
                    IconButton(onClick = { showDetailDialog = true }) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "View Details",
                            tint = FStorePurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Edit product button
                    IconButton(onClick = onEditClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Product",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isAdmin) {
                        val isInDeal = viewModel.dynamicAppConfig.flashSale?.productIds?.contains(product.id) == true
                        IconButton(onClick = { 
                            viewModel.toggleProductInDeal(product.id)
                        }) {
                            Icon(
                                if (isInDeal) Icons.Default.Bolt else Icons.Default.ElectricBolt, 
                                contentDescription = "Toggle Deal",
                                tint = if (isInDeal) Color(0xFFFF9800) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (isAdmin && !product.isVerified) {
                        Button(
                            onClick = { showVerifyDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).padding(end = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("APPROVE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = { 
                        viewModel.updateProduct(product.copy(isComingSoon = !product.isComingSoon))
                    }) {
                        Icon(
                            if (product.isComingSoon) Icons.Default.Timer else Icons.Default.TimerOff, 
                            null, 
                            modifier = Modifier.size(20.dp), 
                            tint = if (product.isComingSoon) Color(0xFFFF9800) else Color.Gray
                        )
                    }
                    IconButton(onClick = { 
                        viewModel.updateProduct(product.copy(stockQuantity = product.stockQuantity + 10, isInStock = true))
                    }) {
                        Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(20.dp), tint = FStorePurple)
                    }
                    IconButton(onClick = { showScheduleDialog = true }) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(20.dp), tint = Color(0xFFFF9800))
                    }
                    IconButton(onClick = { viewModel.deleteProduct(product.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EditSequenceDialog(currentSeq: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var seq by remember { mutableStateOf(currentSeq.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Display Order (Serial)") },
        text = {
            Column {
                Text("Change serial number to move this item. Others will shift automatically.", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = seq,
                    onValueChange = { if(it.all { char -> char.isDigit() }) seq = it },
                    label = { Text("Serial Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if(seq.isNotBlank()) onConfirm(seq.toInt()) }) { Text("MOVE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
