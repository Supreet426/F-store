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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.ui.theme.FStorePurple
import kotlinx.coroutines.launch

@Composable
fun PushNotificationTab(viewModel: StoreViewModel) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var actionUrl by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                val url = viewModel.uploadProductImage(it)
                if (url != null) imageUrl = url
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Broadcast Push Notification", fontWeight = FontWeight.Black, fontSize = 20.sp, color = FStorePurple)
        Text("Send a system tray notification to all customers.", fontSize = 12.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notification Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. Weekend Mega Sale!") }
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Notification Body / Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("e.g. Get 50% off on all dairy products this Sunday. Order now!") }
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { imageLauncher.launch("image/*") }) {
                            Icon(Icons.Default.CloudUpload, null, tint = FStorePurple)
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = actionUrl,
                    onValueChange = { actionUrl = it },
                    label = { Text("Action URL / Target Screen (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. flash_sale or category/Dairy") }
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank() && message.isNotBlank()) {
                            isSending = true
                            viewModel.sendBroadcastPushNotification(title, message, imageUrl.ifBlank { null }, actionUrl.ifBlank { null })
                            title = ""; message = ""; imageUrl = ""; actionUrl = ""
                            isSending = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                    enabled = !isSending && title.isNotBlank() && message.isNotBlank()
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text("SEND BROADCAST NOW", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Text("Notification Tray Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        // System Tray Preview
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF1F3F4),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // App Icon
                Surface(
                    modifier = Modifier.size(36.dp),
                    color = FStorePurple,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("F", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Flash-O", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text(" • now", fontSize = 10.sp, color = Color.Gray)
                    }
                    Text(title.ifBlank { "Notification Title" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(message.ifBlank { "Notification message will appear here..." }, fontSize = 13.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).background(Color.LightGray, RoundedCornerShape(4.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}
