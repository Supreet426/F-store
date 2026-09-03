package com.fryumm.store.ui.screens.promotions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.BannerAd
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.ui.theme.FStorePurple
import java.util.UUID

@Composable
fun BannersTab(config: DynamicAppConfig, onUpdate: (DynamicAppConfig) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var actionUrl by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Active Top Banners", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(config.bannerAds) { banner ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, null, tint = FStorePurple)
                            Spacer(Modifier.width(8.dp))
                            Text(banner.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Switch(checked = banner.isActive, onCheckedChange = { active ->
                                val list = config.bannerAds.toMutableList()
                                val index = list.indexOfFirst { it.id == banner.id }
                                if (index != -1) {
                                    list[index] = list[index].copy(isActive = active)
                                    onUpdate(config.copy(bannerAds = list))
                                }
                            })
                            IconButton(onClick = {
                                onUpdate(config.copy(bannerAds = config.bannerAds.filter { it.id != banner.id }))
                            }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                        Text("Image: ${banner.imageUrl}", fontSize = 11.sp, color = Color.Gray)
                        Text("Action: ${banner.actionUrl}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("ADD NEW BANNER")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Banner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Banner Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = actionUrl, onValueChange = { actionUrl = it }, label = { Text("Action URL (e.g. category/Dairy)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newBanner = BannerAd(id = UUID.randomUUID().toString(), title = title, imageUrl = imageUrl, actionUrl = actionUrl, isActive = true)
                    onUpdate(config.copy(bannerAds = config.bannerAds + newBanner))
                    showAddDialog = false; title = ""; imageUrl = ""; actionUrl = ""
                }) { Text("ADD") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("CANCEL") } }
        )
    }
}
