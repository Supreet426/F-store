package com.fryumm.store.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.fryumm.store.data.model.DarkStore
import com.fryumm.store.data.model.LocationPoint
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.ui.theme.FStorePurple
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.google.android.gms.location.LocationServices
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkStoreManagementScreen(viewModel: StoreViewModel, onBack: () -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var selectedStore by remember { mutableStateOf<DarkStore?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var deleteConfirmStore by remember { mutableStateOf<DarkStore?>(null) }

    val allMerchants = remember(viewModel.allUsers) {
        viewModel.allUsers.filter { it.userRole == UserRole.MERCHANT || it.userRole == UserRole.STORE_OWNER }
    }

    val allRiders = remember(viewModel.allUsers) {
        viewModel.allUsers.filter { it.userRole == UserRole.RIDER }
    }

    val filteredStores = remember(viewModel.darkStores, searchQuery, allMerchants, allRiders) {
        if (searchQuery.isBlank()) {
            viewModel.darkStores
        } else {
            val q = searchQuery.lowercase().trim()
            viewModel.darkStores.filter { ds ->
                ds.name.lowercase().contains(q) ||
                ds.managerName.lowercase().contains(q) ||
                ds.address.lowercase().contains(q) ||
                allMerchants.any { m ->
                    (m.darkStoreId == ds.id || ds.assignedMerchantIds.contains(m.uid)) &&
                    (m.name.lowercase().contains(q) || m.tradeName.lowercase().contains(q) || m.email.lowercase().contains(q))
                } ||
                allRiders.any { r ->
                    (r.darkStoreId == ds.id || ds.assignedRiderIds.contains(r.uid)) &&
                    (r.name.lowercase().contains(q) || r.email.lowercase().contains(q) || r.phoneNumber.contains(q))
                }
            }
        }
    }

    if (isEditing) {
        EditDarkStoreFullScreen(
            store = selectedStore ?: DarkStore(),
            allMerchants = allMerchants,
            allRiders = allRiders,
            allProducts = viewModel.products,
            onBack = {
                isEditing = false
                selectedStore = null
            },
            onSave = { updatedStore, assignedMerchantIds, assignedRiderIds ->
                viewModel.updateDarkStore(updatedStore)
                // Sync merchant assignments
                allMerchants.forEach { merchant ->
                    val shouldBeAssigned = assignedMerchantIds.contains(merchant.uid)
                    val isCurrentlyAssigned = merchant.darkStoreId == updatedStore.id
                    if (shouldBeAssigned && !isCurrentlyAssigned) {
                        viewModel.assignMerchantToDarkStore(merchant.uid, updatedStore.id)
                    } else if (!shouldBeAssigned && isCurrentlyAssigned) {
                        viewModel.assignMerchantToDarkStore(merchant.uid, null)
                    }
                }
                // Sync rider assignments
                allRiders.forEach { rider ->
                    val shouldBeAssigned = assignedRiderIds.contains(rider.uid)
                    val isCurrentlyAssigned = rider.darkStoreId == updatedStore.id
                    if (shouldBeAssigned && !isCurrentlyAssigned) {
                        viewModel.assignRiderToDarkStore(rider.uid, updatedStore.id)
                    } else if (!shouldBeAssigned && isCurrentlyAssigned) {
                        viewModel.assignRiderToDarkStore(rider.uid, null)
                    }
                }
                isEditing = false
                selectedStore = null
            },
            onAssignMerchant = { merchantId, hubId ->
                viewModel.assignMerchantToDarkStore(merchantId, hubId)
            },
            onAssignRider = { riderId, hubId ->
                viewModel.assignRiderToDarkStore(riderId, hubId)
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text("Dark Store Fulfillment Hubs", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                selectedStore = DarkStore()
                                isEditing = true
                            }
                        ) {
                            Icon(Icons.Default.AddBusiness, contentDescription = "Add Hub")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FStorePurple,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedStore = DarkStore()
                        isEditing = true
                    },
                    icon = { Icon(Icons.Default.AddHomeWork, contentDescription = null) },
                    text = { Text("Add Fulfillment Hub", fontWeight = FontWeight.Bold) },
                    containerColor = FStorePurple,
                    contentColor = Color.White
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF6F7F9))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Banner Summary
                item {
                    DarkStoreHeaderBanner(
                        totalHubs = viewModel.darkStores.size,
                        totalAssignedMerchants = allMerchants.count { !it.darkStoreId.isNullOrEmpty() },
                        totalAssignedRiders = allRiders.count { !it.darkStoreId.isNullOrEmpty() }
                    )
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search Hubs, Managers, Merchants or Riders...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FStorePurple) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = FStorePurple,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true
                    )
                }

                if (filteredStores.isEmpty()) {
                    item {
                        EmptyDarkStoresView(
                            searchQuery = searchQuery,
                            onAddClick = {
                                selectedStore = DarkStore()
                                isEditing = true
                            }
                        )
                    }
                } else {
                    items(filteredStores, key = { it.id }) { store ->
                        val hubMerchants = allMerchants.filter { 
                            it.darkStoreId == store.id || store.assignedMerchantIds.contains(it.uid) 
                        }
                        val hubRiders = allRiders.filter {
                            it.darkStoreId == store.id || store.assignedRiderIds.contains(it.uid)
                        }
                        val hubProductsCount = viewModel.products.count { prod ->
                            hubMerchants.any { m -> m.uid == prod.sellerId }
                        }

                        DarkStoreCardFull(
                            store = store,
                            assignedMerchants = hubMerchants,
                            assignedRiders = hubRiders,
                            totalProductsInHub = hubProductsCount,
                            onEdit = {
                                selectedStore = store
                                isEditing = true
                            },
                            onDelete = { deleteConfirmStore = store }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (deleteConfirmStore != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmStore = null },
            title = { Text("Delete Dark Store Hub?", fontWeight = FontWeight.Bold) },
            text = { 
                Text("Are you sure you want to delete '${deleteConfirmStore?.name}'?\n\n🛡️ Safe Protection: All linked merchants and riders will be safely unassigned without losing any account data.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmStore?.id?.let { viewModel.deleteDarkStore(it) }
                        deleteConfirmStore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Safe Delete & Unassign", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmStore = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DarkStoreHeaderBanner(totalHubs: Int, totalAssignedMerchants: Int, totalAssignedRiders: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF311B92), Color(0xFF512DA8), Color(0xFF673AB7))
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warehouse,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp).size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Fulfillment Network & Operations",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Merchants & Delivery Partners assigned to dark store hubs",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BannerMetricItem(title = "Active Hubs", value = totalHubs.toString())
                    BannerMetricItem(title = "Linked Merchants", value = totalAssignedMerchants.toString())
                    BannerMetricItem(title = "Linked Riders", value = totalAssignedRiders.toString())
                }
            }
        }
    }
}

