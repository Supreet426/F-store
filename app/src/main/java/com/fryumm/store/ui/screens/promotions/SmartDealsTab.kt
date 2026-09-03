package com.fryumm.store.ui.screens.promotions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.data.model.FlashSale
import com.fryumm.store.data.model.SmartCollection
import com.fryumm.store.ui.theme.FStorePurple
import java.util.UUID

@Composable
fun SmartDealsTab(config: DynamicAppConfig, viewModel: StoreViewModel, onUpdate: (DynamicAppConfig) -> Unit) {
    var showProductPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Smart Deals & Collections", fontWeight = FontWeight.Black, fontSize = 20.sp, color = FStorePurple)
        Text("Boost customer engagement with specialized sections.", fontSize = 12.sp, color = Color.Gray)
        
        Spacer(Modifier.height(24.dp))
        
        // FLASH SALE SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            border = BorderStroke(1.dp, Color(0xFFFF9800).copy(0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlashOn, null, tint = Color(0xFFFF9800))
                    Spacer(Modifier.width(8.dp))
                    Text("FLASH SALE", fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = config.flashSale?.isActive == true,
                        onCheckedChange = { active ->
                            val current = config.flashSale ?: FlashSale()
                            onUpdate(config.copy(flashSale = current.copy(isActive = active)))
                        }
                    )
                }
                
                OutlinedTextField(
                    value = config.flashSale?.title ?: "",
                    onValueChange = { t -> onUpdate(config.copy(flashSale = (config.flashSale ?: FlashSale()).copy(title = t))) },
                    label = { Text("Sale Title") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = config.flashSale?.discountPercentage?.toString() ?: "0",
                        onValueChange = { d -> onUpdate(config.copy(flashSale = (config.flashSale ?: FlashSale()).copy(discountPercentage = d.toIntOrNull() ?: 0))) },
                        label = { Text("Flat Discount %") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("Selected Products (${config.flashSale?.productIds?.size ?: 0})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                // Horizontal list of selected products
                if ((config.flashSale?.productIds?.size ?: 0) > 0) {
                    Row(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        config.flashSale?.productIds?.take(5)?.forEach { id ->
                            val prod = viewModel.products.find { it.id == id }
                            Surface(color = Color.White, shape = RoundedCornerShape(4.dp), border = BorderStroke(0.5.dp, Color.LightGray)) {
                                Text(prod?.name ?: "Unknown", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp)
                            }
                        }
                        if ((config.flashSale?.productIds?.size ?: 0) > 5) {
                            Text("+${(config.flashSale?.productIds?.size ?: 0) - 5} more", fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                }

                Button(
                    onClick = { showProductPicker = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SELECT DEAL PRODUCTS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Text("Flash sales create urgency and drive quick conversions.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // SMART COLLECTIONS
        Text("Automated Collections", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Highlight products based on behavior.", fontSize = 12.sp, color = Color.Gray)
        
        Spacer(Modifier.height(12.dp))
        
        val collectionTypes = listOf("BESTSELLER", "TRENDING", "NEW_ARRIVAL", "PERSONALIZED")
        collectionTypes.forEach { type ->
            val isEnabled = config.smartCollections.any { it.type == type && it.isActive }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = if(isEnabled) Color.White else Color.Gray.copy(0.05f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val icon = when(type) {
                        "BESTSELLER" -> Icons.Default.EmojiEvents
                        "TRENDING" -> Icons.Default.TrendingUp
                        "PERSONALIZED" -> Icons.Default.AutoAwesome
                        else -> Icons.Default.NewReleases
                    }
                    Icon(icon, null, tint = if(isEnabled) FStorePurple else Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(type.replace("_", " "), fontWeight = FontWeight.Bold)
                        Text(if(type == "PERSONALIZED") "Individual targeting based on history" else "Auto-generated based on sales data", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { active ->
                            val currentList = config.smartCollections.toMutableList()
                            val index = currentList.indexOfFirst { it.type == type }
                            if (index != -1) {
                                currentList[index] = currentList[index].copy(isActive = active)
                            } else {
                                currentList.add(SmartCollection(id = UUID.randomUUID().toString(), name = type, type = type, isActive = active))
                            }
                            onUpdate(config.copy(smartCollections = currentList))
                        }
                    )
                }
            }
        }
    }

    if (showProductPicker) {
        val currentIds = config.flashSale?.productIds ?: emptyList()
        var selectedIds by remember { mutableStateOf(currentIds) }

        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("Select Flash Sale Products") },
            text = {
                Column {
                    Text("Selected: ${selectedIds.size}", fontWeight = FontWeight.Bold, color = FStorePurple)
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(viewModel.products) { product ->
                            val isSelected = selectedIds.contains(product.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (isSelected) selectedIds - product.id else selectedIds + product.id
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = {
                                    selectedIds = if (isSelected) selectedIds - product.id else selectedIds + product.id
                                })
                                Spacer(Modifier.width(8.dp))
                                Text(product.name, modifier = Modifier.weight(1f))
                                Text("₹${product.price}", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val current = config.flashSale ?: FlashSale()
                    onUpdate(config.copy(flashSale = current.copy(productIds = selectedIds)))
                    showProductPicker = false
                }) { Text("DONE") }
            },
            dismissButton = {
                TextButton(onClick = { showProductPicker = false }) { Text("CANCEL") }
            }
        )
    }
}
