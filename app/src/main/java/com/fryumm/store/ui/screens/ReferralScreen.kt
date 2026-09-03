package com.fryumm.store.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.ui.theme.FStorePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(viewModel: StoreViewModel, onBack: () -> Unit) {
    val profile = viewModel.currentUserProfile ?: return
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refer & Earn", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FStorePurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats & Badge Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BadgeSurface(profile.merchantBadge)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Merchant Status", fontSize = 10.sp, color = Color.Gray)
                                Text(profile.merchantBadge, fontWeight = FontWeight.Black, fontSize = 18.sp, color = FStorePurple)
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("Leads", profile.referralCount.toString())
                            StatItem("Verified", profile.referralVerifiedCount.toString(), color = Color(0xFF2E7D32))
                            StatItem("Coins", "©${profile.flashOCoins.toInt()}", color = Color(0xFFFF9800))
                        }

                        Spacer(Modifier.height(24.dp))
                        
                        val progress = (profile.referralVerifiedCount.toFloat() / profile.currentReferralTarget).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = FStorePurple,
                            trackColor = FStorePurple.copy(alpha = 0.1f)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Target: ${profile.currentReferralTarget} Verified", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${(progress * 100).toInt()}% Achieved", fontSize = 11.sp, color = FStorePurple, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Wallet / Coins Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color(0xFFFFD700).copy(alpha = 0.2f)
                        ) {
                            Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFD700), modifier = Modifier.padding(10.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Flash-O Coins Wallet", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            Text("©${profile.flashOCoins.toInt()}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        TextButton(onClick = { /* Navigate to Ads Center */ }) {
                            Text("USE FOR ADS", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Share Referral Code
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FStorePurple.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, FStorePurple.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your Referral Code", fontSize = 12.sp, color = Color.Gray)
                            Text(profile.merchantReferralCode.ifEmpty { "GEN-CODE" }, fontSize = 20.sp, fontWeight = FontWeight.Black, color = FStorePurple)
                        }
                        Button(
                            onClick = {
                                val config = viewModel.dynamicAppConfig
                                val shareText = config.merchantReferralMessage.replace("{CODE}", profile.merchantReferralCode) + 
                                        "\n\n(Enter code ${profile.merchantReferralCode} during signup manually for extra rewards!)\n\nDownload here: ${config.appDownloadLink}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share via"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SHARE")
                        }
                    }
                }
            }

            // Manual Lead Entry
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Add Customer Lead", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Add customers who shop at your physical store to give them exclusive Flash-O app discounts.", fontSize = 12.sp, color = Color.Gray)
                        
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { if(it.length <= 10) customerPhone = it },
                            label = { Text("Mobile Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            prefix = { Text("+91 ") }
                        )
                        
                        Button(
                            onClick = {
                                if (customerName.isNotBlank() && customerPhone.length == 10) {
                                    viewModel.referCustomer(customerName, customerPhone)
                                    customerName = ""
                                    customerPhone = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                            enabled = customerName.isNotBlank() && customerPhone.length == 10
                        ) {
                            Text("REFER CUSTOMER", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Benefits Grid
            item {
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Merchant Privileges", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                        }
                        Spacer(Modifier.height(12.dp))
                        BenefitRow("Platinum", "5% Off Platform Fee + Banner Slot")
                        BenefitRow("Gold", "2% Off Platform Fee + Search Boost")
                        BenefitRow("Silver", "Priority Payouts + Search Boost")
                        BenefitRow("Bronze", "Entry Level")
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeSurface(badge: String) {
    val color = when(badge) {
        "PLATINUM" -> Color(0xFFB4B4B4)
        "GOLD" -> Color(0xFFFFD700)
        "SILVER" -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(2.dp, color)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.WorkspacePremium, null, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color = FStorePurple) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 22.sp, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BenefitRow(tier: String, benefit: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$tier:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1565C0), modifier = Modifier.width(70.dp))
        Text(benefit, fontSize = 12.sp, color = Color(0xFF1565C0))
    }
}
