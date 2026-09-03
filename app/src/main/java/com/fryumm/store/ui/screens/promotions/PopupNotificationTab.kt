package com.fryumm.store.ui.screens.promotions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.ui.theme.FStorePurple
import kotlinx.coroutines.launch

@Composable
fun PopupNotificationTab(config: DynamicAppConfig, viewModel: StoreViewModel, onUpdate: (DynamicAppConfig) -> Unit) {
    val popup = config.popupNotification
    val scope = rememberCoroutineScope()
    
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                val url = viewModel.uploadProductImage(it)
                if (url != null) {
                    onUpdate(config.copy(popupNotification = popup.copy(imageUrl = url)))
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Global Popup Notification", fontWeight = FontWeight.Black, fontSize = 20.sp, color = FStorePurple)
        Text("Show a popup to all users when they open the app.", fontSize = 12.sp, color = Color.Gray)
        
        Spacer(Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Popup Alert", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(
                        checked = popup.isActive, 
                        onCheckedChange = { onUpdate(config.copy(popupNotification = popup.copy(isActive = it))) }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = popup.title,
                    onValueChange = { onUpdate(config.copy(popupNotification = popup.copy(title = it))) },
                    label = { Text("Popup Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = popup.message,
                    onValueChange = { onUpdate(config.copy(popupNotification = popup.copy(message = it))) },
                    label = { Text("Message / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = popup.imageUrl,
                    onValueChange = { onUpdate(config.copy(popupNotification = popup.copy(imageUrl = it))) },
                    label = { Text("Background Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://example.com/image.jpg") },
                    trailingIcon = {
                        IconButton(onClick = { imageLauncher.launch("image/*") }) {
                            Icon(Icons.Default.CloudUpload, null, tint = FStorePurple)
                        }
                    }
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = popup.actionUrl,
                    onValueChange = { onUpdate(config.copy(popupNotification = popup.copy(actionUrl = it))) },
                    label = { Text("Action URL (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("category/Dairy or flash_sale") }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text("Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        
        // Popup Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(Color.Black.copy(0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (popup.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = popup.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(Color.LightGray),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = popup.title.ifBlank { "Popup Title" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FStorePurple
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = popup.message.ifBlank { "Your message will appear here for all users." },
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("VIEW DETAILS")
                        }
                        TextButton(onClick = {}) {
                            Text("Dismiss", color = Color.Gray)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}
