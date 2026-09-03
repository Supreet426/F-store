package com.fryumm.store.data.repository

import android.util.Log
import com.fryumm.store.data.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val productsCollection = firestore.collection("products")

    fun getProducts(): Flow<List<Product>> = callbackFlow {
        val subscription = productsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("StoreRepository", "Error fetching products", error)
                trySend(emptyList()) // Send empty list instead of closing with error
                return@addSnapshotListener
            }
            val products = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("StoreRepository", "Parsing error for ${doc.id}", e)
                    null
                }
            } ?: emptyList()
            Log.d("StoreRepository", "Read ${products.size} products from Firestore with IDs")
            trySend(products)
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addProduct(product: Product): Result<Unit> {
        return try {
            val docRef = if (product.id.isEmpty()) {
                productsCollection.document()
            } else {
                productsCollection.document(product.id)
            }
            val finalProduct = product.copy(id = docRef.id)
            Log.d("StoreRepository", "Attempting to write product: ${product.name}")
            docRef.set(finalProduct).await()
            Log.d("StoreRepository", "SUCCESS: Product written to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StoreRepository", "FAILED to write product", e)
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            productsCollection.document(product.id).set(product).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            productsCollection.document(productId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reserveStock(productId: String, variantId: String?, quantity: Int): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val docRef = productsCollection.document(productId)
                val snapshot = transaction.get(docRef)
                val product = snapshot.toObject(Product::class.java)
                    ?: throw IllegalStateException("Product not found")

                if (variantId.isNullOrEmpty()) {
                    val currentStock = product.stockQuantity
                    if (currentStock < quantity) {
                        throw IllegalStateException("Insufficient stock for ${product.name}")
                    }
                    val newStock = currentStock - quantity
                    transaction.update(docRef, mapOf(
                        "stockQuantity" to newStock,
                        "isInStock" to (newStock > 0)
                    ))
                } else {
                    val updatedVariants = product.variants.map { variant ->
                        if (variant.id == variantId) {
                            if (variant.stockQuantity < quantity) {
                                throw IllegalStateException("Insufficient stock for variant ${variant.title}")
                            }
                            variant.copy(stockQuantity = variant.stockQuantity - quantity)
                        } else variant
                    }
                    transaction.update(docRef, "variants", updatedVariants)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Stock reservation failed", e)
            Result.failure(e)
        }
    }
}
