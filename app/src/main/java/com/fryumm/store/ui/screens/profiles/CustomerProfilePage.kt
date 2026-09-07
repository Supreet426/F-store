package com.fryumm.store.ui.screens.profiles

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fryumm.store.data.model.CustomerEventType
import com.fryumm.store.data.model.CustomerProfile
import com.fryumm.store.data.model.Order
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.ui.theme.FStorePurple
import com.fryumm.store.ui.viewmodels.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfilePage(
    customer: CustomerProfile,
    viewModel: StoreViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateSdf = SimpleDateFormat("dd MMM", Locale.getDefault())

    val customerOrders = viewModel.orders.filter { order ->
        (customer.mobile.isNotBlank() && order.phoneNumber.contains(customer.mobile)) ||
                (customer.name.isNotBlank() && order.customerName.equals(customer.name, ignoreCase = true))
    }

    val customerEvents = remember(customer.id, viewModel.customerEvents, viewModel.searchEvents) {
        val activityEvents = viewModel.customerEvents.filter { it.customerId == customer.id }
        val searchEvents = viewModel.searchEvents.filter { it.userId == customer.id }

        (activityEvents.map {
            TimelineEvent(it.timestamp, it.eventType, it.details)
        } + searchEvents.map {
            TimelineEvent(it.timestamp, CustomerEventType.SEARCH, "Searched for \"${it.query}\"")
        }).sortedByDescending { it.timestamp }
    }

    val trendData = remember(customerOrders) {
        (0..6).map { i ->
            val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            customerOrders.filter { order ->
                val orderCal = Calendar.getInstance().apply { timeInMillis = order.timestamp }
                orderCal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR) &&
                        orderCal.get(Calendar.YEAR) == day.get(Calendar.YEAR)
            }.sumOf { it.totalAmount }.toFloat()
        }.reversed()
    }

    // Category Preference Breakdown (%)
    val categoryDistribution = remember(customerOrders) {
        val allItems = customerOrders.flatMap { it.items }
        val totalItems = allItems.sumOf { it.quantity }
        if (totalItems == 0) emptyList()
        else {
            allItems.groupBy { it.product.category }
                .mapValues { entry -> (entry.value.sumOf { it.quantity }.toFloat() / totalItems * 100f) }
                .toList()
                .sortedByDescending { it.second }
                .take(4)
        }
    }

    // Recency & Churn Risk Tag
    val daysSinceLastOrder = remember(customer.lastOrderDate) {
        if (customer.lastOrderDate == null || customer.lastOrderDate <= 0) null
        else ((System.currentTimeMillis() - customer.lastOrderDate) / (24 * 60 * 60 * 1000L)).toInt()
    }
    val churnStatus = when {
        daysSinceLastOrder == null -> "NEW USER (NO ORDERS)"
        daysSinceLastOrder <= 1 -> "ACTIVE TODAY"
        daysSinceLastOrder <= 7 -> "ACTIVE THIS WEEK"
        daysSinceLastOrder <= 30 -> "REGULAR ACTIVE ($daysSinceLastOrder DAYS AGO)"
        else -> "AT CHURN RISK ($daysSinceLastOrder DAYS INACTIVE)"
    }
    val churnColor = when {
        daysSinceLastOrder == null -> Color(0xFF2196F3)
        daysSinceLastOrder <= 7 -> Color(0xFF2E7D32)
        daysSinceLastOrder <= 30 -> Color(0xFFFF9800)
        else -> Color.Red
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "Profile Details",
        "Order History (${customerOrders.size})",
        "Purchasing Algorithms"
    )

    var showPassDialog by remember { mutableStateOf(false) }
    var showCouponDialog by remember { mutableStateOf(false) }
    var showFraudDialog by remember { mutableStateOf(false) }

    var adminRemarkNote by remember(customer.id) { mutableStateOf(customer.referralEarningsFreezeReason) }

    if (showPassDialog) {
        val passConfig = viewModel.dynamicAppConfig.flashOPass
        AlertDialog(
            onDismissRequest = { showPassDialog = false },
            title = { Text("Grant / Upgrade Flash-O Pass", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select a tier to manually activate a 30-day pass for ${customer.name}.", fontSize = 12.sp, color = Color.Gray)
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

    if (showCouponDialog) {
        var code by remember { mutableStateOf("") }
        var discount by remember { mutableStateOf("10%") }
        AlertDialog(
            onDismissRequest = { showCouponDialog = false },
            title = { Text("Offer Coupon to ${customer.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Coupon Code") },
                        placeholder = { Text("e.g. SPECIAL20") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = discount,
                        onValueChange = { discount = it },
                        label = { Text("Discount Value") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendTargetedCampaign(
                            listOf(customer.id),
                            "Special Gift for You!",
                            "Use code $code to get $discount off on your next order!"
                        )
                        showCouponDialog = false
                    },
                    enabled = code.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
                ) {
                    Text("SEND COUPON")
                }
            },
            dismissButton = { TextButton(onClick = { showCouponDialog = false }) { Text("CANCEL") } }
        )
    }

    if (showFraudDialog) {
        var fraudReason by remember { mutableStateOf("Multi-account duplicate customer detected") }
        AlertDialog(
            onDismissRequest = { showFraudDialog = false },
            title = { Text("Flag Customer as Fraud", fontWeight = FontWeight.Bold) },
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
                ) {
                    Text("FLAG FRAUD")
                }
            },
            dismissButton = { TextButton(onClick = { showFraudDialog = false }) { Text("CANCEL") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold)
                        Text("Customer ID: ${customer.id.take(10)}...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    // Call Direct
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile}"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color.White)
                    }
                    // WhatsApp Direct
                    IconButton(onClick = {
                        val cleanPhone = customer.mobile.replace("+", "").replace(" ", "")
                        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FStorePurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            // Header Top Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FStorePurple
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp),
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.6f))
                        ) {
                            if (customer.photoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(customer.photoUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    Text(
                                        customer.name.take(1).uppercase(),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FStorePurple
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Text(customer.mobile, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            if (customer.email.isNotEmpty()) {
                                Text(customer.email, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                            }

                            Spacer(Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = churnColor
                                ) {
                                    Text(
                                        churnStatus,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showCouponDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.LocalOffer, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Send Coupon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showFraudDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Flag, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Flag Fraud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab Row Navigation (3 Sections)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = FStorePurple
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                // SECTION 1: PROFILE DETAILS
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            SectionTitle("Financial Insights")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard("Lifetime Spend", "₹${customer.lifetimeSpending.toInt()}", Icons.Default.Payments, Color(0xFF4CAF50), Modifier.weight(1f))
                                StatCard("Wallet Balance", "₹${customer.walletBalance.toInt()}", Icons.Default.AccountBalanceWallet, Color(0xFF2196F3), Modifier.weight(1f))
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
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
                            DetailCardSection(
                                title = "Customer Account Identity & Contact",
                                icon = Icons.Default.Person,
                                items = listOf(
                                    "Customer Name" to customer.name,
                                    "Mobile Number" to customer.mobile,
                                    "Email Address" to customer.email.ifBlank { "Not provided" },
                                    "Primary Address" to customer.address.ifBlank { "Not provided" },
                                    "Member Since" to sdf.format(Date(customer.joinedDate)),
                                    "Favorite Category" to customer.favoriteCategory.ifEmpty { "General" },
                                    "Device Specs" to customer.device.ifEmpty { "Android Device" },
                                    "Language" to customer.language,
                                    "App Version" to customer.appVersion.ifEmpty { "v1.0.0" }
                                )
                            )
                        }

                        // NEW: Saved Addresses & Delivery Notes
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Saved Delivery Addresses & Instructions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FStorePurple)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    if (customer.savedAddresses.isEmpty()) {
                                        Text(
                                            "Primary: ${customer.address.ifBlank { "No default address saved" }}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.DarkGray
                                        )
                                    } else {
                                        customer.savedAddresses.forEach { addr ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("${addr.label} ${if (addr.isDefault) "(Default)" else ""}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FStorePurple)
                                                    Text(addr.addressLine, fontSize = 12.sp, color = Color.Gray)
                                                    if (addr.landmark.isNotEmpty()) {
                                                        Text("Landmark: ${addr.landmark} - ${addr.pincode}", fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                }
                                            }
                                            Divider(color = Color(0xFFEEEEEE), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF3E5F5)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.TwoWheeler, null, tint = FStorePurple, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text("Rider Delivery Instructions:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                                                Text(
                                                    customer.defaultDeliveryInstructions.ifEmpty { "Leave at door / Call on arrival" },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // NEW: Refunds, Return Risk & Support Disputes Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.History, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Refunds & Support Disputes", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFD32F2F))
                                        }
                                        val isHighRefundRisk = customer.totalRefundsCount > 3 || (customer.totalOrders > 0 && (customer.cancelledOrders.toDouble() / customer.totalOrders) > 0.3)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isHighRefundRisk) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                if (isHighRefundRisk) "HIGH REFUND RISK" else "LOW DISPUTE RISK",
                                                color = if (isHighRefundRisk) Color.Red else Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StatCard("Total Refunds", "₹${customer.totalRefundedAmount.toInt()}", Icons.Default.PriceChange, Color(0xFFE53935), Modifier.weight(1f))
                                        StatCard("Refund Count", "${customer.totalRefundsCount} Claims", Icons.Default.Receipt, Color(0xFFFB8C00), Modifier.weight(1f))
                                        StatCard("Open Tickets", "${customer.openSupportTicketsCount}", Icons.Default.SupportAgent, Color(0xFF1E88E5), Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // NEW: Cart Abandonment & Communication Preferences Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Cart Abandonment & Notifications", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFE65100))
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Pending Cart Items", fontSize = 12.sp, color = Color.Gray)
                                            Text(
                                                if (customer.cartAbandonedItemsCount > 0) "${customer.cartAbandonedItemsCount} Items (Worth ₹${customer.cartAbandonedTotalValue.toInt()})" else "Cart is empty",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (customer.cartAbandonedItemsCount > 0) Color(0xFFD84315) else Color.DarkGray
                                            )
                                        }

                                        if (customer.cartAbandonedItemsCount > 0) {
                                            Button(
                                                onClick = { showCouponDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Nudge Checkout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Divider(color = Color(0xFFEEEEEE), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Push Notifications", fontSize = 11.sp, color = Color.Gray)
                                            Text(if (customer.pushNotificationEnabled) "Enabled" else "Disabled", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (customer.pushNotificationEnabled) Color(0xFF2E7D32) else Color.Red)
                                        }
                                        Column {
                                            Text("WhatsApp Alerts", fontSize = 11.sp, color = Color.Gray)
                                            Text(if (customer.whatsappUpdatesOptIn) "Opted In" else "Opted Out", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (customer.whatsappUpdatesOptIn) Color(0xFF2E7D32) else Color.Red)
                                        }
                                        Column {
                                            Text("Customer Rating", fontSize = 11.sp, color = Color.Gray)
                                            Text("⭐ ${customer.avgCustomerRating} / 5.0", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFF57F17))
                                        }
                                    }
                                }
                            }
                        }

                        // Admin Internal Notes Box
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.NoteAlt, null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Admin Internal Notes & Remarks", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FStorePurple)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = adminRemarkNote,
                                        onValueChange = { adminRemarkNote = it },
                                        placeholder = { Text("Enter internal notes, special requests or fraud investigation details...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(
                                            onClick = {
                                                viewModel.verifyCustomerReferralEarnings(customer.id, !customer.referralEarningsFrozen, "STAFF_ADMIN", adminRemarkNote)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
                                        ) {
                                            Text("Save Internal Note", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: ORDER HISTORY
                1 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Customer Order Record", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${customerOrders.size} Total Orders Placed", fontSize = 12.sp, color = Color.Gray)
                                }
                                val deliveredCount = customerOrders.count { it.status == OrderStatus.DELIVERED }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF2E7D32).copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        "$deliveredCount Delivered",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (customerOrders.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.LightGray
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("No orders placed by this customer yet.", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(customerOrders, key = { it.id }) { order ->
                                    CustomerOrderItemCard(order = order, sdf = sdf)
                                }
                            }
                        }
                    }
                }

                // SECTION 3: PURCHASING ALGORITHMS
                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // 1. AI Targeted Product Recommendations
                        item {
                            val suggestions = remember(customer.id) { viewModel.getTargetedProductSuggestions(customer.id) }
                            if (suggestions.isNotEmpty()) {
                                SectionTitle("AI Smart Recommendations", FStorePurple)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, null, tint = FStorePurple, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Personalized Product Suggestions", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FStorePurple)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("Calculated from search history & category browsing", fontSize = 11.sp, color = Color.Gray)
                                        Spacer(Modifier.height(12.dp))
                                        suggestions.forEachIndexed { index, product ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = FStorePurple.copy(alpha = 0.1f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                                                    }
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(product.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    Text(product.category, fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Text("₹${product.price}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                            }
                                            if (index < suggestions.size - 1) {
                                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Category Preference Breakdown (%)
                        if (categoryDistribution.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Category, null, tint = FStorePurple, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Category Preference Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FStorePurple)
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        categoryDistribution.forEach { (cat, pct) ->
                                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(String.format(Locale.getDefault(), "%.1f%%", pct), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = { pct / 100f },
                                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                                    color = FStorePurple,
                                                    trackColor = FStorePurple.copy(alpha = 0.1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Behavioral & Purchasing Analytics
                        item {
                            SectionTitle("Spending & Behavioral Analytics")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("7-Day Spending Analytics", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1B5E20))
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    SimpleLineChart(data = trendData, color = Color(0xFF4CAF50))
                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Average Order Value (AOV)", fontSize = 12.sp, color = Color.Gray)
                                        Text("₹${customer.avgOrderValue.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }

                        item {
                            DetailCardSection(
                                title = "Acquisition & Fraud Intelligence",
                                icon = Icons.Default.Psychology,
                                items = listOf(
                                    "Acquisition Channel" to (customer.acquisitionSource ?: "Direct / App Store"),
                                    "Referred By Merchant ID" to (customer.referredByMerchantId ?: "None"),
                                    "Preferred Payment Method" to customer.preferredPaymentMethod.ifBlank { "COD / UPI" },
                                    "Unique Device Hardware ID" to customer.deviceId.ifBlank { "Unregistered" },
                                    "Duplicate Device Detected" to if (customer.isDuplicateDeviceDetected) "YES (Flagged)" else "NO (Clean)",
                                    "Duplicate Flags Count" to customer.registrationDuplicateFlags.size.toString()
                                )
                            )
                        }

                        // 3. Sequential Activity Timeline
                        item {
                            SectionTitle("Sequential Activity Log")
                            if (customerEvents.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("No recent activity tracked for this customer.", color = Color.Gray, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        items(customerEvents) { event ->
                            ActivityTimelineItem(event, timeSdf, dateSdf)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerOrderItemCard(order: Order, sdf: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Order #${order.id.takeLast(8)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(sdf.format(Date(order.timestamp)), fontSize = 11.sp, color = Color.Gray)
                }

                val statusColor = when (order.status) {
                    OrderStatus.DELIVERED -> Color(0xFF2E7D32)
                    OrderStatus.CANCELLED -> Color.Red
                    else -> Color(0xFFFF9800)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        order.status.uppercase(),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.4f))

            Text("Items Ordered (${order.items.size}):", fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• ${item.product.name} x${item.quantity}", fontSize = 12.sp, color = Color.DarkGray, maxLines = 1, modifier = Modifier.weight(1f))
                    Text("₹${(item.product.price * item.quantity).toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Payment: ${order.paymentMethod}", fontSize = 11.sp, color = Color.Gray)
                Text("Total: ₹${order.totalAmount.toInt()}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = FStorePurple)
            }
        }
    }
}
