package com.fryumm.store.ui.screens.promotions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.data.model.SponsoredBrand
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun SponsoredTab(config: DynamicAppConfig, onUpdate: (DynamicAppConfig) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var brandName by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = FStorePurple.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Retail Media Network (Brand PPC Bidding)", fontWeight = FontWeight.Black, color = FStorePurple, fontSize = 14.sp)
                Text("Brands bid on top-of-search & category positions. Auto-track ROAS & Impressions.", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Active Campaigns", fontSize = 10.sp, color = Color.Gray)
                        Text("${config.sponsoredBrands.size}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = FStorePurple)
                    }
                    Column {
                        Text("Avg CPC Bid", fontSize = 10.sp, color = Color.Gray)
                        Text("₹4.50", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF2E7D32))
                    }
                    Column {
                        Text("Target ROAS", fontSize = 10.sp, color = Color.Gray)
                        Text("4.2x", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF1976D2))
                    }
                }
            }
        }

        Text("Sponsored Brands & Keyword Bids", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Brands that appear at top of search and category results.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(config.sponsoredBrands) { brand ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFFFFB300))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(brand.name, fontWeight = FontWeight.Bold)
                            Text("Max Daily Bid: ₹${brand.bidAmount} | PPC Active", fontSize = 11.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { onUpdate(config.copy(sponsoredBrands = config.sponsoredBrands - brand)) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f))
                        }
                    }
                }
            }
        }

        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("ADD SPONSORED BRAND")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Sponsored Brand") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = brandName, onValueChange = { brandName = it }, label = { Text("Brand Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = cost, 
                        onValueChange = { if(it.all { c -> c.isDigit() }) cost = it }, 
                        label = { Text("Daily Budget / Bid") }, 
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (brandName.isNotBlank()) {
                        val newBrand = SponsoredBrand(name = brandName, bidAmount = cost.toDoubleOrNull() ?: 0.0, expiryDate = System.currentTimeMillis() + 30L*24*60*60*1000, isActive = true)
                        onUpdate(config.copy(sponsoredBrands = config.sponsoredBrands + newBrand))
                        showAddDialog = false; brandName = ""; cost = ""
                    }
                }) { Text("ADD") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("CANCEL") } }
        )
    }
}
