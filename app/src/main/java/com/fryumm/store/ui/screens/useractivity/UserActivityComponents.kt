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
import java.text.SimpleDateFormat
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

fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "N/A"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDateOnly(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "N/A"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun UserSummaryCard(
    name: String,
    photoUrl: String,
    mobile: String,
    joinedDate: Long?,
    lastActive: Long?,
    lastOrderDate: Long?,
    roleText: String,
    onClick: () -> Unit,
    onRoleChangeClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = FStorePurple.copy(alpha = 0.1f)
            ) {
                if (photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.clip(CircleShape)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = FStorePurple)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = FStorePurple.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = roleText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = FStorePurple,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Mobile: ${mobile.ifEmpty { "N/A" }}", fontSize = 12.sp, color = Color.DarkGray)
                Text("Joined: ${formatDateOnly(joinedDate)}", fontSize = 11.sp, color = Color.Gray)
                Text("Last Active: ${formatTimestamp(lastActive)}", fontSize = 11.sp, color = Color.Gray)
                Text("Last Order: ${formatDateOnly(lastOrderDate)}", fontSize = 11.sp, color = Color.Gray)
            }

            Spacer(Modifier.width(8.dp))

            OutlinedButton(
                onClick = onRoleChangeClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FStorePurple)
            ) {
                Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Role Change", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomerListItem(customer: CustomerProfile, viewModel: StoreViewModel?, onClick: () -> Unit, onRoleChangeClick: () -> Unit) {
    UserSummaryCard(
        name = customer.name,
        photoUrl = customer.photoUrl,
        mobile = customer.mobile,
        joinedDate = customer.joinedDate,
        lastActive = customer.lastLogin,
        lastOrderDate = customer.lastOrderDate,
        roleText = "CUSTOMER",
        onClick = onClick,
        onRoleChangeClick = onRoleChangeClick
    )
}

@Composable
fun UserListItem(user: UserProfile, viewModel: StoreViewModel?, onClick: () -> Unit, onRoleChangeClick: () -> Unit) {
    val lastOrderDate = viewModel?.getLastOrderDateForUser(user.uid, user.email, user.phoneNumber)
    UserSummaryCard(
        name = user.name,
        photoUrl = user.photoUrl,
        mobile = if (user.phoneNumber.isNotEmpty()) user.phoneNumber else user.email,
        joinedDate = user.lastLogin,
        lastActive = user.lastLogin,
        lastOrderDate = lastOrderDate,
        roleText = user.userRole.name,
        onClick = onClick,
        onRoleChangeClick = onRoleChangeClick
    )
}

@Composable
fun StoreManagementCard(user: UserProfile, darkStores: List<DarkStore>, viewModel: StoreViewModel?, onClick: () -> Unit, onRoleChangeClick: () -> Unit) {
    val lastOrderDate = viewModel?.getLastOrderDateForUser(user.uid, user.email, user.phoneNumber)
    UserSummaryCard(
        name = user.name,
        photoUrl = user.photoUrl,
        mobile = if (user.phoneNumber.isNotEmpty()) user.phoneNumber else user.email,
        joinedDate = user.lastLogin,
        lastActive = user.lastLogin,
        lastOrderDate = lastOrderDate,
        roleText = user.userRole.name,
        onClick = onClick,
        onRoleChangeClick = onRoleChangeClick
    )
}

@Composable
fun RiderManagementCard(
    rider: UserProfile,
    viewModel: StoreViewModel?,
    onClick: () -> Unit,
    onRoleChangeClick: () -> Unit
) {
    val lastOrderDate = viewModel?.getLastOrderDateForUser(rider.uid, rider.email, rider.phoneNumber)
    UserSummaryCard(
        name = rider.name,
        photoUrl = rider.photoUrl,
        mobile = if (rider.phoneNumber.isNotEmpty()) rider.phoneNumber else rider.email,
        joinedDate = rider.lastLogin,
        lastActive = rider.lastLogin,
        lastOrderDate = lastOrderDate,
        roleText = rider.userRole.name,
        onClick = onClick,
        onRoleChangeClick = onRoleChangeClick
    )
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
