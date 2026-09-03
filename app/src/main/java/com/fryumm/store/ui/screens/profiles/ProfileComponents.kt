package com.fryumm.store.ui.screens.profiles

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.*

data class TimelineEvent(val timestamp: Long, val type: String, val details: String)

@Composable
fun ActivityTimelineItem(event: TimelineEvent, timeSdf: SimpleDateFormat, dateSdf: SimpleDateFormat) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(FStorePurple, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(Color.LightGray)
            )
        }
        
        Card(
            modifier = Modifier.weight(1f).padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(event.type, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FStorePurple)
                    if (event.details.isNotEmpty()) {
                        Text(event.details, fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(timeSdf.format(Date(event.timestamp)), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(dateSdf.format(Date(event.timestamp)), fontSize = 9.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    name: String, 
    subTitle: String, 
    tag: String, 
    onBack: () -> Unit, 
    backgroundColor: Color,
    photoUrl: String = ""
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(backgroundColor)) {
        IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape, 
                modifier = Modifier.size(60.dp), 
                color = Color.White.copy(alpha = 0.2f)
            ) {
                if (photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp), tint = Color.White)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subTitle, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(4.dp), color = Color.White.copy(alpha = 0.15f)) {
                Text(tag, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, color: Color = Color.Black) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = color.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.1f), modifier = Modifier.size(36.dp)) {
                Icon(icon, null, modifier = Modifier.padding(8.dp), tint = color)
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.width(100.dp), maxLines = 1, softWrap = false)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SimpleLineChart(data: List<Float>, color: Color, modifier: Modifier = Modifier.fillMaxWidth().height(80.dp)) {
    if (data.isEmpty() || data.all { it == 0f }) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Insufficient data for trend", fontSize = 10.sp, color = Color.LightGray)
        }
        return
    }

    val maxVal = data.maxOrNull() ?: 1f
    val animatedProgress = animateFloatAsState(targetValue = 1f, animationSpec = tween(1000), label = "")

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1)
        
        val points = data.indices.map { i ->
            val x = i * spacing
            val y = height - (data[i] / maxVal * height * animatedProgress.value)
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                cubicTo(
                    (points[i - 1].x + points[i].x) / 2, points[i - 1].y,
                    (points[i - 1].x + points[i].x) / 2, points[i].y,
                    points[i].x, points[i].y
                )
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Gradient fill
        val fillPath = android.graphics.Path(path.asAndroidPath()).asComposePath().apply {
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
            )
        )
    }
}
