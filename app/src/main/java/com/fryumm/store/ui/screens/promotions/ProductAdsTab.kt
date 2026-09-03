package com.fryumm.store.ui.screens.promotions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AdsClick
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
import com.fryumm.store.data.model.ProductAd
import com.fryumm.store.ui.theme.FStorePurple
import java.util.UUID

@Composable
fun ProductAdsTab(config: DynamicAppConfig, viewModel: StoreViewModel, onUpdate: (DynamicAppConfig) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProductId by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("In-Feed Product Ads", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Highlight specific products within the home feed.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(config.productAds) { ad ->
                val product = viewModel.products.find { it.id == ad.productId }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AdsClick, null, tint = Color(0xFF2E7D32)) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product?.name ?: "Unknown Product", fontWeight = FontWeight.Bold)
                            Text("ID: ${ad.productId}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(checked = ad.isActive, onCheckedChange = { active ->
                            val list = config.productAds.toMutableList()
                            val index = list.indexOfFirst { it.id == ad.id }
                            if (index != -1) {
                                list[index] = list[index].copy(isActive = active)
                                onUpdate(config.copy(productAds = list))
                            }
                        })
                        IconButton(onClick = { onUpdate(config.copy(productAds = config.productAds.filter { it.id != ad.id })) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f))
                        }
                    }
                }
            }
        }

        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("ADD PRODUCT AD")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Select Product for Ad") },
            text = {
                Column(modifier = Modifier.height(300.dp)) {
                    LazyColumn {
                        items(viewModel.products) { product ->
                            val isAlreadyAdded = config.productAds.any { it.productId == product.id }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedProductId == product.id, onClick = { selectedProductId = product.id }, enabled = !isAlreadyAdded)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(product.name, color = if(isAlreadyAdded) Color.Gray else Color.Black)
                                    Text(product.category, fontSize = 10.sp, color = Color.Gray)
                                }
                                if(isAlreadyAdded) {
                                    Spacer(Modifier.weight(1f))
                                    Text("Added", fontSize = 10.sp, color = FStorePurple)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (selectedProductId.isNotBlank()) {
                        val newAd = ProductAd(id = UUID.randomUUID().toString(), productId = selectedProductId, isActive = true)
                        onUpdate(config.copy(productAds = config.productAds + newAd))
                        showAddDialog = false; selectedProductId = ""
                    }
                }) { Text("ADD") }
            }
        )
    }
}
