package com.fryumm.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.RecommendationItem
import com.fryumm.store.ui.theme.FStorePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationManagerScreen(viewModel: StoreViewModel, onBack: () -> Unit) {
    var config by remember { mutableStateOf(viewModel.dynamicAppConfig) }
    var showAddDialog by remember { mutableStateOf(false) }

    // States for new recommendation
    var newItemId by remember { mutableStateOf("") }
    var newItemName by remember { mutableStateOf("") }
    var newItemType by remember { mutableStateOf("PRODUCT") }

    LaunchedEffect(viewModel.dynamicAppConfig) {
        config = viewModel.dynamicAppConfig
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promotions & Suggestions", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    Button(
                        onClick = { viewModel.updateDynamicConfig(config) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("GO LIVE", color = FStorePurple, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FStorePurple, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = FStorePurple, contentColor = Color.White) {
                Icon(Icons.Default.Add, "Add Suggestion")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF3E0)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Drag-and-drop or use arrows to set the exact sequence customers see in their cart.",
                        fontSize = 11.sp,
                        color = Color(0xFFE65100)
                    )
                }
            }

            if (config.cartSuggestions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No promotions active. Click + to add.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(config.cartSuggestions) { index, item ->
                        RecommendationRow(
                            item = item,
                            index = index,
                            isFirst = index == 0,
                            isLast = index == config.cartSuggestions.size - 1,
                            onMoveUp = {
                                val list = config.cartSuggestions.toMutableList()
                                val temp = list[index]
                                list[index] = list[index - 1]
                                list[index - 1] = temp
                                config = config.copy(cartSuggestions = list)
                            },
                            onMoveDown = {
                                val list = config.cartSuggestions.toMutableList()
                                val temp = list[index]
                                list[index] = list[index + 1]
                                list[index + 1] = temp
                                config = config.copy(cartSuggestions = list)
                            },
                            onDelete = {
                                config = config.copy(cartSuggestions = config.cartSuggestions.filterIndexed { i, _ -> i != index })
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Promotion/Suggestion") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = newItemType == "PRODUCT", onClick = { newItemType = "PRODUCT" })
                        Text("Product")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = newItemType == "BRAND", onClick = { newItemType = "BRAND" })
                        Text("Brand")
                    }
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Display Name (e.g. Amul Milk)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newItemId,
                        onValueChange = { newItemId = it },
                        label = { Text(if (newItemType == "PRODUCT") "Product ID" else "Brand Name/Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newItem = RecommendationItem(id = newItemId, name = newItemName, type = newItemType)
                    config = config.copy(cartSuggestions = config.cartSuggestions + newItem)
                    showAddDialog = false
                    newItemId = ""; newItemName = ""
                }) {
                    Text("ADD TO LIST")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun RecommendationRow(
    item: RecommendationItem,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = FStorePurple.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("#${index + 1}", fontWeight = FontWeight.Bold, color = FStorePurple)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = if (item.type == "PRODUCT") Color(0xFF4CAF50) else Color(0xFF2196F3)
                    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            item.type,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("ID: ${item.id}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Row {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.ArrowUpward, "Move Up", tint = if (isFirst) Color.LightGray else Color.Black)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.ArrowDownward, "Move Down", tint = if (isLast) Color.LightGray else Color.Black)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                }
            }
        }
    }
}
