package com.fryumm.store.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.Order
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.data.model.PaymentStatus
import com.fryumm.store.data.model.Picker
import com.fryumm.store.ui.theme.FStorePurple
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrderCard(
    order: Order,
    viewModel: StoreViewModel,
    onStartScan: (String, String) -> Unit,
    showDetails: Boolean = true,
    onDetailClick: ((String) -> Unit)? = null
) {
    val timeAgo = DateUtils.getRelativeTimeSpanString(order.timestamp).toString()
    val isVeryNew = System.currentTimeMillis() - order.timestamp < 120000 // 2 minutes
    val isDelayed = order.status == OrderStatus.PLACED && System.currentTimeMillis() - order.timestamp > 600000 // 10 minutes
    
    val isAdmin = viewModel.currentUserProfile?.userRole == UserRole.ADMIN
    val isStoreOwner = viewModel.currentUserProfile?.userRole == UserRole.STORE_OWNER
    val merchantName = if (isAdmin) {
        viewModel.allUsers.find { it.uid == order.sellerId }?.name ?: "Unknown Merchant"
    } else null

    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        CancelReasonDialog(
            onConfirm = { reason ->
                viewModel.cancelOrder(order.id, reason)
                showCancelDialog = false
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = onDetailClick != null) { onDetailClick?.invoke(order.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDelayed -> Color(0xFFFFF3E0) // Light Orange for delayed
                isVeryNew && order.status == OrderStatus.PLACED -> Color(0xFFFFEBEE) // Light Red for new
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isVeryNew || isDelayed) 4.dp else 2.dp),
        border = BorderStroke(
            width = if (isVeryNew || isDelayed) 2.dp else 1.dp, 
            color = when {
                isDelayed -> Color(0xFFFF9800).copy(alpha = 0.5f)
                isVeryNew -> Color.Red.copy(alpha = 0.5f)
                else -> Color.LightGray.copy(alpha = 0.2f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Order #${order.id}", fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.weight(1f, fill = false))
                        if (isVeryNew && order.status == OrderStatus.PLACED) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                                Text(" NEW ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        if (isDelayed) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(color = Color(0xFFFF9800), shape = RoundedCornerShape(4.dp)) {
                                Text(" DELAYED ", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(order.status)
                    }
                    Text("Placed: $timeAgo", fontSize = 10.sp, color = Color.Gray)
                    if (merchantName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Default.Store, null, modifier = Modifier.size(10.dp), tint = FStorePurple)
                            Spacer(Modifier.width(4.dp))
                            Text(merchantName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (!isStoreOwner) {
                        Text(order.customerName, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        Text(order.phoneNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                    Text(order.area, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                }
            }
            
            if (!showDetails) return@Column
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (order.status == OrderStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("CANCELLED BY: ${order.cancelledBy ?: "UNKNOWN"}", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        Text("Reason: ${order.cancellationReason ?: "No reason provided"}", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!isStoreOwner) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color.LightGray.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(12.dp), tint = FStorePurple)
                            Spacer(Modifier.width(4.dp))
                            Text(order.customerAddress.ifEmpty { order.shippingAddress }, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (order.pickerId != null) {
                    val picker = viewModel.pickers.firstOrNull { it.id == order.pickerId }
                    WorkerInfo(label = "PICKER", name = picker?.name ?: "Unknown", icon = Icons.Default.Person)
                }
                if (order.deliveryAgentId != null) {
                    val rider = viewModel.riders.firstOrNull { it.id == order.deliveryAgentId }
                    WorkerInfo(label = "RIDER", name = rider?.name ?: "Assigned", icon = Icons.Default.DeliveryDining)
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
            
            // Items List (Sorted by Aisle & Rack for optimization)
            val sortedGroups = order.items.groupBy { it.product.id }
                .toList()
                .sortedBy { (_, items) -> 
                    val product = items.first().product
                    "${product.aisle}-${product.rack}"
                }

            for ((productId, items) in sortedGroups) {
                val item = items.first()
                val product = item.product
                val totalQty = items.sumOf { it.quantity }
                val collectedQty = order.collectedProductIds.count { it == productId }
                val isFullyCollected = collectedQty >= totalQty
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isFullyCollected) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "$collectedQty / $totalQty",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isFullyCollected) Color(0xFF2E7D32) else Color(0xFF1565C0)
                                )
                            }
                            Column {
                                Text(
                                    text = item.product.name, 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFullyCollected) Color.Gray else Color.Black,
                                    style = if (isFullyCollected) TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default
                                )
                                Text(
                                    "Aisle: ${product.aisle} | Rack: ${product.rack}", 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = FStorePurple.copy(alpha = 0.7f)
                                )
                            }
                            if (isFullyCollected) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.padding(start = 8.dp).size(16.dp), tint = Color(0xFF2E7D32))
                            }
                        }
                        Text("Price: ₹${item.product.price} | SKU: ${item.product.id.takeLast(6)}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
            
            if (order.status == OrderStatus.SEALED || order.status == OrderStatus.READY || order.status == OrderStatus.DISPATCHED || order.status == OrderStatus.ASSIGNED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(48.dp), tint = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("BAG QR CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(order.bagQrCode ?: "GENERATING...", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            if (!isStoreOwner) {
                                Text("Address: ${order.customerAddress}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
            
            // Billing Details Section
            Surface(
                color = Color.LightGray.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    BillingRow("Items Subtotal", "₹${order.totalPrice}")
                    
                    if (order.couponCode != null) {
                        BillingRow("Coupon (${order.couponCode})", "-₹${order.discountAmount}", Color(0xFF2E7D32))
                    } else if (order.discountAmount > 0) {
                        BillingRow("Discount", "-₹${order.discountAmount}", Color(0xFF2E7D32))
                    }

                    if (order.deliveryCharge > 0) BillingRow("Rider Fee", "₹${order.deliveryCharge}")
                    if (order.handlingFee > 0) BillingRow("Handling Charge", "₹${order.handlingFee}")
                    if (order.roundingOff != 0.0) BillingRow("Rounding Off", "₹${order.roundingOff}")
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.2f))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Total Bill", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("(As per Admin Logic)", fontSize = 8.sp, color = Color.Gray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${order.totalAmount}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = FStorePurple)
                            if (isAdmin && order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED) {
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.finalizeOrderBilling(order) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("VERIFY & LOCK BILL", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    if (isAdmin) {
                        val economics = viewModel.calculateOrderEconomics(order)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = FStorePurple.copy(alpha = 0.1f))
                        Text("ADMIN: EARNINGS BREAKDOWN", fontSize = 9.sp, fontWeight = FontWeight.Black, color = FStorePurple)
                        BillingRow("Merchant Payout", "₹${String.format("%.2f", economics.totalMerchantPayout)}", Color(0xFFE65100))
                        BillingRow("Commission Earned", "₹${String.format("%.2f", economics.totalCommission)}")
                        BillingRow("Base Profit", "₹${String.format("%.2f", economics.totalAdminProfit)}")
                        BillingRow("Platform Fee", "₹${String.format("%.2f", economics.platformFee)}")
                        BillingRow("Tax Collected (GST)", "₹${String.format("%.2f", economics.totalTax)}")
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Est. Net Profit", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF2E7D32))
                            Text("₹${String.format("%.2f", economics.totalEstimatedProfit)}", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = Color(0xFF2E7D32))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Payment Mode", fontSize = 11.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(order.paymentMethod, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(Modifier.width(8.dp))
                            PaymentStatusBadge(order.paymentStatus)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
            
            // Footer & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Amount", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    val displayPrice = if (order.totalAmount > 0) order.totalAmount else order.totalPrice
                    Text("₹$displayPrice", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.Black)
                }
                
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    when (order.status) {
                        OrderStatus.PLACED -> {
                            OrderActionButton(
                                text = "CANCEL",
                                color = Color.White,
                                contentColor = Color.Red,
                                borderColor = Color.Red,
                                onClick = { showCancelDialog = true }
                            )
                            Spacer(Modifier.width(8.dp))
                            OrderActionButton(
                                text = "ACCEPT",
                                color = Color(0xFF2E7D32),
                                onClick = { viewModel.acceptOrder(order.id) },
                                icon = Icons.Default.Check
                            )
                        }
                        OrderStatus.ACCEPTED -> {
                            OrderActionButton(
                                text = "MARK READY",
                                color = Color(0xFF2E7D32),
                                onClick = { viewModel.markReady(order.id) },
                                icon = Icons.Default.DoneAll
                            )
                        }
                        OrderStatus.READY -> {
                            if (isAdmin) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.printInvoice(order) }) {
                                        Icon(Icons.Default.Print, null, tint = FStorePurple)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (order.deliveryAgentId == null) {
                                        Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(4.dp)) {
                                            Text(" NEEDS RIDER ", color = Color(0xFFFF9800), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    } else {
                                        OrderActionButton(
                                            text = "DISPATCH",
                                            color = Color.Black,
                                            onClick = { viewModel.markDispatched(order.id) },
                                            icon = Icons.Default.Send
                                        )
                                    }
                                }
                            } else {
                                if (order.deliveryAgentId == null) {
                                    Text("WAITING FOR RIDER", color = Color(0xFFFF9800), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                } else {
                                    Text("RIDER ON THE WAY", color = Color(0xFF2E7D32), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }
                        }
                        else -> {
                            StatusBadge(order.status)
                        }
                    }
                }
            }

            if (isAdmin) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (order.status != OrderStatus.CANCELLED) {
                        TextButton(onClick = { showCancelDialog = true }) {
                            Text("ADMIN CANCEL", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { viewModel.deleteOrder(order.id) }) {
                        Text("DELETE PERMANENTLY", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (isAdmin && order.status == OrderStatus.READY) {
                // Rider suggestions removed from orders page
            }
        }
    }
}

@Composable
fun HistoryLogItem(order: Order, viewModel: StoreViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val date = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.timestamp))
    
    val isAdmin = viewModel.currentUserProfile?.userRole == UserRole.ADMIN
    val isStoreOwner = viewModel.currentUserProfile?.userRole == UserRole.STORE_OWNER
    val merchantName = if (isAdmin) {
        viewModel.allUsers.find { it.uid == order.sellerId }?.name ?: "Unknown Merchant"
    } else null

    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        CancelReasonDialog(
            onConfirm = { reason ->
                viewModel.cancelOrder(order.id, reason)
                showCancelDialog = false
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(8.dp),
        border = if (expanded) BorderStroke(1.dp, FStorePurple) else null
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(date, fontSize = 11.sp, color = Color.Gray)
                    if (merchantName != null) {
                        Text("Merchant: $merchantName", fontSize = 10.sp, color = FStorePurple, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${order.totalAmount}", fontWeight = FontWeight.Black, color = Color.Black)
                    StatusBadge(order.status)
                }
            }
            if (expanded) {
                Box(modifier = Modifier.padding(8.dp)) {
                    OrderCard(order = order, viewModel = viewModel, onStartScan = { _, _ -> }, showDetails = true)
                }
            }
        }
    }
}

@Composable
fun CancelReasonDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val reasons = listOf("Out of Stock", "Delayed Delivery", "Customer Mind Change", "Incorrect Address", "Merchant Request", "Other")
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var customReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Cancellation Reason", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason })
                        Text(reason, modifier = Modifier.padding(start = 8.dp), fontSize = 14.sp)
                    }
                }
                if (selectedReason == "Other") {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Enter custom reason") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(if (selectedReason == "Other") customReason else selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("CONFIRM CANCEL")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("BACK") }
        }
    )
}

@Composable
fun QCTimer(startTime: Long) {
    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while(true) {
            elapsed = System.currentTimeMillis() - startTime
            delay(1000)
        }
    }
    val sec = (elapsed / 1000) % 60
    val min = (elapsed / 60000)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Timer, null, tint = if (min >= 1) Color.Red else Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
        Text(text = String.format(Locale.getDefault(), " %02d:%02d", min, sec), color = if (min >= 1) Color.Red else Color(0xFF2E7D32), fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}
