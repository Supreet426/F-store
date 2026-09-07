package com.fryumm.store.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
                        editingAppInfo = currentInfo.copy(
                            directApkUrl = uploadedUrl,
                            downloadUrl = uploadedUrl,
                            timestamp = System.currentTimeMillis()
                        )
                        Toast.makeText(context, "APK Uploaded successfully! Fill in release details.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "APK Upload failed", Toast.LENGTH_SHORT).show()
                    }
                    isUploading = false
                }
            }
        }
    }

    val defaultAppIds = listOf("customer", "partner", "store", "web")

    // Merge saved updates with default list so all ecosystem apps are visible
    val allEcosystemApps = remember(updates) {
        val mappedList = mutableListOf<AppUpdateInfo>()
        defaultAppIds.forEach { id ->
            val existing = updates.find { it.appId == id }
            mappedList.add(existing ?: getInitialAppInfo(id))
        }
        // Include any additional custom apps saved in Firestore
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
                        Text("Ecosystem Update Center", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Manage releases, APKs & links for all Flash-O apps", fontSize = 11.sp, color = Color.Gray)
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
                            appName = "New Flash-O Module",
                            category = "Ecosystem Service",
                            versionName = "1.0.0",
                            versionCode = 1
                        )
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add New App")
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
                                Text("Flash-O Unified Ecosystem", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FStorePurple)
                                Text(
                                    "Configure Play Store links, Google Drive/Direct APKs, GitHub releases, and website URLs. Apps become fully clickable with detailed release notes.",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }

                items(allEcosystemApps, key = { it.appId }) { appInfo ->
                    EcosystemAppAdminCard(
                        info = appInfo,
                        onEdit = { editingAppInfo = appInfo },
                        onUploadApk = {
                            selectedAppId = appInfo.appId
                            launcher.launch("application/vnd.android.package-archive")
                        },
                        onPreview = { previewingAppInfo = appInfo }
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
                        Text("Please wait while the direct download link is generated", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
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

    // Customer Detail Sheet Preview Modal
    if (previewingAppInfo != null) {
        EcosystemAppDetailsSheet(
            info = previewingAppInfo!!,
            onDismiss = { previewingAppInfo = null }
        )
    }
}

@Composable
fun EcosystemAppAdminCard(
    info: AppUpdateInfo,
    onEdit: () -> Unit,
    onUploadApk: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = FStorePurple.copy(alpha = 0.1f)
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
                            Icon(getAppDefaultIcon(info.appId), null, tint = FStorePurple, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(info.appName.ifEmpty { info.appId }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = FStorePurple.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "v${info.versionName}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = FStorePurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Text(
                        info.appDescription.ifEmpty { info.category.ifEmpty { "Flash-O Ecosystem Application" } },
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPreview) {
                    Icon(Icons.Default.Visibility, contentDescription = "Preview Details", tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Active Download Channels Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelChip(name = "Play Store", active = info.playStoreUrl.isNotBlank(), icon = Icons.Default.Shop)
                ChannelChip(name = "Direct APK", active = info.directApkUrl.isNotBlank() || info.downloadUrl.isNotBlank(), icon = Icons.Default.FileDownload)
                ChannelChip(name = "GitHub", active = info.githubUrl.isNotBlank(), icon = Icons.Default.Code)
                ChannelChip(name = "Website", active = info.websiteUrl.isNotBlank(), icon = Icons.Default.Language)
            }

            Spacer(Modifier.height(12.dp))

            // What's New Snippet
            if (info.releaseNotes.isNotBlank()) {
                Surface(
                    color = Color(0xFFF1F3F5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("✨ What's New in v${info.versionName}:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
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

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onUploadApk,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Upload APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit & Release", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) Color(0xFF2E7D32) else Color.Gray
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
                Icon(Icons.Default.CloudSync, null, tint = FStorePurple)
                Spacer(Modifier.width(8.dp))
                Text("Manage Ecosystem App", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("BASIC APP DETAILS", fontWeight = FontWeight.Black, fontSize = 11.sp, color = FStorePurple)

                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = appDescription,
                    onValueChange = { appDescription = it },
                    label = { Text("App Description / Tagline") },
                    placeholder = { Text("e.g. Order food & groceries delivered in 10 minutes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = iconUrl,
                    onValueChange = { iconUrl = it },
                    label = { Text("Icon / Logo URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Logo Presets
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Presets:", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                    AssistChip(
                        onClick = { iconUrl = "https://cdn-icons-png.flaticon.com/512/3081/3081559.png" },
                        label = { Text("Customer", fontSize = 10.sp) }
                    )
                    AssistChip(
                        onClick = { iconUrl = "https://cdn-icons-png.flaticon.com/512/2830/2830312.png" },
                        label = { Text("Rider", fontSize = 10.sp) }
                    )
                    AssistChip(
                        onClick = { iconUrl = "https://cdn-icons-png.flaticon.com/512/869/869636.png" },
                        label = { Text("Store", fontSize = 10.sp) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text("Version Name") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = versionCode,
                        onValueChange = { if (it.all { c -> c.isDigit() }) versionCode = it },
                        label = { Text("Version Code") },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("RELEASE NOTES (WHAT'S NEW)", fontWeight = FontWeight.Black, fontSize = 11.sp, color = FStorePurple)
                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("What's New in this update") },
                    placeholder = { Text("• Faster checkout flow\n• Live rider tracking\n• Bug fixes and performance improvements") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isMandatory, onCheckedChange = { isMandatory = it })
                    Text("Force Update (Mandatory for users)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("4 ACCESS & DOWNLOAD CHANNELS", fontWeight = FontWeight.Black, fontSize = 11.sp, color = FStorePurple)

                // 1. Play Store Link
                OutlinedTextField(
                    value = playStoreUrl,
                    onValueChange = { playStoreUrl = it },
                    label = { Text("1. Google Play Store Link") },
                    placeholder = { Text("https://play.google.com/store/apps/details?id=...") },
                    leadingIcon = { Icon(Icons.Default.Shop, null, tint = Color(0xFF4CAF50)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Direct APK / Google Drive Link
                Column {
                    OutlinedTextField(
                        value = directApkUrl,
                        onValueChange = { directApkUrl = it },
                        label = { Text("2. Direct APK / Google Drive Link") },
                        placeholder = { Text("Paste Google Drive link or Cloud storage URL") },
                        leadingIcon = { Icon(Icons.Default.FileDownload, null, tint = Color(0xFF2196F3)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = onUploadApkClick) {
                        Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Or Upload APK File directly to Firebase", fontSize = 11.sp)
                    }
                }

                // 3. GitHub Link
                OutlinedTextField(
                    value = githubUrl,
                    onValueChange = { githubUrl = it },
                    label = { Text("3. GitHub Releases / Repo Link") },
                    placeholder = { Text("https://github.com/org/repo/releases") },
                    leadingIcon = { Icon(Icons.Default.Code, null, tint = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. Website Link
                OutlinedTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it },
                    label = { Text("4. Official Website / Web App Link") },
                    placeholder = { Text("https://flash-o.com or web app URL") },
                    leadingIcon = { Icon(Icons.Default.Language, null, tint = Color(0xFF9C27B0)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        info.copy(
                            appName = appName,
                            appDescription = appDescription,
                            category = category.ifEmpty { "Ecosystem App" },
                            iconUrl = iconUrl,
                            versionName = versionName,
                            versionCode = versionCode.toIntOrNull() ?: info.versionCode,
                            releaseNotes = releaseNotes,
                            isMandatory = isMandatory,
                            playStoreUrl = playStoreUrl.trim(),
                            directApkUrl = directApkUrl.trim(),
                            downloadUrl = directApkUrl.trim().ifEmpty { playStoreUrl.trim() },
                            githubUrl = githubUrl.trim(),
                            websiteUrl = websiteUrl.trim(),
                            timestamp = System.currentTimeMillis()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)
            ) {
                Text("PUBLISH UPDATE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcosystemAppDetailsSheet(
    info: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val updateDate = remember(info.timestamp) { sdf.format(Date(info.timestamp)) }

    fun openUrl(url: String) {
        if (url.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Link not configured by Admin yet", Toast.LENGTH_SHORT).show()
        }
    }

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
                    color = FStorePurple.copy(alpha = 0.1f)
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
                            Icon(getAppDefaultIcon(info.appId), null, tint = FStorePurple, modifier = Modifier.size(36.dp))
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(info.appName.ifEmpty { info.appId.replaceFirstChar { it.uppercase() } }, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(info.category.ifEmpty { "Flash-O Ecosystem Application" }, fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = FStorePurple.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "v${info.versionName} (${info.versionCode})",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = FStorePurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Updated $updateDate", fontSize = 11.sp, color = Color.Gray)
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
                        Text("About App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(info.appDescription, fontSize = 13.sp, color = Color.DarkGray)
                    }
                }
            }

            // What's New in Update
            Surface(
                color = FStorePurple.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FStorePurple.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = FStorePurple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("What's New in this Update", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FStorePurple)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        info.releaseNotes.ifEmpty { "General speed improvements, stability enhancements, and bug fixes." },
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }
            }

            Text("Download & Launch Options", fontWeight = FontWeight.Black, fontSize = 14.sp)

            // 4 DOWNLOAD & ACCESS OPTIONS
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. Play Store
                DownloadOptionCard(
                    title = "Google Play Store",
                    subtitle = "Official app store download",
                    icon = Icons.Default.Shop,
                    color = Color(0xFF4CAF50),
                    enabled = info.playStoreUrl.isNotBlank(),
                    onClick = { openUrl(info.playStoreUrl) }
                )

                // 2. Direct APK / Google Drive
                val directUrl = info.directApkUrl.ifEmpty { info.downloadUrl }
                DownloadOptionCard(
                    title = "Direct APK / Google Drive",
                    subtitle = "Fast direct download link or Drive location",
                    icon = Icons.Default.FileDownload,
                    color = Color(0xFF2196F3),
                    enabled = directUrl.isNotBlank(),
                    onClick = { openUrl(directUrl) }
                )

                // 3. GitHub Releases
                DownloadOptionCard(
                    title = "GitHub Release & Source",
                    subtitle = "Open source release notes and APK artifacts",
                    icon = Icons.Default.Code,
                    color = Color.DarkGray,
                    enabled = info.githubUrl.isNotBlank(),
                    onClick = { openUrl(info.githubUrl) }
                )

                // 4. Web Application / Website
                DownloadOptionCard(
                    title = "Visit Web Portal / Website",
                    subtitle = "Launch web application directly in browser",
                    icon = Icons.Default.Language,
                    color = Color(0xFF9C27B0),
                    enabled = info.websiteUrl.isNotBlank(),
                    onClick = { openUrl(info.websiteUrl) }
                )
            }

            // Share & Copy Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val shareText = "Download ${info.appName} (v${info.versionName}):\nPlay Store: ${info.playStoreUrl.ifEmpty { "N/A" }}\nDirect Download: ${info.getEffectiveDownloadUrl()}\nWebsite: ${info.websiteUrl}"
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
                    Text("Share App")
                }

                OutlinedButton(
                    onClick = {
                        val effectiveUrl = info.getEffectiveDownloadUrl()
                        if (effectiveUrl.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(effectiveUrl))
                            Toast.makeText(context, "Download link copied!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No download link available to copy", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Link")
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
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (enabled) Color.Black else Color.Gray)
                Text(
                    if (enabled) subtitle else "Not configured by admin",
                    fontSize = 11.sp,
                    color = if (enabled) Color.Gray else Color.Red.copy(alpha = 0.7f)
                )
            }

            if (enabled) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun getAppDefaultIcon(appId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (appId) {
        "customer" -> Icons.Default.ShoppingBag
        "partner" -> Icons.Default.TwoWheeler
        "store" -> Icons.Default.Storefront
        "web" -> Icons.Default.Language
        else -> Icons.Default.Apps
    }
}

private fun getInitialAppInfo(appId: String): AppUpdateInfo {
    return when (appId) {
        "customer" -> AppUpdateInfo(
            appId = "customer",
            appName = "Flash-O (Customer App)",
            appDescription = "Instant 10-minute delivery for groceries, daily essentials & organic fresh foods.",
            category = "Consumer Quick-Commerce",
            iconUrl = "https://cdn-icons-png.flaticon.com/512/3081/3081559.png",
            versionName = "1.2.0",
            versionCode = 12
        )
        "partner" -> AppUpdateInfo(
            appId = "partner",
            appName = "Flash-O Partner (Rider)",
            appDescription = "Realtime delivery partner app for order dispatch, navigation & daily payouts.",
            category = "Rider & Logistics",
            iconUrl = "https://cdn-icons-png.flaticon.com/512/2830/2830312.png",
            versionName = "1.1.5",
            versionCode = 8
        )
        "store" -> AppUpdateInfo(
            appId = "store",
            appName = "F-Store (Merchant & Admin)",
            appDescription = "Dark store fulfillment, inventory cataloging, order packing & CEO dashboard.",
            category = "Merchant Enterprise",
            iconUrl = "https://cdn-icons-png.flaticon.com/512/869/869636.png",
            versionName = "1.2.0",
            versionCode = 15
        )
        "web" -> AppUpdateInfo(
            appId = "web",
            appName = "Flash-O Web Portal",
            appDescription = "Web-based customer ordering, merchant management, and analytics portal.",
            category = "Web Platform",
            iconUrl = "https://cdn-icons-png.flaticon.com/512/1006/1006771.png",
            versionName = "2.0.0",
            versionCode = 20
        )
        else -> AppUpdateInfo(appId = appId, appName = appId.replaceFirstChar { it.uppercase() })
    }
}
