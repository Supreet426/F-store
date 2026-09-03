package com.fryumm.store.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.Order
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.data.model.PaymentStatus
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun StatusBadge(status: String) {
    val color = when(status) {
        OrderStatus.PLACED -> Color(0xFFE53935)
        OrderStatus.ACCEPTED, OrderStatus.PREPARING -> Color(0xFFFFC107)
        OrderStatus.PICKING_ASSIGNED, OrderStatus.PICKING -> Color(0xFF1E88E5)
        OrderStatus.PICKED -> Color(0xFF8E24AA)
        OrderStatus.BAGGED -> Color(0xFFF57C00)
        OrderStatus.VERIFYING -> Color(0xFFFF9800)
        OrderStatus.SEALED -> Color(0xFF43A047)
        OrderStatus.READY, OrderStatus.ASSIGNED -> Color(0xFF2E7D32)
        OrderStatus.DISPATCHED -> Color(0xFF00ACC1)
        OrderStatus.DELIVERED -> Color(0xFF4CAF50)
        else -> Color.Gray
    }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(text = status, color = color, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PaymentStatusBadge(status: String) {
    val color = when (status) {
        PaymentStatus.SUCCESS -> Color(0xFF2E7D32)
        PaymentStatus.FAILED -> Color(0xFFC62828)
        else -> Color(0xFFEF6C00)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
fun StoreBottomNavigation(selectedItem: Int, isAdmin: Boolean, onItemClick: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White, 
        tonalElevation = 4.dp,
        modifier = Modifier.navigationBarsPadding().height(64.dp)
    ) {
        if (isAdmin) {
            NavigationBarItem(selected = selectedItem == 0, onClick = { onItemClick(0) }, icon = { Icon(Icons.Default.ListAlt, null) }, label = { Text("Orders", fontSize = 9.sp) })
            NavigationBarItem(selected = selectedItem == 1, onClick = { onItemClick(1) }, icon = { Icon(Icons.Default.Store, null) }, label = { Text("Stores", fontSize = 9.sp) })
            NavigationBarItem(selected = selectedItem == 2, onClick = { onItemClick(2) }, icon = { Icon(Icons.Default.People, null) }, label = { Text("Users", fontSize = 9.sp) })
            NavigationBarItem(selected = selectedItem == 3, onClick = { onItemClick(3) }, icon = { Icon(Icons.Default.AdsClick, null) }, label = { Text("Promotion", fontSize = 9.sp) })
            NavigationBarItem(selected = selectedItem == 4, onClick = { onItemClick(4) }, icon = { Icon(Icons.Default.QueryStats, null) }, label = { Text("Insights", fontSize = 9.sp) })
        } else {
            NavigationBarItem(selected = selectedItem == 0, onClick = { onItemClick(0) }, icon = { Icon(Icons.Default.ListAlt, null) }, label = { Text("Orders", fontSize = 10.sp) })
            NavigationBarItem(selected = selectedItem == 1, onClick = { onItemClick(1) }, icon = { Icon(Icons.Default.Inventory2, null) }, label = { Text("Stock", fontSize = 10.sp) })
            NavigationBarItem(selected = selectedItem == 2, onClick = { onItemClick(2) }, icon = { Icon(Icons.Default.FlashOn, null) }, label = { Text("Dispatch", fontSize = 10.sp) })
            NavigationBarItem(selected = selectedItem == 3, onClick = { onItemClick(3) }, icon = { Icon(Icons.Default.QueryStats, null) }, label = { Text("Insights", fontSize = 10.sp) })
        }
    }
}

@Composable
fun WorkerInfo(label: String, name: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun OrderActionButton(text: String, color: Color, contentColor: Color = Color.White, borderColor: Color? = null, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick, 
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor), 
        shape = RoundedCornerShape(12.dp), 
        modifier = Modifier.height(44.dp),
        border = borderColor?.let { BorderStroke(1.dp, it) }
    ) {
        if (icon != null) { Icon(icon, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

@Composable
fun BillingRow(label: String, value: String, color: Color = Color.DarkGray) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun PriceBreakdownRow(label: String, value: String, color: Color = Color.Black) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium); Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier, icon: ImageVector) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = FStorePurple, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun InventoryStatCard(title: String, value: String, modifier: Modifier, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f))) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = color.copy(alpha = 0.7f))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun AnalyticsMiniCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = FStorePurple)
        }
    }
}

@Composable
fun HandoverSection(order: Order, viewModel: StoreViewModel, onStartScan: (String?, String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { viewModel.markDispatched(order.id) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
        }
        
        Spacer(Modifier.width(4.dp))
        
        Button(
            onClick = { onStartScan(order.id, "ORDER_HANDOVER") },
            colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(28.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("Scan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectorSheet(
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit,
    categories: List<String>,
    isAdmin: Boolean,
    onAddCategory: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var newCategoryName by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .heightIn(max = 600.dp)
        ) {
            Text(
                "Select Category",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = FStorePurple
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search categories...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = FStorePurple) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FStorePurple,
                    focusedLeadingIconColor = FStorePurple
                )
            )

            Spacer(Modifier.height(16.dp))

            val filteredCategories = categories.filter { it.contains(searchQuery, ignoreCase = true) }

            LazyColumn(modifier = Modifier.weight(1f)) {
                // If searching and category doesn't exist, allow creating it (Admin only)
                if (isAdmin && searchQuery.isNotBlank() && !categories.any { it.equals(searchQuery, true) }) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddCategory(searchQuery.trim())
                                    onCategorySelected(searchQuery.trim())
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = FStorePurple.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AddCircle, null, tint = FStorePurple)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Create New Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("\"$searchQuery\"", color = FStorePurple, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                items(filteredCategories) { cat ->
                    ListItem(
                        headlineContent = { Text(cat, fontWeight = FontWeight.Medium) },
                        leadingContent = { 
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = Color(0xFFF5F5F5)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Category, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                        },
                        modifier = Modifier.clickable {
                            onCategorySelected(cat)
                            onDismiss()
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                }
                
                if (filteredCategories.isEmpty() && !isAdmin) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Category, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(16.dp))
                            Text("No categories match your search", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
            
            if (isAdmin && !isAddingNew && searchQuery.isBlank()) {
                Button(
                    onClick = { isAddingNew = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("ADD NEW CATEGORY", fontWeight = FontWeight.ExtraBold)
                }
            }

            if (isAddingNew) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Create New Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("e.g. Beverages, Snacks") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FStorePurple)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newCategoryName.isNotBlank()) {
                                        onAddCategory(newCategoryName.trim())
                                        onCategorySelected(newCategoryName.trim())
                                        onDismiss()
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = FStorePurple, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Check, null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LayoutChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) FStorePurple else Color(0xFFF3F4F6),
        border = if (isSelected) null else BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}
