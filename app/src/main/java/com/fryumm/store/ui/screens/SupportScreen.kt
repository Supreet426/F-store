package com.fryumm.store.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.theme.FStorePurple
import com.fryumm.store.ui.viewmodels.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    viewModel: StoreViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val config = viewModel.dynamicAppConfig
    val supportNumber = config.supportNumber
    val supportEmail = config.supportEmail
    var showChat by remember { mutableStateOf(false) }

    if (showChat) {
        SupportChatScreen(
            viewModel = viewModel,
            onDismiss = { showChat = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flash-O Care", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FStorePurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FStorePurple),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                Icons.Default.SupportAgent,
                                null,
                                tint = Color.White,
                                modifier = Modifier.padding(12.dp).size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "How can we help you?",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            "Flash-O Care 24/7 Support",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(6.dp).background(Color.White, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "ONLINE",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions: Call Back & Live Chat
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SupportQuickAction(
                        title = "Request Callback",
                        icon = Icons.Default.PhoneCallback,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.submitCallRequest() }
                    )
                    SupportQuickAction(
                        title = "Live Chat",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f),
                        onClick = { showChat = true }
                    )
                }
            }

            item {
                Text(
                    "Contact Channels",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                SupportOptionItem(
                    title = "Call Support",
                    subtitle = "Directly speak to our executive",
                    icon = Icons.Default.Call,
                    iconColor = Color(0xFF2196F3),
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$supportNumber")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                SupportOptionItem(
                    title = "WhatsApp Chat",
                    subtitle = "Chat with us instantly",
                    icon = Icons.Default.QuestionAnswer,
                    iconColor = Color(0xFF4CAF50),
                    onClick = {
                        val url = "https://api.whatsapp.com/send?phone=91$supportNumber"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(url)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                SupportOptionItem(
                    title = "Email Support",
                    subtitle = "Send us your query via email",
                    icon = Icons.Default.Email,
                    iconColor = Color(0xFFEA4335),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$supportEmail")
                            putExtra(Intent.EXTRA_SUBJECT, "F-Store Support Request")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                Text(
                    "Frequently Asked Questions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 4.dp)
                )
            }

            item {
                FAQItem(
                    question = "How long does payout take?",
                    answer = "Merchant payouts are processed every Monday and Thursday automatically to your registered UPI ID."
                )
            }

            item {
                FAQItem(
                    question = "How to add a new product?",
                    answer = "Go to the Inventory tab, click on the '+' icon, enter product details, and upload a clear image."
                )
            }

            // Support Request History
            val requests = viewModel.serviceRequests.filter { it.userId == viewModel.currentUserProfile?.uid }
            if (requests.isNotEmpty()) {
                item {
                    Text(
                        "Your Recent Support Requests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, end = 4.dp)
                    )
                }
                
                items(requests.sortedByDescending { it.timestamp }) { request ->
                    SupportTicketItem(request)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SupportTicketItem(request: com.fryumm.store.data.model.ServiceRequest) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ID: ${request.id}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (request.type == "CALLBACK_REQUEST") "Callback Requested" else "Support Request",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                val time = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(request.timestamp))
                Text(text = time, fontSize = 11.sp, color = Color.Gray)
            }
            
            val (statusText, statusColor) = when (request.status) {
                "PENDING" -> "In Progress" to Color(0xFFFF9800)
                "RESOLVED" -> "Resolved" to Color(0xFF4CAF50)
                "CALLED" -> "Call Completed" to Color(0xFF2196F3)
                else -> request.status to Color.Gray
            }
            
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SupportQuickAction(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(110.dp),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun SupportOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    icon,
                    null,
                    tint = iconColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
private fun FAQItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    question,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = Color.Gray
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    answer,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
