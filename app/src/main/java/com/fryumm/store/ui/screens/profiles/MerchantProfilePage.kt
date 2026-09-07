package com.fryumm.store.ui.screens.profiles

import android.content.Intent
import android.net.Uri
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
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.ui.components.ProductCard
import com.fryumm.store.ui.theme.FStorePurple
import com.fryumm.store.ui.viewmodels.StoreViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantProfilePage(
    profile: UserProfile,
    viewModel: StoreViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userOrders = viewModel.orders.filter { it.sellerId == profile.uid }
    val merchantProducts = viewModel.products.filter {
        it.sellerId == profile.uid || (it.sellerName.isNotBlank() && it.sellerName.equals(profile.tradeName, ignoreCase = true))
    }

    val deliveredOrders = userOrders.filter { it.status == OrderStatus.DELIVERED }
    val totalRevenue = deliveredOrders.sumOf { it.totalAmount }
    val pendingOrdersCount = userOrders.count { it.status in listOf(OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING) }
    val cancelledOrdersCount = userOrders.count { it.status == OrderStatus.CANCELLED }

    val netMerchantPayout = deliveredOrders.sumOf { order ->
        order.items.sumOf { item ->
            val rate = if (item.product.merchantRate > 0) item.product.merchantRate else item.product.price
            rate * item.quantity
        }
    }
    val platformCommission = totalRevenue - netMerchantPayout
    val avgOrderValue = if (deliveredOrders.isNotEmpty()) totalRevenue / deliveredOrders.size else 0.0
    val completionRate = if (userOrders.isNotEmpty()) (deliveredOrders.size.toFloat() / userOrders.size.toFloat() * 100f) else 0f

    // Calculate Top 3 Bestsellers
    val topBestsellers = remember(deliveredOrders) {
        deliveredOrders.flatMap { it.items }
            .groupBy { it.product.name }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
    }

    // Store Status & Availability
    val isVacation = profile.vacationStart != null && profile.vacationEnd != null &&
            System.currentTimeMillis() in profile.vacationStart..profile.vacationEnd
    val storeStatusText = when {
        !profile.active -> "BLOCKED"
        isVacation -> "ON VACATION"
        else -> "OPEN / LIVE"
    }
    val storeStatusColor = when {
        !profile.active -> Color.Red
        isVacation -> Color(0xFFFF9800)
        else -> Color(0xFF2E7D32)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "Profile Details",
        "Products (${merchantProducts.size})",
        "Financials & Analytics"
    )

    var showBlockDialog by remember { mutableStateOf(false) }
    var blockReasonInput by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var adminRemark by remember(profile.uid) { mutableStateOf(profile.blockReason ?: "") }
    var isSavingRemark by remember { mutableStateOf(false) }

    var platformDiscountInput by remember(profile.platformFeeDiscount) { mutableStateOf(profile.platformFeeDiscount.toString()) }
    var showCommissionDialog by remember { mutableStateOf(false) }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block Merchant Account", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter reason for blocking this merchant. It will be recorded in their profile.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = blockReasonInput,
                        onValueChange = { blockReasonInput = it },
                        placeholder = { Text("e.g. Policy violation, fraudulent items") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (blockReasonInput.isNotBlank()) {
                            viewModel.updateUserStatus(profile.uid, false, blockReasonInput)
                            showBlockDialog = false
                            blockReasonInput = ""
                        }
                    },
                    enabled = blockReasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Block Merchant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Merchant Permanently?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete ${profile.name} and their records. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(profile.uid)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCommissionDialog) {
        AlertDialog(
            onDismissRequest = { showCommissionDialog = false },
            title = { Text("Adjust Platform Discount Fee (%)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Set custom platform fee discount for ${profile.tradeName.ifBlank { profile.name }}.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = platformDiscountInput,
                        onValueChange = { platformDiscountInput = it },
                        label = { Text("Discount Percentage (%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val discountVal = platformDiscountInput.toDoubleOrNull() ?: 0.0
                        viewModel.updateUserProfileDetails(profile.copy(platformFeeDiscount = discountVal))
                        showCommissionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
                ) {
                    Text("SAVE DISCOUNT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommissionDialog = false }) { Text("CANCEL") }
            }
        )
    }

    // Revenue trend for last 7 days
    val trendData = remember(userOrders) {
        (0..6).map { i ->
            val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            userOrders.filter { order ->
                val orderCal = Calendar.getInstance().apply { timeInMillis = order.timestamp }
                orderCal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR) &&
                        orderCal.get(Calendar.YEAR) == day.get(Calendar.YEAR)
            }.sumOf { it.totalAmount }.toFloat()
        }.reversed()
    }

    val isMasterAdmin = profile.email.lowercase().trim() in viewModel.masterAdmins

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(profile.tradeName.ifBlank { profile.name }, fontWeight = FontWeight.Black)
                        Text("Merchant ID: ${profile.uid.take(10)}...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Phone Call
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.phoneNumber}"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color.White)
                    }
                    // WhatsApp Direct
                    IconButton(onClick = {
                        val cleanPhone = profile.phoneNumber.replace("+", "").replace(" ", "")
                        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                    }
                    // Email
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${profile.email}"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Email, contentDescription = "Email", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A237E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA))) {
            // Profile Card Top Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1A237E)
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
                            color = Color.White,
                            modifier = Modifier.size(64.dp)
                        ) {
                            if (profile.photoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(profile.photoUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        profile.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 24.sp,
                                        color = Color(0xFF1A237E)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Text(profile.email, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("Phone: ${profile.phoneNumber}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))

                            Spacer(Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = storeStatusColor
                                ) {
                                    Text(
                                        storeStatusText,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFD700)
                                ) {
                                    Text(
                                        "BADGE: ${profile.merchantBadge}",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Admin Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.sendPasswordResetEmail(profile.email) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.LockReset, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset Password", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (!isMasterAdmin) {
                            if (profile.active) {
                                Button(
                                    onClick = { showBlockDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Block, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Block Store", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.updateUserStatus(profile.uid, true) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Unblock Store", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Tab Navigation - 3 Sections
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
                // Section 1: Merchants Profile Details
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            DetailCardSection(
                                title = "Business & Legal Details",
                                icon = Icons.Default.Business,
                                items = listOf(
                                    "Trade Name" to profile.tradeName.ifBlank { "Not provided" },
                                    "Legal Name" to profile.legalName.ifBlank { "Not provided" },
                                    "Business Type" to profile.businessType.ifBlank { "Retail Merchant" },
                                    "Contact Person" to profile.contactPerson.ifBlank { profile.name },
                                    "GSTIN Number" to profile.gstin.ifBlank { "N/A" },
                                    "FSSAI License No." to profile.fssaiNumber.ifBlank { "N/A" },
                                    "FSSAI Expiry" to profile.fssaiExpiryDate.ifBlank { "N/A" },
                                    "PAN Number" to profile.panNumber.ifBlank { "N/A" }
                                )
                            )
                        }

                        item {
                            DetailCardSection(
                                title = "Bank & Settlement Account",
                                icon = Icons.Default.AccountBalance,
                                items = listOf(
                                    "Bank Name" to profile.bankName.ifBlank { "Not provided" },
                                    "Account Holder" to profile.bankAccountName.ifBlank { "Not provided" },
                                    "Account Number" to profile.bankAccountNumber.ifBlank { "Not provided" },
                                    "IFSC Code" to profile.bankIfscCode.ifBlank { "Not provided" },
                                    "UPI ID (VPA)" to profile.upiId.ifBlank { "Not provided" }
                                )
                            )
                        }

                        item {
                            DetailCardSection(
                                title = "Store Operations & Hub",
                                icon = Icons.Default.LocationOn,
                                items = listOf(
                                    "Store Operating Status" to storeStatusText,
                                    "Store Address" to profile.storeAddress.ifBlank { "Not provided" },
                                    "City / State" to "${profile.city}, ${profile.state} - ${profile.pincode}".trim(' ', ','),
                                    "Avg Prep Time" to "${profile.averagePreparationTime} Minutes",
                                    "Assigned Hub ID" to (profile.darkStoreId ?: "").ifBlank { "Default Main Hub" }
                                )
                            )
                        }

                        item {
                            DetailCardSection(
                                title = "Referral & Ecosystem Metrics",
                                icon = Icons.Default.CardGiftcard,
                                items = listOf(
                                    "Merchant Referral Code" to profile.merchantReferralCode.ifBlank { "N/A" },
                                    "Total Merchant Leads" to profile.referralCount.toString(),
                                    "Verified Onboardings" to profile.referralVerifiedCount.toString(),
                                    "Flash-O Coins Balance" to "©${profile.flashOCoins.toInt()}",
                                    "Platform Fee Discount" to "${profile.platformFeeDiscount}%"
                                )
                            )
                        }

                        // Admin Remarks / Notes Box
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.NoteAlt, null, tint = Color(0xFF1A237E), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Admin Internal Notes & Remarks", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A237E))
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = adminRemark,
                                        onValueChange = { adminRemark = it },
                                        placeholder = { Text("Enter internal notes, audit remarks or compliance flags...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(
                                            onClick = {
                                                isSavingRemark = true
                                                viewModel.updateUserProfileDetails(profile.copy(blockReason = adminRemark))
                                                isSavingRemark = false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
                                        ) {
                                            Text("Save Internal Note", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            if (!isMasterAdmin) {
                                Button(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.DeleteForever, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("PERMANENTLY DELETE MERCHANT ACCOUNT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Section 2: Products
                1 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Summary Card at Top
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
                                    Text("Merchant Product Catalog", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${merchantProducts.size} Total Items Listed", fontSize = 12.sp, color = Color.Gray)
                                }
                                val verifiedCount = merchantProducts.count { it.isVerified }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF2E7D32).copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        "$verifiedCount Verified",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (merchantProducts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.LightGray
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text("No products listed by this merchant.", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(merchantProducts, key = { it.id }) { product ->
                                    ProductCard(
                                        product = product,
                                        viewModel = viewModel,
                                        onEditClick = { }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Financial Details & Business Analytics
                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // 1. Financial Overview
                        item {
                            SectionTitle("Financial Overview & Commission")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard("Gross Sales", "₹${totalRevenue.toInt()}", Icons.Default.Payments, Color(0xFF4CAF50), Modifier.weight(1f))
                                StatCard("Merchant Payout", "₹${netMerchantPayout.toInt()}", Icons.Default.AccountBalanceWallet, Color(0xFF2196F3), Modifier.weight(1f))
                            }
                        }

                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard("Platform Profit", "₹${platformCommission.toInt()}", Icons.Default.Savings, Color(0xFF673AB7), Modifier.weight(1f))
                                StatCard("Total Orders", userOrders.size.toString(), Icons.Default.ShoppingBag, Color(0xFFFF9800), Modifier.weight(1f))
                            }
                        }

                        // Custom Commission / Fee Adjustment Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Platform Fee Discount", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Current Applied Discount: ${profile.platformFeeDiscount}%", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    OutlinedButton(
                                        onClick = { showCommissionDialog = true },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Adjust Fee", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Bestselling Products Card
                        if (topBestsellers.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Top 3 Bestselling Products", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A237E))
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        topBestsellers.forEachIndexed { index, (name, qty) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${index + 1}. $name", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, maxLines = 1, modifier = Modifier.weight(1f))
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFF1A237E).copy(alpha = 0.1f)
                                                ) {
                                                    Text("$qty Units Sold", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                                }
                                            }
                                            if (index < topBestsellers.size - 1) {
                                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Order Status Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A237E))
                                    Spacer(Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Delivered Orders", fontSize = 13.sp, color = Color.Gray)
                                        Text("${deliveredOrders.size}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Pending Orders", fontSize = 13.sp, color = Color.Gray)
                                        Text("$pendingOrdersCount", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Cancelled Orders", fontSize = 13.sp, color = Color.Gray)
                                        Text("$cancelledOrdersCount", fontWeight = FontWeight.Bold, color = Color.Red)
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Average Order Value (AOV)", fontSize = 13.sp, color = Color.Gray)
                                        Text("₹${avgOrderValue.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                                    }
                                }
                            }
                        }

                        // 2. Flasho Business Analytics
                        item {
                            SectionTitle("Flasho Business Analytics")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF1A237E), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("7-Day Revenue Trend", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A237E))
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    SimpleLineChart(data = trendData, color = Color(0xFF1A237E))
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Min Daily: ₹${trendData.minOrNull()?.toInt() ?: 0}", fontSize = 11.sp, color = Color.Gray)
                                        Text("Max Daily: ₹${trendData.maxOrNull()?.toInt() ?: 0}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        item {
                            DetailCardSection(
                                title = "Flasho Performance Metrics",
                                icon = Icons.Default.Analytics,
                                items = listOf(
                                    "Fulfillment Success Rate" to String.format(Locale.getDefault(), "%.1f%%", completionRate),
                                    "Avg Preparation Time" to "${profile.averagePreparationTime} mins",
                                    "Flash-O Coins Earned" to "©${profile.flashOCoins.toInt()}",
                                    "Merchant Badge Tier" to profile.merchantBadge,
                                    "Referral Leads Generated" to profile.referralCount.toString()
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailCardSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFF1A237E), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A237E))
            }

            Spacer(Modifier.height(12.dp))

            items.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, fontSize = 12.sp, color = Color.Gray)
                    Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                if (index < items.size - 1) {
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
