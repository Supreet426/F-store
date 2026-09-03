package com.fryumm.store.ui.screens.promotions

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.data.model.VideoAd
import com.fryumm.store.ui.theme.FStorePurple
import java.util.UUID

@Composable
fun VideoAdsTab(config: DynamicAppConfig, onUpdate: (DynamicAppConfig) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var actionUrl by remember { mutableStateOf("") }
    
    var videoPreviewUrl by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Video Advertisements", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("In-feed video ads shown between categories.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(config.videoAds) { ad ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VideoLibrary, null, tint = Color.Red)
                            Spacer(Modifier.width(8.dp))
                            Text(ad.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Switch(checked = ad.isActive, onCheckedChange = { active ->
                                val list = config.videoAds.toMutableList()
                                val index = list.indexOfFirst { it.id == ad.id }
                                if (index != -1) {
                                    list[index] = list[index].copy(isActive = active)
                                    onUpdate(config.copy(videoAds = list))
                                }
                            })
                            IconButton(onClick = {
                                onUpdate(config.copy(videoAds = config.videoAds.filter { it.id != ad.id }))
                            }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Video: ${ad.videoUrl}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (ad.videoUrl.isNotBlank()) {
                                IconButton(onClick = { videoPreviewUrl = ad.videoUrl }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.PlayArrow, null, tint = FStorePurple, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("ADD VIDEO AD")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Video Ad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Ad Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = videoUrl, 
                        onValueChange = { videoUrl = it }, 
                        label = { Text("URL (Direct, Drive, YouTube, or GIF)") }, 
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://...") }
                    )
                    OutlinedTextField(value = actionUrl, onValueChange = { actionUrl = it }, label = { Text("Action URL") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalUrl = if (videoUrl.contains("drive.google.com")) {
                        videoUrl.replace("/file/d/", "/uc?export=view&id=").replace("/view?usp=sharing", "")
                    } else videoUrl
                    val newAd = VideoAd(id = UUID.randomUUID().toString(), title = title, videoUrl = finalUrl, actionUrl = actionUrl, isActive = true)
                    onUpdate(config.copy(videoAds = config.videoAds + newAd))
                    showAddDialog = false; title = ""; videoUrl = ""; actionUrl = ""
                }) { Text("ADD") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("CANCEL") } }
        )
    }

    if (videoPreviewUrl != null) {
        AlertDialog(
            onDismissRequest = { videoPreviewUrl = null },
            title = { Text("Video/GIF Preview") },
            text = {
                val isYouTube = videoPreviewUrl!!.contains("youtube.com") || videoPreviewUrl!!.contains("youtu.be")
                val isGif = videoPreviewUrl!!.lowercase().endsWith(".gif") || videoPreviewUrl!!.contains("giphy.com")

                Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                    if (isYouTube) {
                        val videoId = when {
                            videoPreviewUrl!!.contains("v=") -> videoPreviewUrl!!.substringAfter("v=").substringBefore("&")
                            videoPreviewUrl!!.contains("youtu.be/") -> videoPreviewUrl!!.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
                            videoPreviewUrl!!.contains("/shorts/") -> videoPreviewUrl!!.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
                            videoPreviewUrl!!.contains("embed/") -> videoPreviewUrl!!.substringAfter("embed/").substringBefore("?").substringBefore("/")
                            else -> videoPreviewUrl!!.substringAfterLast("/")
                        }

                        val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&mute=1&origin=https://www.youtube.com"

                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                                    }
                                    webViewClient = WebViewClient()
                                    loadUrl(embedUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isGif) {
                        AsyncImage(
                            model = videoPreviewUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    setVideoURI(Uri.parse(videoPreviewUrl))
                                    val controller = MediaController(context)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setOnPreparedListener { it.start() }
                                    setOnErrorListener { _, _, _ -> false }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { videoPreviewUrl = null }) { Text("CLOSE") }
            }
        )
    }
}
