package com.fryumm.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fryumm.store.data.model.*
import com.fryumm.store.ui.components.ProductDetailDialog
import com.fryumm.store.ui.screens.profiles.MerchantProfilePage
import com.fryumm.store.ui.theme.FStorePurple
import com.fryumm.store.ui.viewmodels.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingApprovalsScreen(
    viewModel: StoreViewModel,
    navController: NavHostController,
    onBack: () -> Unit
) {
    val allUsers = viewModel.allUsers
    val allCustomers = viewModel.allCustomers

    // Filter Pending Items per category
    val pendingProducts = viewModel.pendingReviewProducts
    val pendingProductsLoading = viewModel.pendingProductsLoading
    val pendingProductsError = viewModel.pendingProductsError
    val pendingMerchants = remember(allUsers) {
        allUsers.filter { (it.userRole == UserRole.STORE_OWNER || it.userRole == UserRole.MERCHANT) && (!it.active || it.blockReason?.contains("Pending Verification") == true) }
    }
    val pendingRoleRequests = remember(allUsers) {
        allUsers.filter { it.roleChangeStatus == "PENDING" || it.requestedRole.isNotBlank() }
    }
    val pendingRiders = remember(allUsers) {
        allUsers.filter { it.userRole == UserRole.RIDER && !it.active }
    }
    val doubtfulProfiles = remember(allUsers) {
        allUsers.filter { !it.active && (it.blockReason?.isNotBlank() == true) }
    }
    val pendingReferrals = remember(allCustomers) {
        allCustomers.filter { it.referralVerificationStatus == "PENDING" || it.referralEarningsFrozen }
    }

    val totalPendingCount = pendingProducts.size + pendingMerchants.size + pendingRoleRequests.size +
            pendingRiders.size + doubtfulProfiles.size + pendingReferrals.size

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "Products (${pendingProducts.size})",
        "New Merchants (${pendingMerchants.size})",
        "Role Changes (${pendingRoleRequests.size})",
        "Riders Jobs (${pendingRiders.size})",
        "Flagged Profiles (${doubtfulProfiles.size})",
        "Refer & Rewards (${pendingReferrals.size})"
    )

    var selectedMerchantForDetails by remember { mutableStateOf<UserProfile?>(null) }
    var selectedProductForApproval by remember { mutableStateOf<Product?>(null) }

    if (selectedMerchantForDetails != null) {
        MerchantProfilePage(
            profile = selectedMerchantForDetails!!,
            viewModel = viewModel,
            onBack = { selectedMerchantForDetails = null }
        )
        return
    }

    // Product Approval Config Dialog
    if (selectedProductForApproval != null) {
        val prod = selectedProductForApproval!!
        var commissionInput by remember { mutableStateOf("10.0") }
        var profitInput by remember { mutableStateOf("5.0") }
        var deliveryChargeInput by remember { mutableStateOf(prod.deliveryCharge.toString()) }
        var gstInput by remember { mutableStateOf(prod.taxRate.toString()) }
        var rejectReason by remember { mutableStateOf("") }
        var isRejecting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedProductForApproval = null },
            title = {
                Text(
                    if (isRejecting) "Reject Product" else "Approve Product Listing",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(prod.name, fontWeight = FontWeight.Bold, color = FStorePurple, fontSize = 16.sp)
                    Text("Merchant: ${prod.merchantName} | Price: ₹${prod.price}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))

                    if (!isRejecting) {
                        OutlinedTextField(
                            value = commissionInput,
                            onValueChange = { commissionInput = it },
                            label = { Text("Platform Commission %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = profitInput,
                            onValueChange = { profitInput = it },
                            label = { Text("Admin Profit %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = deliveryChargeInput,
                            onValueChange = { deliveryChargeInput = it },
                            label = { Text("Delivery Charge (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = gstInput,
                            onValueChange = { gstInput = it },
                            label = { Text("GST Rate %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            label = { Text("Reason for Rejection") },
                            placeholder = { Text("e.g. Low image quality, invalid GST/price") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (!isRejecting) {
                    Button(
                        onClick = {
                            val comm = commissionInput.toDoubleOrNull() ?: 10.0
                            val prof = profitInput.toDoubleOrNull() ?: 5.0
                            val del = deliveryChargeInput.toDoubleOrNull() ?: 0.0
                            val gst = gstInput.toDoubleOrNull() ?: 5.0
                            viewModel.approveProduct(prod.id, comm, prof, del, gst)
                            selectedProductForApproval = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("CONFIRM APPROVAL")
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.updateProductByAdmin(prod, "REJECTED: $rejectReason")
                            selectedProductForApproval = null
                        },
                        enabled = rejectReason.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("CONFIRM REJECT")
                    }
                }
            },
            dismissButton = {
                if (!isRejecting) {
                    TextButton(onClick = { isRejecting = true }) {
                        Text("REJECT PRODUCT", color = Color.Red)
                    }
                } else {
                    TextButton(onClick = { isRejecting = false }) {
                        Text("Back")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pending Approvals", fontWeight = FontWeight.Black)
                        Text("$totalPendingCount total items awaiting Admin review", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (selectedTab == 0 && pendingProducts.isNotEmpty()) {
                        TextButton(onClick = { viewModel.approveAllProducts() }) {
                            Text("APPROVE ALL", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
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
            // Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = FStorePurple,
                edgePadding = 12.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                // 1. Products Approvals
                0 -> {
                    when {
                        pendingProductsLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = FStorePurple)
                            }
                        }
                        pendingProductsError != null -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Failed to Load Approval Queue", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(pendingProductsError, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                                Button(onClick = { viewModel.loadPendingProductsForReview() }) {
                                    Text("Retry")
                                }
                            }
                        }
                        pendingProducts.isEmpty() -> {
                            EmptyStateCard("No Pending Products", "All submitted products have been verified.")
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(pendingProducts, key = { it.id }) { product ->
                                    PendingProductCard(
                                        product = product,
                                        viewModel = viewModel,
                                        onApprove = { selectedProductForApproval = product },
                                        onEdit = { navController.navigate("edit_product/${product.id}") }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. New Merchant Approvals
                1 -> {
                    if (pendingMerchants.isEmpty()) {
                        EmptyStateCard("No Pending Merchants", "All merchant accounts are currently active and verified.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(pendingMerchants, key = { it.uid }) { merchant ->
                                PendingMerchantCard(
                                    merchant = merchant,
                                    onApprove = { viewModel.approveMerchant(merchant.uid) },
                                    onViewProfile = { selectedMerchantForDetails = merchant }
                                )
                            }
                        }
                    }
                }

                // 3. Role Change Approvals
                2 -> {
                    if (pendingRoleRequests.isEmpty()) {
                        EmptyStateCard("No Role Change Requests", "There are no pending user role change applications.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(pendingRoleRequests, key = { it.uid }) { user ->
                                PendingRoleChangeCard(
                                    user = user,
                                    onApprove = { viewModel.approveRoleChange(user.uid, user.requestedRole.ifBlank { "MERCHANT" }) },
                                    onReject = { viewModel.rejectRoleChange(user.uid, "Rejected by Admin") }
                                )
                            }
                        }
                    }
                }

                // 4. Riders Job Requests
                3 -> {
                    if (pendingRiders.isEmpty()) {
                        EmptyStateCard("No Pending Rider Requests", "All delivery partner accounts are onboarded.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(pendingRiders, key = { it.uid }) { rider ->
                                PendingRiderCard(
                                    rider = rider,
                                    darkStores = viewModel.darkStores,
                                    onApprove = { darkStoreId -> viewModel.approveRider(rider.uid, darkStoreId) },
                                    onReject = { viewModel.deleteUser(rider.uid) }
                                )
                            }
                        }
                    }
                }

                // 5. Flagged / Doubtful Profiles
                4 -> {
                    if (doubtfulProfiles.isEmpty()) {
                        EmptyStateCard("No Flagged Profiles", "No account is currently blocked or under suspicious audit.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(doubtfulProfiles, key = { it.uid }) { profile ->
                                FlaggedProfileCard(
                                    profile = profile,
                                    onUnblock = { viewModel.updateUserStatus(profile.uid, true) },
                                    onDelete = { viewModel.deleteUser(profile.uid) }
                                )
                            }
                        }
                    }
                }

                // 6. Refer & Rewards Approvals
                5 -> {
                    if (pendingReferrals.isEmpty()) {
                        EmptyStateCard("No Pending Reward Verification", "No referral earnings or coins are frozen pending review.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(pendingReferrals, key = { it.id }) { customer ->
                                PendingRewardCard(
                                    customer = customer,
                                    onApprove = { viewModel.verifyCustomerReferralEarnings(customer.id, true, "ADMIN") },
                                    onReject = { viewModel.verifyCustomerReferralEarnings(customer.id, false, "ADMIN", "Fraud suspect") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(title: String, description: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircleOutline, null, modifier = Modifier.size(64.dp), tint = Color(0xFF2E7D32))
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(description, fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun PendingProductCard(
    product: Product,
    viewModel: StoreViewModel,
    onApprove: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    var showDetailDialog by remember { mutableStateOf(false) }

    if (showDetailDialog) {
        ProductDetailDialog(
            product = product,
            viewModel = viewModel,
            onDismiss = { showDetailDialog = false },
            onEdit = {
                showDetailDialog = false
                onEdit()
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetailDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F3F4)
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(product.imageUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Inventory2, null, tint = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text("Category: ${product.category} | Merchant: ${product.merchantName}", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MRP: ₹${product.originalPrice}", fontSize = 11.sp, color = Color.Gray, style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                    Spacer(Modifier.width(6.dp))
                    Text("Price: ₹${product.price}", fontWeight = FontWeight.Bold, color = FStorePurple, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFF3E0)) {
                        Text("Rate: ₹${product.merchantRate}", fontSize = 10.sp, color = Color(0xFFE65100), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = onApprove,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PendingMerchantCard(
    merchant: UserProfile,
    onApprove: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1A237E).copy(alpha = 0.1f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(merchant.name.take(1).uppercase(), fontWeight = FontWeight.Black, color = Color(0xFF1A237E), fontSize = 18.sp)
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(merchant.tradeName.ifBlank { merchant.name }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Owner: ${merchant.name} | Phone: ${merchant.phoneNumber}", fontSize = 12.sp, color = Color.Gray)
                    if (merchant.gstin.isNotBlank()) {
                        Text("GSTIN: ${merchant.gstin} | FSSAI: ${merchant.fssaiNumber}", fontSize = 11.sp, color = Color(0xFF1A237E))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onViewProfile,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("VIEW PROFILE", fontSize = 12.sp)
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Verified, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("APPROVE STORE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PendingRoleChangeCard(
    user: UserProfile,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(user.email, fontSize = 12.sp, color = Color.Gray)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = FStorePurple.copy(alpha = 0.1f)) {
                    Text(
                        "Request: ${user.requestedRole.ifBlank { "MERCHANT" }}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        color = FStorePurple,
                        fontSize = 11.sp
                    )
                }
            }

            if (user.roleChangeReason.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Reason: ${user.roleChangeReason}", fontSize = 12.sp, color = Color.DarkGray)
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("REJECT", fontSize = 12.sp)
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("APPROVE ROLE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PendingRiderCard(
    rider: UserProfile,
    darkStores: List<DarkStore>,
    onApprove: (String?) -> Unit,
    onReject: () -> Unit
) {
    var selectedDarkStoreId by remember { mutableStateOf(darkStores.firstOrNull()?.id ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(rider.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Phone: ${rider.phoneNumber} | Email: ${rider.email}", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))

            if (darkStores.isNotEmpty()) {
                Text("Assign Hub / Dark Store:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    darkStores.take(3).forEach { ds ->
                        FilterChip(
                            selected = selectedDarkStoreId == ds.id,
                            onClick = { selectedDarkStoreId = ds.id },
                            label = { Text(ds.name, fontSize = 11.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("DECLINE", fontSize = 12.sp)
                }

                Button(
                    onClick = { onApprove(selectedDarkStoreId) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("ONBOARD RIDER", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FlaggedProfileCard(
    profile: UserProfile,
    onUnblock: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Role: ${profile.userRole.name} | ${profile.email}", fontSize = 12.sp, color = Color.Gray)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = Color.Red.copy(alpha = 0.1f)) {
                    Text("BLOCKED / FLAGGED", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Block Reason: ${profile.blockReason ?: "Flagged for security review"}", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onUnblock,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("UNBLOCK ACCOUNT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("DELETE ACCOUNT", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PendingRewardCard(
    customer: CustomerProfile,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Email: ${customer.email} | Phone: ${customer.mobile}", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Referral Earnings Frozen: ₹${customer.lifetimeSpending}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("REVOKE & FLAG FRAUD", fontSize = 11.sp)
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("APPROVE PAYOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
