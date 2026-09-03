package com.fryumm.store.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.fryumm.store.ui.components.InventoryStatCard
import com.fryumm.store.ui.components.ProductCard
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun InventoryScreen(navController: NavHostController, viewModel: StoreViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyLowStock by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Approved, 1: Pending (For Merchants)
    var showImportOptions by remember { mutableStateOf(false) }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importProductsFromCsv(it) }
    }
    val jsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importProductsFromJson(it) }
    }

    if (showImportOptions) {
        AlertDialog(
            onDismissRequest = { showImportOptions = false },
            title = { Text("Bulk Import Products") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select a file format to import products into your inventory.", fontSize = 12.sp, color = Color.Gray)
                    
                    OutlinedButton(onClick = { csvPicker.launch("text/*"); showImportOptions = false }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.TableChart, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import from CSV")
                    }
                    
                    OutlinedButton(onClick = { jsonPicker.launch("application/json"); showImportOptions = false }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Code, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import from JSON")
                    }
                    
                    HorizontalDivider()
                    Text("CSV Header Format:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("brand,name,category,price,mrp,imageUrl,weight,stock,hsn,description", fontSize = 10.sp, color = Color.DarkGray)
                }
            },
            confirmButton = { TextButton(onClick = { showImportOptions = false }) { Text("CANCEL") } }
        )
    }
    
    val profile = viewModel.currentUserProfile
    val isAdmin = profile?.userRole == com.fryumm.store.data.model.UserRole.ADMIN || viewModel.isMasterAdmin

    val myProducts = viewModel.products // Already filtered in ViewModel for Merchants

    val filteredProducts = myProducts.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.category.contains(searchQuery, ignoreCase = true) ||
        it.barcode.contains(searchQuery)
    }

    val totalProducts = myProducts.size
    val outOfStock = myProducts.count { !it.isInStock || it.stockQuantity == 0 }
    val lowStockCount = myProducts.count { it.stockQuantity < it.minStockLevel && it.stockQuantity > 0 && it.isInStock }
    val pendingVerificationCount = myProducts.count { !it.isVerified }

    var displayProducts = if (showOnlyLowStock) {
        filteredProducts.filter { it.stockQuantity < it.minStockLevel || !it.isInStock }
    } else filteredProducts

    // Merchant specific tab filtering
    if (!isAdmin) {
        displayProducts = if (selectedTab == 0) {
            displayProducts.filter { it.isVerified }
        } else {
            displayProducts.filter { !it.isVerified }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Header - Compact Search & Filter Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).height(48.dp),
                    placeholder = { Text("Search products...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FStorePurple,
                        cursorColor = FStorePurple,
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color.White
                    )
                )
                
                Surface(
                    onClick = { showOnlyLowStock = !showOnlyLowStock },
                    shape = RoundedCornerShape(24.dp),
                    color = if (showOnlyLowStock) FStorePurple else Color(0xFFF5F5F5),
                    contentColor = if (showOnlyLowStock) Color.White else Color.DarkGray,
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (showOnlyLowStock) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Low Stock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(onClick = { showImportOptions = true }) {
                    Icon(Icons.Default.FileUpload, null, tint = FStorePurple)
                }
            }

            if (!isAdmin) {
                Spacer(Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = FStorePurple,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Approved (${myProducts.count { it.isVerified }})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Pending (${myProducts.count { !it.isVerified }})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)

        if (displayProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                    val emptyMsg = when {
                        searchQuery.isNotEmpty() -> "No results found"
                        !isAdmin && selectedTab == 1 -> "No pending products"
                        else -> "Your inventory is empty"
                    }
                    Text(emptyMsg, color = Color.Gray, fontSize = 14.sp)
                    if (!isAdmin && searchQuery.isEmpty()) {
                        Button(
                            onClick = { navController.navigate("add_product") },
                            modifier = Modifier.padding(top = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
                        ) {
                            Text("Add Your First Product")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Inline Stats - Scrolls away
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), 
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InventoryStatCard("Total", totalProducts.toString(), Modifier.weight(1f), FStorePurple)
                        InventoryStatCard("Low", lowStockCount.toString(), Modifier.weight(1f), Color(0xFFF57C00))
                        InventoryStatCard("OOS", outOfStock.toString(), Modifier.weight(1f), Color.Red)
                    }
                }

                items(displayProducts, key = { it.id }) { product ->
                    ProductCard(product, viewModel, onEditClick = {
                        navController.navigate("edit_product/${product.id}")
                    })
                }
                
                // Extra space at bottom
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
