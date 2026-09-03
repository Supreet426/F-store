package com.fryumm.store.ui.screens.useractivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.*
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun RiderMerchantTab(
    viewModel: StoreViewModel,
    mainTab: String,
    filteredUsers: List<UserProfile>,
    onUserClick: (UserProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (mainTab == "Riders") {
            item {
                val allUsers = viewModel.allUsers
                val totalRiders = allUsers.count { it.userRole == UserRole.RIDER }
                val activeRiders = viewModel.riders.size
                val busyOrders = viewModel.orders.filter { it.status == OrderStatus.ASSIGNED || it.status == OrderStatus.DISPATCHED }
                val busyRiders = busyOrders.mapNotNull { it.deliveryAgentId }.distinct().size
                val idleRiders = (activeRiders - busyRiders).coerceAtLeast(0)

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FStorePurple.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, FStorePurple.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(shape = CircleShape, color = FStorePurple.copy(0.1f), modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsBike, null, tint = FStorePurple, modifier = Modifier.padding(6.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Rider Ecosystem", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text("$activeRiders Online / $totalRiders Total", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(busyRiders.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE53935))
                                Text("Busy", fontSize = 9.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(idleRiders.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                                Text("Idle", fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        items(filteredUsers) { user ->
            if (mainTab == "Riders") {
                val analytics = viewModel.getAdvancedAnalytics()
                val performance = analytics.riderPerformance.find { it.riderId == user.uid }
                RiderManagementCard(
                    rider = user,
                    performance = performance,
                    onClick = { onUserClick(user) },
                    onStatusChange = { isActive -> viewModel.updateUserStatus(user.uid, isActive) }
                )
            } else if (mainTab == "Merchants") {
                 StoreManagementCard(
                    user = user,
                    darkStores = viewModel.darkStores,
                    onAssignHub = { hubId -> viewModel.assignMerchantToDarkStore(user.uid, hubId) },
                    onClick = { onUserClick(user) },
                    onStatusChange = { viewModel.updateUserStatus(user.uid, it) }
                )
            }
        }
    }
}
