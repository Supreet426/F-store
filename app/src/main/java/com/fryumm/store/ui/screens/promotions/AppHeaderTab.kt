package com.fryumm.store.ui.screens.promotions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun AppHeaderTab(config: DynamicAppConfig, viewModel: StoreViewModel, onUpdate: (DynamicAppConfig) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Flash-O Deals & Header", fontWeight = FontWeight.Black, fontSize = 20.sp, color = FStorePurple)
        Text("Manage the special 'Deals' title and button shown in the Customer App.", fontSize = 12.sp, color = Color.Gray)
        
        Spacer(Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Deals Button Display", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Changes the 'Flash-O Deals' text next to search in customer app.", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = config.dealsButtonText,
                    onValueChange = { onUpdate(config.copy(dealsButtonText = it)) },
                    label = { Text("Deal Title (e.g. Flash-O Deals)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter title") }
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = config.dealsButtonIcon,
                    onValueChange = { onUpdate(config.copy(dealsButtonIcon = it)) },
                    label = { Text("Icon URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(24.dp))
                Text("Click Action", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Where should this button take the customer?", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                
                val actions = listOf(
                    "flash_sale" to "Flash Sale (Steal Deals)",
                    "trending" to "Trending Products",
                    "Categories" to "Categories Screen"
                )
                
                actions.forEach { (action, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdate(config.copy(dealsButtonAction = action)) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.dealsButtonAction == action,
                            onClick = { onUpdate(config.copy(dealsButtonAction = action)) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(label)
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                Text("Or enter a specific Category Name:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = if (config.dealsButtonAction !in actions.map { it.first }) config.dealsButtonAction else "",
                    onValueChange = { if(it.isNotEmpty()) onUpdate(config.copy(dealsButtonAction = it)) },
                    label = { Text("Custom Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Beverages") }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Preview Box
        Text("Live Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF3E007C),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    modifier = Modifier.height(48.dp).width(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val parts = config.dealsButtonText.split(" ")
                        Column { 
                            Text(parts.firstOrNull() ?: "", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E007C))
                            Text(if(parts.size > 1) parts.drop(1).joinToString(" ") else "", fontSize = 11.sp, color = Color(0xFF3E007C)) 
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Celebration, null, tint = Color(0xFF3E007C), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
