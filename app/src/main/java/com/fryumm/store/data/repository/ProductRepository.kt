package com.fryumm.store.data.repository

import com.fryumm.store.data.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    // Read products with REALTIME SNAPSHOT LISTENER
    fun getProducts(usePublicCatalog: Boolean = false): Flow<Result<List<Product>>> = callbackFlow {
        val collectionName = if (usePublicCatalog) "public_products" else "products"
        val listener = firestore.collection(collectionName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("ProductRepository", "Realtime products listener error: ${error.message}", error)
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val products = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java) ?: doc.toProductSafe()
                    } catch (e: Exception) {
                        doc.toProductSafe()
                    }
                }
                android.util.Log.d("ProductRepository", "getProducts realtime: loaded ${products.size} products")
                trySend(Result.success(products))
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getProductById(productId: String, usePublicCatalog: Boolean = false): Result<Product?> {
        return try {
            val collectionName = if (usePublicCatalog) "public_products" else "products"
            val snapshot = firestore.collection(collectionName).document(productId).get().await()
            val product = try {
                snapshot.toObject(Product::class.java) ?: snapshot.toProductSafe()
            } catch (e: Exception) {
                snapshot.toProductSafe()
            }
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Call Cloud Function: getPendingProductsForReview (Secure Server-Side Read Path for Approval Queue)
    suspend fun getPendingProductsForReview(): Result<List<Product>> {
        return try {
            val result: HttpsCallableResult = functions
                .getHttpsCallable("getPendingProductsForReview")
                .call()
                .await()

            val dataMap = result.data as? Map<*, *>
            val rawList = dataMap?.get("products") as? List<*> ?: emptyList<Any?>()
            android.util.Log.d("ProductRepository", "getPendingProductsForReview: received ${rawList.size} raw items")

            val products = rawList.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                try {
                    Product(
                        id = (map["id"] as? String) ?: "",
                        name = (map["name"] as? String) ?: "",
                        brand = (map["brand"] as? String) ?: "",
                        description = (map["description"] as? String) ?: "",
                        price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                        originalPrice = (map["originalPrice"] as? Number)?.toDouble() ?: 0.0,
                        imageUrl = (map["imageUrl"] as? String) ?: "",
                        category = (map["category"] as? String) ?: "",
                        subCategory = (map["subCategory"] as? String) ?: "",
                        stockQuantity = (map["stockQuantity"] as? Number)?.toInt() ?: 0,
                        merchantId = (map["merchantId"] as? String) ?: "",
                        merchantName = (map["merchantName"] as? String) ?: "",
                        darkStoreId = map["darkStoreId"] as? String,
                        darkHubId = (map["darkHubId"] as? String) ?: "",
                        isVerified = (map["isVerified"] as? Boolean) ?: false,
                        finalApproval = (map["finalApproval"] as? Boolean) ?: false,
                        publishingStatus = (map["publishingStatus"] as? String) ?: "DRAFT",
                        revBilling = (map["revBilling"] as? Number)?.toInt() ?: 1,
                        revLegal = (map["revLegal"] as? Number)?.toInt() ?: 1,
                        revQuality = (map["revQuality"] as? Number)?.toInt() ?: 1,
                        revInventory = (map["revInventory"] as? Number)?.toInt() ?: 1,
                        approvedRevBilling = (map["approvedRevBilling"] as? Number)?.toInt() ?: 0,
                        approvedRevLegal = (map["approvedRevLegal"] as? Number)?.toInt() ?: 0,
                        approvedRevQuality = (map["approvedRevQuality"] as? Number)?.toInt() ?: 0,
                        approvedRevInventory = (map["approvedRevInventory"] as? Number)?.toInt() ?: 0,
                        deliveryCharge = (map["deliveryCharge"] as? Number)?.toDouble() ?: 0.0,
                        taxRate = (map["taxRate"] as? Number)?.toDouble() ?: 5.0
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ProductRepository", "Failed to map product item", e)
                    null
                }
            }
            android.util.Log.d("ProductRepository", "getPendingProductsForReview: successfully mapped ${products.size} products")
            Result.success(products)
        } catch (e: com.google.firebase.functions.FirebaseFunctionsException) {
            android.util.Log.e("ProductRepository", "FirebaseFunctionsException in getPendingProductsForReview: code=${e.code}, message=${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "Exception in getPendingProductsForReview", e)
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
                    "sequenceNumber" to product.sequenceNumber,
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
                "sequenceNumber" to product.sequenceNumber,
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

fun com.google.firebase.firestore.DocumentSnapshot.toProductSafe(): Product {
    val map = this.data ?: return Product(id = this.id)

    fun getDouble(key: String, fallbackKey: String? = null): Double {
        val v = map[key] ?: (if (fallbackKey != null) map[fallbackKey] else null)
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    fun getInt(key: String, fallbackKey: String? = null): Int {
        val v = map[key] ?: (if (fallbackKey != null) map[fallbackKey] else null)
        return when (v) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: 0
            else -> 0
        }
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        val v = map[key]
        return when (v) {
            is Boolean -> v
            is String -> v.toBoolean()
            else -> default
        }
    }

    fun getString(key: String, fallbackKey: String? = null): String {
        val v = map[key] ?: (if (fallbackKey != null) map[fallbackKey] else null)
        return v?.toString() ?: ""
    }

    val nameStr = getString("name", "title")
    val priceVal = getDouble("price", "sellingPrice")
    val origPriceVal = getDouble("originalPrice", "mrp")
    val imgUrlStr = getString("imageUrl", "image")

    val base = Product(
        id = this.id,
        name = nameStr,
        brand = getString("brand"),
        description = getString("description"),
        shortDescription = getString("shortDescription"),
        price = priceVal,
        originalPrice = if (origPriceVal > 0.0) origPriceVal else priceVal,
        imageUrl = imgUrlStr,
        images = (map["images"] as? List<*>)?.mapNotNull { it?.toString() } ?: (if (imgUrlStr.isNotBlank()) listOf(imgUrlStr) else emptyList()),
        category = getString("category"),
        subCategory = getString("subCategory"),
        stockQuantity = getInt("stockQuantity", "stockCount"),
        stockCount = getInt("stockCount", "stockQuantity"),
        isAvailable = getBoolean("isAvailable", true),
        merchantId = getString("merchantId", "sellerId"),
        merchantName = getString("merchantName", "sellerName"),
        darkStoreId = map["darkStoreId"]?.toString(),
        darkHubId = getString("darkHubId")
    )

    base.isInStock = getBoolean("isInStock", getBoolean("inStock", true))

    return base.copy(
        finalApproval = getBoolean("finalApproval", true),
        publishingStatus = getString("publishingStatus").ifEmpty { "PUBLISHED" },
        isVerified = getBoolean("isVerified", true),
        verifiedBy = getString("verifiedBy"),
        unit = getString("unit", "quantity").ifEmpty { "1 unit" },
        weight = getString("weight"),
        basePrice = getDouble("basePrice", "sellingPrice"),
        merchantRate = getDouble("merchantRate"),
        discountValue = getDouble("discountValue"),
        preorderStart = (map["preorderStart"] as? Number)?.toLong(),
        preorderEnd = (map["preorderEnd"] as? Number)?.toLong(),
        flashSale = getBoolean("flashSale", getBoolean("isFlashSale", false)),
        saleStart = (map["saleStart"] as? Number)?.toLong(),
        saleEnd = (map["saleEnd"] as? Number)?.toLong(),
        discountPercent = getDouble("discountPercent", "discountPercentage")
    )
}
