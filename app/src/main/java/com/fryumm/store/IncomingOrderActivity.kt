package com.fryumm.store

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.data.repository.OrderRepository
import com.fryumm.store.ui.theme.FStoreTheme
import com.fryumm.store.ui.theme.FStorePurple
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IncomingOrderActivity : ComponentActivity() {
    @Inject lateinit var orderRepository: OrderRepository
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup for lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        startRinging()

        val orderId = intent.getStringExtra("orderId") ?: "Unknown"
        val customerName = intent.getStringExtra("customerName") ?: "New Customer"
        val amount = intent.getStringExtra("amount") ?: "0.0"

        setContent {
            FStoreTheme {
                IncomingOrderScreen(
                    customerName = customerName,
                    amount = amount,
                    onAccept = { 
                        CoroutineScope(Dispatchers.IO).launch {
                            orderRepository.updateOrderFields(orderId, mapOf("status" to OrderStatus.ACCEPTED))
                        }
                        stopAndFinish() 
                    },
                    onDecline = { stopAndFinish() }
                )
            }
        }
    }

    private fun startRinging() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@IncomingOrderActivity, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 0))
            } else {
                vibrator?.vibrate(longArrayOf(0, 1000, 500), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAndFinish() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAndFinish()
    }
}

@Composable
fun IncomingOrderScreen(
    customerName: String,
    amount: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FStorePurple),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(120.dp)
            ) {
                Icon(
                    Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(24.dp).fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "NEW ORDER RECEIVED!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                customerName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            
            Text(
                "Amount: ₹$amount",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Decline Button
                FloatingActionButton(
                    onClick = onDecline,
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.Close, "Decline", modifier = Modifier.size(32.dp))
                }
                
                // Accept Button
                FloatingActionButton(
                    onClick = onAccept,
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.Check, "Accept", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
