package com.fryumm.store.ui.screens.appmanagement

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.fryumm.store.data.model.*
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun HomeScreenLayoutTab(
    config: DynamicAppConfig,
    onConfigChange: (DynamicAppConfig) -> Unit
) {
    var showAddSectionDialog by remember { mutableStateOf<String?>(null) }
    var sectionUrlToAdd by remember { mutableStateOf("") }
    var videoPreviewUrl by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            StrategyCard(
                title = "Home Screen Layout",
                subtitle = "Manage the sequence and content of sections",
                icon = Icons.Default.Dashboard,
                color = Color(0xFFE91E63)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("QUICK ADD SECTIONS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = FStorePurple)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { showAddSectionDialog = "BANNER_AD"; sectionUrlToAdd = "" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(containerColor = Color.Gray.copy(0.05f))
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("BANNER", fontSize = 10.sp)
                        }
                        
                        TextButton(
                            onClick = { showAddSectionDialog = "VIDEO_AD"; sectionUrlToAdd = "" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(containerColor = Color.Gray.copy(0.05f))
                        ) {
                            Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("VIDEO AD", fontSize = 10.sp)
                        }

                        TextButton(
                            onClick = { showAddSectionDialog = "PRODUCT_AD"; sectionUrlToAdd = "" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(containerColor = Color.Gray.copy(0.05f))
                        ) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PROMO AD", fontSize = 10.sp)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { 
                                val current = config.homeSectionOrder.toMutableList()
                                current.add("FLASH_D_PASS_99")
                                onConfigChange(config.copy(homeSectionOrder = current))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E007C))
                        ) {
                            Icon(Icons.Default.CardMembership, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ADD PASS 99", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { 
                                val current = config.homeSectionOrder.toMutableList()
                                current.add("FLASH_D_PASS_199")
                                onConfigChange(config.copy(homeSectionOrder = current))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3F6C))
                        ) {
                            Icon(Icons.Default.WorkspacePremium, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ADD PREMIUM 199", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(0.3f))

                    config.homeSectionOrder.forEachIndexed { index, section ->
                        val parts = section.split("|")
                        val sectionType = parts[0]
                        val sectionId = if (parts.size > 1) parts[1] else ""
                        
                        val isSpecial = sectionType == "BANNER_AD" || sectionType == "VIDEO_AD" || sectionType == "PRODUCT_AD" || sectionType == "Banners" || sectionType.startsWith("FLASH_D_PASS") || sectionType.startsWith("FRYUMM_PASS")
                        val displayName = when {
                            sectionType == "BANNER_AD" -> "Mini Banner (Inline Ad)"
                            sectionType == "VIDEO_AD" -> "Video Advertisement"
                            sectionType == "PRODUCT_AD" -> "Product Promotion"
                            sectionType == "FLASH_D_PASS_99" || sectionType == "FRYUMM_PASS_99" -> "Flash-O Pass 99"
                            sectionType == "FLASH_D_PASS_199" || sectionType == "FRYUMM_PASS_199" -> "Flash-O Premium 199"
                            sectionType == "FLASH_D_PASS" || sectionType == "FRYUMM_PASS" -> "Flash-O Pass (Subscription)"
                            else -> sectionType
                        }
                        
                        SectionOrderItem(
                            name = displayName,
                            index = index + 1,
                            isSpecial = isSpecial,
                            url = sectionId.takeIf { isSpecial && sectionType != "Banners" && !sectionType.startsWith("FLASH_D_PASS") && !sectionType.startsWith("FRYUMM_PASS") },
                            isProductAd = sectionType == "PRODUCT_AD",
                            onUrlChange = { newUrl ->
                                val newList = config.homeSectionOrder.toMutableList()
                                newList[index] = "$sectionType|$newUrl"
                                onConfigChange(config.copy(homeSectionOrder = newList))
                            },
                            onPreviewVideo = if (sectionType == "VIDEO_AD") { { videoPreviewUrl = sectionId } } else null,
                            onDelete = if(isSpecial && sectionType != "Banners") { {
                                val newList = config.homeSectionOrder.toMutableList()
                                newList.removeAt(index)
                                onConfigChange(config.copy(homeSectionOrder = newList))
                            } } else null,
                            onReorder = { newSerial ->
                                val targetIndex = (newSerial - 1).coerceIn(0, config.homeSectionOrder.size - 1)
                                val newList = config.homeSectionOrder.toMutableList()
                                val item = newList.removeAt(index)
                                newList.add(targetIndex, item)
                                onConfigChange(config.copy(homeSectionOrder = newList))
                            }
                        )
                    }
                    Text("Use Checkbox in 'Product Catalog' to add categories here.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }

    if (showAddSectionDialog != null) {
        AddSectionDialog(
            type = showAddSectionDialog!!,
            sectionUrlToAdd = sectionUrlToAdd,
            onUrlChange = { sectionUrlToAdd = it },
            onDismiss = { showAddSectionDialog = null },
            onConfirm = { type, finalUrl ->
                val current = config.homeSectionOrder.toMutableList()
                val entry = if (finalUrl.isEmpty()) type else "$type|$finalUrl"
                current.add(entry)

                val newBanners = config.bannerAds.toMutableList()
                val newVideos = config.videoAds.toMutableList()
                val newPromos = config.productAds.toMutableList()

                val timestampId = System.currentTimeMillis().toString()
                when (type) {
                    "BANNER_AD" -> newBanners.add(BannerAd(id = "b_$timestampId", imageUrl = finalUrl, title = "New Banner"))
                    "VIDEO_AD" -> newVideos.add(VideoAd(id = "v_$timestampId", videoUrl = finalUrl, title = "New Video"))
                    "PRODUCT_AD" -> newPromos.add(ProductAd(id = "p_$timestampId", label = finalUrl))
                }

                onConfigChange(config.copy(
                    homeSectionOrder = current.distinct(),
                    bannerAds = newBanners,
                    videoAds = newVideos,
                    productAds = newPromos
                ))
                showAddSectionDialog = null
            }
        )
    }

    if (videoPreviewUrl != null) {
        VideoPreviewDialog(
            videoUrl = videoPreviewUrl!!,
            onDismiss = { videoPreviewUrl = null }
        )
    }
}

@Composable
fun AddSectionDialog(
    type: String,
    sectionUrlToAdd: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val isProduct = type == "PRODUCT_AD"
    val isVideo = type == "VIDEO_AD"
    val isBanner = type == "BANNER_AD"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${if(isVideo) "Video Ad" else if(isBanner) "Banner" else "Promotion"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val helpText = when {
                    isProduct -> "Enter the badge label (e.g. Featured, Sponsored) to group products."
                    isVideo -> "Enter a direct MP4 URL or YouTube link. YouTube will use a player."
                    else -> "Enter the direct image URL for this banner."
                }
                Text(text = helpText, fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(
                    value = sectionUrlToAdd,
                    onValueChange = onUrlChange,
                    label = { Text(if(isProduct) "Section Label" else "Media URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if(isProduct) "Featured" else "https://...") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalUrl = if (sectionUrlToAdd.contains("drive.google.com")) {
                    sectionUrlToAdd.replace("/file/d/", "/uc?export=view&id=").replace("/view?usp=sharing", "")
                } else sectionUrlToAdd
                onConfirm(type, finalUrl)
            }) { Text("ADD SECTION") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun VideoPreviewDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Video/GIF Preview") },
        text = {
            val isYouTube = videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")
            val isGif = videoUrl.lowercase().endsWith(".gif") || videoUrl.contains("giphy.com")

            Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                if (isYouTube) {
                    val videoId = when {
                        videoUrl.contains("v=") -> videoUrl.substringAfter("v=").substringBefore("&")
                        videoUrl.contains("youtu.be/") -> videoUrl.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
                        videoUrl.contains("/shorts/") -> videoUrl.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
                        videoUrl.contains("embed/") -> videoUrl.substringAfter("embed/").substringBefore("?").substringBefore("/")
                        else -> videoUrl.substringAfterLast("/")
                    }
                    val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&mute=1&origin=https://www.youtube.com"
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
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
                        model = videoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(videoUrl))
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setOnPreparedListener { 
                                    it.isLooping = true
                                    it.setVolume(0f, 0f)
                                    it.start() 
                                }
                                setOnErrorListener { _, _, _ -> false }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}
