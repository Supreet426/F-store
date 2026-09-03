package com.fryumm.store.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.ui.components.AvailabilitySchedulingDialog
import com.fryumm.store.ui.theme.FStorePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: StoreViewModel, 
    onBack: () -> Unit, 
    onNavigateToUpdate: () -> Unit,
    onNavigateToLocation: () -> Unit
) {
    val currentProfile = viewModel.currentUserProfile ?: return
    var editableProfile by remember { mutableStateOf(currentProfile) }
    var isEditing by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var showAvailabilityDialog by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfilePhoto(it) }
    }

    LaunchedEffect(viewModel.currentUserProfile) {
        if (!isEditing) {
            viewModel.currentUserProfile?.let { editableProfile = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    if (isEditing) {
                        Button(
                            onClick = { 
                                viewModel.updateUserProfileDetails(editableProfile)
                                isEditing = false 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.padding(end = 8.dp).height(36.dp)
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, "Edit Profile", tint = FStorePurple)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF1F3F4))
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // COMPACT HEADER SECTION
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = FStorePurple.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, FStorePurple.copy(alpha = 0.5f))
                            ) {
                                if (currentProfile.photoUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(currentProfile.photoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Storefront, null, modifier = Modifier.size(32.dp), tint = FStorePurple)
                                    }
                                }
                            }
                            
                            Surface(
                                modifier = Modifier.size(24.dp).clickable { launcher.launch("image/*") },
                                shape = CircleShape,
                                color = FStorePurple,
                                border = BorderStroke(2.dp, Color.White)
                            ) {
                                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.padding(4.dp), tint = Color.White)
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentProfile.tradeName.ifEmpty { currentProfile.name }, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(currentProfile.email, fontSize = 13.sp, color = Color.Gray)
                            
                            Surface(
                                modifier = Modifier.padding(top = 6.dp),
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = currentProfile.role.replace("_", " "),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = Color(0xFF2E7D32),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // OPERATIONAL DETAILS (F-Store Style)
            item {
                ProfileSection(title = "Operational Details", icon = Icons.Default.PrecisionManufacturing) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileTextField("Store / Trade Name", editableProfile.tradeName, isEditing, placeholder = "e.g. Flash-O Express") { 
                            editableProfile = editableProfile.copy(tradeName = it, name = it) 
                        }
                        ProfileTextField("Contact Person Name", editableProfile.contactPerson, isEditing, placeholder = "Full Name") { 
                            editableProfile = editableProfile.copy(contactPerson = it) 
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileTextField("Primary Phone", editableProfile.phoneNumber, isEditing, keyboardType = KeyboardType.Phone) { 
                                    editableProfile = editableProfile.copy(phoneNumber = it) 
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileTextField("Prep Time (Mins)", editableProfile.averagePreparationTime.toString(), isEditing, keyboardType = KeyboardType.Number) { 
                                    editableProfile = editableProfile.copy(averagePreparationTime = it.toIntOrNull() ?: 15) 
                                }
                            }
                        }
                    }
                }
            }

            // BUSINESS & KYC
            item {
                ProfileSection(title = "Business Identity (KYC)", icon = Icons.Default.VerifiedUser) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileTextField("Legal Entity Name", editableProfile.legalName, isEditing, placeholder = "Name on PAN/GST") { 
                            editableProfile = editableProfile.copy(legalName = it) 
                        }
                        ProfileTextField("Business Type", editableProfile.businessType, isEditing, placeholder = "e.g. Private Limited, Individual") { 
                            editableProfile = editableProfile.copy(businessType = it) 
                        }
                        ProfileTextField("GSTIN", editableProfile.gstin, isEditing, placeholder = "22AAAAA0000A1Z5") { 
                            editableProfile = editableProfile.copy(gstin = it) 
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1.5f)) {
                                ProfileTextField("FSSAI License", editableProfile.fssaiNumber, isEditing) { 
                                    editableProfile = editableProfile.copy(fssaiNumber = it) 
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileTextField("Expiry Date", editableProfile.fssaiExpiryDate, isEditing, placeholder = "DD/MM/YYYY") { 
                                    editableProfile = editableProfile.copy(fssaiExpiryDate = it) 
                                }
                            }
                        }
                        ProfileTextField("PAN Number", editableProfile.panNumber, isEditing, placeholder = "ABCDE1234F") { 
                            editableProfile = editableProfile.copy(panNumber = it) 
                        }
                    }
                }
            }

            // BANKING & PAYOUTS
            item {
                ProfileSection(title = "Banking & Payouts", icon = Icons.Default.Payments) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileTextField("Bank Name", editableProfile.bankName, isEditing, placeholder = "e.g. HDFC Bank") { 
                            editableProfile = editableProfile.copy(bankName = it) 
                        }
                        ProfileTextField("Account Holder Name", editableProfile.bankAccountName, isEditing) { 
                            editableProfile = editableProfile.copy(bankAccountName = it) 
                        }
                        ProfileTextField("Account Number", editableProfile.bankAccountNumber, isEditing, keyboardType = KeyboardType.Number) { 
                            editableProfile = editableProfile.copy(bankAccountNumber = it) 
                        }
                        ProfileTextField("IFSC Code", editableProfile.bankIfscCode, isEditing, placeholder = "HDFC0001234") { 
                            editableProfile = editableProfile.copy(bankIfscCode = it) 
                        }
                    }
                }
            }

            // ADDRESS
            item {
                ProfileSection(title = "Store Address", icon = Icons.Default.LocationOn) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileTextField("Full Address", editableProfile.storeAddress, isEditing, singleLine = false) { 
                            editableProfile = editableProfile.copy(storeAddress = it) 
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileTextField("City", editableProfile.city, isEditing) { 
                                    editableProfile = editableProfile.copy(city = it) 
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileTextField("Pincode", editableProfile.pincode, isEditing, keyboardType = KeyboardType.Number) { 
                                    editableProfile = editableProfile.copy(pincode = it) 
                                }
                            }
                        }
                        ProfileTextField("State", editableProfile.state, isEditing) { 
                            editableProfile = editableProfile.copy(state = it) 
                        }
                    }
                }
            }

            // SETTINGS
            item {
                ProfileSection(title = "Quick Settings", icon = Icons.Default.Settings) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Default.Schedule,
                            title = "Operating Hours",
                            onClick = { showAvailabilityDialog = true }
                        )

                        if (currentProfile.userRole == UserRole.ADMIN) {
                            ProfileMenuItem(
                                icon = Icons.Default.Map,
                                title = "Service Radius & Map",
                                onClick = onNavigateToLocation
                            )
                            ProfileMenuItem(
                                icon = Icons.Default.SystemUpdate,
                                title = "App Release Management",
                                onClick = onNavigateToUpdate
                            )
                        }
                        
                        ProfileMenuItem(
                            icon = Icons.Default.LockReset,
                            title = "Change Password",
                            onClick = { viewModel.sendPasswordResetEmail(currentProfile.email) }
                        )
                        
                        ProfileMenuItem(
                            icon = Icons.Default.Logout,
                            title = "Logout from Session",
                            color = Color.Red,
                            onClick = { viewModel.logout() }
                        )
                    }
                }
            }
            
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAvailabilityDialog) {
        AvailabilitySchedulingDialog(
            title = "Store Availability",
            initialDailySchedules = currentProfile.dailySchedules,
            initialVacationStart = currentProfile.vacationStart,
            initialVacationEnd = currentProfile.vacationEnd,
            onDismiss = { showAvailabilityDialog = false },
            onSave = { schedules, start, end ->
                viewModel.updateMerchantSettings(schedules, start, end)
                showAvailabilityDialog = false
            }
        )
    }
}

@Composable
fun ProfileSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = FStorePurple, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ProfileTextField(
    label: String, 
    value: String, 
    isEditing: Boolean, 
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        if (isEditing) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.LightGray) },
                singleLine = singleLine,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8F9FA),
                    unfocusedContainerColor = Color(0xFFF8F9FA),
                    focusedIndicatorColor = FStorePurple,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )
        } else {
            Text(
                text = value.ifEmpty { "Not Added" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (value.isEmpty()) Color.LightGray else Color.Black,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color = Color.Black,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if(color == Color.Red) Color(0xFFFFEBEE) else Color(0xFFF5F5F5),
            shape = CircleShape,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
    }
}
