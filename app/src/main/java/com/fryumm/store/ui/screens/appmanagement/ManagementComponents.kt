package com.fryumm.store.ui.screens.appmanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun SectionOrderItem(
    name: String, 
    index: Int, 
    isSpecial: Boolean = false,
    url: String? = null,
    isProductAd: Boolean = false,
    onUrlChange: ((String) -> Unit)? = null,
    onPreviewVideo: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onReorder: (Int) -> Unit
) {
    var showJumpDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }

    var jumpValue by remember { mutableStateOf(index.toString()) }
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Move Section") },
            text = {
                Column {
                    Text("Current Position: #$index", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jumpValue,
                        onValueChange = { if(it.all { char -> char.isDigit() }) jumpValue = it },
                        label = { Text("Move to Serial Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { 
                    if(jumpValue.isNotBlank()) onReorder(jumpValue.toInt())
                    showJumpDialog = false
                }) { Text("MOVE") }
            },
            dismissButton = { TextButton(onClick = { showJumpDialog = false }) { Text("CANCEL") } }
        )
    }

    if (showUrlDialog && url != null && onUrlChange != null) {
        var tempUrl by remember { mutableStateOf(url) }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text(if(isProductAd) "Edit Section Label" else "Edit Media URL") },
            text = {
                Column {
                    Text(
                        text = if(isProductAd) "Enter the label to filter products for this section." else "Update the link for this $name.", 
                        fontSize = 12.sp, 
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text(if(isProductAd) "Label" else "URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { 
                    onUrlChange(tempUrl)
                    showUrlDialog = false
                }) { Text("SAVE") }
            },
            dismissButton = { TextButton(onClick = { showUrlDialog = false }) { Text("CANCEL") } }
        )
    }

    Surface(
        color = if(isSpecial) Color(0xFFFFF3E0) else Color.Gray.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = if(isSpecial) BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = FStorePurple,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(32.dp).clickable { 
                            jumpValue = index.toString()
                            showJumpDialog = true 
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = index.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Icon(
                        imageVector = if(isSpecial) Icons.Default.Image else Icons.Default.ViewCarousel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                        tint = if(isSpecial) Color(0xFFFF9800) else Color.Gray
                    )
                    Text(name, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = Color.LightGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (url != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if(isProductAd) Color(0xFFF1F8E9) else Color(0xFFFFF8E1))
                        .clickable { showUrlDialog = true }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if(isProductAd) Icons.AutoMirrored.Filled.Label else Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if(isProductAd) Color(0xFF388E3C) else Color(0xFFFF8F00)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if(isProductAd) "Filter: ${url.ifEmpty { "All Products" }}" else url.ifEmpty { "Click to add link" },
                        fontSize = 10.sp,
                        color = if(url.isEmpty()) Color.Red else (if(isProductAd) Color(0xFF1B5E20) else Color(0xFF795548)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                    
                    if (onPreviewVideo != null && url.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.PlayArrow, 
                            null, 
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onPreviewVideo() },
                            tint = FStorePurple
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StrategyCard(
    title: String, 
    subtitle: String, 
    icon: ImageVector, 
    color: Color, 
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                    Icon(icon, null, modifier = Modifier.padding(8.dp).size(20.dp), tint = color)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(subtitle, fontSize = 11.sp, color = Color.Gray)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            content()
        }
    }
}

@Composable
fun AddButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp)
    }
}
