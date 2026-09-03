package com.fryumm.store.ui.screens.useractivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.*
import com.fryumm.store.ui.theme.FStorePurple
import java.util.*

@Composable
fun IntelligenceCard(label: String, value: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(110.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) color.copy(alpha = 0.1f) else Color.White),
        border = if (isSelected) BorderStroke(1.dp, color) else null
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun CustomerListItem(customer: CustomerProfile, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = FStorePurple.copy(alpha = 0.1f)
            ) {
                if (customer.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(customer.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.clip(CircleShape)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PersonOutline, null, tint = FStorePurple)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(customer.name, fontWeight = FontWeight.Bold)
                Text("${customer.totalOrders} orders • Spent: ₹${customer.lifetimeSpending.toInt()}", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            if (customer.totalOrders > 1) {
                Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(4.dp)) {
                    Text("REPEAT", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                }
            }
        }
    }
}

@Composable
fun UserListItem(user: UserProfile, onClick: () -> Unit, onStatusChange: ((Boolean) -> Unit)? = null) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = FStorePurple.copy(alpha = 0.1f)
            ) {
                if (user.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(user.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.clip(CircleShape)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(when(user.userRole) {
                            UserRole.RIDER -> Icons.Default.DeliveryDining
                            UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                            UserRole.OTHER -> Icons.Default.HeadsetMic
                            else -> Icons.Default.Store
                        }, null, tint = FStorePurple)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold)
                Text(if (user.email.isNotEmpty()) user.email else user.phoneNumber, fontSize = 12.sp, color = Color.Gray)
                if (user.userRole == UserRole.OTHER && user.staffDepartment.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFFE8EAF6),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = user.staffDepartment,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF283593),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Switch(checked = user.active, onCheckedChange = onStatusChange)
        }
    }
}

@Composable
fun StoreManagementCard(user: UserProfile, darkStores: List<DarkStore>, onAssignHub: (String?) -> Unit, onClick: () -> Unit, onStatusChange: (Boolean) -> Unit) {
    var isPromoted by remember { mutableStateOf(false) }
    var showHubMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
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
                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(user.email, fontSize = 11.sp, color = Color.Gray)
                }
                Switch(checked = user.active, onCheckedChange = { onStatusChange(it) })
            }
            
            Spacer(Modifier.height(8.dp))
            
            Surface(
                color = Color(0xFFF3E5F5),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).clickable { showHubMenu = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Hub, null, modifier = Modifier.size(16.dp), tint = FStorePurple)
                    Spacer(Modifier.width(8.dp))
                    val hubName = darkStores.find { it.id == user.darkStoreId }?.name ?: "Assign to Hub (Dark Store)"
                    Text(hubName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FStorePurple, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null, tint = FStorePurple)
                    
                    DropdownMenu(expanded = showHubMenu, onDismissRequest = { showHubMenu = false }) {
                        DropdownMenuItem(text = { Text("None / De-assign") }, onClick = { onAssignHub(null); showHubMenu = false })
                        darkStores.forEach { ds ->
                            DropdownMenuItem(text = { Text(ds.name) }, onClick = { onAssignHub(ds.id); showHubMenu = false })
                        }
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isPromoted) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = null,
                        tint = if (isPromoted) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Promoted Store", fontSize = 13.sp)
                }
                Checkbox(checked = isPromoted, onCheckedChange = { isPromoted = it })
            }
        }
    }
}

@Composable
fun RiderManagementCard(
    rider: UserProfile,
    performance: RiderPerformance?,
    onClick: () -> Unit,
    onStatusChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (rider.active) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (rider.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(rider.photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.clip(CircleShape)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DirectionsBike,
                                    contentDescription = null,
                                    tint = if (rider.active) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(rider.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(if (rider.active) "Active" else "Inactive", fontSize = 11.sp, color = if (rider.active) Color(0xFF2E7D32) else Color.Gray)
                    }
                }
                Switch(checked = rider.active, onCheckedChange = { onStatusChange(it) })
            }

            if (performance != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RiderStatItem("Deliveries", performance.totalDeliveries.toString())
                    RiderStatItem("Rating", String.format(Locale.getDefault(), "%.1f ★", performance.avgRating))
                    RiderStatItem("Earnings", "₹${performance.totalEarnings.toInt()}")
                }
            }
        }
    }
}

