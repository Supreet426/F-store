package com.fryumm.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavHostController
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.*
import com.fryumm.store.ui.screens.useractivity.*
import com.fryumm.store.ui.screens.profiles.*
import com.fryumm.store.ui.theme.FStorePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserActivityScreen(viewModel: StoreViewModel, navController: NavHostController, onTitleChange: (String) -> Unit = {}) {
    val allUsers = viewModel.allUsers
    
    var mainTab by remember { mutableStateOf("Customers") }
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedUserForDetails by remember { mutableStateOf<UserProfile?>(null) }
    var selectedCustomerForDetails by remember { mutableStateOf<CustomerProfile?>(null) }
    var selectedOrderForDetails by remember { mutableStateOf<Order?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userForRoleChange by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    // Notify parent of title changes based on current view
    LaunchedEffect(mainTab, selectedUserForDetails, selectedCustomerForDetails) {
        val title = when {
            selectedCustomerForDetails != null -> "Customer"
            selectedUserForDetails != null -> {
                when(selectedUserForDetails?.userRole) {
                    UserRole.STORE_OWNER -> "Merchant"
                    UserRole.RIDER -> "Rider"
                    else -> "Admin"
                }
            }
            else -> mainTab
        }
        onTitleChange(title)
    }
    
    // --- FULL PAGE NAVIGATION LOGIC ---
    if (selectedCustomerForDetails != null) {
        CustomerProfilePage(
            customer = selectedCustomerForDetails!!,
            viewModel = viewModel,
            onBack = { selectedCustomerForDetails = null }
        )
        return
    }

    if (selectedUserForDetails != null) {
        when(selectedUserForDetails?.userRole) {
            UserRole.STORE_OWNER, UserRole.MERCHANT -> {
                MerchantProfilePage(
                    profile = selectedUserForDetails!!,
                    viewModel = viewModel,
                    onBack = { selectedUserForDetails = null }
                )
            }
            UserRole.RIDER -> {
                val analytics = viewModel.getAdvancedAnalytics()
                RiderProfilePage(
                    profile = selectedUserForDetails!!,
                    performance = analytics.riderPerformance.find { it.riderId == selectedUserForDetails!!.uid },
                    viewModel = viewModel,
                    onBack = { selectedUserForDetails = null },
                    onStatusChange = { viewModel.updateUserStatus(selectedUserForDetails!!.uid, it) }
                )
            }
            else -> {
                UserProfileDetailsDialog(
                    user = selectedUserForDetails!!,
                    viewModel = viewModel,
                    onDismiss = { selectedUserForDetails = null }
                )
            }
        }
        // Only return for non-Admin roles that have full pages
        if (selectedUserForDetails?.userRole != UserRole.ADMIN) return
    }
    // -----------------------------------

    val filteredUsers = when(mainTab) {
        "Merchants" -> allUsers.filter { it.userRole == UserRole.STORE_OWNER || it.userRole == UserRole.MERCHANT }
        "Admins" -> allUsers.filter { it.userRole == UserRole.ADMIN }
        "Riders" -> allUsers.filter { it.userRole == UserRole.RIDER }
        "Staff (Others)" -> allUsers.filter { it.userRole == UserRole.OTHER }
        else -> emptyList()
    }.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.phoneNumber.contains(searchQuery, ignoreCase = true) ||
        it.staffDepartment.contains(searchQuery, ignoreCase = true)
    }

    if (selectedOrderForDetails != null) {
        OrderAnalyticsDetailsDialog(
            order = selectedOrderForDetails!!,
            onDismiss = { selectedOrderForDetails = null }
        )
    }

    if (userForRoleChange != null) {
        RoleChangeDialog(
            userName = userForRoleChange!!.second,
            currentRole = userForRoleChange!!.third,
            onDismiss = { userForRoleChange = null },
            onConfirmNewRole = { newRole ->
                viewModel.changeUserRole(userForRoleChange!!.first, newRole)
                userForRoleChange = null
            }
        )
    }

    if (showAddUserDialog) {
        AddUserDialog(
            initialRole = when(mainTab) {
                "Merchants" -> UserRole.STORE_OWNER
                "Riders" -> UserRole.RIDER
                "Staff (Others)" -> UserRole.OTHER
                else -> UserRole.STORE_OWNER
            },
            isMasterAdmin = viewModel.isMasterAdmin,
            onDismiss = { showAddUserDialog = false },
            onConfirm = { email, password, name, phone, role, dept ->
                viewModel.register(email, password, name, phone, role, dept)
                showAddUserDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (mainTab != "Customers") {
                FloatingActionButton(
                    onClick = { showAddUserDialog = true },
                    containerColor = FStorePurple,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add User")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8F9FA))) {
            // Pending Approvals Quick Action Banner
            val pendingCount = viewModel.products.count { !it.isVerified } + 
                    viewModel.allUsers.count { !it.active || it.roleChangeStatus == "PENDING" }
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { navController.navigate("pending_approvals") },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1A237E),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassTop, null, tint = Color.Yellow, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Pending Approvals Center", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("$pendingCount items require Admin verification", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search users or customers...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FStorePurple) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FStorePurple,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            ScrollableTabRow(
                selectedTabIndex = when(mainTab) {
                    "Customers" -> 0
                    "Riders" -> 1
                    "Merchants" -> 2
                    "Admins" -> 3
                    "Staff (Others)" -> 4
                    else -> 0
                },
                containerColor = Color.White,
                contentColor = FStorePurple,
                edgePadding = 16.dp
            ) {
                listOf("Customers", "Riders", "Merchants", "Admins", "Staff (Others)").forEach { tab ->
                    Tab(
                        selected = mainTab == tab,
                        onClick = { mainTab = tab },
                        text = { 
                            Text(text = tab, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    )
                }
            }

            when(mainTab) {
                "Customers" -> CustomerTab(
                    viewModel = viewModel,
                    searchQuery = searchQuery,
                    onCustomerClick = { selectedCustomerForDetails = it },
                    onOrderClick = { selectedOrderForDetails = it },
                    onRoleChangeClick = { uid, name, role -> userForRoleChange = Triple(uid, name, role) }
                )
                "Riders", "Merchants" -> RiderMerchantTab(
                    viewModel = viewModel,
                    mainTab = mainTab,
                    filteredUsers = filteredUsers,
                    onUserClick = { selectedUserForDetails = it },
                    onRoleChangeClick = { uid, name, role -> userForRoleChange = Triple(uid, name, role) }
                )
                else -> StaffAdminTab(
                    viewModel = viewModel,
                    filteredUsers = filteredUsers,
                    onUserClick = { selectedUserForDetails = it },
                    onRoleChangeClick = { uid, name, role -> userForRoleChange = Triple(uid, name, role) }
                )
            }
        }
    }
}
