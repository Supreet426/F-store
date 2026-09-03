package com.fryumm.store.ui.screens.promotions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.DynamicAppConfig

@Composable
fun RetargetingTab(config: DynamicAppConfig, onUpdate: (DynamicAppConfig) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Search Redirects", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Convert failed searches to sales by mapping keywords.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(config.searchRetargeting.toList()) { (missing, alternative) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("User searches:", fontSize = 10.sp, color = Color.Gray)
                            Text(missing, color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Show instead:", fontSize = 10.sp, color = Color.Gray)
                            Text(alternative, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { onUpdate(config.copy(searchRetargeting = config.searchRetargeting - missing)) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Gray)
                        }
                    }
                }
            }
        }

        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("ADD SEARCH MAPPING")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Search Mapping") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Redirect searches for unavailable keywords to relevant items.", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("Missing Keyword (e.g. Organic)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Redirect to (e.g. Dabur)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (keyword.isNotBlank() && target.isNotBlank()) {
                        onUpdate(config.copy(searchRetargeting = config.searchRetargeting + (keyword to target)))
                        showAddDialog = false; keyword = ""; target = ""
                    }
                }) { Text("MAPPING") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("CANCEL") } }
        )
    }
}
