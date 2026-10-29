package com.fryumm.store.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fryumm.store.data.model.AppUpdateInfo
import com.fryumm.store.ui.theme.FStorePurple
import com.fryumm.store.ui.viewmodels.StoreViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(viewModel: StoreViewModel, onBack: () -> Unit) {
    val updates = viewModel.appUpdates
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedAppId by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    var editingAppInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var previewingAppInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedAppId?.let { appId ->
                scope.launch {
                    isUploading = true
                    val uploadedUrl = viewModel.uploadApk(uri, appId)
                    if (uploadedUrl != null) {
                        val currentInfo = updates.find { it.appId == appId } ?: getInitialAppInfo(appId)
                        val updatedInfo = currentInfo.copy(
                            directApkUrl = uploadedUrl,
                            downloadUrl = uploadedUrl,
                            timestamp = System.currentTimeMillis()
                        )
                        viewModel.updateAppRelease(updatedInfo)
                        Toast.makeText(context, "APK Uploaded & Direct Download Link generated!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "APK Upload failed", Toast.LENGTH_SHORT).show()
                    }
                    isUploading = false
                }
            }
        }
    }

    val defaultAppIds = listOf("customer", "partner", "store", "web", "merchant_web")

    // Merge saved updates with default list
    val allApps = remember(updates) {
        val mappedList = mutableListOf<AppUpdateInfo>()
        defaultAppIds.forEach { id ->
            val existing = updates.find { it.appId == id }
            mappedList.add(existing ?: getInitialAppInfo(id))
        }
        updates.forEach { customApp ->
            if (defaultAppIds.none { it == customApp.appId }) {
                mappedList.add(customApp)
            }
        }
        mappedList
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Update Applications & Portals", fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Manage Android APK builds & Web Portal URLs", fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingAppInfo = AppUpdateInfo(
                            appId = "app_${System.currentTimeMillis()}",
                            appName = "New Flash-O Service",
                            category = "Application",
                            versionName = "1.0.0",
                            versionCode = 1,
                            isWebApp = false
                        )
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add New App/Portal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8F9FA))) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FStorePurple.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FStorePurple.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(36.dp), tint = FStorePurple)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Application & Web Release Management", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FStorePurple)
                                Text(
                                    "For Android Apps: Upload APKs, open installed apps, or set Play Store links.\nFor Web Portals: Configure domain & open live website.",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }

                items(allApps, key = { it.appId }) { appInfo ->
                    AppReleaseAdminCard(
                        info = appInfo,
                        onEdit = { editingAppInfo = appInfo },
                        onUploadApk = {
                            selectedAppId = appInfo.appId
                            launcher.launch("application/vnd.android.package-archive")
                        },
                        onPreview = { previewingAppInfo = appInfo },
                        onLaunchOrDownload = {
                            handleAppAction(context, appInfo, onConfigure = { editingAppInfo = appInfo })
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (isUploading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text("Uploading APK to Cloud Storage...", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Please wait while direct download link is generated", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Edit & Release Details Dialog
    if (editingAppInfo != null) {
        UpdateDetailsDialog(
            info = editingAppInfo!!,
            onDismiss = { editingAppInfo = null },
            onConfirm = { updatedInfo ->
                viewModel.updateAppRelease(updatedInfo)
                editingAppInfo = null
            },
            onUploadApkClick = {
                selectedAppId = editingAppInfo!!.appId
                editingAppInfo = null
                launcher.launch("application/vnd.android.package-archive")
            }
        )
    }

    // Application Detail Sheet Modal
    if (previewingAppInfo != null) {
        AppReleaseDetailsSheet(
            info = previewingAppInfo!!,
            onDismiss = { previewingAppInfo = null },
            onEditClick = {
                val appToEdit = previewingAppInfo
                previewingAppInfo = null
                editingAppInfo = appToEdit
            }
        )
    }
}

@Composable
fun AppReleaseAdminCard(
    info: AppUpdateInfo,
    onEdit: () -> Unit,
    onUploadApk: () -> Unit,
    onPreview: () -> Unit,
    onLaunchOrDownload: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isWebPortal = remember(info) { info.isWebPortal() }
    val isAppInstalled = remember(info.appId, isWebPortal) {
        if (isWebPortal) false else isAppPackageInstalled(context, info.appId)
    }
    val hasLink = if (isWebPortal) info.websiteUrl.isNotBlank() else info.getEffectiveDownloadUrl().isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with Icon, Name, Version/Badge and Preview action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isWebPortal) Color(0xFF009688).copy(alpha = 0.1f) else FStorePurple.copy(alpha = 0.1f)
                ) {
                    if (info.iconUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(info.iconUrl).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                getAppDefaultIcon(info.appId),
                                null,
                                tint = if (isWebPortal) Color(0xFF009688) else FStorePurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            info.appName.ifEmpty { info.appId },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = if (isWebPortal) Color(0xFF009688).copy(alpha = 0.15f) else FStorePurple.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                if (isWebPortal) "WEB PORTAL" else "v${info.versionName}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = if (isWebPortal) Color(0xFF00796B) else FStorePurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        info.appDescription.ifEmpty { if (isWebPortal) "Flash-O Web Portal" else info.category },
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPreview) {
                    Icon(Icons.Default.Visibility, contentDescription = "View Details", tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Active Channels / Domain Status Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isWebPortal) {
                    ChannelChip(
                        name = if (info.websiteUrl.isNotBlank()) "Web Domain Configured" else "No Domain Set",
                        active = info.websiteUrl.isNotBlank(),
                        icon = Icons.Default.Language
                    )
                    ChannelChip(
                        name = "Browser Accessible",
                        active = info.websiteUrl.isNotBlank(),
                        icon = Icons.Default.Public
                    )
                } else {
                    ChannelChip(name = "Play Store", active = info.playStoreUrl.isNotBlank(), icon = Icons.Default.Shop)
                    ChannelChip(name = "Direct APK", active = info.directApkUrl.isNotBlank() || info.downloadUrl.isNotBlank(), icon = Icons.Default.FileDownload)
                    ChannelChip(name = "GitHub", active = info.githubUrl.isNotBlank(), icon = Icons.Default.Code)
                    ChannelChip(name = "Website", active = info.websiteUrl.isNotBlank(), icon = Icons.Default.Language)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Release / Update Notes Snippet
            if (info.releaseNotes.isNotBlank()) {
                Surface(
                    color = Color(0xFFF1F3F5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            if (isWebPortal) "✨ Portal Updates:" else "✨ What's New in v${info.versionName}:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            info.releaseNotes,
                            fontSize = 12.sp,
                            color = Color.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Clean Actions Layout (WEB PORTAL vs ANDROID APK)
            if (isWebPortal) {
                // WEB PORTAL ACTIONS: No APK Upload or APK Download options!
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary Action: Visit Web Portal
                    Button(
                        onClick = {
                            if (info.websiteUrl.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.websiteUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open website link", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                onEdit()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (info.websiteUrl.isNotBlank()) "VISIT WEB PORTAL" else "CONFIGURE WEB URL",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Secondary Actions: Edit Web Link & Copy Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Edit Web Link",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (info.websiteUrl.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(info.websiteUrl))
                                    Toast.makeText(context, "Web URL copied!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No web URL configured yet", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Copy Web Link",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // ANDROID APK ACTIONS: Open App, Download APK, Upload APK, Edit Details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ROW 1: Primary Action Button (Full Width)
                    Button(
                        onClick = if (!isAppInstalled && !hasLink) onEdit else onLaunchOrDownload,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isAppInstalled -> Color(0xFF2E7D32)
                                hasLink -> FStorePurple
                                else -> FStorePurple.copy(alpha = 0.85f)
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            if (isAppInstalled) Icons.Default.PlayArrow else Icons.Default.GetApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                isAppInstalled -> "OPEN APP"
                                hasLink -> "DOWNLOAD APK"
                                else -> "DOWNLOAD APK (CONFIGURE LINK)"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // ROW 2: Secondary Action Buttons (Upload APK & Edit Details side by side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onUploadApk,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Upload APK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Edit Details",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelChip(name: String, active: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = if (active) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (active) Color(0xFF4CAF50) else Color.LightGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (active) Color(0xFF2E7D32) else Color.Gray,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) Color(0xFF2E7D32) else Color.Gray,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDetailsDialog(
    info: AppUpdateInfo,
    onDismiss: () -> Unit,
    onConfirm: (AppUpdateInfo) -> Unit,
    onUploadApkClick: () -> Unit
) {
    var isWebApp by remember { mutableStateOf(info.isWebPortal()) }
    var appName by remember { mutableStateOf(info.appName) }
    var appDescription by remember { mutableStateOf(info.appDescription) }
    var category by remember { mutableStateOf(info.category) }
    var iconUrl by remember { mutableStateOf(info.iconUrl) }
    var versionName by remember { mutableStateOf(info.versionName) }
    var versionCode by remember { mutableStateOf(info.versionCode.toString()) }
    var releaseNotes by remember { mutableStateOf(info.releaseNotes) }
    var isMandatory by remember { mutableStateOf(info.isMandatory) }

    var playStoreUrl by remember { mutableStateOf(info.playStoreUrl) }
    var directApkUrl by remember { mutableStateOf(info.directApkUrl.ifEmpty { info.downloadUrl }) }
    var githubUrl by remember { mutableStateOf(info.githubUrl) }
    var websiteUrl by remember { mutableStateOf(info.websiteUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isWebApp) Icons.Default.Language else Icons.Default.CloudSync,
                    null,
                    tint = if (isWebApp) Color(0xFF00796B) else FStorePurple
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isWebApp) "Manage Web Portal Details" else "Manage Android App Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Application Type Selector
                Text("APPLICATION TYPE", fontWeight = FontWeight.Black, fontSize = 11.sp, color = FStorePurple)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isWebApp,
                        onClick = { isWebApp = false },
                        label = { Text("📱 Android App (APK)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isWebApp,
                        onClick = { isWebApp = true },
                        label = { Text("🌐 Web Portal / Website") },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("BASIC DETAILS", fontWeight = FontWeight.Black, fontSize = 11.sp, color = FStorePurple)

                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text(if (isWebApp) "Web Portal Name" else "App Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = appDescription,
                    onValueChange = { appDescription = it },
                    label = { Text("Description / Tagline") },
                    placeholder = { Text(if (isWebApp) "e.g. Web portal for customer orders" else "e.g. Food & grocery delivery in 10 minutes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = iconUrl,
                    onValueChange = { iconUrl = it },
                    label = { Text("Icon / Logo URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Quick Logo Presets
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Presets:", fontSize = 11.sp, color = Color.Gray)
                    AssistChip(
                        onClick = { iconUrl = "https://cdn-icons-png.flaticon.com/512/3081/3081559.png" },
                        label = { Text("Customer", fontSize = 10.sp, maxLines = 1) }
                    )
                    AssistChip(
                        onClick = { iconUrl = "https://cdn-icons-png.flaticon.com/512/2830/2830312.png" },
                        label = { Text("Rider", fontSize = 10.sp, maxLines = 1) }
                    )
                    AssistChip(
                        onClick = { iconUrl = "https://cdn-icons-png.flaticon.com/512/869/869636.png" },
                        label = { Text("Store", fontSize = 10.sp, maxLines = 1) }
                    )
                    AssistChip(
                        onClick = { iconUrl = "https://cdn-icons-png.flaticon.com/512/1006/1006771.png" },
                        label = { Text("Web Portal", fontSize = 10.sp, maxLines = 1) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text(if (isWebApp) "Version" else "Version Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    if (!isWebApp) {
                        OutlinedTextField(
                            value = versionCode,
                            onValueChange = { if (it.all { c -> c.isDigit() }) versionCode = it },
                            label = { Text("Version Code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("UPDATE NOTES", fontWeight = FontWeight.Black, fontSize = 11.sp, color = FStorePurple)
                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("What's New in this release") },
                    placeholder = { Text("• Speed improvements\n• UI redesign\n• Bug fixes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                if (!isWebApp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isMandatory, onCheckedChange = { isMandatory = it })
                        Text("Force Update (Mandatory for users)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (isWebApp) {
                    // WEB PORTAL URL INPUT
                    Text("WEB PORTAL URL & DOMAIN", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFF00796B))
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("Official Website / Web Portal URL") },
                        placeholder = { Text("https://flash-o.com or https://flash-o.com/merchant") },
                        leadingIcon = { Icon(Icons.Default.Language, null, tint = Color(0xFF00796B)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    // ANDROID DOWNLOAD & ACCESS CHANNELS
                    Text("ANDROID DOWNLOAD CHANNELS", fontWeight = FontWeight.Black, fontSize = 11.sp, color = FStorePurple)

                    // 1. Play Store Link
                    OutlinedTextField(
                        value = playStoreUrl,
                        onValueChange = { playStoreUrl = it },
                        label = { Text("1. Google Play Store Link") },
                        placeholder = { Text("https://play.google.com/store/apps/details?id=...") },
                        leadingIcon = { Icon(Icons.Default.Shop, null, tint = Color(0xFF4CAF50)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 2. Direct APK / Google Drive Link
                    Column {
                        OutlinedTextField(
                            value = directApkUrl,
                            onValueChange = { directApkUrl = it },
                            label = { Text("2. Direct APK / Google Drive Link") },
                            placeholder = { Text("Paste Google Drive or direct download URL") },
                            leadingIcon = { Icon(Icons.Default.FileDownload, null, tint = Color(0xFF2196F3)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        TextButton(onClick = onUploadApkClick) {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Or Upload APK File directly to Cloud Storage", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    // 3. GitHub Link
                    OutlinedTextField(
                        value = githubUrl,
                        onValueChange = { githubUrl = it },
                        label = { Text("3. GitHub Releases Link") },
                        placeholder = { Text("https://github.com/org/repo/releases") },
                        leadingIcon = { Icon(Icons.Default.Code, null, tint = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 4. Website Link
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("4. Official Website Link") },
                        placeholder = { Text("https://flash-o.com") },
                        leadingIcon = { Icon(Icons.Default.Language, null, tint = Color(0xFF9C27B0)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        info.copy(
                            appName = appName,
                            appDescription = appDescription,
                            category = if (isWebApp) "Web Portal" else category.ifEmpty { "Application" },
                            isWebApp = isWebApp,
                            iconUrl = iconUrl,
                            versionName = versionName,
                            versionCode = versionCode.toIntOrNull() ?: info.versionCode,
                            releaseNotes = releaseNotes,
                            isMandatory = if (isWebApp) false else isMandatory,
                            playStoreUrl = if (isWebApp) "" else playStoreUrl.trim(),
                            directApkUrl = if (isWebApp) "" else directApkUrl.trim(),
                            downloadUrl = if (isWebApp) websiteUrl.trim() else directApkUrl.trim().ifEmpty { playStoreUrl.trim() },
                            githubUrl = if (isWebApp) "" else githubUrl.trim(),
                            websiteUrl = websiteUrl.trim(),
                            timestamp = System.currentTimeMillis()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isWebApp) Color(0xFF00796B) else FStorePurple)
            ) {
                Text(if (isWebApp) "SAVE WEB CONFIG" else "PUBLISH UPDATE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppReleaseDetailsSheet(
    info: AppUpdateInfo,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isWebPortal = remember(info) { info.isWebPortal() }
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val updateDate = remember(info.timestamp) { sdf.format(Date(info.timestamp)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isWebPortal) Color(0xFF009688).copy(alpha = 0.1f) else FStorePurple.copy(alpha = 0.1f)
                ) {
                    if (info.iconUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(info.iconUrl).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                getAppDefaultIcon(info.appId),
                                null,
                                tint = if (isWebPortal) Color(0xFF009688) else FStorePurple,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        info.appName.ifEmpty { info.appId.replaceFirstChar { it.uppercase() } },
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(if (isWebPortal) "Web Portal" else info.category, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (isWebPortal) Color(0xFF009688).copy(alpha = 0.15f) else FStorePurple.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                if (isWebPortal) "WEB PORTAL" else "v${info.versionName} (${info.versionCode})",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = if (isWebPortal) Color(0xFF00796B) else FStorePurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Updated $updateDate", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                    }
                }
            }

            // Description
            if (info.appDescription.isNotBlank()) {
                Surface(
                    color = Color(0xFFF8F9FA),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(if (isWebPortal) "About Web Portal" else "About App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(info.appDescription, fontSize = 13.sp, color = Color.DarkGray)
                    }
                }
            }

            // Updates / Notes
            Surface(
                color = if (isWebPortal) Color(0xFF009688).copy(alpha = 0.06f) else FStorePurple.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isWebPortal) Color(0xFF009688).copy(alpha = 0.15f) else FStorePurple.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            tint = if (isWebPortal) Color(0xFF00796B) else FStorePurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isWebPortal) "Latest Portal Notes" else "What's New in this Update",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isWebPortal) Color(0xFF00796B) else FStorePurple
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        info.releaseNotes.ifEmpty { "General performance enhancements and minor fixes." },
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }
            }

            if (isWebPortal) {
                // WEB PORTAL DETAILS & ACCESS OPTIONS
                Text("Web Portal Access", fontWeight = FontWeight.Black, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DownloadOptionCard(
                        title = "Visit Web Portal",
                        subtitle = if (info.websiteUrl.isNotBlank()) info.websiteUrl else "Configure Web URL",
                        icon = Icons.Default.Language,
                        color = Color(0xFF00796B),
                        enabled = info.websiteUrl.isNotBlank(),
                        onClick = { openUrlOrFallback(context, info.websiteUrl, info, onEditClick) }
                    )
                }
            } else {
                // ANDROID APK OPTIONS
                Text("Download & Launch Options", fontWeight = FontWeight.Black, fontSize = 14.sp)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DownloadOptionCard(
                        title = "Google Play Store",
                        subtitle = if (info.playStoreUrl.isNotBlank()) "Official app store download" else "Configure Play Store link",
                        icon = Icons.Default.Shop,
                        color = Color(0xFF4CAF50),
                        enabled = info.playStoreUrl.isNotBlank(),
                        onClick = { openUrlOrFallback(context, info.playStoreUrl, info, onEditClick) }
                    )

                    val directUrl = info.directApkUrl.ifEmpty { info.downloadUrl }
                    DownloadOptionCard(
                        title = "Direct APK / Cloud Storage",
                        subtitle = if (directUrl.isNotBlank()) "Fast direct download link" else "Upload or set direct APK link",
                        icon = Icons.Default.FileDownload,
                        color = Color(0xFF2196F3),
                        enabled = directUrl.isNotBlank(),
                        onClick = { openUrlOrFallback(context, directUrl, info, onEditClick) }
                    )

                    DownloadOptionCard(
                        title = "GitHub Release & Source",
                        subtitle = if (info.githubUrl.isNotBlank()) "Release notes & APK artifacts" else "Configure GitHub releases link",
                        icon = Icons.Default.Code,
                        color = Color.DarkGray,
                        enabled = info.githubUrl.isNotBlank(),
                        onClick = { openUrlOrFallback(context, info.githubUrl, info, onEditClick) }
                    )

                    DownloadOptionCard(
                        title = "Visit Website",
                        subtitle = if (info.websiteUrl.isNotBlank()) "Launch website in browser" else "Configure website URL",
                        icon = Icons.Default.Language,
                        color = Color(0xFF9C27B0),
                        enabled = info.websiteUrl.isNotBlank(),
                        onClick = { openUrlOrFallback(context, info.websiteUrl, info, onEditClick) }
                    )
                }
            }

            // Share & Copy Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val linkToShare = if (isWebPortal) info.websiteUrl else info.getEffectiveDownloadUrl()
                        val shareText = "Check out ${info.appName}:\n${linkToShare.ifEmpty { "https://flash-o.com" }}"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share ${info.appName}"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isWebPortal) "Share Portal" else "Share App", maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        val effectiveUrl = if (isWebPortal) info.websiteUrl else info.getEffectiveDownloadUrl()
                        if (effectiveUrl.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(effectiveUrl))
                            Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No link configured yet. Click Edit to set one.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Link", maxLines = 1)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun DownloadOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) color.copy(alpha = 0.08f) else Color(0xFFF5F5F5),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (enabled) color else Color.Gray,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (enabled) Color.Black else Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = if (enabled) color else Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

private fun openUrlOrFallback(context: Context, url: String, info: AppUpdateInfo, onConfigure: () -> Unit) {
    if (url.isNotBlank()) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    } else {
        handleAppAction(context, info, onConfigure)
    }
}

private fun handleAppAction(context: Context, info: AppUpdateInfo, onConfigure: () -> Unit) {
    if (info.isWebPortal()) {
        if (info.websiteUrl.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.websiteUrl))
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open website URL", Toast.LENGTH_SHORT).show()
            }
        }
        Toast.makeText(context, "No web URL configured. Opening editor...", Toast.LENGTH_SHORT).show()
        onConfigure()
        return
    }

    val packageName = when (info.appId) {
        "store" -> "com.fryumm.store"
        "customer" -> "com.flasho.customer"
        "partner" -> "com.flasho.partner"
        else -> ""
    }

    if (packageName.isNotBlank()) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            try {
                context.startActivity(launchIntent)
                Toast.makeText(context, "Opening ${info.appName}...", Toast.LENGTH_SHORT).show()
                return
            } catch (e: Exception) {
                // Ignore and fallback to link
            }
        }
    }

    val url = info.getEffectiveDownloadUrl()
    if (url.isNotBlank()) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open download URL", Toast.LENGTH_SHORT).show()
        }
    } else {
        if (packageName.isNotBlank()) {
            try {
                val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                context.startActivity(playStoreIntent)
                return
            } catch (e: Exception) {
                val webPlayStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                context.startActivity(webPlayStoreIntent)
                return
            }
        }
        Toast.makeText(context, "No download link configured. Opening editor...", Toast.LENGTH_SHORT).show()
        onConfigure()
    }
}

private fun isAppPackageInstalled(context: Context, appId: String): Boolean {
    val packageName = when (appId) {
        "store" -> "com.fryumm.store"
        "customer" -> "com.flasho.customer"
        "partner" -> "com.flasho.partner"
        else -> ""
    }
    if (packageName.isEmpty()) return false
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }
}

private fun getAppDefaultIcon(appId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (appId) {
        "customer" -> Icons.Default.ShoppingBag
        "partner" -> Icons.Default.TwoWheeler
        "store" -> Icons.Default.Storefront
        "web" -> Icons.Default.Language
        "merchant_web" -> Icons.Default.Web
        else -> Icons.Default.Apps
    }
}

private fun getInitialAppInfo(appId: String): AppUpdateInfo {
    return when (appId) {
        "customer" -> AppUpdateInfo(
            appId = "customer",
            appName = "Flash-O Customer App",
            appDescription = "Instant 10-minute delivery for groceries & daily essentials.",
            category = "Customer App",
            isWebApp = false,
            iconUrl = "https://cdn-icons-png.flaticon.com/512/3081/3081559.png",
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.flasho.customer",
            websiteUrl = "https://flash-o.com",
            versionName = "1.2.0",
            versionCode = 12
        )
        "partner" -> AppUpdateInfo(
            appId = "partner",
            appName = "Flash-O Partner (Rider)",
            appDescription = "Realtime delivery partner app for order dispatch & navigation.",
            category = "Rider & Logistics",
            isWebApp = false,
            iconUrl = "https://cdn-icons-png.flaticon.com/512/2830/2830312.png",
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.flasho.partner",
            websiteUrl = "https://flash-o.com",
            versionName = "1.1.5",
            versionCode = 8
        )
        "store" -> AppUpdateInfo(
            appId = "store",
            appName = "F-Store (Merchant & Admin)",
            appDescription = "Dark store fulfillment, inventory cataloging & store dashboard.",
            category = "Merchant App",
            isWebApp = false,
            iconUrl = "https://cdn-icons-png.flaticon.com/512/869/869636.png",
            websiteUrl = "https://flash-o.com",
            versionName = "1.2.0",
            versionCode = 15
        )
        "web" -> AppUpdateInfo(
            appId = "web",
            appName = "Flash-O Web Portal",
            appDescription = "Web-based customer ordering portal accessible from any browser.",
            category = "Web Portal",
            isWebApp = true,
            iconUrl = "https://cdn-icons-png.flaticon.com/512/1006/1006771.png",
            websiteUrl = "https://flash-o.com",
            versionName = "2.0.0",
            versionCode = 20
        )
        "merchant_web" -> AppUpdateInfo(
            appId = "merchant_web",
            appName = "Merchant Web Dashboard",
            appDescription = "Web-based merchant management dashboard for orders & inventory.",
            category = "Web Portal",
            isWebApp = true,
            iconUrl = "https://cdn-icons-png.flaticon.com/512/2921/2921222.png",
            websiteUrl = "https://flash-o.com/merchant",
            versionName = "1.5.0",
            versionCode = 15
        )
        else -> AppUpdateInfo(
            appId = appId,
            appName = appId.replaceFirstChar { it.uppercase() },
            isWebApp = appId.contains("web", ignoreCase = true)
        )
    }
}
