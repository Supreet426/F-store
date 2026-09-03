package com.fryumm.store.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fryumm.store.BuildConfig
import com.fryumm.store.ui.viewmodels.StoreViewModel

@Composable
fun AppUpdateChecker(viewModel: StoreViewModel) {
    val context = LocalContext.current
    val updates = viewModel.appUpdates
    val storeUpdate = updates.find { it.appId == "store" }
    
    var showDialog by remember { mutableStateOf(false) }
    
    // Check if current version is less than the released version
    LaunchedEffect(storeUpdate) {
        if (storeUpdate != null && storeUpdate.versionCode > BuildConfig.VERSION_CODE) {
            showDialog = true
        }
    }

    if (showDialog && storeUpdate != null) {
        AlertDialog(
            onDismissRequest = { if (!storeUpdate.isMandatory) showDialog = false },
            title = { Text("Update Available") },
            text = {
                Column {
                    Text("A new version of F-Store (v${storeUpdate.versionName}) is available.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("What's New:", style = MaterialTheme.typography.labelLarge)
                    Text(storeUpdate.releaseNotes)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(storeUpdate.downloadUrl))
                    context.startActivity(intent)
                }) {
                    Text("UPDATE NOW")
                }
            },
            dismissButton = if (!storeUpdate.isMandatory) {
                {
                    TextButton(onClick = { showDialog = false }) {
                        Text("LATER")
                    }
                }
            } else null
        )
    }
}
