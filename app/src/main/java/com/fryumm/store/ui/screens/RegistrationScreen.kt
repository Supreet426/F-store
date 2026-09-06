package com.fryumm.store.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.theme.FStoreLightPurple
import com.fryumm.store.ui.theme.FStorePurple
import com.fryumm.store.ui.viewmodels.AuthViewModel
import com.fryumm.store.ui.viewmodels.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    authViewModel: AuthViewModel,
    storeViewModel: StoreViewModel,
    onBack: () -> Unit,
    onGoogleLogin: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("") }
    var showChat by remember { mutableStateOf(false) }
    var showCallbackDialog by remember { mutableStateOf(false) }

    if (showChat) {
        SupportChatScreen(
            viewModel = storeViewModel,
            onDismiss = { showChat = false }
        )
    }

    if (showCallbackDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCallbackDialog = false },
            title = { Text("Request Callback", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Our expert will call you to help with registration.", fontSize = 14.sp)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            storeViewModel.submitCallRequest(name, phone)
                            showCallbackDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCallbackDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Flash-O Partner Network", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Choose Your Business Category",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = FStorePurple,
                textAlign = TextAlign.Center
            )
            Text(
                "Select how you want to grow with India's fastest hyperlocal delivery network.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            RegistrationCategoryCard(
                title = "Sell on Flash-O (0% MDR)",
                description = "List your store products and get orders from nearby customers with zero commission fees.",
                icon = Icons.Default.Storefront,
                isSelected = selectedCategory == "SELL_ON_FLASHO",
                onClick = { selectedCategory = "SELL_ON_FLASHO" }
            )

            Spacer(Modifier.height(16.dp))

            RegistrationCategoryCard(
                title = "Supply to Dark Stores",
                description = "Become a bulk supplier for Flash-O Dark Stores and Fulfillment Hubs in your city.",
                icon = Icons.Default.Inventory,
                isSelected = selectedCategory == "SUPPLY_TO_DARK_STORES",
                onClick = { selectedCategory = "SUPPLY_TO_DARK_STORES" }
            )

            Spacer(Modifier.height(16.dp))

            RegistrationCategoryCard(
                title = "Protect & Build Your Brand",
                description = "Exclusive brand protection, advertisement tools, and premium logistics support.",
                icon = Icons.Default.Verified,
                isSelected = selectedCategory == "BRAND_PARTNER",
                onClick = { selectedCategory = "BRAND_PARTNER" }
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (selectedCategory.isNotEmpty()) {
                        authViewModel.pendingBusinessType = selectedCategory
                        onGoogleLogin()
                    }
                },
                enabled = selectedCategory.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
            ) {
                Text("Get Started with Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(40.dp))

            // Support Section
            Text(
                "Need assistance with registration?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RegistrationSupportButton(
                    label = "Live Query",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    color = Color(0xFF2196F3),
                    onClick = { showChat = true }
                )
                RegistrationSupportButton(
                    label = "WhatsApp",
                    icon = Icons.Default.QuestionAnswer,
                    color = Color(0xFF4CAF50),
                    onClick = {
                        val url = "https://api.whatsapp.com/send?phone=919235900175&text=Hi, I want to register as a Flash-O Partner."
                        val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
                        context.startActivity(intent)
                    }
                )
                RegistrationSupportButton(
                    label = "Request Call",
                    icon = Icons.Default.PhoneCallback,
                    color = Color(0xFFFF9800),
                    onClick = { showCallbackDialog = true }
                )
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun RegistrationCategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, if (isSelected) FStorePurple else Color.Transparent),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) FStorePurple else Color(0xFFF0F2F5)
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (isSelected) Color.White else FStorePurple,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isSelected) FStorePurple else Color.Black)
                Text(description, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun RegistrationSupportButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
    }
}
