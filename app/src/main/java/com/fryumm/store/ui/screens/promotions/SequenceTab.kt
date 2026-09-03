package com.fryumm.store.ui.screens.promotions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.data.model.RecommendationItem
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun SequenceTab(config: DynamicAppConfig, onUpdate: (DynamicAppConfig) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newItemId by remember { mutableStateOf("") }
    var newItemName by remember { mutableStateOf("") }
    var newItemType by remember { mutableStateOf("PRODUCT") }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = Color(0xFFE1F5FE), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoGraph, null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Rank products and brands to show first in user's cart.", fontSize = 12.sp, color = Color(0xFF01579B))
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(config.cartSuggestions) { index, item ->
                PromotionSequenceRow(
                    item = item,
                    index = index,
                    isFirst = index == 0,
                    isLast = index == config.cartSuggestions.size - 1,
                    onMoveUp = {
                        val list = config.cartSuggestions.toMutableList()
                        val temp = list[index]
                        list[index] = list[index - 1]
                        list[index - 1] = temp
                        onUpdate(config.copy(cartSuggestions = list))
                    },
                    onMoveDown = {
                        val list = config.cartSuggestions.toMutableList()
                        val temp = list[index]
                        list[index] = list[index + 1]
                        list[index + 1] = temp
                        onUpdate(config.copy(cartSuggestions = list))
                    },
                    onDelete = {
                        onUpdate(config.copy(cartSuggestions = config.cartSuggestions.filterIndexed { i, _ -> i != index }))
                    }
                )
            }

            item {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = FStorePurple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FStorePurple)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("ADD NEW PROMOTION", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Item to Promotion") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = newItemType == "PRODUCT", onClick = { newItemType = "PRODUCT" })
                        Text("Product")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = newItemType == "BRAND", onClick = { newItemType = "BRAND" })
                        Text("Brand")
                    }
                    OutlinedTextField(value = newItemName, onValueChange = { newItemName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newItemId, onValueChange = { newItemId = it }, label = { Text("ID / Key") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newItem = RecommendationItem(id = newItemId, name = newItemName, type = newItemType)
                    onUpdate(config.copy(cartSuggestions = config.cartSuggestions + newItem))
                    showAddDialog = false; newItemId = ""; newItemName = ""
                }) { Text("ADD") }
            }
        )
    }
}
