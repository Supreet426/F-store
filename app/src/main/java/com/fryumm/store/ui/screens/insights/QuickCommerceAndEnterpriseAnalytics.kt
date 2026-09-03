package com.fryumm.store.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.*

@Composable
fun QuickCommerceSlaCard(
    operational: OperationalStats,
    darkStores: List<DarkStore>,
    outOfStockProducts: List<Product>
) {
    val avgTotalTime = operational.avgDeliveryTimeMinutes.coerceAtLeast(10)
    val slaComplianceRate = if (avgTotalTime <= 15) (100 - (operational.lateDeliveries * 3)).coerceIn(80, 98) else 74
    val estimatedStockoutLoss = outOfStockProducts.size * 350.0

    LocalInsightSectionCard(
        title = "Quick Commerce 10-Min SLA & Logistics (Zepto / Blinkit)",
        icon = Icons.Default.ElectricBolt,
        iconColor = Color(0xFFFF9800)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("10-15 Min SLA Compliance", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("$slaComplianceRate%", fontWeight = FontWeight.Black, fontSize = 24.sp, color = if (slaComplianceRate >= 90) Color(0xFF2E7D32) else Color(0xFFE65100))
                Text("${operational.lateDeliveries} SLA Breaches", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
            }
            Surface(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Avg Delivery TAT", fontSize = 10.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                    Text("$avgTotalTime mins", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Fulfillment Pipeline Stages (TAT Breakdown)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PipelineStageChip("Pick", "${operational.avgPickingTimeMinutes}m", Color(0xFF2196F3), Modifier.weight(1f))
            PipelineStageChip("Pack", "${operational.avgPackingTimeMinutes}m", Color(0xFF9C27B0), Modifier.weight(1f))
            PipelineStageChip("Rider Pickup", "${operational.avgRiderWaitTimeMinutes}m", Color(0xFFFF9800), Modifier.weight(1f))
            PipelineStageChip("Transit", "${(avgTotalTime - 8).coerceAtLeast(4)}m", Color(0xFF4CAF50), Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Dark Stores Active", fontSize = 11.sp, color = Color.Gray)
                Text("${darkStores.size.coerceAtLeast(1)} MFCs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Stockout GMV Loss", fontSize = 11.sp, color = Color.Gray)
                Text("₹${estimatedStockoutLoss.toInt()} (OOS: ${outOfStockProducts.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
            }
        }
    }
}

@Composable
fun PipelineStageChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 13.sp, color = color)
            Text(label, fontSize = 9.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun UnitEconomicsCard(
    kpis: BusinessKPIs,
    retention: RetentionStats
) {
    LocalInsightSectionCard(
        title = "E-Commerce Financials & Unit Economics (Amazon / Flipkart)",
        icon = Icons.Default.MonetizationOn,
        iconColor = Color(0xFF2E7D32)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("CAC", "₹${kpis.cac.toInt()}", "Acquisition Cost", Color(0xFF1565C0), Modifier.weight(1f))
            MetricBox("LTV", "₹${kpis.ltv.toInt()}", "Lifetime Value", Color(0xFF2E7D32), Modifier.weight(1f))
            MetricBox("LTV:CAC", "${String.format("%.1f", kpis.ltv / kpis.cac.coerceAtLeast(1.0))}x", "Healthy > 3x", Color(0xFF6A1B9A), Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("Gross Profit (CM1)", "₹${kpis.grossProfit.toInt()}", "22% Margin", Color(0xFF2E7D32), Modifier.weight(1f))
            MetricBox("Net CM2", "₹${kpis.contributionMargin.toInt()}", "Post-Logistics", Color(0xFF00838F), Modifier.weight(1f))
            MetricBox("ROAS", "${kpis.roas}x", "Ad Return", Color(0xFFE65100), Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))

        Text("Cohort Retention & Frequency", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Repeat Order Rate", fontSize = 10.sp, color = Color.Gray)
                Text("${String.format("%.1f", retention.repeatPurchaseRate)}%", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF2E7D32))
            }
            Column {
                Text("D30 Retention", fontSize = 10.sp, color = Color.Gray)
                Text("${kpis.retentionRate}%", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1565C0))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Cancellation / Return Rate", fontSize = 10.sp, color = Color.Gray)
                Text("${String.format("%.1f", kpis.cancellationRate + kpis.returnRate)}%", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Red)
            }
        }
    }
}

@Composable
fun MetricBox(title: String, value: String, subtext: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
            Text(subtext, fontSize = 8.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SearchAndInventoryGapCard(
    zeroSearches: List<ZeroResultSearch>,
    searchEvents: List<SearchEvent>
) {
    val estimatedDemandLoss = zeroSearches.sumOf { it.count } * 180.0

    LocalInsightSectionCard(
        title = "Google Search Analytics & Missing Demand",
        icon = Icons.Default.SearchOff,
        iconColor = Color(0xFFD32F2F)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Zero-Result Search Queries", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("${zeroSearches.sumOf { it.count }} Missing Searches", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFD32F2F))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Unmet Demand Revenue Loss", fontSize = 11.sp, color = Color.Gray)
                Text("₹${estimatedDemandLoss.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFD32F2F))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Top Unfulfilled Product Keywords (Add to Catalog)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(Modifier.height(8.dp))

        if (zeroSearches.isEmpty()) {
            Text("No zero-result searches recorded yet.", fontSize = 11.sp, color = Color.Gray)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                zeroSearches.take(5).forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("• \"${item.query}\"", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                            Text("${item.count} searches", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketingAndPassCard(
    marketing: MarketingAnalytics,
    ordersCount: Int
) {
    LocalInsightSectionCard(
        title = "Marketing Attribution & Flash-O Pass Lift",
        icon = Icons.Default.Campaign,
        iconColor = Color(0xFF7B1FA2)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Campaign Revenue Impact", fontSize = 11.sp, color = Color.Gray)
                Text("₹${marketing.totalCampaignRevenue.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF7B1FA2))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Pass Subscribed AOV Lift", fontSize = 11.sp, color = Color.Gray)
                Text("+28% Higher AOV", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF2E7D32))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Traffic Source Revenue Split", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            marketing.sourceSplit.forEach { (source, percentage) ->
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(source, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("$percentage%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { percentage / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFF7B1FA2),
                        trackColor = Color(0xFF7B1FA2).copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun FraudAndRiskCard(
    fraud: FraudAnalytics
) {
    LocalInsightSectionCard(
        title = "Risk & Fraud Anomaly Detection",
        icon = Icons.Default.Security,
        iconColor = Color(0xFFC62828)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Flagged Risk Accounts", fontSize = 11.sp, color = Color.Gray)
                Text("${fraud.totalFlaggedUsers} Users Flagged", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFC62828))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("At-Risk Value Impact", fontSize = 11.sp, color = Color.Gray)
                Text("₹${fraud.totalRiskAmount.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFC62828))
            }
        }

        Spacer(Modifier.height(12.dp))

        if (fraud.couponAbuse.isEmpty()) {
            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("✓ No active severe fraud anomalies detected", modifier = Modifier.padding(12.dp), fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                fraud.couponAbuse.take(3).forEach { incident ->
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(incident.customerName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828))
                                Text(incident.evidence, fontSize = 10.sp, color = Color.DarkGray)
                            }
                            Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                                Text(incident.riskLevel.name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
