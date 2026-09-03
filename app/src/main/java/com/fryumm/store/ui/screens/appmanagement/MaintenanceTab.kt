package com.fryumm.store.ui.screens.appmanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun MaintenanceTab(
    viewModel: StoreViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            StrategyCard(
                title = "System Maintenance",
                subtitle = "Initialize or Reset system data",
                icon = Icons.Default.Dns,
                color = Color.Red
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("DANGER ZONE: These actions affect live data.", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Red)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.repairEcosystem() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
                        ) {
                            Icon(Icons.Default.Build, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("REPAIR HOME", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.addMarketTrendProducts() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("POPULATE 100", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.resetAndRepopulateStore() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("RESET ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "Repair: Fixes disappearing products, activates seller profile, and cleans home layout.\nPopulate: Adds 100 products with images and repair config.\nReset: Deletes EVERYTHING and adds 5 sample products.",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 11.sp,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}
