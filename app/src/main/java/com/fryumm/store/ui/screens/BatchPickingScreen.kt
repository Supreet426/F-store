package com.fryumm.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
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
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.data.model.Product
import com.fryumm.store.ui.theme.FStorePurple
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchPickingScreen(navController: NavHostController, viewModel: StoreViewModel) {
    // SLA Timer Counter (2 Min Target)
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    // Target orders ready for picking
    val targetOrders = viewModel.orders.filter { it.status == OrderStatus.ACCEPTED || it.status == OrderStatus.PICKING }
    
    // Shortest Path Walk Sort: Group & Sort by Aisle (lexicographical) then Rack
    val batchedItems = remember(targetOrders) {
        targetOrders.flatMap { order -> 
            order.items.map { it.product to it.quantity }
        }.groupBy({ it.first.id }, { it.first to it.second })
        .mapValues { entry ->
            val product = entry.value.first().first
            val totalQuantity = entry.value.sumOf { it.second }
            product to totalQuantity
        }.values.toList().sortedWith(compareBy({ it.first.aisle.lowercase() }, { it.first.rack.lowercase() }))
    }

    // State for picked items
    val pickedProductIds = remember { mutableStateListOf<String>() }
    var selectedItemForSubstitute by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Picking (TSP Route)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
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
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (targetOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No orders ready for picking.", color = Color.Gray)
                }
            } else {
                // Dark Store SLA Header
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (elapsedSeconds > 120) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer, 
                                contentDescription = null, 
                                tint = if (elapsedSeconds > 120) Color.Red else Color(0xFF2E7D32)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Picking Time: ${elapsedSeconds / 60}m ${elapsedSeconds % 60}s / 2m Target",
                                fontWeight = FontWeight.Bold,
                                color = if (elapsedSeconds > 120) Color.Red else Color(0xFF2E7D32),
                                fontSize = 13.sp
                            )
                        }
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "${targetOrders.size} Orders",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = FStorePurple
                            )
                        }
                    }
                }

                Text(
                    "Shortest path sequence applied (Aisle -> Rack):",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(batchedItems) { (product, totalQty) ->
                        val isPicked = pickedProductIds.contains(product.id)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPicked) Color(0xFFF1F8E9) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (isPicked) Color(0xFF4CAF50) else FStorePurple.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = totalQty.toString(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = if (isPicked) Color.White else FStorePurple
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "Aisle: ${product.aisle} | Rack: ${product.rack}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                                
                                Row {
                                    IconButton(
                                        onClick = { selectedItemForSubstitute = product }
                                    ) {
                                        Icon(
                                            Icons.Default.SwapHoriz, 
                                            contentDescription = "Substitute",
                                            tint = if (product.substituteProductIds.isNotEmpty()) FStorePurple else Color.Gray
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (isPicked) pickedProductIds.remove(product.id)
                                            else pickedProductIds.add(product.id)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPicked) Color(0xFF2E7D32) else Color.Black
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        if (isPicked) {
                                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(if (isPicked) "PICKED" else "PICK", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        targetOrders.forEach { order -> 
                            if (order.status == OrderStatus.ACCEPTED) {
                                viewModel.startPicking(order.id) 
                            }
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Inventory, null)
                    Spacer(Modifier.width(8.dp))
                    Text("CONFIRM BATCH PICKED (${pickedProductIds.size}/${batchedItems.size})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Substitute Request Dialog
    if (selectedItemForSubstitute != null) {
        val substitutes = viewModel.products.filter { it.id in selectedItemForSubstitute!!.substituteProductIds }
        
        AlertDialog(
            onDismissRequest = { selectedItemForSubstitute = null },
            title = { Text("Smart Substitutes for ${selectedItemForSubstitute!!.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (substitutes.isEmpty()) {
                        Text("No predefined substitutes found for this item. Please select manually from inventory.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        Text("Select an alternative to suggest to the customer:", fontSize = 13.sp)
                        substitutes.forEach { sub ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    // Find orders containing this item and request substitute
                                    targetOrders.filter { order -> order.items.any { it.product.id == selectedItemForSubstitute!!.id } }
                                        .forEach { order ->
                                            viewModel.requestSubstitute(order.id, sub)
                                        }
                                    selectedItemForSubstitute = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory, null, tint = FStorePurple, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("₹${sub.price} • ${sub.stockQuantity} in stock", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedItemForSubstitute = null }) {
                    Text("CLOSE")
                }
            }
        )
    }
}
