package com.fryumm.store.ui.screens.promotions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun NotificationTab(viewModel: StoreViewModel) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var targetTopic by remember { mutableStateOf("all_users") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Broadcast Center", fontWeight = FontWeight.Black, fontSize = 20.sp, color = FStorePurple)
        Text("Send push notifications to all customers instantly.", fontSize = 12.sp, color = Color.Gray)
        
        Spacer(Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = FStorePurple)
                    Spacer(Modifier.width(8.dp))
                    Text("New Push Message", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Message Title (Short & Catchy)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Message Body") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Large Image URL (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))
                Text("Audience Segment", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = targetTopic == "all_users", onClick = { targetTopic = "all_users" })
                    Text("All App Users", fontSize = 14.sp)
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = targetTopic == "store_partners", onClick = { targetTopic = "store_partners" })
                    Text("Delivery Partners", fontSize = 14.sp)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = {
                viewModel.sendNotification(title, body, imageUrl.ifBlank { null }, targetTopic)
                title = ""; body = ""; imageUrl = ""
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = title.isNotBlank() && body.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null)
            Spacer(Modifier.width(8.dp))
            Text("BROADCAST MESSAGE NOW", fontWeight = FontWeight.ExtraBold)
        }
        
        Text(
            "Note: Messages are delivered via Firebase Cloud Messaging. Delivery to individual devices may vary based on OS battery optimizations.",
            fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 12.dp)
        )
    }
}
