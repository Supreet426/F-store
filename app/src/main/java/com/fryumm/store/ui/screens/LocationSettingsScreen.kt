package com.fryumm.store.ui.screens

import androidx.compose.ui.platform.LocalContext
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.LocationPoint
import com.fryumm.store.data.model.ServiceRequest
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSettingsScreen(viewModel: StoreViewModel, onBack: () -> Unit) {
    var config by remember { mutableStateOf(viewModel.storeConfig) }
    
    LaunchedEffect(viewModel.storeConfig) {
        config = viewModel.storeConfig
    }

    // Form states for new exceptions/blocked areas
    var newPointName by remember { mutableStateOf("") }
    var newLat by remember { mutableStateOf("") }
    var newLng by remember { mutableStateOf("") }
    var newRadius by remember { mutableStateOf("500") }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                config = config.copy(notificationSoundUri = uri.toString())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Alerts & Delivery Settings", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    TextButton(onClick = { viewModel.updateStoreConfig(config) }) {
                        Text("SAVE", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FStorePurple, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("ORDER ALERTS (RINGING)", fontWeight = FontWeight.Bold, color = FStorePurple, fontSize = 12.sp)
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Enabled Loud Alert", modifier = Modifier.weight(1f))
                            Switch(checked = config.isLoudAlertEnabled, onCheckedChange = { config = config.copy(isLoudAlertEnabled = it) })
                        }
                        
                        HorizontalDivider(thickness = 0.5.dp)
                        
                        Text("Alert Sound", fontSize = 11.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val currentSoundName = if (config.notificationSoundUri != null) {
                                RingtoneManager.getRingtone(LocalContext.current, Uri.parse(config.notificationSoundUri))?.getTitle(LocalContext.current) ?: "Custom Tone"
                            } else "System Default Ringtone"
                            
                            Text(currentSoundName, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Order Alert Tone")
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                                    }
                                    ringtonePickerLauncher.launch(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                            ) {
                                Text("CHANGE", fontSize = 11.sp)
                            }
                        }
                        Text("Note: This will ring like a phone call for 10 seconds when a new order arrives.", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            item {
                Text("MAIN DELIVERY CONFIG", fontWeight = FontWeight.Bold, color = FStorePurple, fontSize = 12.sp)
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = config.storeLatitude.toString(),
                            onValueChange = { config = config.copy(storeLatitude = it.toDoubleOrNull() ?: config.storeLatitude) },
                            label = { Text("Store Latitude") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = config.storeLongitude.toString(),
                            onValueChange = { config = config.copy(storeLongitude = it.toDoubleOrNull() ?: config.storeLongitude) },
                            label = { Text("Store Longitude") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Service Radius (KM):", modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = config.deliveryRadiusKm.toString(),
                                onValueChange = { config = config.copy(deliveryRadiusKm = it.toDoubleOrNull() ?: config.deliveryRadiusKm) },
                                modifier = Modifier.width(100.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold)
                            )
                        }
                        Slider(
                            value = config.deliveryRadiusKm.toFloat(),
                            onValueChange = { config = config.copy(deliveryRadiusKm = String.format(Locale.US, "%.1f", it).toDouble()) },
                            valueRange = 1f..30f,
                            colors = SliderDefaults.colors(thumbColor = FStorePurple, activeTrackColor = FStorePurple)
                        )
                    }
                }
            }

            item {
                Text("ADD SPECIAL ZONE (ALLOWED / BLOCKED)", fontWeight = FontWeight.Bold, color = FStorePurple, fontSize = 12.sp)
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = newPointName, onValueChange = { newPointName = it }, label = { Text("Area Name (e.g. Village X)") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = newLat, onValueChange = { newLat = it }, label = { Text("Lat") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = newLng, onValueChange = { newLng = it }, label = { Text("Lng") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        OutlinedTextField(value = newRadius, onValueChange = { newRadius = it }, label = { Text("Coverage Radius (Meters)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val point = LocationPoint(newLat.toDoubleOrNull() ?: 0.0, newLng.toDoubleOrNull() ?: 0.0, newPointName, newRadius.toDoubleOrNull() ?: 500.0)
                                    config = config.copy(allowedExceptions = config.allowedExceptions + point)
                                    newPointName = ""; newLat = ""; newLng = ""
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.AddLocation, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ALLOW", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val point = LocationPoint(newLat.toDoubleOrNull() ?: 0.0, newLng.toDoubleOrNull() ?: 0.0, newPointName, newRadius.toDoubleOrNull() ?: 500.0)
                                    config = config.copy(blockedAreas = config.blockedAreas + point)
                                    newPointName = ""; newLat = ""; newLng = ""
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Icon(Icons.Default.LocationOff, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("BLOCK", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (config.allowedExceptions.isNotEmpty()) {
                item { Text("ALLOWED EXCEPTIONS (OUTSIDE RADIUS)", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 12.sp) }
                items(config.allowedExceptions) { point ->
                    ZoneItem(point, Color(0xFFE8F5E9)) { config = config.copy(allowedExceptions = config.allowedExceptions - point) }
                }
            }

            if (config.blockedAreas.isNotEmpty()) {
                item { Text("BLOCKED AREAS (INSIDE RADIUS)", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 12.sp) }
                items(config.blockedAreas) { point ->
                    ZoneItem(point, Color(0xFFFFEBEE)) { config = config.copy(blockedAreas = config.blockedAreas - point) }
                }
            }

            item {
                val requests = viewModel.serviceRequests
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SERVICE EXPANSION REQUESTS (${requests.size})", fontWeight = FontWeight.Bold, color = Color(0xFF673AB7), fontSize = 12.sp)
                    if (requests.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAllServiceRequests() }) {
                            Text("CLEAR ALL", color = Color.Red, fontSize = 10.sp)
                        }
                    }
                }
                if (requests.isEmpty()) {
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No out-of-area requests yet.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            items(viewModel.serviceRequests) { request ->
                ServiceRequestItem(request)
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun ServiceRequestItem(request: ServiceRequest) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFE1F5FE), shape = CircleShape) {
                    Icon(Icons.Default.PersonPinCircle, null, modifier = Modifier.padding(6.dp).size(16.dp), tint = Color(0xFF0288D1))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.userName.ifEmpty { request.userEmail }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(request.userPhone.ifEmpty { "No Mobile" }, fontSize = 11.sp, color = Color.Gray)
                }
                Text(sdf.format(Date(request.timestamp)), fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text(request.address, fontSize = 12.sp, color = Color.DarkGray)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Map, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text("${request.lat}, ${request.lng}", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ZoneItem(point: LocationPoint, bgColor: Color, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(point.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${point.lat}, ${point.lng} • Radius: ${point.radiusMeters}m", fontSize = 11.sp, color = Color.Gray)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) }
    }
}
