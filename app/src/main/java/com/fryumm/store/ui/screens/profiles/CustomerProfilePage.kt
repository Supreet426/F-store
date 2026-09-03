package com.fryumm.store.ui.screens.profiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.CustomerEventType
import com.fryumm.store.data.model.CustomerProfile
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomerProfilePage(customer: CustomerProfile, viewModel: StoreViewModel, onBack: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateSdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    
    val customerOrders = viewModel.orders.filter { it.customerName == customer.name || it.phoneNumber == customer.mobile }
    
    val customerEvents = remember(customer.id, viewModel.customerEvents, viewModel.searchEvents) {
        val activityEvents = viewModel.customerEvents.filter { it.customerId == customer.id }
        val searchEvents = viewModel.searchEvents.filter { it.userId == customer.id }
        
        // Merge and sort
        (activityEvents.map { 
            TimelineEvent(it.timestamp, it.eventType, it.details) 
        } + searchEvents.map { 
            TimelineEvent(it.timestamp, CustomerEventType.SEARCH, "Searched for \"${it.query}\"") 
        }).sortedByDescending { it.timestamp }
    }

    // Spending trend
    val trendData = remember(customerOrders) {
        val calendar = Calendar.getInstance()
        (0..6).map { i ->
            val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            customerOrders.filter { order ->
                val orderCal = Calendar.getInstance().apply { timeInMillis = order.timestamp }
                orderCal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
            }.sumOf { it.totalAmount }.toFloat()
        }.reversed()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Brush.verticalGradient(listOf(FStorePurple, FStorePurple.copy(alpha = 0.8f))))) {
            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp),
                    border = BorderStroke(3.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    if (customer.photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(customer.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.background(Color.White)) {
                            Text(customer.name.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(customer.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(customer.mobile, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                val segment = when {
                    customer.lifetimeSpending >= 5000 -> "VIP CUSTOMER"
                    customer.lifetimeSpending >= 2000 -> "GOLD MEMBER"
                    else -> "REGULAR CUSTOMER"
                }
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, FStorePurple.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Stars, null, tint = Color(0xFFFFC107))
                        Spacer(Modifier.width(8.dp))
                        Text(segment, fontWeight = FontWeight.Bold, color = FStorePurple)
                    }
                }
            }

            item {
                SectionTitle("Flash-O Pass Status")
                if (customer.isSubscribed && customer.subscriptionExpiry != null) {
                    val isExpired = customer.subscriptionExpiry < System.currentTimeMillis()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isExpired) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)),
                        border = BorderStroke(1.dp, if (isExpired) Color.Red.copy(alpha = 0.3f) else Color(0xFF4CAF50).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isExpired) Icons.Default.Error else Icons.Default.CheckCircle,
                                    null,
                                    tint = if (isExpired) Color.Red else Color(0xFF2E7D32)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isExpired) "PASS EXPIRED" else "ACTIVE PASS: ${customer.subscriptionTier?.replace("_", " ")?.uppercase()}",
                                    fontWeight = FontWeight.Black,
                                    color = if (isExpired) Color.Red else Color(0xFF1B5E20)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (isExpired) "Membership ended on: ${sdf.format(Date(customer.subscriptionExpiry))}"
                                else "Valid until: ${sdf.format(Date(customer.subscriptionExpiry))}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            if (!isExpired) {
                                val remainingDays = ((customer.subscriptionExpiry - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt()
                                Text("$remainingDays days remaining", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                            }
                        }
                    }
                } else if (customer.isTrialEligible && customer.totalOrders == 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, null, tint = Color(0xFFE65100))
                                Spacer(Modifier.width(8.dp))
                                Text("TRIAL PACK READY", fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("28-Day Premium Trial will activate automatically on FIRST ORDER.", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No active pass found for this customer", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                
                var showPassDialog by remember { mutableStateOf(false) }
                if (showPassDialog) {
                    val passConfig = viewModel.dynamicAppConfig.flashOPass
                    AlertDialog(
                        onDismissRequest = { showPassDialog = false },
                        title = { Text("Grant / Upgrade Pass") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Select a tier to manually activate a 30-day pass for this customer.", fontSize = 12.sp, color = Color.Gray)
                                passConfig?.tiers?.forEach { tier ->
                                    Button(
                                        onClick = {
                                            viewModel.activateCustomerPass(customer.id, tier.id)
                                            showPassDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(tier.title)
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = { TextButton(onClick = { showPassDialog = false }) { Text("CANCEL") } }
                    )
                }

                TextButton(
                    onClick = { showPassDialog = true },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.AddCard, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MANUALLY ACTIVATE PASS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                SectionTitle("Referral & Security Status")
                val isFrozen = customer.referralEarningsFrozen || customer.referralVerificationStatus == "FROZEN_PENDING_VERIFICATION"
                val isFraudRejected = customer.referralVerificationStatus == "REJECTED_FRAUD"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isFraudRejected -> Color(0xFFFFEBEE)
                            isFrozen -> Color(0xFFFFF3E0)
                            else -> Color(0xFFE8F5E9)
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        when {
                            isFraudRejected -> Color.Red.copy(alpha = 0.5f)
                            isFrozen -> Color(0xFFFF9800).copy(alpha = 0.5f)
                            else -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when {
                                    isFraudRejected -> Icons.Default.Block
                                    isFrozen -> Icons.Default.Lock
                                    else -> Icons.Default.VerifiedUser
                                },
                                null,
                                tint = when {
                                    isFraudRejected -> Color.Red
                                    isFrozen -> Color(0xFFE65100)
                                    else -> Color(0xFF2E7D32)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    isFraudRejected -> "REFERRAL EARNINGS REVOKED (FRAUD)"
                                    isFrozen -> "REFERRAL EARNINGS FROZEN (PENDING VERIFICATION)"
                                    else -> "REFERRAL & ACCOUNT VERIFIED"
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = when {
                                    isFraudRejected -> Color.Red
                                    isFrozen -> Color(0xFFE65100)
                                    else -> Color(0xFF1B5E20)
                                }
                            )
                        }

                        if (customer.referralEarningsFreezeReason.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Reason: ${customer.referralEarningsFreezeReason}",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }

                        if (isFrozen) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Staff/Admin Action Required: Verify this customer is not a duplicate account before releasing referral rewards.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFBF360C)
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.verifyCustomerReferralEarnings(customer.id, true, "STAFF_ADMIN")
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("VERIFY & APPROVE", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.verifyCustomerReferralEarnings(customer.id, false, "STAFF_ADMIN", "Multi-account fraud confirmed by Staff/Admin.")
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("REJECT & BLOCK", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("Financial Insights")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Lifetime Spend", "₹${customer.lifetimeSpending.toInt()}", Icons.Default.Payments, Color(0xFF4CAF50), Modifier.weight(1f))
                    StatCard("Wallet", "₹${customer.walletBalance.toInt()}", Icons.Default.AccountBalanceWallet, Color(0xFF2196F3), Modifier.weight(1f))
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Spending Analysis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        SimpleLineChart(data = trendData, color = Color(0xFF4CAF50))
                        Spacer(Modifier.height(8.dp))
                        Text("Average Order Value: ₹${customer.avgOrderValue.toInt()}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            item {
                SectionTitle("Customer Profile Details")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DetailItem(Icons.Default.Category, "Fav Category", customer.favoriteCategory.ifEmpty { "General" })
                        DetailItem(Icons.Default.Home, "Address", customer.address)
                        DetailItem(Icons.Default.CalendarToday, "Member Since", sdf.format(Date(customer.joinedDate)))
                        DetailItem(Icons.Default.Smartphone, "Device", customer.device.ifEmpty { "Android" })
                    }
                }
            }

            item {
                val suggestions = remember(customer.id) { viewModel.getTargetedProductSuggestions(customer.id) }
                if (suggestions.isNotEmpty()) {
                    SectionTitle("AI Smart Recommendations", FStorePurple)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Based on search & activity history", fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            suggestions.forEach { product ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = FStorePurple, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(product.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("₹${product.price}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            item {
                SectionTitle("Sequential Activity Timeline")
                if (customerEvents.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No recent activity tracked", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }

            items(customerEvents) { event ->
                ActivityTimelineItem(event, timeSdf, dateSdf)
            }

            item {
                var showCouponDialog by remember { mutableStateOf(false) }
                
                if (showCouponDialog) {
                    var code by remember { mutableStateOf("") }
                    var discount by remember { mutableStateOf("10%") }
                    AlertDialog(
                        onDismissRequest = { showCouponDialog = false },
                        title = { Text("Offer Coupon to ${customer.name}") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Coupon Code") }, placeholder = { Text("e.g. SPECIAL20") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Discount Value") }, modifier = Modifier.fillMaxWidth())
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.sendTargetedCampaign(
                                    listOf(customer.id),
                                    "Special Gift for You!",
                                    "Use code $code to get $discount off on your next order!"
                                )
                                showCouponDialog = false
                            }) { Text("SEND COUPON") }
                        },
                        dismissButton = { TextButton(onClick = { showCouponDialog = false }) { Text("CANCEL") } }
                    )
                }

                var showFraudDialog by remember { mutableStateOf(false) }
                if (showFraudDialog) {
                    var fraudReason by remember { mutableStateOf("Multi-account duplicate customer detected") }
                    AlertDialog(
                        onDismissRequest = { showFraudDialog = false },
                        title = { Text("Flag Customer as Fraud") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("This action will lock referral earnings and blacklist this customer account.", fontSize = 12.sp, color = Color.Red)
                                OutlinedTextField(
                                    value = fraudReason,
                                    onValueChange = { fraudReason = it },
                                    label = { Text("Reason") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.verifyCustomerReferralEarnings(customer.id, false, "STAFF_ADMIN", fraudReason)
                                    showFraudDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) { Text("FLAG FRAUD") }
                        },
                        dismissButton = { TextButton(onClick = { showFraudDialog = false }) { Text("CANCEL") } }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showCouponDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
                    ) {
                        Icon(Icons.Default.LocalOffer, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("COUPON", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { showFraudDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Flag, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("FRAUD", fontSize = 12.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
