package com.fryumm.store.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.ui.components.StatusBadge
import com.fryumm.store.ui.components.PaymentStatusBadge
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: String,
    viewModel: StoreViewModel,
    onBack: () -> Unit
) {
    val order = viewModel.orders.find { it.id == orderId } ?: return
    val context = LocalContext.current
    val merchant = viewModel.allUsers.find { it.uid == order.sellerId }
    val rider = viewModel.riders.find { it.id == order.deliveryAgentId }
    
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val orderTime = sdf.format(Date(order.timestamp))
    
    val assignmentStep = order.trackingHistory.find { it.title == OrderStatus.ASSIGNED }
    val assignmentTime = assignmentStep?.time ?: "Not assigned yet"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF7F7F7)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Status & ID Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Order #${order.id}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Text(orderTime, fontSize = 12.sp, color = Color.Gray)
                            }
                            StatusBadge(order.status)
                        }
                    }
                }
            }

            // Customer Details
            item {
                InfoSection(
                    title = "Customer Details",
                    icon = Icons.Default.Person,
                    content = {
                        InfoRow(label = "Name", value = order.customerName)
                        InfoRow(label = "Address", value = order.customerAddress.ifEmpty { order.shippingAddress })
                        InfoRow(label = "Phone", value = order.phoneNumber)
                    }
                )
            }

            // Store/Merchant Details
            item {
                InfoSection(
                    title = "Merchant Details",
                    icon = Icons.Default.Store,
                    content = {
                        InfoRow(label = "Store Name", value = merchant?.name ?: "Merchant ID: ${order.sellerId}")
                        InfoRow(label = "Location", value = order.area)
                    }
                )
            }

            // Rider Details
            item {
                InfoSection(
                    title = "Delivery Partner",
                    icon = Icons.Default.DeliveryDining,
                    content = {
                        if (order.deliveryAgentId != null) {
                            InfoRow(label = "Rider Name", value = rider?.name ?: "ID: ${order.deliveryAgentId}")
                            InfoRow(label = "Assigned At", value = assignmentTime)
                        } else {
                            Text("Not assigned to any rider yet", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                )
            }

            // Product List
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Products Ordered", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("₹${item.product.price} x ${item.quantity}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("₹${(item.quantity * item.product.price).toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            if (order.items.indexOf(item) < order.items.size - 1) {
                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            // Payment & Bill
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Bill Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        
                        InfoRow(label = "Payment Mode", value = order.paymentMethod)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Payment Status", fontSize = 13.sp, color = Color.Gray)
                            PaymentStatusBadge(order.paymentStatus)
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Bill", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("₹${order.totalAmount.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = FStorePurple)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FStorePurple.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, FStorePurple.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Need help with this order?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Flash-O Care is available 24/7 to assist you.", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val supportNumber = "9235900175"
                                val url = "https://api.whatsapp.com/send?phone=91$supportNumber&text=I need help with Order ID: ${order.id}"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(url)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("CONTACT FLASH-O CARE")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = FStorePurple, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.width(110.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}
