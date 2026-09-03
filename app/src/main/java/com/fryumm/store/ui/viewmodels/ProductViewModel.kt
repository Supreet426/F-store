package com.fryumm.store.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.data.model.Product
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.data.repository.NotificationRepository
import com.fryumm.store.data.repository.ProductRepository
import com.fryumm.store.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    application: Application,
    private val productRepository: ProductRepository,
    private val storageRepository: StorageRepository,
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AndroidViewModel(application) {

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowSuccess(val message: String) : UiEvent()
    }

    var products by mutableStateOf<List<Product>>(emptyList())
        private set

    private var rawProducts = emptyList<Product>()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        observeProducts()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.getProducts().collect { list ->
                rawProducts = list
                filterProducts(null)
            }
        }
    }

    fun filterProducts(currentUserProfile: UserProfile?, masterAdmins: List<String> = emptyList()) {
        val profile = currentUserProfile
        if (profile == null) {
            products = rawProducts
            return
        }
        val isAdmin = profile.userRole == UserRole.ADMIN || profile.email.lowercase().trim() in masterAdmins

        products = if (isAdmin) {
            rawProducts
        } else {
            rawProducts.filter { it.sellerId == profile.uid }
        }.sortedBy { it.sequenceNumber }
    }

    fun addProduct(
        product: Product,
        isMasterAdmin: Boolean,
        currentUserProfile: UserProfile?,
        allUsers: List<UserProfile>,
        masterAdmins: List<String>
    ) {
        val isAdmin = isMasterAdmin || currentUserProfile?.userRole == UserRole.ADMIN
        val finalProduct = product.copy(
            isVerified = isAdmin,
            isAvailable = isAdmin
        )
        viewModelScope.launch {
            productRepository.addProduct(finalProduct)
            if (!isAdmin) {
                _uiEvent.emit(UiEvent.ShowError("Product submitted. It will be live after Admin verification."))

                allUsers.filter { user ->
                    user.userRole == UserRole.ADMIN || masterAdmins.contains(user.email.lowercase().trim())
                }.forEach { admin ->
                    notificationRepository.sendNotification(
                        userId = admin.uid,
                        title = "New Product Approval Request",
                        body = "${currentUserProfile?.name ?: "Merchant"} added '${product.name}'. Please review.",
                        actionUrl = "stores"
                    )
                }
            }
        }
    }

    fun updateProduct(
        product: Product,
        isMasterAdmin: Boolean,
        currentUserProfile: UserProfile?,
        allUsers: List<UserProfile>,
        masterAdmins: List<String>
    ) {
        viewModelScope.launch {
            try {
                val existing = rawProducts.find { it.id == product.id }
                val isPriceAffected = existing != null && (
                    existing.basePrice != product.basePrice ||
                    existing.price != product.price ||
                    existing.deliveryCharge != product.deliveryCharge ||
                    existing.freeDeliveryMinAmount != product.freeDeliveryMinAmount ||
                    existing.freeDeliveryMinQuantity != product.freeDeliveryMinQuantity ||
                    existing.minOrderQuantity != product.minOrderQuantity ||
                    existing.taxRate != product.taxRate ||
                    existing.cgstRate != product.cgstRate ||
                    existing.sgstRate != product.sgstRate
                )

                val updatedProduct = if (isPriceAffected && !isMasterAdmin && currentUserProfile?.userRole != UserRole.ADMIN) {
                    allUsers.filter { user ->
                        user.userRole == UserRole.ADMIN || masterAdmins.contains(user.email.lowercase().trim())
                    }.forEach { admin ->
                        notificationRepository.sendNotification(
                            userId = admin.uid,
                            title = "Product Price Update: Re-verification Needed",
                            body = "${currentUserProfile?.name ?: "Merchant"} updated '${product.name}'. Please re-approve.",
                            actionUrl = "stores"
                        )
                    }
                    product.copy(isVerified = false, isAvailable = false)
                } else {
                    product.copy(isInStock = product.stockQuantity > 0)
                }
                productRepository.updateProduct(updatedProduct)

                if (isPriceAffected && !isMasterAdmin && currentUserProfile?.userRole != UserRole.ADMIN) {
                    _uiEvent.emit(UiEvent.ShowError("Price changed. Product hidden until Admin re-verifies."))
                } else {
                    _uiEvent.emit(UiEvent.ShowSuccess("Product updated successfully!"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to update product: ${e.message}"))
            }
        }
    }

    fun updateProductSequence(productId: String, newSequence: Int) {
        viewModelScope.launch {
            try {
                firestore.collection("products").document(productId)
                    .update("sequenceNumber", newSequence).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Sequence updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to update sequence"))
            }
        }
    }

    fun bulkUpdateProductSequences(updatedProducts: List<Product>) {
        viewModelScope.launch {
            try {
                val batch = firestore.batch()
                updatedProducts.forEach { product ->
                    val docRef = firestore.collection("products").document(product.id)
                    batch.update(docRef, "sequenceNumber", product.sequenceNumber)
                }
                batch.commit().await()
                _uiEvent.emit(UiEvent.ShowSuccess("All product sequences saved"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to update product sequences: ${e.message}"))
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch { productRepository.deleteProduct(productId) }
    }

    fun approveProduct(
        productId: String,
        commission: Double,
        profitPercent: Double,
        deliveryCharge: Double,
        gstRate: Double
    ) {
        viewModelScope.launch {
            try {
                firestore.collection("products").document(productId).update(
                    mapOf(
                        "isVerified" to true,
                        "isAvailable" to true,
                        "verifiedBy" to (auth.currentUser?.uid ?: "admin"),
                        "verificationDate" to System.currentTimeMillis(),
                        "commissionRate" to commission,
                        "adminProfitPercent" to profitPercent,
                        "deliveryCharge" to deliveryCharge,
                        "taxRate" to gstRate,
                        "cgstRate" to gstRate / 2.0,
                        "sgstRate" to gstRate / 2.0
                    )
                ).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Product Approved with configured margins"))
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Failed to approve product", e)
                _uiEvent.emit(UiEvent.ShowError("Approval Failed"))
            }
        }
    }

    fun approveAllProducts(currentUserProfile: UserProfile?) {
        viewModelScope.launch {
            try {
                val validProducts = rawProducts.filter { it.id.isNotBlank() }
                val pendingList = validProducts.filter { !it.isVerified || !it.isAvailable || !it.isInStock }
                val targetList = if (pendingList.isNotEmpty()) pendingList else validProducts

                if (targetList.isEmpty()) {
                    _uiEvent.emit(UiEvent.ShowSuccess("All products are already approved!"))
                    return@launch
                }

                _uiEvent.emit(UiEvent.ShowSuccess("Approving & publishing ${targetList.size} products..."))

                val sellerId = currentUserProfile?.uid.takeIf { !it.isNullOrBlank() } ?: "seller_1"
                val sellerName = currentUserProfile?.name.takeIf { !it.isNullOrBlank() } ?: "Flash-O Seller"

                targetList.chunked(200).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { product ->
                        val docRef = firestore.collection("products").document(product.id)
                        val updates = mutableMapOf<String, Any>(
                            "isVerified" to true,
                            "isAvailable" to true,
                            "isInStock" to true,
                            "inStock" to true,
                            "verifiedBy" to (auth.currentUser?.uid ?: "admin"),
                            "verificationDate" to System.currentTimeMillis()
                        )
                        if (product.sellerId.isBlank() || product.sellerId == "seller_1") {
                            updates["sellerId"] = sellerId
                            updates["sellerName"] = sellerName
                        }
                        if (product.stockQuantity <= 0) {
                            updates["stockQuantity"] = 50
                            updates["stockCount"] = 50
                        }
                        batch.set(docRef, updates, com.google.firebase.firestore.SetOptions.merge())
                    }
                    batch.commit().await()
                }

                _uiEvent.emit(UiEvent.ShowSuccess("Successfully approved & published ${targetList.size} products!"))
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Failed to approve all products", e)
                _uiEvent.emit(UiEvent.ShowError("Bulk Approval Failed: ${e.message}"))
            }
        }
    }

    suspend fun uploadProductImage(uri: Uri): String? {
        return storageRepository.uploadImage(uri, "products/${UUID.randomUUID()}")
    }
}
