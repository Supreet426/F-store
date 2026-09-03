package com.fryumm.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavHostController
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.MerchantSettlement
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.ui.theme.FStorePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(viewModel: StoreViewModel, navController: NavHostController) {
    val settlements = remember { viewModel.getMerchantSettlements() }
    val isAdmin = viewModel.currentUserProfile?.userRole == UserRole.ADMIN || viewModel.isMasterAdmin

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settlements", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if(isAdmin) Color(0xFF1A237E) else FStorePurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (settlements.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("No delivered orders found for settlement.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(settlements) { settlement ->
                    MerchantSettlementCard(settlement, isAdmin)
                }
            }
        }
    }
}

@Composable
fun MerchantSettlementCard(settlement: MerchantSettlement, isAdmin: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if(isAdmin) settlement.merchantName else "Your Earnings Summary",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = if(isAdmin) Color(0xFF1A237E) else FStorePurple
                    )
                    Text("${settlement.totalOrders} Delivered Orders", fontSize = 12.sp, color = Color.Gray)
                }
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "₹${settlement.totalPayout}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            Text("Product Wise Breakdown (Merchant Rate)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            
            settlement.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.productName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("${item.quantity} units sold @ ₹${item.merchantRate}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("₹${item.totalAmount}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            
            if (isAdmin) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { /* Implement Mark as Paid logic if needed */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MARK AS SETTLED")
                }
            }
        }
    }
}
