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
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.util.OrderNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationSyncService : Service() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var orderListener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var currentUserProfile: UserProfile? = null

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
                currentUserProfile = null
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

    private var slaMonitorJob: kotlinx.coroutines.Job? = null
    private val escalatedOrders = mutableSetOf<String>()

    private fun fetchAndObserveUserProfile(uid: String, onFetched: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val collections = listOf("sellers", "users", "staff", "riders", "customers")
            for (coll in collections) {
                try {
                    val doc = firestore.collection(coll).document(uid).get().await()
                    if (doc.exists()) {
                        currentUserProfile = doc.toObject(UserProfile::class.java)?.copy(uid = uid)
                        if (currentUserProfile != null) break
                    }
                } catch (e: Exception) {
                    Log.e("NotificationSyncService", "Error fetching profile from $coll", e)
                }
            }
            if (currentUserProfile == null) {
                val email = auth.currentUser?.email ?: ""
                currentUserProfile = UserProfile(
                    uid = uid,
                    email = email,
                    role = if (OrderNotificationHelper.MASTER_ADMIN_EMAILS.contains(email.lowercase())) "ADMIN" else "STORE_OWNER"
                )
            }

            // Subscribe to FCM Topics for instant targeting
            currentUserProfile?.let { profile ->
                try {
                    val repo = com.fryumm.store.data.repository.NotificationRepository(firestore)
                    repo.subscribeToUserTopics(
                        userId = profile.uid,
                        role = profile.role,
                        darkStoreId = profile.darkStoreId,
                        email = profile.email
                    )
                } catch (e: Exception) {
                    Log.e("NotificationSyncService", "FCM topic subscription error", e)
                }
            }

            onFetched()
        }
    }

    private fun startSlaEscalationMonitor() {
        if (slaMonitorJob != null) return
        slaMonitorJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                kotlinx.coroutines.delay(30000) // Check every 30 seconds
                val now = System.currentTimeMillis()
                val threeMinutes = 3 * 60 * 1000L

                try {
                    val query = firestore.collection("orders")
                        .whereEqualTo("status", OrderStatus.PLACED)
                        .get().await()

                    for (doc in query.documents) {
                        val order = doc.toObject(Order::class.java) ?: continue
                        if ((now - order.timestamp) > threeMinutes && !escalatedOrders.contains(order.id)) {
                            escalatedOrders.add(order.id)
                            Log.w("NotificationSyncService", "SLA BREACH: Order ${order.id} unaccepted after 3 mins. Escalating to Admin/Dark Store!")

                            val escalationData = mapOf(
                                "type" to "SLA_ESCALATION",
                                "targetRole" to "ADMIN",
                                "title" to "⚠️ URGENT: Order SLA Escalation",
                                "body" to "Order #${order.id} from ${order.customerName} (₹${order.totalAmount}) not accepted after 3 mins!",
                                "orderId" to order.id,
                                "timestamp" to now,
                                "status" to "pending"
                            )
                            firestore.collection("notifications").add(escalationData)

                            val email = currentUserProfile?.email?.lowercase() ?: ""
                            val isAdmin = OrderNotificationHelper.MASTER_ADMIN_EMAILS.contains(email) || currentUserProfile?.userRole?.name == "ADMIN"
                            val isAssignedDarkStore = currentUserProfile?.darkStoreId?.isNotEmpty() == true && currentUserProfile?.darkStoreId == order.merchantId

                            if (isAdmin || isAssignedDarkStore) {
                                triggerCallScreen(order)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NotificationSyncService", "Error in SLA Escalation Monitor", e)
                }
            }
        }
    }

    private fun observeNewOrders() {
        if (orderListener != null) return // Already observing

        val currentUser = auth.currentUser ?: return

        fetchAndObserveUserProfile(currentUser.uid) {
            startSlaEscalationMonitor()

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
                                val profile = currentUserProfile
                                val shouldNotify = OrderNotificationHelper.shouldNotifyUserForOrder(profile, order)
                                if (shouldNotify) {
                                    Log.d("NotificationSyncService", "New Order Match for User ${profile?.uid} (${profile?.userRole}): ${order.id}")
                                    triggerCallScreen(order)
                                } else {
                                    Log.d("NotificationSyncService", "Skipping Order ${order.id} for User ${profile?.uid} - Not item/store owner.")
                                }
                            }
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

        val profile = currentUserProfile
        val currentUid = profile?.uid ?: ""
        val darkStoreId = profile?.darkStoreId ?: ""

        val merchantItems = order.items.filter {
            it.product.merchantId == currentUid ||
            order.merchantId == currentUid ||
            (darkStoreId.isNotEmpty() && (it.product.merchantId == darkStoreId || order.merchantId == darkStoreId))
        }

        val relevantItems = if (merchantItems.isNotEmpty()) merchantItems else order.items
        val itemSummary = relevantItems.joinToString(", ") { "${it.quantity}x ${it.product.name}" }
        val itemCount = relevantItems.sumOf { it.quantity }.toString()

        val fullScreenIntent = Intent(this, IncomingOrderActivity::class.java).apply {
            putExtra("orderId", order.id)
            putExtra("customerName", order.customerName)
            putExtra("amount", order.totalAmount.toString())
            putExtra("itemSummary", itemSummary)
            putExtra("itemCount", itemCount)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 1001, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("NEW ORDER RECEIVED!")
            .setContentText("From ${order.customerName} - ₹${order.totalAmount} ($itemCount items)")
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
