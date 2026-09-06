package com.fryumm.store.ui.screens.addproduct

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun SpecificationsSection(
    specifications: Map<String, String>,
    onSpecificationsChange: (Map<String, String>) -> Unit
) {
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Product Specifications", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FStorePurple)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (specifications.isNotEmpty()) {
                    specifications.forEach { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = key,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = value,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(onClick = {
                                val newMap = specifications.toMutableMap()
                                newMap.remove(key)
                                onSpecificationsChange(newMap)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                            }
                        }
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                }

                Text("Add Specification", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Key (e.g. Color)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Value (e.g. Red)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                val newMap = specifications.toMutableMap()
                                newMap[newKey] = newValue
                                onSpecificationsChange(newMap)
                                newKey = ""
                                newValue = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = FStorePurple, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
                Text(
                    "Examples: Color, Material, Model, Size, Weight, Brand, Battery Life etc.",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
