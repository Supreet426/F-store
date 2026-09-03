package com.fryumm.store.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.AppUpdateInfo
import com.fryumm.store.ui.theme.FStorePurple
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(viewModel: StoreViewModel, onBack: () -> Unit) {
    val updates = viewModel.appUpdates
    val scope = rememberCoroutineScope()
    var selectedAppId by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    var showUpdateDetailsDialog by remember { mutableStateOf<AppUpdateInfo?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedAppId?.let { appId ->
                scope.launch {
                    isUploading = true
                    val downloadUrl = viewModel.uploadApk(uri, appId)
                    if (downloadUrl != null) {
                        val currentInfo = updates.find { it.appId == appId } ?: AppUpdateInfo(appId = appId, appName = appId.replaceFirstChar { it.uppercase() })
                        // Open dialog to fill details
                        showUpdateDetailsDialog = currentInfo.copy(downloadUrl = downloadUrl)
                    }
                    isUploading = false
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Update Management", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Upload new versions for the Flash-O Ecosystem apps. Once uploaded, apps will automatically prompt users to update.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                val appConfigs = listOf(
                    "customer" to "Flash-O (Customer)",
                    "partner" to "Flash-O Partner (Rider)",
                    "store" to "F-Store (Admin/Merchant)"
                )

                items(appConfigs) { (appId, name) ->
                    val currentUpdate = updates.find { it.appId == appId }
                    AppVersionCard(
                        appName = name,
                        currentInfo = currentUpdate,
                        onUpload = {
                            selectedAppId = appId
                            launcher.launch("application/vnd.android.package-archive")
                        }
                    )
                }
            }

            if (isUploading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text("Uploading APK... Please wait", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showUpdateDetailsDialog != null) {
        UpdateDetailsDialog(
            info = showUpdateDetailsDialog!!,
            onDismiss = { showUpdateDetailsDialog = null },
            onConfirm = { updatedInfo ->
                viewModel.updateAppRelease(updatedInfo)
                showUpdateDetailsDialog = null
            }
        )
    }
}

@Composable
fun AppVersionCard(
    appName: String,
    currentInfo: AppUpdateInfo?,
    onUpload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(appName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (currentInfo != null) {
                    Surface(
                        color = FStorePurple.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "v${currentInfo.versionName} (${currentInfo.versionCode})",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = FStorePurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text("No Version Data", color = Color.Red, fontSize = 11.sp)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            if (currentInfo != null) {
                Text("Release Notes:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(currentInfo.releaseNotes, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                if (currentInfo.isMandatory) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PriorityHigh, null, tint = Color.Red, modifier = Modifier.size(12.dp))
                        Text(" Mandatory Update", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onUpload,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("UPLOAD NEW APK")
            }
        }
    }
}

@Composable
fun UpdateDetailsDialog(
    info: AppUpdateInfo,
    onDismiss: () -> Unit,
    onConfirm: (AppUpdateInfo) -> Unit
) {
    var versionName by remember { mutableStateOf(info.versionName) }
    var versionCode by remember { mutableStateOf(info.versionCode.toString()) }
    var releaseNotes by remember { mutableStateOf(info.releaseNotes) }
    var isMandatory by remember { mutableStateOf(info.isMandatory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Details: ${info.appName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("Version Name (e.g. 1.2.0)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = versionCode,
                    onValueChange = { if(it.all { c -> c.isDigit() }) versionCode = it },
                    label = { Text("Version Code (e.g. 15)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("Release Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isMandatory, onCheckedChange = { isMandatory = it })
                    Text("Force Update (Mandatory)", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(info.copy(
                        versionName = versionName,
                        versionCode = versionCode.toIntOrNull() ?: info.versionCode,
                        releaseNotes = releaseNotes,
                        isMandatory = isMandatory,
                        timestamp = System.currentTimeMillis()
                    ))
                }
            ) {
                Text("RELEASE UPDATE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
