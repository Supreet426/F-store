package com.fryumm.store

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fryumm.store.data.model.Order
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.util.OrderNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val type = remoteMessage.data["type"]
        val actionUrl = remoteMessage.data["actionUrl"] ?: remoteMessage.notification?.clickAction
        
        if (type == "ORDER_CALL") {
            // Data extraction for order calls
            val orderId = remoteMessage.data["orderId"] ?: ""
            val customerName = remoteMessage.data["customerName"] ?: "New Order"
            val amount = remoteMessage.data["amount"] ?: "0.0"
            val targetUid = remoteMessage.data["targetUid"] ?: ""
            val merchantId = remoteMessage.data["merchantId"] ?: remoteMessage.data["sellerId"] ?: ""
            val darkStoreId = remoteMessage.data["darkStoreId"] ?: ""

            handleOrderCall(orderId, customerName, amount, targetUid, merchantId, darkStoreId)
        } else {
            // General Notification
            val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "F-Store"
            val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
            val imageUrl = remoteMessage.data["imageUrl"] ?: remoteMessage.notification?.imageUrl?.toString()
            sendGeneralNotification(title, body, actionUrl, imageUrl)
        }
    }

    private fun handleOrderCall(
        orderId: String,
        customerName: String,
        amount: String,
        targetUid: String,
        merchantId: String,
        darkStoreId: String
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Log.d("FCM", "No logged in user, ignoring order call")
            return
        }

        val userEmail = currentUser.email?.lowercase() ?: ""
        val isAdmin = OrderNotificationHelper.MASTER_ADMIN_EMAILS.contains(userEmail)
        val isTargetUser = targetUid.isNotEmpty() && targetUid == currentUser.uid
        val isDirectMerchant = merchantId.isNotEmpty() && merchantId == currentUser.uid
        val isDarkStoreMatch = darkStoreId.isNotEmpty() && (merchantId == darkStoreId || targetUid == darkStoreId)

        // Fast-path approval if Admin, direct merchant, or dark store matches
        if (isAdmin || isTargetUser || isDirectMerchant || isDarkStoreMatch) {
            triggerWakeAndNotification(customerName, amount, orderId)
            return
        }

        // Deep verification check against Firestore order and user profile
        if (orderId.isNotEmpty()) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("orders").document(orderId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val order = doc.toObject(Order::class.java)
                        if (order != null) {
                            val collections = listOf("merchants", "users", "staff")
                            for (coll in collections) {
                                firestore.collection(coll).document(currentUser.uid).get()
                                    .addOnSuccessListener { userDoc ->
                                        if (userDoc.exists()) {
                                            val profile = userDoc.toObject(UserProfile::class.java)?.copy(uid = currentUser.uid)
                                            if (OrderNotificationHelper.shouldNotifyUserForOrder(profile, order)) {
                                                triggerWakeAndNotification(customerName, amount, orderId)
                                            } else {
                                                Log.d("FCM", "Ignored order call for $orderId - merchant does not own products in order.")
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
        }
    }

    private fun triggerWakeAndNotification(customerName: String, amount: String, orderId: String) {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "FStore:OrderNotification"
        )
        wakeLock.acquire(10000)

        sendOrderCallNotification(customerName, "Amount: ₹$amount", orderId, amount)
    }

    private fun sendGeneralNotification(title: String, body: String, actionUrl: String?, imageUrl: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("actionUrl", actionUrl)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "general_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "General Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Order updates and broadcast notifications"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                setSound(defaultSoundUri, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        // Handle Image if present
        if (!imageUrl.isNullOrBlank()) {
            try {
                val url = java.net.URL(imageUrl)
                val bitmap = android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream())
                notificationBuilder.setLargeIcon(bitmap)
                notificationBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as android.graphics.Bitmap?))
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Error loading notification image", e)
            }
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun sendOrderCallNotification(customerName: String, body: String, orderId: String, amount: String) {
        // This Intent will launch the full-screen "Calling" Activity
        val fullScreenIntent = Intent(this, IncomingOrderActivity::class.java).apply {
            putExtra("orderId", orderId)
            putExtra("customerName", customerName)
            putExtra("amount", amount)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 1001, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "order_call_channel"
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Incoming Orders (Call Mode)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "WhatsApp-like calling screen and ring for new orders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(ringtoneUri, attributes)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("New Order from $customerName")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(ringtoneUri)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenPendingIntent) // Tapping notification opens order screen

        // Check if Full Screen Intent is permitted (Android 14+ / API 34 restriction check)
        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }

        if (canUseFullScreen) {
            notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        notificationManager.notify(1001, notificationBuilder.build())
    }
}
