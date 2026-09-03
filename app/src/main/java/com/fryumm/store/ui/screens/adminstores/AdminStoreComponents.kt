package com.fryumm.store.ui.screens.adminstores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.components.LayoutChip
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun CategoryCard(
    category: String, 
    serialNumber: Int, 
    productCount: Int, 
    isVisibleOnHome: Boolean,
    isComingSoon: Boolean = false,
    currentLayout: String = "Carousel",
    onVisibilityToggle: (Boolean) -> Unit,
    onComingSoonToggle: () -> Unit = {},
    onLayoutChange: (String) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onReorder: (Int) -> Unit
) {
    var showReorderDialog by remember { mutableStateOf(false) }

    if (showReorderDialog) {
        ReorderCategoryDialog(
            category = category,
            currentSerial = serialNumber,
            onDismiss = { showReorderDialog = false },
            onConfirm = { 
                onReorder(it)
                showReorderDialog = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FStorePurple.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp).clickable { showReorderDialog = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = serialNumber.toString(), fontWeight = FontWeight.Bold, color = FStorePurple, fontSize = 16.sp)
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (isComingSoon) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFFF9800).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    " COMING SOON ",
                                    color = Color(0xFFFF9800),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
                    Text("$productCount Products", fontSize = 12.sp, color = Color.Gray)
                }

                IconButton(onClick = onComingSoonToggle) {
                    Icon(
                        if (isComingSoon) Icons.Default.Timer else Icons.Default.TimerOff,
                        null,
                        tint = if (isComingSoon) Color(0xFFFF9800) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Checkbox(
                        checked = isVisibleOnHome,
                        onCheckedChange = onVisibilityToggle,
                        colors = CheckboxDefaults.colors(checkedColor = FStorePurple)
                    )
                    Text("HOME", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if(isVisibleOnHome) FStorePurple else Color.Gray)
                }
                
                Spacer(Modifier.width(12.dp))
                
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.6f))
                }
                
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }

            if (isVisibleOnHome) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Layout:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LayoutChip(label = "Carousel", isSelected = currentLayout == "Carousel", onClick = { onLayoutChange("Carousel") })
                        LayoutChip(label = "3x2 Grid", isSelected = currentLayout == "Grid3x2", onClick = { onLayoutChange("Grid3x2") })
                        LayoutChip(label = "3x3 Grid", isSelected = currentLayout == "Grid3x3", onClick = { onLayoutChange("Grid3x3") })
                    }
                }
            }
        }
    }
}
