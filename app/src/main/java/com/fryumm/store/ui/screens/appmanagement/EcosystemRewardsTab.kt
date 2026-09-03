package com.fryumm.store.ui.screens.appmanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.data.model.FlashOPassConfig
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun EcosystemRewardsTab(
    config: DynamicAppConfig,
    onConfigChange: (DynamicAppConfig) -> Unit
) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            StrategyCard(
                title = "Merchant Referral Campaign",
                subtitle = "Control what merchants share with customers",
                icon = Icons.Default.Campaign,
                color = Color(0xFF4CAF50)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = config.merchantReferralMessage,
                        onValueChange = { onConfigChange(config.copy(merchantReferralMessage = it)) },
                        label = { Text("WhatsApp/SMS Share Message") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Use {CODE} to inject referral code") },
                        supportingText = { Text("Make sure to tell customers to enter the code manually during signup for rewards.") }
                    )

                    OutlinedTextField(
                        value = config.appDownloadLink,
                        onValueChange = { onConfigChange(config.copy(appDownloadLink = it)) },
                        label = { Text("App Download Link") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://play.google.com/store/apps/details?id=...") }
                    )
                }
            }
        }

        item {
            StrategyCard(
                title = "Flash-O Pass Membership",
                subtitle = "Configure subscription tiers and benefits",
                icon = Icons.Default.CardMembership,
                color = Color(0xFFFF9800)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val pass = config.flashOPass ?: FlashOPassConfig()
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Activate Pass System", modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Switch(checked = pass.isActive, onCheckedChange = { a -> onConfigChange(config.copy(flashOPass = pass.copy(isActive = a))) })
                    }

                    pass.tiers.forEachIndexed { index, tier ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, FStorePurple.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = tier.title,
                                    onValueChange = { t -> 
                                        val newTiers = pass.tiers.toMutableList()
                                        newTiers[index] = tier.copy(title = t)
                                        onConfigChange(config.copy(flashOPass = pass.copy(tiers = newTiers)))
                                    },
                                    label = { Text("Tier Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontWeight = FontWeight.Bold, color = FStorePurple)
                                )
                                
                                OutlinedTextField(
                                    value = tier.subtitle,
                                    onValueChange = { s -> 
                                        val newTiers = pass.tiers.toMutableList()
                                        newTiers[index] = tier.copy(subtitle = s)
                                        onConfigChange(config.copy(flashOPass = pass.copy(tiers = newTiers)))
                                    },
                                    label = { Text("Benefit Summary") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = tier.price.toString(),
                                        onValueChange = { p -> 
                                            val newTiers = pass.tiers.toMutableList()
                                            newTiers[index] = tier.copy(price = p.toDoubleOrNull() ?: 0.0)
                                            onConfigChange(config.copy(flashOPass = pass.copy(tiers = newTiers)))
                                        },
                                        label = { Text("Price (₹)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                    OutlinedTextField(
                                        value = tier.minOrderForFreeDelivery.toString(),
                                        onValueChange = { m -> 
                                            val newTiers = pass.tiers.toMutableList()
                                            newTiers[index] = tier.copy(minOrderForFreeDelivery = m.toDoubleOrNull() ?: 0.0)
                                            onConfigChange(config.copy(flashOPass = pass.copy(tiers = newTiers)))
                                        },
                                        label = { Text("Min Order (₹)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }
                                
                                OutlinedTextField(
                                    value = tier.imageUrl,
                                    onValueChange = { i -> 
                                        val newTiers = pass.tiers.toMutableList()
                                        newTiers[index] = tier.copy(imageUrl = i)
                                        onConfigChange(config.copy(flashOPass = pass.copy(tiers = newTiers)))
                                    },
                                    label = { Text("Banner Image URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("https://...") }
                                )
                            }
                        }
                    }

                    if (pass.isActive) {
                        pass.tiers.forEach { tier ->
                            Card(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = FStorePurple)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (tier.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(tier.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tier.title.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                            Text(tier.subtitle, color = Color.White.copy(0.8f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Button(
                                            onClick = { },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("JOIN @ ₹${tier.price.toInt()}", color = FStorePurple, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
