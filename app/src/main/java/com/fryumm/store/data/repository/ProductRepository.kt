package com.fryumm.store.data.repository

import com.fryumm.store.data.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Read products from private or public catalog
    fun getProducts(usePublicCatalog: Boolean = false): Flow<Result<List<Product>>> = flow {
        try {
            val collectionName = if (usePublicCatalog) "public_products" else "products"
            val snapshot = firestore.collection(collectionName).get().await()
            val products = snapshot.toObjects(Product::class.java)
            emit(Result.success(products))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun getProductById(productId: String, usePublicCatalog: Boolean = false): Result<Product?> {
        return try {
            val collectionName = if (usePublicCatalog) "public_products" else "products"
            val snapshot = firestore.collection(collectionName).document(productId).get().await()
            Result.success(snapshot.toObject(Product::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Call Cloud Function: createProductDraft
    suspend fun addProduct(product: Product, merchantId: String? = null, darkStoreId: String? = null): Result<String> {
        return try {
            val currentUid = auth.currentUser?.uid
            val effectiveMerchantId = merchantId ?: product.merchantId ?: (if (product.darkStoreId == null) currentUid else null)
            val effectiveDarkStoreId = darkStoreId ?: product.darkStoreId

            val payload = hashMapOf<String, Any?>(
                "merchantId" to effectiveMerchantId,
                "darkStoreId" to effectiveDarkStoreId,
                "productData" to hashMapOf(
                    "name" to product.name,
                    "brand" to product.brand,
                    "description" to product.description,
                    "shortDescription" to product.shortDescription,
                    "price" to product.price,
                    "originalPrice" to product.originalPrice,
                    "imageUrl" to product.imageUrl,
                    "images" to product.images,
                    "category" to product.category,
                    "subCategory" to product.subCategory,
                    "stockQuantity" to product.stockQuantity,
                    "isInStock" to product.isInStock,
                    "aisle" to product.aisle,
                    "rack" to product.rack,
                    "barcode" to product.barcode,
                    "hsnCode" to product.hsnCode,
                    "taxRate" to product.taxRate,
                    "unit" to product.unit,
                    "weight" to product.weight,
                    "basePrice" to product.basePrice,
                    "merchantRate" to product.merchantRate,
                    "discountValue" to product.discountValue,
                    "legalDisclaimer" to product.legalDisclaimer,
                    "fssaiNumber" to product.fssaiNumber,
                    "isFoodProduct" to product.isFoodProduct,
                    "minOrderQuantity" to product.minOrderQuantity,
                    "isPreorderEnabled" to product.isPreorderEnabled,
                    "maxPreorderCapacity" to product.maxPreorderCapacity,
                    "preorderStart" to product.preorderStart,
                    "preorderEnd" to product.preorderEnd,
                ),
            )

            val result: HttpsCallableResult = functions
                .getHttpsCallable("createProductDraft")
                .call(payload)
                .await()

            @Suppress("UNCHECKED_CAST")
            val dataMap = result.data as? Map<String, Any?>
            val createdId = (dataMap?.get("productId") as? String) ?: ""
            Result.success(createdId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Call Cloud Function: updateProductDraft
    suspend fun updateProduct(productId: String, updateData: Map<String, Any?>): Result<Boolean> {
        return try {
            val payload = hashMapOf(
                "productId" to productId,
                "updateData" to updateData,
            )
            functions.getHttpsCallable("updateProductDraft").call(payload).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Boolean> {
        return updateProduct(
            productId = product.id,
            updateData = mapOf(
                "name" to product.name,
                "brand" to product.brand,
                "description" to product.description,
                "shortDescription" to product.shortDescription,
                "price" to product.price,
                "originalPrice" to product.originalPrice,
                "imageUrl" to product.imageUrl,
                "images" to product.images,
                "category" to product.category,
                "subCategory" to product.subCategory,
                "stockQuantity" to product.stockQuantity,
                "isInStock" to product.isInStock,
                "isAvailable" to product.isAvailable,
                "aisle" to product.aisle,
                "rack" to product.rack,
                "barcode" to product.barcode,
                "hsnCode" to product.hsnCode,
                "taxRate" to product.taxRate,
                "unit" to product.unit,
                "weight" to product.weight,
                "basePrice" to product.basePrice,
                "merchantRate" to product.merchantRate,
                "discountValue" to product.discountValue,
                "legalDisclaimer" to product.legalDisclaimer,
                "fssaiNumber" to product.fssaiNumber,
                "isFoodProduct" to product.isFoodProduct,
                "minOrderQuantity" to product.minOrderQuantity,
                "isPreorderEnabled" to product.isPreorderEnabled,
                "maxPreorderCapacity" to product.maxPreorderCapacity,
                "preorderStart" to product.preorderStart,
                "preorderEnd" to product.preorderEnd,
            )
        )
    }

    suspend fun deleteProduct(productId: String): Result<Boolean> {
        return updateProduct(
            productId = productId,
            updateData = mapOf(
                "publishingStatus" to "ARCHIVED",
                "isAvailable" to false,
                "isInStock" to false
            )
        )
    }

    // Call Cloud Function: reviewProductDepartment
    suspend fun reviewProductDepartment(productId: String, department: String, approved: Boolean): Result<Boolean> {
        return try {
            val payload = hashMapOf(
                "productId" to productId,
                "department" to department,
                "approved" to approved,
            )
            functions.getHttpsCallable("reviewProductDepartment").call(payload).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Call Cloud Function: grantFinalApproval
    suspend fun grantFinalApproval(productId: String, commissionRate: Double? = null, taxRate: Double? = null): Result<Boolean> {
        return try {
            val payload = hashMapOf<String, Any?>(
                "productId" to productId,
                "commissionRate" to commissionRate,
                "taxRate" to taxRate,
            )
            functions.getHttpsCallable("grantFinalApproval").call(payload).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Call Cloud Function: grantAdminOverride
    suspend fun grantAdminOverride(productId: String, overrideReason: String): Result<Boolean> {
        return try {
            val payload = hashMapOf(
                "productId" to productId,
                "overrideReason" to overrideReason,
            )
            functions.getHttpsCallable("grantAdminOverride").call(payload).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Call Cloud Function: publishProduct
    suspend fun publishProduct(productId: String): Result<Boolean> {
        return try {
            val payload = hashMapOf("productId" to productId)
            functions.getHttpsCallable("publishProduct").call(payload).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Call Cloud Function: reservePreorderItem
    suspend fun reservePreorderItem(productId: String, quantity: Int = 1, idempotencyKey: String = UUID.randomUUID().toString()): Result<Boolean> {
        return try {
            val payload = hashMapOf(
                "productId" to productId,
                "quantity" to quantity,
                "idempotencyKey" to idempotencyKey,
            )
            functions.getHttpsCallable("reservePreorderItem").call(payload).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Call Cloud Function: cancelPreorderReservation
    suspend fun cancelPreorderReservation(reservationId: String): Result<Boolean> {
        return try {
            val payload = hashMapOf("reservationId" to reservationId)
            functions.getHttpsCallable("cancelPreorderReservation").call(payload).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
