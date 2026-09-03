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
import androidx.core.app.NotificationCompat
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
            
            // Wake up the device
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "FStore:OrderNotification"
            )
            wakeLock.acquire(10000)

            sendOrderCallNotification(customerName, "Amount: ₹$amount", orderId, amount)
        } else {
            // General Notification
            val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "F-Store"
            val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
            sendGeneralNotification(title, body, actionUrl)
        }
    }

    private fun sendGeneralNotification(title: String, body: String, actionUrl: String?) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "General Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

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
                description = "WhatsApp-like calling screen for new orders"
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
            .setCategory(NotificationCompat.CATEGORY_CALL) // CATEGORY_CALL is important
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(ringtoneUri)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true) // CRITICAL: Launch activity over lockscreen

        notificationManager.notify(1001, notificationBuilder.build())
    }
}
