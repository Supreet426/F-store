package com.fryumm.store.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.ui.screens.adminstores.*
import com.fryumm.store.ui.theme.FStorePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStoresScreen(
    viewModel: StoreViewModel, 
    navController: NavHostController,
    externalSearchQuery: String = ""
) {
    val allProducts = viewModel.products
    val config = viewModel.dynamicAppConfig
    val categorySequence = config.categorySequence
    val homeSectionOrder = config.homeSectionOrder

    var showImportOptions by remember { mutableStateOf(false) }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importProductsFromCsv(it) }
    }
    val jsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importProductsFromJson(it) }
    }

    if (showImportOptions) {
        BulkImportOptionsDialog(
            onDismiss = { showImportOptions = false },
            onCsvImport = { csvPicker.launch("text/*"); showImportOptions = false },
            onJsonImport = { jsonPicker.launch("application/json"); showImportOptions = false }
        )
    }
    
    val allKnownCategories = remember(categorySequence, homeSectionOrder, allProducts) {
        val fromProducts = allProducts.map { it.category }.distinct().filter { it.isNotEmpty() }
        val fromLayout = homeSectionOrder.map { it.split("|")[0] }
            .filter { it != "Banners" && it != "BANNER_AD" && it != "VIDEO_AD" && it != "PRODUCT_AD" && it != "FRYUMM_PASS" && it != "FLASH_D_PASS" }
        (categorySequence + fromProducts + fromLayout).distinct()
    }

    val orderedCategories = remember(allKnownCategories, categorySequence) {
        val ordered = categorySequence.filter { it in allKnownCategories }.toMutableList()
        val remaining = allKnownCategories.filter { it !in ordered }.sorted()
        ordered.addAll(remaining)
        ordered
    }
    
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var internalSearchQuery by remember { mutableStateOf("") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    val searchQuery = externalSearchQuery.ifEmpty { internalSearchQuery }
    val pendingApprovals = allProducts.filter { !it.isVerified }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { 
                viewModel.addCategory(it)
                showAddCategoryDialog = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column {
                if (selectedCategory != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedCategory = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Categories", tint = FStorePurple)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Category: $selectedCategory",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FStorePurple,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { selectedCategory = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Filter", tint = Color.Gray)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.White,
                        contentColor = FStorePurple,
                        modifier = Modifier.weight(1f),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = FStorePurple
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0; selectedCategory = null },
                            text = { Text("CATALOG", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1; selectedCategory = null },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("APPROVALS", fontWeight = FontWeight.Bold)
                                    if (pendingApprovals.isNotEmpty()) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            color = Color.Red,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = pendingApprovals.size.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                    if (selectedTabIndex == 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            IconButton(
                                onClick = { showImportOptions = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Import/Export CSV", tint = FStorePurple)
                            }
                            IconButton(
                                onClick = { showAddCategoryDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.PostAdd, contentDescription = "Categories", tint = FStorePurple)
                            }
                            IconButton(
                                onClick = { navController.navigate("add_product") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add Product", tint = FStorePurple)
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                selectedTabIndex == 1 -> {
                    ApprovalsView(
                        pendingApprovals = pendingApprovals,
                        searchQuery = searchQuery,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                selectedCategory != null -> {
                    ProductListView(
                        selectedCategory = selectedCategory!!,
                        allProducts = allProducts,
                        searchQuery = searchQuery,
                        viewModel = viewModel,
                        navController = navController,
                        onBack = { selectedCategory = null }
                    )
                }
                else -> {
                    CategoryListView(
                        orderedCategories = orderedCategories,
                        allProducts = allProducts,
                        homeSectionOrder = homeSectionOrder,
                        config = config,
                        viewModel = viewModel,
                        onCategoryClick = { selectedCategory = it }
                    )
                }
            }
        }
    }
}
