package com.fryumm.store.data.repository

import android.util.Log
import com.fryumm.store.data.model.WalletTransaction
import com.fryumm.store.data.model.WalletTransactionType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val walletCollection = firestore.collection("wallet_transactions")
    private val usersCollection = firestore.collection("users")

    fun getWalletHistory(userId: String): Flow<List<WalletTransaction>> = callbackFlow {
        val query = walletCollection
            .whereEqualTo("userId", userId)
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("WalletRepository", "Error fetching wallet transactions", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(WalletTransaction::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            trySend(list.sortedByDescending { it.timestamp })
        }
        awaitClose { subscription.remove() }
    }

    suspend fun creditWallet(
        userId: String,
        userName: String,
        amount: Double,
        category: String,
        description: String,
        orderId: String? = null
    ): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val userRef = usersCollection.document(userId)
                val userSnap = transaction.get(userRef)
                val currentBalance = userSnap.getDouble("walletBalance") ?: 0.0
                val newBalance = currentBalance + amount

                transaction.update(userRef, "walletBalance", newBalance)

                val txRef = walletCollection.document()
                val tx = WalletTransaction(
                    id = txRef.id,
                    userId = userId,
                    userName = userName,
                    amount = amount,
                    type = WalletTransactionType.CREDIT,
                    category = category,
                    description = description,
                    orderId = orderId,
                    timestamp = System.currentTimeMillis()
                )
                transaction.set(txRef, tx)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WalletRepository", "Failed to credit wallet", e)
            Result.failure(e)
        }
    }

    suspend fun debitWallet(
        userId: String,
        userName: String,
        amount: Double,
        category: String,
        description: String,
        orderId: String? = null
    ): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val userRef = usersCollection.document(userId)
                val userSnap = transaction.get(userRef)
                val currentBalance = userSnap.getDouble("walletBalance") ?: 0.0
                if (currentBalance < amount) {
                    throw IllegalStateException("Insufficient wallet balance")
                }
                val newBalance = currentBalance - amount

                transaction.update(userRef, "walletBalance", newBalance)

                val txRef = walletCollection.document()
                val tx = WalletTransaction(
                    id = txRef.id,
                    userId = userId,
                    userName = userName,
                    amount = amount,
                    type = WalletTransactionType.DEBIT,
                    category = category,
                    description = description,
                    orderId = orderId,
                    timestamp = System.currentTimeMillis()
                )
                transaction.set(txRef, tx)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WalletRepository", "Failed to debit wallet", e)
            Result.failure(e)
        }
    }
}
