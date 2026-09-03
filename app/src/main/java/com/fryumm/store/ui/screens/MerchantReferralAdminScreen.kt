package com.fryumm.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.ui.theme.FStorePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantReferralAdminScreen(viewModel: StoreViewModel, onBack: () -> Unit) {
    val merchants = viewModel.allUsers.filter { it.userRole == UserRole.STORE_OWNER }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredMerchants = merchants.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.tradeName.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.referralVerifiedCount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Referral Ecosystem", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A237E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA))) {
            // Stats Header
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A237E)).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Verified Users", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    Text("${merchants.sumOf { it.referralVerifiedCount }}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Flash-O Coins Distributed", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    Text("©${merchants.sumOf { it.flashOCoins }.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by Shop Name or Email...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMerchants) { merchant ->
                    MerchantReferralAdminCard(merchant, viewModel)
                }
            }
        }
    }
}

@Composable
fun MerchantReferralAdminCard(merchant: UserProfile, viewModel: StoreViewModel) {
    var showEditDialog by remember { mutableStateOf(false) }
    var targetValue by remember { mutableStateOf(merchant.currentReferralTarget.toString()) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Manage ${merchant.name}") },
            text = {
                Column {
                    Text("Current Target: ${merchant.currentReferralTarget}", fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetValue,
                        onValueChange = { if(it.all { c -> c.isDigit() }) targetValue = it },
                        label = { Text("New Referral Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Merchant Payout: -${merchant.platformFeeDiscount}% Platform Fee", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updatedMerchant = merchant.copy(currentReferralTarget = targetValue.toIntOrNull() ?: 10)
                    viewModel.updateUserStatusForReferral(updatedMerchant)
                    showEditDialog = false
                }) { Text("SAVE CHANGES") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("CANCEL") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    BadgeSurfaceAdmin(merchant.merchantBadge)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(merchant.tradeName.ifEmpty { merchant.name }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Coins: ©${merchant.flashOCoins.toInt()}", fontSize = 12.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Settings, null, tint = Color.Gray)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            val verifiedCount = merchant.referralVerifiedCount
            val progress = (verifiedCount.toFloat() / merchant.currentReferralTarget).coerceIn(0f, 1f)
            val isCompleted = verifiedCount >= merchant.currentReferralTarget
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${verifiedCount}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = if(isCompleted) Color(0xFF2E7D32) else FStorePurple)
                Text(" / ${merchant.currentReferralTarget}", fontSize = 14.sp, color = Color.Gray)
                Text(" Verified", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.weight(1f))
                if (isCompleted) {
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                        Text("UPGRADE READY", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if(isCompleted) Color(0xFF4CAF50) else FStorePurple,
                trackColor = Color.LightGray.copy(alpha = 0.2f)
            )
            
            Spacer(Modifier.height(8.dp))
            Text("Total Leads: ${merchant.referralCount}", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun BadgeSurfaceAdmin(badge: String) {
    val color = when(badge) {
        "PLATINUM" -> Color(0xFFB4B4B4)
        "GOLD" -> Color(0xFFFFD700)
        "SILVER" -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }
    Surface(
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = color.copy(alpha = 0.2f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.WorkspacePremium, null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}
