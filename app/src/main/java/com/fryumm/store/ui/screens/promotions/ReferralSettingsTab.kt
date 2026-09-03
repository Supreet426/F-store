package com.fryumm.store.ui.screens.promotions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.data.model.ReferralMilestone
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun ReferralSettingsTab(config: DynamicAppConfig, onUpdate: (DynamicAppConfig) -> Unit) {
    val referral = config.referralConfig
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Refer & Earn Program", fontWeight = FontWeight.Black, fontSize = 20.sp, color = FStorePurple)
        Text("Manage rewards and growth schemes.", fontSize = 12.sp, color = Color.Gray)
        
        Spacer(Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Referral Program", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(checked = referral.isActive, onCheckedChange = { onUpdate(config.copy(referralConfig = referral.copy(isActive = it))) })
                }
                
                Spacer(Modifier.height(16.dp))
                Text("Rewards for Referrer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = referral.referrerFirstOrderReward.toInt().toString(),
                        onValueChange = { val v = it.toDoubleOrNull() ?: 0.0; onUpdate(config.copy(referralConfig = referral.copy(referrerFirstOrderReward = v))) },
                        label = { Text("1st Order (₹)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = referral.referrerSecondOrderReward.toInt().toString(),
                        onValueChange = { val v = it.toDoubleOrNull() ?: 0.0; onUpdate(config.copy(referralConfig = referral.copy(referrerSecondOrderReward = v))) },
                        label = { Text("2nd Order (₹)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text("Reward for New User (Referee)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = referral.refereePassDurationDays.toString(),
                    onValueChange = { val v = it.toIntOrNull() ?: 30; onUpdate(config.copy(referralConfig = referral.copy(refereePassDurationDays = v))) },
                    label = { Text("Free Premium Pass Duration (Days)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Milestone Bonuses", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Extra rewards for reaching referral targets.", fontSize = 11.sp, color = Color.Gray)
        
        Spacer(Modifier.height(12.dp))
        
        referral.milestones.forEachIndexed { index, milestone ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${milestone.count} Referrals", fontWeight = FontWeight.Bold)
                        Text("Bonus: ₹${milestone.bonus.toInt()}", fontSize = 12.sp, color = FStorePurple)
                    }
                    IconButton(onClick = {
                        val newList = referral.milestones.toMutableList()
                        newList.removeAt(index)
                        onUpdate(config.copy(referralConfig = referral.copy(milestones = newList)))
                    }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f)) }
                }
            }
        }
        
        var showAddMilestone by remember { mutableStateOf(false) }
        if (showAddMilestone) {
            var mCount by remember { mutableStateOf("") }
            var mBonus by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddMilestone = false },
                title = { Text("Add Milestone") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = mCount, onValueChange = { mCount = it }, label = { Text("Referral Count") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = mBonus, onValueChange = { mBonus = it }, label = { Text("Bonus Amount (₹)") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val count = mCount.toIntOrNull() ?: 0
                        val bonus = mBonus.toDoubleOrNull() ?: 0.0
                        if (count > 0) {
                            val newList = referral.milestones + ReferralMilestone(count = count, bonus = bonus, label = "Milestone")
                            onUpdate(config.copy(referralConfig = referral.copy(milestones = newList.sortedBy { it.count })))
                            showAddMilestone = false
                        }
                    }) { Text("ADD") }
                }
            )
        }

        Button(onClick = { showAddMilestone = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = FStorePurple), border = BorderStroke(1.dp, FStorePurple)) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("ADD MILESTONE BONUS")
        }
        
        Spacer(Modifier.height(40.dp))
    }
}
