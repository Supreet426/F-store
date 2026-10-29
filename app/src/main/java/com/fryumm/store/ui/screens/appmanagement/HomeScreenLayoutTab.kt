package com.fryumm.store.ui.screens.appmanagement

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.*
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenLayoutTab(
    config: DynamicAppConfig,
    onConfigChange: (DynamicAppConfig) -> Unit
) {
    val context = LocalContext.current
    var selectedPlatform by remember { mutableStateOf("app") } // "app", "web", "global"
    var activeSubTab by remember { mutableStateOf("sections") } // "sections", "banners", "flash_sale", "pass", "popup"
    
    var localConfig by remember(config) { mutableStateOf(config) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showPublishDialog by remember { mutableStateOf(false) }
    var publishTarget by remember { mutableStateOf("app") }

    // Dialog state for adding banner
    var showBannerDialog by remember { mutableStateOf(false) }
    var bannerTitle by remember { mutableStateOf("") }
    var bannerImageUrl by remember { mutableStateOf("") }
    var bannerActionUrl by remember { mutableStateOf("") }

    // Dialog state for adding section
    var showSectionDialog by remember { mutableStateOf(false) }
    var newSectionName by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val lastUpdatedStr = remember(localConfig.lastUpdated) {
        if (localConfig.lastUpdated > 0) dateFormat.format(Date(localConfig.lastUpdated)) else "Just now"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Prominent Main Platform Tabs (Customer App, Customer Website, Global Settings) stuck right to top title bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "app" to "Customer App", 
                    "web" to "Customer Website", 
                    "global" to "Global Settings"
                ).forEach { (key, label) ->
                    val isSelected = selectedPlatform == key
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPlatform = key },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) FStorePurple else Color.Transparent,
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }

        // 2. Sub-Navigation Tabs & Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sub-Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = when(activeSubTab) {
                    "sections" -> 0
                    "banners" -> 1
                    "flash_sale" -> 2
                    "pass" -> 3
                    "popup" -> 4
                    else -> 0
                },
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                divider = {}
            ) {
                listOf(
                    "sections" to "Visual Section Builder",
                    "banners" to "Banners & Campaigns",
                    "flash_sale" to "Flash Sale Engine",
                    "pass" to "Flash-O Pass Tiers",
                    "popup" to "Popup Alert"
                ).forEach { (key, label) ->
                    Tab(
                        selected = activeSubTab == key,
                        onClick = { activeSubTab = key },
                        text = {
                            Text(
                                label,
                                fontWeight = if (activeSubTab == key) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeSubTab == key) FStorePurple else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Content Area based on Sub-Tab
            Box(modifier = Modifier.weight(1f)) {
                when (activeSubTab) {
                    "sections" -> SectionsBuilderView(
                        config = localConfig,
                        onAddSection = { showSectionDialog = true },
                        onReorder = { newOrder ->
                            localConfig = localConfig.copy(homeSectionOrder = newOrder, lastUpdated = System.currentTimeMillis())
                            hasUnsavedChanges = true
                            onConfigChange(localConfig)
                        },
                        onDeleteSection = { sectionName ->
                            val updated = localConfig.homeSectionOrder.filter { it != sectionName }
                            localConfig = localConfig.copy(homeSectionOrder = updated, lastUpdated = System.currentTimeMillis())
                            hasUnsavedChanges = true
                            onConfigChange(localConfig)
                        }
                    )
                    "banners" -> BannersManagerView(
                        banners = localConfig.bannerAds,
                        onAddClick = { showBannerDialog = true },
                        onToggle = { id ->
                            val updated = localConfig.bannerAds.map { if (it.id == id) it.copy(isActive = !it.isActive) else it }
                            localConfig = localConfig.copy(bannerAds = updated, lastUpdated = System.currentTimeMillis())
                            hasUnsavedChanges = true
                            onConfigChange(localConfig)
                        },
                        onDelete = { id ->
                            val updated = localConfig.bannerAds.filter { it.id != id }
                            localConfig = localConfig.copy(bannerAds = updated, lastUpdated = System.currentTimeMillis())
                            hasUnsavedChanges = true
                            onConfigChange(localConfig)
                        }
                    )
                    "flash_sale" -> FlashSaleManagerView(
                        flashSale = localConfig.flashSale,
                        onUpdate = { fs ->
                            localConfig = localConfig.copy(flashSale = fs, lastUpdated = System.currentTimeMillis())
                            hasUnsavedChanges = true
                            onConfigChange(localConfig)
                        }
                    )
                    "pass" -> FlashOPassManagerView(
                        passConfig = localConfig.flashOPass,
                        onUpdate = { pass ->
                            localConfig = localConfig.copy(flashOPass = pass, lastUpdated = System.currentTimeMillis())
                            hasUnsavedChanges = true
                            onConfigChange(localConfig)
                        }
                    )
                    "popup" -> PopupManagerView(
                        popup = localConfig.popupNotification,
                        onUpdate = { pop ->
                            localConfig = localConfig.copy(popupNotification = pop, lastUpdated = System.currentTimeMillis())
                            hasUnsavedChanges = true
                            onConfigChange(localConfig)
                        }
                    )
                }
            }
        }

        // 3. Bottom Status & Publish Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Target: ${selectedPlatform.uppercase()} • Status: ${if (hasUnsavedChanges) "Unsaved Draft" else "Live"}", 
                        fontSize = 11.sp, 
                        color = if (hasUnsavedChanges) Color(0xFFD97706) else Color(0xFF059669),
                        fontWeight = FontWeight.Bold
                    )
                    Text("Last: $lastUpdatedStr", fontSize = 10.sp, color = Color.Gray)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            onConfigChange(localConfig.copy(lastUpdated = System.currentTimeMillis()))
                            hasUnsavedChanges = false
                            Toast.makeText(context, "Draft saved locally!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, FStorePurple),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                    }

                    Button(
                        onClick = {
                            publishTarget = selectedPlatform
                            showPublishDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Publish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Add Banner Dialog
    if (showBannerDialog) {
        AlertDialog(
            onDismissRequest = { showBannerDialog = false },
            title = { Text("Add Promotional Banner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = bannerTitle, onValueChange = { bannerTitle = it }, label = { Text("Banner Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bannerImageUrl, onValueChange = { bannerImageUrl = it }, label = { Text("Image URL (Unsplash / Storage)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bannerActionUrl, onValueChange = { bannerActionUrl = it }, label = { Text("Action URL / Target (e.g. flash_sale)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (bannerTitle.isNotBlank() && bannerImageUrl.isNotBlank()) {
                        val newBanner = BannerAd(
                            id = "banner_" + System.currentTimeMillis(),
                            title = bannerTitle,
                            imageUrl = bannerImageUrl,
                            actionUrl = bannerActionUrl,
                            isActive = true
                        )
                        localConfig = localConfig.copy(bannerAds = localConfig.bannerAds + newBanner, lastUpdated = System.currentTimeMillis())
                        hasUnsavedChanges = true
                        onConfigChange(localConfig)
                        bannerTitle = ""
                        bannerImageUrl = ""
                        bannerActionUrl = ""
                        showBannerDialog = false
                        Toast.makeText(context, "Banner added to draft", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("ADD BANNER") }
            },
            dismissButton = { TextButton(onClick = { showBannerDialog = false }) { Text("CANCEL") } }
        )
    }

    // Add Section Dialog
    if (showSectionDialog) {
        AlertDialog(
            onDismissRequest = { showSectionDialog = false },
            title = { Text("Add Home Section") },
            text = {
                OutlinedTextField(
                    value = newSectionName,
                    onValueChange = { newSectionName = it },
                    label = { Text("Section Name (e.g. BestSellers, CuratedDeals)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newSectionName.isNotBlank()) {
                        val currentList = localConfig.homeSectionOrder.ifEmpty {
                            listOf("hero_banners", "shop_by_category", "flash_sale", "best_sellers", "curated_collections", "flash_o_pass")
                        }
                        val updated = currentList + newSectionName.trim().lowercase().replace(" ", "_")
                        localConfig = localConfig.copy(homeSectionOrder = updated, lastUpdated = System.currentTimeMillis())
                        hasUnsavedChanges = true
                        onConfigChange(localConfig)
                        newSectionName = ""
                        showSectionDialog = false
                        Toast.makeText(context, "Section added to layout", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("ADD") }
            },
            dismissButton = { TextButton(onClick = { showSectionDialog = false }) { Text("CANCEL") } }
        )
    }

    // Publish Confirmation Dialog
    if (showPublishDialog) {
        AlertDialog(
            onDismissRequest = { showPublishDialog = false },
            title = { Text("Publish to ${publishTarget.uppercase()} Live") },
            text = { Text("Are you sure you want to publish this Home Screen configuration live? Changes will reflect immediately on client apps and websites.") },
            confirmButton = {
                Button(onClick = {
                    showPublishDialog = false
                    // Trigger final config save & publish
                    onConfigChange(localConfig.copy(lastUpdated = System.currentTimeMillis()))
                    hasUnsavedChanges = false
                    Toast.makeText(context, "Successfully published live to ${publishTarget.uppercase()}!", Toast.LENGTH_LONG).show()
                }) { Text("CONFIRM & PUBLISH") }
            },
            dismissButton = { TextButton(onClick = { showPublishDialog = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
fun SectionsBuilderView(
    config: DynamicAppConfig,
    onAddSection: () -> Unit,
    onReorder: (List<String>) -> Unit,
    onDeleteSection: (String) -> Unit
) {
    val sections = config.homeSectionOrder.ifEmpty {
        listOf("hero_banners", "shop_by_category", "flash_sale", "best_sellers", "curated_collections", "flash_o_pass")
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Configured Section Sequence", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Button(onClick = onAddSection, colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Section", fontSize = 12.sp)
            }
        }

        if (sections.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sections configured. Click 'Add Section' to build layout.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                itemsIndexed(sections) { index, section ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = FStorePurple.copy(alpha = 0.1f)
                                ) {
                                    Text("#${index + 1}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = FStorePurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Column {
                                    Text(section.replace("_", " ").replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Active Section Component", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {
                                    if (index > 0) {
                                        val mutable = sections.toMutableList()
                                        val item = mutable.removeAt(index)
                                        mutable.add(index - 1, item)
                                        onReorder(mutable)
                                    }
                                }) {
                                    Icon(Icons.Default.ArrowUpward, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    if (index < sections.size - 1) {
                                        val mutable = sections.toMutableList()
                                        val item = mutable.removeAt(index)
                                        mutable.add(index + 1, item)
                                        onReorder(mutable)
                                    }
                                }) {
                                    Icon(Icons.Default.ArrowDownward, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeleteSection(section) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BannersManagerView(
    banners: List<BannerAd>,
    onAddClick: () -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Promotional Banners (${banners.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Button(onClick = onAddClick, colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Banner", fontSize = 12.sp)
            }
        }

        if (banners.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No banners configured. Click 'Add Banner' to create one.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                itemsIndexed(banners) { _, banner ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(banner.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Switch(
                                    checked = banner.isActive,
                                    onCheckedChange = { onToggle(banner.id) }
                                )
                            }
                            Text("Action: ${banner.actionUrl.ifEmpty { "None" }}", fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onDelete(banner.id) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlashSaleManagerView(
    flashSale: FlashSale?,
    onUpdate: (FlashSale) -> Unit
) {
    var title by remember(flashSale) { mutableStateOf(flashSale?.title ?: "Flash Sale 50% Off") }
    var discount by remember(flashSale) { mutableStateOf((flashSale?.discountPercentage ?: 50).toString()) }
    var isActive by remember(flashSale) { mutableStateOf(flashSale?.isActive ?: true) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Flash Sale Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Flash Sale Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Switch(checked = isActive, onCheckedChange = { 
                        isActive = it
                        onUpdate(FlashSale(title = title, discountPercentage = discount.toIntOrNull() ?: 50, isActive = isActive, startTime = System.currentTimeMillis(), endTime = System.currentTimeMillis() + 86400000))
                    })
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        onUpdate(FlashSale(title = title, discountPercentage = discount.toIntOrNull() ?: 50, isActive = isActive, startTime = System.currentTimeMillis(), endTime = System.currentTimeMillis() + 86400000))
                    },
                    label = { Text("Flash Sale Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = discount,
                    onValueChange = { 
                        discount = it
                        onUpdate(FlashSale(title = title, discountPercentage = discount.toIntOrNull() ?: 50, isActive = isActive, startTime = System.currentTimeMillis(), endTime = System.currentTimeMillis() + 86400000))
                    },
                    label = { Text("Discount Percentage (%)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun FlashOPassManagerView(
    passConfig: FlashOPassConfig?,
    onUpdate: (FlashOPassConfig) -> Unit
) {
    var title by remember(passConfig) { mutableStateOf(passConfig?.title ?: "Flash-O Pass") }
    var isActive by remember(passConfig) { mutableStateOf(passConfig?.isActive ?: true) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Flash-O Pass Membership Tiers", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Flash-O Pass", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Switch(checked = isActive, onCheckedChange = { 
                        isActive = it
                        onUpdate(FlashOPassConfig(title = title, isActive = isActive, tiers = passConfig?.tiers ?: emptyList()))
                    })
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        onUpdate(FlashOPassConfig(title = title, isActive = isActive, tiers = passConfig?.tiers ?: emptyList()))
                    },
                    label = { Text("Pass Program Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Configured Tiers: ${(passConfig?.tiers ?: emptyList()).size} Active Tiers", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun PopupManagerView(
    popup: PopupNotification?,
    onUpdate: (PopupNotification) -> Unit
) {
    var title by remember(popup) { mutableStateOf(popup?.title ?: "") }
    var message by remember(popup) { mutableStateOf(popup?.message ?: "") }
    var imageUrl by remember(popup) { mutableStateOf(popup?.imageUrl ?: "") }
    var isActive by remember(popup) { mutableStateOf(popup?.isActive ?: false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Customer App Popup Alert", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Broadcast Popup to App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Switch(checked = isActive, onCheckedChange = { 
                        isActive = it
                        onUpdate(PopupNotification(title = title, message = message, imageUrl = imageUrl, isActive = isActive))
                    })
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        onUpdate(PopupNotification(title = title, message = message, imageUrl = imageUrl, isActive = isActive))
                    },
                    label = { Text("Popup Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { 
                        message = it
                        onUpdate(PopupNotification(title = title, message = message, imageUrl = imageUrl, isActive = isActive))
                    },
                    label = { Text("Popup Message") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { 
                        imageUrl = it
                        onUpdate(PopupNotification(title = title, message = message, imageUrl = imageUrl, isActive = isActive))
                    },
                    label = { Text("Image URL (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
