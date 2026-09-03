package com.fryumm.store.ui.screens.adminstores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
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
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.data.model.Product
import com.fryumm.store.ui.components.ProductCard
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun ApprovalsView(
    pendingApprovals: List<Product>,
    searchQuery: String,
    viewModel: StoreViewModel,
    navController: NavHostController
) {
    val filteredPending = pendingApprovals.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.brand.contains(searchQuery, ignoreCase = true)
    }
    
    if (filteredPending.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircleOutline, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))
                Text("No pending approvals", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${filteredPending.size} items waiting for review", fontSize = 14.sp, color = Color.Gray)
                    Button(
                        onClick = { viewModel.approveAllProducts() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("APPROVE ALL (${filteredPending.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(filteredPending) { product ->
                ProductCard(
                    product = product,
                    viewModel = viewModel,
                    onEditClick = {
                        navController.navigate("edit_product/${product.id}")
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryListView(
    orderedCategories: List<String>,
    allProducts: List<Product>,
    homeSectionOrder: List<String>,
    config: DynamicAppConfig,
    viewModel: StoreViewModel,
    onCategoryClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Manage all catalogs including Home sections", fontSize = 14.sp, color = Color.Gray)
        }
        itemsIndexed(orderedCategories) { index, category ->
            val sectionEntry = homeSectionOrder.find { it == category || it.startsWith("$category|") }
            val isVisibleOnHome = sectionEntry != null
            val currentLayout = sectionEntry?.split("|")?.getOrNull(1) ?: "Carousel"

            var showDeleteConfirm by remember { mutableStateOf(false) }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Delete Catalog?") },
                    text = { Text("Do you want to permanently remove '$category' from Catalog and Home Layout?") },
                    confirmButton = {
                        TextButton(onClick = { 
                            viewModel.deleteCategory(category)
                            showDeleteConfirm = false
                        }) { Text("DELETE", color = Color.Red) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCEL") }
                    }
                )
            }

            CategoryCard(
                category = category,
                serialNumber = index + 1,
                productCount = allProducts.count { it.category == category },
                isVisibleOnHome = isVisibleOnHome,
                isComingSoon = config.comingSoonCategories.contains(category),
                currentLayout = currentLayout,
                onVisibilityToggle = { checked ->
                    val currentOrder = homeSectionOrder.toMutableList()
                    if (checked) {
                        if (!currentOrder.any { it == category || it.startsWith("$category|") }) currentOrder.add(category)
                    } else {
                        currentOrder.removeAll { it == category || it.startsWith("$category|") }
                    }
                    viewModel.updateHomeSectionOrder(currentOrder)
                },
                onComingSoonToggle = {
                    viewModel.toggleCategoryComingSoon(category)
                },
                onLayoutChange = { newLayout: String ->
                    val currentOrder = homeSectionOrder.toMutableList()
                    val idx = currentOrder.indexOfFirst { it == category || it.startsWith("$category|") }
                    if (idx != -1) {
                        currentOrder[idx] = if (newLayout == "Carousel") category else "$category|$newLayout"
                        viewModel.updateHomeSectionOrder(currentOrder)
                    }
                },
                onDelete = { showDeleteConfirm = true },
                onClick = { onCategoryClick(category) },
                onReorder = { newSerial ->
                    val newIndex = (newSerial - 1).coerceIn(0, orderedCategories.size - 1)
                    val newList = orderedCategories.toMutableList()
                    val item = newList.removeAt(index)
                    newList.add(newIndex, item)
                    viewModel.updateCategorySequence(newList)
                }
            )
        }
    }
}

@Composable
fun ProductListView(
    selectedCategory: String,
    allProducts: List<Product>,
    searchQuery: String,
    viewModel: StoreViewModel,
    navController: NavHostController,
    onBack: () -> Unit
) {
    val filteredProducts = allProducts.filter {
        it.category == selectedCategory &&
        (it.name.contains(searchQuery, ignoreCase = true) || it.brand.contains(searchQuery, ignoreCase = true))
    }.sortedBy { it.sequenceNumber }

    var showImportDialog by remember { mutableStateOf(false) }

    if (showImportDialog) {
        ImportProductDialog(
            targetCategory = selectedCategory,
            allProducts = allProducts,
            onDismiss = { showImportDialog = false },
            onConfirm = { productsToMove ->
                productsToMove.forEach { p ->
                    viewModel.updateProduct(p.copy(category = selectedCategory))
                }
                showImportDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Text(
                        text = "${filteredProducts.size} Products in order",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row {
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(32.dp).padding(end = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ASSIGN", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { 
                            navController.navigate("add_product?category=$selectedCategory")
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ADD NEW", fontSize = 11.sp)
                    }
                }
            }
        }

        itemsIndexed(filteredProducts) { index, product ->
            ProductCard(
                product = product,
                viewModel = viewModel,
                serialNumber = index + 1,
                onEditClick = {
                    navController.navigate("edit_product/${product.id}")
                },
                onReorder = { newSerial ->
                    val targetIndex = (newSerial - 1).coerceIn(0, filteredProducts.size - 1)
                    val newList = filteredProducts.toMutableList()
                    val item = newList.removeAt(index)
                    newList.add(targetIndex, item)
                    viewModel.bulkUpdateProductSequences(newList)
                }
            )
        }
    }
}