@Composable
fun BannerMetricItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
        Text(title, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
fun DarkStoreCardFull(
    store: DarkStore,
    assignedMerchants: List<UserProfile>,
    assignedRiders: List<UserProfile>,
    totalProductsInHub: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = FStorePurple.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(24.dp),
                        tint = FStorePurple
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            store.name.ifEmpty { "Unnamed Dark Store" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = if (store.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                if (store.isActive) "ACTIVE" else "INACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (store.isActive) Color(0xFF2E7D32) else Color.Red,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (store.address.isNotEmpty()) {
                        Text(store.address, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text("Manager: ${store.managerName.ifEmpty { "Not Assigned" }}", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Hub", tint = Color.Red.copy(alpha = 0.6f))
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

            // Radius Badges & Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFFEDE7F6),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = FStorePurple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Delivery Radius", fontSize = 9.sp, color = Color.Gray)
                            Text("${store.deliveryRadiusKm} KM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                        }
                    }
                }

                Surface(
                    color = Color(0xFFE1F5FE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Visibility Radius", fontSize = 9.sp, color = Color.Gray)
                            Text("${store.visibilityRadiusKm} KM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                        }
                    }
                }
            }

            // Radius & Product Delivery Scope Notice
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, Color(0xFFFFB300))
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Merchants (${assignedMerchants.size}) & Riders (${assignedRiders.size}): Linked riders handle order delivery within ${store.deliveryRadiusKm}km radius.",
                        fontSize = 10.sp,
                        color = Color(0xFF5D4037)
                    )
                }
            }

            // Merchants List Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "REGISTERED MERCHANTS (${assignedMerchants.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FStorePurple
                    )
                    Text(
                        "$totalProductsInHub Products",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }

                if (assignedMerchants.isEmpty()) {
                    Surface(
                        color = Color(0xFFFAFAFA),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit() }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = FStorePurple, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Merchants to this Dark Store Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(assignedMerchants) { merchant ->
                            Surface(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, Color.LightGray)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = FStorePurple,
                                        shape = CircleShape,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                merchant.name.take(1).uppercase(),
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        merchant.tradeName.ifEmpty { merchant.name },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Riders List Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "LINKED DELIVERY RIDERS (${assignedRiders.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0288D1)
                    )
                }

                if (assignedRiders.isEmpty()) {
                    Surface(
                        color = Color(0xFFFAFAFA),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit() }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Assign Delivery Riders to this Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(assignedRiders) { rider ->
                            Surface(
                                color = Color(0xFFE1F5FE),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, Color(0xFF81D4FA))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        rider.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF01579B),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Edit & Manage Action Button
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit Hub, Merchants & Riders", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyDarkStoresView(searchQuery: String, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Warehouse, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
            Text(
                if (searchQuery.isBlank()) "No Dark Store Hubs Found" else "No matching hubs for '$searchQuery'",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "Set up a dark store fulfillment hub to service nearby customers and connect local merchants & riders.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Create New Dark Store")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDarkStoreFullScreen(
    store: DarkStore,
    allMerchants: List<UserProfile>,
    allRiders: List<UserProfile>,
    allProducts: List<com.fryumm.store.data.model.Product>,
    onBack: () -> Unit,
    onSave: (DarkStore, List<String>, List<String>) -> Unit,
    onAssignMerchant: (String, String?) -> Unit,
    onAssignRider: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var name by remember { mutableStateOf(store.name) }
    var address by remember { mutableStateOf(store.address) }
    var lat by remember { mutableStateOf(if (store.centerLatitude == 0.0) "" else store.centerLatitude.toString()) }
    var lng by remember { mutableStateOf(if (store.centerLongitude == 0.0) "" else store.centerLongitude.toString()) }
    var deliveryRadius by remember { mutableStateOf(store.deliveryRadiusKm.toString()) }
    var visibilityRadius by remember { mutableStateOf(store.visibilityRadiusKm.toString()) }
    var isActive by remember { mutableStateOf(store.isActive) }

    var managerName by remember { mutableStateOf(store.managerName) }
    var managerMobile by remember { mutableStateOf(store.managerMobile) }
    var aadharNumber by remember { mutableStateOf(store.aadharNumber) }

    var bankName by remember { mutableStateOf(store.bankName) }
    var bankAccount by remember { mutableStateOf(store.bankAccountNumber) }
    var bankIfsc by remember { mutableStateOf(store.bankIfsc) }

    // Merchant assignment list
    var assignedMerchantIds by remember {
        mutableStateOf(
            (store.assignedMerchantIds + allMerchants.filter { it.darkStoreId == store.id }.map { it.uid }).distinct().toSet()
        )
    }
    var merchantSearchQuery by remember { mutableStateOf("") }
    var merchantFilter by remember { mutableStateOf("ALL") }

    // Rider assignment list
    var assignedRiderIds by remember {
        mutableStateOf(
            (store.assignedRiderIds + allRiders.filter { it.darkStoreId == store.id }.map { it.uid }).distinct().toSet()
        )
    }
    var riderSearchQuery by remember { mutableStateOf("") }
    var riderFilter by remember { mutableStateOf("ALL") }

    // Exception Management
    var exceptions by remember { mutableStateOf(store.allowedExceptions) }
    var newExName by remember { mutableStateOf("") }
    var newExLat by remember { mutableStateOf("") }
    var newExLng by remember { mutableStateOf("") }
    var newExRadius by remember { mutableStateOf("500") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    lat = String.format(Locale.US, "%.6f", it.latitude)
                    lng = String.format(Locale.US, "%.6f", it.longitude)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (store.id.isEmpty()) "Add New Fulfillment Hub" else "Edit Hub: ${store.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val updatedStore = store.copy(
                                name = name,
                                address = address,
                                centerLatitude = lat.toDoubleOrNull() ?: 0.0,
                                centerLongitude = lng.toDoubleOrNull() ?: 0.0,
                                deliveryRadiusKm = deliveryRadius.toDoubleOrNull() ?: 5.0,
                                visibilityRadiusKm = visibilityRadius.toDoubleOrNull() ?: 5.0,
                                isActive = isActive,
                                managerName = managerName,
                                managerMobile = managerMobile,
                                aadharNumber = aadharNumber,
                                bankName = bankName,
                                bankAccountNumber = bankAccount,
                                bankIfsc = bankIfsc,
                                allowedExceptions = exceptions,
                                assignedMerchantIds = assignedMerchantIds.toList(),
                                assignedRiderIds = assignedRiderIds.toList()
                            )
                            onSave(updatedStore, assignedMerchantIds.toList(), assignedRiderIds.toList())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("SAVE HUB", color = FStorePurple, fontWeight = FontWeight.Black, fontSize = 12.sp)
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF6F7F9))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. BASIC INFORMATION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("1. HUB BASIC DETAILS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FStorePurple)
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Fulfillment Hub Name *") },
                            placeholder = { Text("e.g. Flash-O Central Dark Store") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Full Address / Landmark") },
                            placeholder = { Text("e.g. Sector 18, Cyber Hub, Metro Pillar 42") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Active Status:", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                    }
                }
            }

            // 2. LOCATION & SERVICE RADIUS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("2. LOCATION & COVERAGE RADIUS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FStorePurple)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lat,
                                onValueChange = { lat = it },
                                label = { Text("Center Lat") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = lng,
                                onValueChange = { lng = it },
                                label = { Text("Center Lng") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        location?.let {
                                            lat = String.format(Locale.US, "%.6f", it.latitude)
                                            lng = String.format(Locale.US, "%.6f", it.longitude)
                                        }
                                    }
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("GET CURRENT GPS LOCATION", fontSize = 11.sp)
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

                        // Delivery Radius
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Delivery Service Radius:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${deliveryRadius} KM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                            }
                            Slider(
                                value = deliveryRadius.toFloatOrNull() ?: 5f,
                                onValueChange = { deliveryRadius = String.format(Locale.US, "%.1f", it) },
                                valueRange = 1f..30f,
                                colors = SliderDefaults.colors(thumbColor = FStorePurple, activeTrackColor = FStorePurple)
                            )
                        }

                        // Visibility Radius
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Visibility Radius (Catalog Display):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${visibilityRadius} KM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                            }
                            Slider(
                                value = visibilityRadius.toFloatOrNull() ?: 5f,
                                onValueChange = { visibilityRadius = String.format(Locale.US, "%.1f", it) },
                                valueRange = 1f..30f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF0288D1), activeTrackColor = Color(0xFF0288D1))
                            )
                        }

                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF2E7D32))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Merchants and Riders added below share this hub's ${deliveryRadius}km delivery radius. Orders from linked merchants are dispatched to riders linked to this hub.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                }
            }

            // 3. REGISTERED MERCHANTS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("3. REGISTERED MERCHANTS IN THIS HUB", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FStorePurple)
                                Text("Select local merchants who fulfill orders from this Dark Store", fontSize = 11.sp, color = Color.Gray)
                            }
                            Surface(color = FStorePurple, shape = CircleShape) {
                                Text(
                                    "${assignedMerchantIds.size}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = merchantSearchQuery,
                            onValueChange = { merchantSearchQuery = it },
                            placeholder = { Text("Search merchant by name, trade name or email...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                            trailingIcon = {
                                if (merchantSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { merchantSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = merchantFilter == "ALL",
                                onClick = { merchantFilter = "ALL" },
                                label = { Text("All (${allMerchants.size})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = merchantFilter == "ASSIGNED",
                                onClick = { merchantFilter = "ASSIGNED" },
                                label = { Text("Assigned (${assignedMerchantIds.size})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = merchantFilter == "UNASSIGNED",
                                onClick = { merchantFilter = "UNASSIGNED" },
                                label = { Text("Unassigned", fontSize = 11.sp) }
                            )
                        }

                        val filteredMerchantsList = remember(allMerchants, merchantSearchQuery, merchantFilter, assignedMerchantIds) {
                            allMerchants.filter { merchant ->
                                val matchesSearch = merchantSearchQuery.isBlank() ||
                                        merchant.name.contains(merchantSearchQuery, ignoreCase = true) ||
                                        merchant.tradeName.contains(merchantSearchQuery, ignoreCase = true) ||
                                        merchant.email.contains(merchantSearchQuery, ignoreCase = true)

                                val isAssigned = assignedMerchantIds.contains(merchant.uid)
                                val matchesFilter = when (merchantFilter) {
                                    "ASSIGNED" -> isAssigned
                                    "UNASSIGNED" -> !isAssigned
                                    else -> true
                                }

                                matchesSearch && matchesFilter
                            }
                        }

                        if (filteredMerchantsList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No merchants found matching your filter.", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredMerchantsList.forEach { merchant ->
                                    val isAssigned = assignedMerchantIds.contains(merchant.uid)
                                    val merchantProductsCount = allProducts.count { it.sellerId == merchant.uid }

                                    Surface(
                                        color = if (isAssigned) Color(0xFFF3E5F5) else Color(0xFFFAFAFA),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isAssigned) FStorePurple.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    merchant.tradeName.ifEmpty { merchant.name },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                if (merchant.email.isNotEmpty()) {
                                                    Text(merchant.email, fontSize = 11.sp, color = Color.Gray)
                                                }
                                                Text(
                                                    "Catalog: $merchantProductsCount products • ${merchant.city.ifEmpty { "Local Area" }}",
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val newSet = assignedMerchantIds.toMutableSet()
                                                    if (isAssigned) {
                                                        newSet.remove(merchant.uid)
                                                        if (store.id.isNotEmpty()) {
                                                            onAssignMerchant(merchant.uid, null)
                                                        }
                                                    } else {
                                                        newSet.add(merchant.uid)
                                                        if (store.id.isNotEmpty()) {
                                                            onAssignMerchant(merchant.uid, store.id)
                                                        }
                                                    }
                                                    assignedMerchantIds = newSet
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isAssigned) Color.Red.copy(alpha = 0.8f) else FStorePurple
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(
                                                    if (isAssigned) Icons.Default.RemoveCircle else Icons.Default.AddCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    if (isAssigned) "Remove" else "Add to Hub",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
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

            // 4. LINKED DELIVERY RIDERS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("4. LINKED DELIVERY RIDERS IN THIS HUB", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0288D1))
                                Text("Assign riders operating exclusively within this dark store radius", fontSize = 11.sp, color = Color.Gray)
                            }
                            Surface(color = Color(0xFF0288D1), shape = CircleShape) {
                                Text(
                                    "${assignedRiderIds.size}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = riderSearchQuery,
                            onValueChange = { riderSearchQuery = it },
                            placeholder = { Text("Search rider by name, phone or email...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                            trailingIcon = {
                                if (riderSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { riderSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = riderFilter == "ALL",
                                onClick = { riderFilter = "ALL" },
                                label = { Text("All (${allRiders.size})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = riderFilter == "ASSIGNED",
                                onClick = { riderFilter = "ASSIGNED" },
                                label = { Text("Assigned (${assignedRiderIds.size})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = riderFilter == "UNASSIGNED",
                                onClick = { riderFilter = "UNASSIGNED" },
                                label = { Text("Unassigned", fontSize = 11.sp) }
                            )
                        }

                        val filteredRidersList = remember(allRiders, riderSearchQuery, riderFilter, assignedRiderIds) {
                            allRiders.filter { rider ->
                                val matchesSearch = riderSearchQuery.isBlank() ||
                                        rider.name.contains(riderSearchQuery, ignoreCase = true) ||
                                        rider.phoneNumber.contains(riderSearchQuery, ignoreCase = true) ||
                                        rider.email.contains(riderSearchQuery, ignoreCase = true)

                                val isAssigned = assignedRiderIds.contains(rider.uid)
                                val matchesFilter = when (riderFilter) {
                                    "ASSIGNED" -> isAssigned
                                    "UNASSIGNED" -> !isAssigned
                                    else -> true
                                }

                                matchesSearch && matchesFilter
                            }
                        }

                        if (filteredRidersList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No riders found matching your filter.", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredRidersList.forEach { rider ->
                                    val isAssigned = assignedRiderIds.contains(rider.uid)

                                    Surface(
                                        color = if (isAssigned) Color(0xFFE1F5FE) else Color(0xFFFAFAFA),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isAssigned) Color(0xFF0288D1).copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    rider.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                if (rider.phoneNumber.isNotEmpty()) {
                                                    Text(rider.phoneNumber, fontSize = 11.sp, color = Color.Gray)
                                                }
                                                Text(
                                                    if (rider.active) "Status: Online & Ready" else "Status: Offline",
                                                    fontSize = 10.sp,
                                                    color = if (rider.active) Color(0xFF2E7D32) else Color.Gray
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val newSet = assignedRiderIds.toMutableSet()
                                                    if (isAssigned) {
                                                        newSet.remove(rider.uid)
                                                        if (store.id.isNotEmpty()) {
                                                            onAssignRider(rider.uid, null)
                                                        }
                                                    } else {
                                                        newSet.add(rider.uid)
                                                        if (store.id.isNotEmpty()) {
                                                            onAssignRider(rider.uid, store.id)
                                                        }
                                                    }
                                                    assignedRiderIds = newSet
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isAssigned) Color.Red.copy(alpha = 0.8f) else Color(0xFF0288D1)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(
                                                    if (isAssigned) Icons.Default.RemoveCircle else Icons.Default.AddCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    if (isAssigned) "Remove" else "Assign Rider",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
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

            // 5. MANAGER DETAILS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("5. HUB MANAGER DETAILS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FStorePurple)
                        }

                        OutlinedTextField(
                            value = managerName,
                            onValueChange = { managerName = it },
                            label = { Text("Manager Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = managerMobile,
                                onValueChange = { managerMobile = it },
                                label = { Text("Manager Mobile") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            OutlinedTextField(
                                value = aadharNumber,
                                onValueChange = { aadharNumber = it },
                                label = { Text("Aadhar Number") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }

            // 6. BANK DETAILS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("6. BANK DETAILS FOR SETTLEMENTS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FStorePurple)
                        }

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bankAccount,
                                onValueChange = { bankAccount = it },
                                label = { Text("Account Number") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = bankIfsc,
                                onValueChange = { bankIfsc = it },
                                label = { Text("IFSC Code") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 7. SPECIAL ZONE EXCEPTIONS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("7. SPECIAL COVERAGE ZONES (EXCEPTIONS)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FStorePurple)
                        }

                        if (exceptions.isEmpty()) {
                            Text("No special exception zones added.", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            exceptions.forEach { ex ->
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ex.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("${ex.lat}, ${ex.lng} • Coverage: ${ex.radiusMeters}m", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        IconButton(onClick = { exceptions = exceptions - ex }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

                        Text("Add New Exception Point:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = newExName,
                            onValueChange = { newExName = it },
                            label = { Text("Area / Point Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newExLat,
                                onValueChange = { newExLat = it },
                                label = { Text("Lat") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = newExLng,
                                onValueChange = { newExLng = it },
                                label = { Text("Lng") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = newExRadius,
                                onValueChange = { newExRadius = it },
                                label = { Text("Radius (m)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Button(
                            onClick = {
                                if (newExName.isNotBlank() && newExLat.isNotBlank() && newExLng.isNotBlank()) {
                                    val point = LocationPoint(
                                        lat = newExLat.toDoubleOrNull() ?: 0.0,
                                        lng = newExLng.toDoubleOrNull() ?: 0.0,
                                        name = newExName,
                                        radiusMeters = newExRadius.toDoubleOrNull() ?: 500.0
                                    )
                                    exceptions = exceptions + point
                                    newExName = ""
                                    newExLat = ""
                                    newExLng = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("ADD EXCEPTION ZONE", fontSize = 11.sp)
                        }
                    }
                }
            }

            // SAVE BUTTON AT BOTTOM
            item {
                Button(
                    onClick = {
                        val updatedStore = store.copy(
                            name = name,
                            address = address,
                            centerLatitude = lat.toDoubleOrNull() ?: 0.0,
                            centerLongitude = lng.toDoubleOrNull() ?: 0.0,
                            deliveryRadiusKm = deliveryRadius.toDoubleOrNull() ?: 5.0,
                            visibilityRadiusKm = visibilityRadius.toDoubleOrNull() ?: 5.0,
                            isActive = isActive,
                            managerName = managerName,
                            managerMobile = managerMobile,
                            aadharNumber = aadharNumber,
                            bankName = bankName,
                            bankAccountNumber = bankAccount,
                            bankIfsc = bankIfsc,
                            allowedExceptions = exceptions,
                            assignedMerchantIds = assignedMerchantIds.toList(),
                            assignedRiderIds = assignedRiderIds.toList()
                        )
                        onSave(updatedStore, assignedMerchantIds.toList(), assignedRiderIds.toList())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("SAVE FULFILLMENT HUB", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
