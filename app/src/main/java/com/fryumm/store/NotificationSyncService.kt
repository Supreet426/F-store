package com.fryumm.store

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fryumm.store.data.model.OrderStatus
import com.fryumm.store.data.model.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationSyncService : Service() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var orderListener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationSyncService", "Service Created")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        startForegroundService()
        
        // Listen for Auth changes to start/stop the order listener
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("NotificationSyncService", "User logged in: ${user.email}, starting observer")
                observeNewOrders()
            } else {
                Log.d("NotificationSyncService", "User logged out, stopping observer")
                orderListener?.remove()
                orderListener = null
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun startForegroundService() {
        val channelId = "sync_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Order Sync Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("F-Store is Active")
            .setContentText("Watching for new orders...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1002, notification)
        }
    }

    private fun observeNewOrders() {
        if (orderListener != null) return // Already observing

        val currentUser = auth.currentUser ?: return
        
        // Listen specifically for PLACED orders
        orderListener = firestore.collection("orders")
            .whereEqualTo("status", OrderStatus.PLACED)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("NotificationSyncService", "Listen failed. Check Firestore indexes.", e)
                    return@addSnapshotListener
                }

                val now = System.currentTimeMillis()
                snapshot?.documentChanges?.forEach { dc ->
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val order = dc.document.toObject(Order::class.java)
                        
                        // Only trigger for orders created in the last 10 minutes 
                        // to avoid old orders triggering popups on service start
                        if (order.timestamp > (now - 600000)) {
                            Log.d("NotificationSyncService", "New Order Detected: ${order.id}")
                            triggerCallScreen(order)
                        }
                    }
                }
            }
    }

    private fun triggerCallScreen(order: Order) {
        // High-priority notification with Full Screen Intent is REQUIRED to show popup from background
        val channelId = "order_alert_channel"
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "New Order Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full screen alerts for incoming orders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(ringtoneUri, attributes)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(this, IncomingOrderActivity::class.java).apply {
            putExtra("orderId", order.id)
            putExtra("customerName", order.customerName)
            putExtra("amount", order.totalAmount.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 1001, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("NEW ORDER RECEIVED!")
            .setContentText("From ${order.customerName} - ₹${order.totalAmount}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(ringtoneUri)
            .setFullScreenIntent(fullScreenPendingIntent, true) // This triggers the popup/activity
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(1001, notificationBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        authListener?.let { auth.removeAuthStateListener(it) }
        orderListener?.remove()
        super.onDestroy()
    }
}
