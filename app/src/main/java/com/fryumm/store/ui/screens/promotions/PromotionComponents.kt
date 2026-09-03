package com.fryumm.store.ui.screens.promotions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.RecommendationItem
import com.fryumm.store.ui.components.LayoutChip
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun PromotionSequenceRow(
    item: RecommendationItem,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = FStorePurple.copy(0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${index + 1}", fontWeight = FontWeight.Bold, color = FStorePurple, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("${item.type}: ${item.id}", fontSize = 11.sp, color = Color.Gray)
            }
            Row {
                IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowDropUp, null)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(0.6f))
                }
            }
        }
    }
}

@Composable
fun LayoutSequenceRow(
    name: String,
    layout: String,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onLayoutChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = FStorePurple.copy(0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", fontWeight = FontWeight.Bold, color = FStorePurple, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("Layout: $layout", fontSize = 11.sp, color = FStorePurple, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowDropUp, null)
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(0.6f))
                    }
                }
            }
            
            val isCategory = !listOf("Banners", "FLASH_D_PASS", "BANNER_AD", "VIDEO_AD", "PRODUCT_AD").contains(name)
            if (isCategory) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LayoutChip(label = "Carousel", isSelected = layout == "Carousel", onClick = { onLayoutChange("Carousel") })
                    LayoutChip(label = "3x2 Grid", isSelected = layout == "Grid3x2", onClick = { onLayoutChange("Grid3x2") })
                    LayoutChip(label = "3x3 Grid", isSelected = layout == "Grid3x3", onClick = { onLayoutChange("Grid3x3") })
                }
            }
        }
    }
}
