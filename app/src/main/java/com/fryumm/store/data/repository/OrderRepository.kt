package com.fryumm.store.data.repository

import com.fryumm.store.data.model.Order
import com.fryumm.store.data.model.OrderStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val ordersCollection = firestore.collection("orders")

    fun getNewOrders(): Flow<List<Order>> = callbackFlow {
        val subscription = ordersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("OrderRepository", "Firestore Error: ${error.message}")
                    trySend(emptyList()) // Send empty list instead of closing with error
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val ordersList = mutableListOf<Order>()
                for (doc in snapshot.documents) {
                    try {
                        val order = try { 
                            doc.toObject(Order::class.java) 
                        } catch (e: Exception) { 
                            android.util.Log.e("OrderRepository", "toObject failed for ${doc.id}, falling back", e)
                            null 
                        }
                        
                        if (order != null) {
                            ordersList.add(order.copy(id = doc.id))
                        } else {
                            // Manual fallback with more field options
                            val id = doc.id
                            val status = doc.getString("status") ?: doc.getString("orderStatus") ?: "PLACED"
                            val customerName = doc.getString("customerName") ?: doc.getString("userName") ?: doc.getString("name") ?: "Unknown"
                            val amount = doc.getDouble("totalAmount") ?: doc.getDouble("totalPrice") ?: doc.getDouble("amount") ?: doc.getDouble("total") ?: 0.0
                            val time = doc.getLong("timestamp") ?: doc.getLong("date") ?: System.currentTimeMillis()
                            val sellerId = doc.getString("sellerId") ?: doc.getString("merchantId") ?: doc.getString("storeId") ?: "seller_1"
                            
                            ordersList.add(Order(
                                id = id,
                                status = status,
                                customerName = customerName,
                                totalAmount = amount,
                                timestamp = if (time > 0) time else System.currentTimeMillis(),
                                sellerId = sellerId
                            ))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OrderRepository", "Critical parsing error for ${doc.id}", e)
                    }
                }
                trySend(ordersList.sortedByDescending { it.timestamp })
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateOrderFields(orderId: String, fields: Map<String, Any?>): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update(fields).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
