package com.fryumm.store.ui.screens.adminstores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.Product

@Composable
fun ImportProductDialog(
    targetCategory: String,
    allProducts: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (List<Product>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedProducts = remember { mutableStateListOf<Product>() }
    
    val availableProducts = allProducts.filter { 
        it.category != targetCategory && 
        (it.name.contains(searchQuery, ignoreCase = true) || it.brand.contains(searchQuery, ignoreCase = true))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Products to '$targetCategory'") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search all products...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Select products from other categories or Merchant listings to move them here.", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(availableProducts) { product ->
                        val isSelected = selectedProducts.contains(product)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (isSelected) selectedProducts.remove(product) else selectedProducts.add(product) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Current: ${product.category}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedProducts.toList()) },
                enabled = selectedProducts.isNotEmpty()
            ) {
                Text("MOVE ${selectedProducts.size} ITEMS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Catalog (Category)") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if(name.isNotBlank()) onConfirm(name) }) { Text("ADD") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun ReorderCategoryDialog(category: String, currentSerial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var serial by remember { mutableStateOf(currentSerial.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reorder '$category'") },
        text = {
            Column {
                Text("Change serial number to move this category.", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = serial,
                    onValueChange = { if(it.all { char -> char.isDigit() }) serial = it },
                    label = { Text("New Serial Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if(serial.isNotBlank()) onConfirm(serial.toInt()) }) { Text("MOVE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun BulkImportOptionsDialog(
    onDismiss: () -> Unit,
    onCsvImport: () -> Unit,
    onJsonImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Import Products") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select a file format to import products into the catalog.", fontSize = 12.sp, color = Color.Gray)
                
                OutlinedButton(onClick = onCsvImport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.TableChart, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import from CSV")
                }
                
                OutlinedButton(onClick = onJsonImport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Code, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import from JSON")
                }
                
                HorizontalDivider()
                Text("CSV Header Format:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("brand,name,category,price,mrp,imageUrl,weight,stock,hsn,description", fontSize = 10.sp, color = Color.DarkGray)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
