package com.fryumm.store.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.data.model.Rider
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.ui.components.HandoverSection
import com.fryumm.store.ui.components.StatusBadge
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun DispatchScreen(
    viewModel: StoreViewModel,
    navController: NavHostController,
    onStartScan: (String?, String) -> Unit
) {
    val isMerchant = viewModel.currentUserProfile?.userRole == UserRole.STORE_OWNER
    val isAdmin = viewModel.isMasterAdmin || viewModel.currentUserProfile?.userRole == UserRole.ADMIN
    val isRider = viewModel.currentUserProfile?.userRole == UserRole.RIDER

    val readyToAssign = viewModel.orders.filter { it.status == OrderStatus.SEALED || it.status == OrderStatus.READY }
    val assignedOrders = viewModel.orders.filter { it.status == OrderStatus.ASSIGNED }
    val dispatchedOrders = viewModel.orders.filter { it.status == OrderStatus.DISPATCHED }
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (readyToAssign.isEmpty() && assignedOrders.isEmpty() && dispatchedOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                    Text("No dispatch pending", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                if (readyToAssign.isNotEmpty()) {
                    item {
                        Text("Ready to Assign", fontWeight = FontWeight.Bold, color = FStorePurple, fontSize = 14.sp)
                    }
                    
                    items(readyToAssign, key = { "ready_${it.id}" }) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("order_details/${order.id}") },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Order #${order.id}", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                        Text(order.area, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FStorePurple)
                                    }
                                    Text(if (order.status == OrderStatus.READY) "Slot: ${order.shelfSlot}" else "Packing...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                
                                // Rider assignment moved to Rider Profiles in Users page
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        if (order.deliveryAgentId == null) "Waiting for Rider Assignment (Manage in Rider Profiles)" else "Assigned to Rider",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                if (assignedOrders.isNotEmpty()) {
                    item {
                        Text("Waiting for Pickup", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 14.sp)
                    }
                    items(assignedOrders, key = { "assigned_${it.id}" }) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("order_details/${order.id}") },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9).copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Rider ID: ${order.deliveryAgentId}", fontSize = 11.sp)
                                }
                                HandoverSection(order, viewModel, onStartScan)
                            }
                        }
                    }
                }

                if (dispatchedOrders.isNotEmpty()) {
                    item {
                        Text("Recently Dispatched", fontWeight = FontWeight.Bold, color = Color(0xFF00ACC1), fontSize = 14.sp)
                    }
                    items(dispatchedOrders, key = { "dispatched_${it.id}" }) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("order_details/${order.id}") },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Order #${order.id}", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                                    if (!isMerchant) {
                                        Text(order.customerName, fontSize = 11.sp)
                                    }
                                }
                                StatusBadge(order.status)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}
