package com.fryumm.store.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun updateToken(userId: String, role: String) {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("NotificationRepo", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                if (token != null) {
                    saveTokenToFirestore(userId, role, token)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Firebase Messaging not initialized", e)
        }
    }

    private fun saveTokenToFirestore(userId: String, role: String, token: String) {
        if (userId.isEmpty()) {
            Log.e("NotificationRepo", "Cannot save token: userId is empty")
            return
        }

        val collection = when (role) {
            "customer" -> "users"
            "seller" -> "sellers"
            "delivery" -> "riders"
            else -> "users"
        }

        firestore.collection(collection).document(userId)
            .update("fcmToken", token)
            .addOnFailureListener { e ->
                // If document doesn't exist, try to set it (merge)
                firestore.collection(collection).document(userId)
                    .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                    .addOnFailureListener { e2 ->
                        Log.e("NotificationRepo", "Failed to save token", e2)
                    }
            }
    }

    suspend fun sendOrderCallNotification(targetUserId: String, orderId: String, customerName: String, amount: Double) {
        try {
            // In a real production app, this would be a call to a Cloud Function.
            // For your ecosystem, we will write to a 'notifications' collection 
            // that a Cloud Function can listen to, or send via FCM REST API.
            
            val notificationData = mapOf(
                "targetUid" to targetUserId,
                "type" to "ORDER_CALL",
                "priority" to "high",
                "data" to mapOf(
                    "orderId" to orderId,
                    "customerName" to customerName,
                    "amount" to amount.toString()
                ),
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            
            firestore.collection("notifications").add(notificationData).await()
            Log.d("NotificationRepo", "Order call notification queued for $targetUserId")
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error triggering notification", e)
        }
    }

    suspend fun sendNotification(userId: String, title: String, body: String, actionUrl: String? = null) {
        try {
            val notification = mapOf(
                "userId" to userId,
                "title" to title,
                "body" to body,
                "actionUrl" to actionUrl,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            firestore.collection("notifications").add(notification).await()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error sending notification", e)
        }
    }

    suspend fun sendBroadcastNotification(title: String, body: String, imageUrl: String? = null, actionUrl: String? = null, target: String = "ALL_CUSTOMERS") {
        try {
            val notification = mapOf(
                "type" to "BROADCAST",
                "target" to target,
                "title" to title,
                "body" to body,
                "imageUrl" to imageUrl,
                "actionUrl" to actionUrl,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            firestore.collection("notifications").add(notification).await()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error sending broadcast", e)
        }
    }
}