@Composable
fun RiderStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun StrategyInfoCard() {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, null, tint = Color(0xFF2E7D32))
                Spacer(Modifier.width(12.dp))
                Text("Flash-O Smart Targeting Policy", fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                Spacer(Modifier.weight(1f))
                Icon(if(expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Text("Duniya ke top targeting formulas jo hum use kar rahe hain:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                StrategyBullet("RFM Model", "Recency, Frequency, Monetary data se customers ko VIP aur Dormant groups mein baant-na.")
                StrategyBullet("Intent Recovery", "Search results 0 hone par un products ko 'Back in Stock' ya 'Similar' dikhana.")
                StrategyBullet("Cart Optimization", "Abandoners ko personalize coupons bhej kar conversion 3x karna.")
                StrategyBullet("Predictive Suggestions", "History aur searches ke base par 'Personalized for You' section generate karna.")
            }
        }
    }
}

@Composable
fun StrategyBullet(title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("•", fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(desc, fontSize = 11.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun ActivityItem(user: String, action: String, time: String, area: String, results: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text(action, fontSize = 13.sp)
                Text(if (results > 0) "Found $results items" else "No items found", fontSize = 11.sp, color = if (results > 0) Color(0xFF4CAF50) else Color.Red)
            }
            Text(time, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun OrderAnalyticsItem(order: Order, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Receipt, null, tint = Color(0xFFE91E63))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("#${order.id}", fontWeight = FontWeight.Bold)
                Text("${order.customerName} • ₹${order.totalAmount}", fontSize = 12.sp, color = Color.Gray)
            }
            Text("Profit: ₹${order.estimatedProfit.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun MarketingSummaryCard(analytics: MarketingAnalytics) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Conversion Funnel", fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(analytics.overallFunnel.sent.toString(), fontWeight = FontWeight.Black); Text("SENT", fontSize = 9.sp) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(analytics.overallFunnel.opened.toString(), fontWeight = FontWeight.Black); Text("OPENED", fontSize = 9.sp) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(analytics.overallFunnel.ordered.toString(), fontWeight = FontWeight.Black); Text("ORDERED", fontSize = 9.sp) }
            }
        }
    }
}

@Composable
fun CampaignItem(campaign: CampaignStats) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(campaign.campaignName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("₹${campaign.totalRevenue.toInt()}", fontWeight = FontWeight.Black, color = Color(0xFF388E3C))
        }
    }
}

@Composable
fun FraudSummaryCard(analytics: FraudAnalytics) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Risk Summary", fontWeight = FontWeight.Bold, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Flagged: ${analytics.totalFlaggedUsers}", color = Color.Red, fontWeight = FontWeight.Bold)
                Text("At Risk: ₹${analytics.totalRiskAmount.toInt()}", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FraudIncidentItem(incident: FraudIncident) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(incident.customerName, fontWeight = FontWeight.Bold)
            Text(incident.reason, color = Color.Red, fontSize = 12.sp)
            Text(incident.evidence, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun BusinessKPIsDashboard(kpis: BusinessKPIs) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KPICard("Gross Profit", "₹${kpis.grossProfit.toInt()}", Color(0xFF4CAF50), Modifier.weight(1f))
            KPICard("Net Profit", "₹${kpis.netProfit.toInt()}", Color(0xFF2E7D32), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KPICard("CAC", "₹${kpis.cac.toInt()}", Color(0xFFE91E63), Modifier.weight(1f))
            KPICard("LTV", "₹${kpis.ltv.toInt()}", Color(0xFF9C27B0), Modifier.weight(1f))
        }
    }
}

@Composable
fun KPICard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)), border = BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = Color.DarkGray)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}
