package com.fryumm.store.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fryumm.store.data.model.Product
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.data.repository.NotificationRepository
import com.fryumm.store.data.repository.ProductRepository
import com.fryumm.store.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    application: Application,
    private val productRepository: ProductRepository,
    private val storageRepository: StorageRepository,
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth
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
            productRepository.getProducts().collect { result ->
                result.onSuccess { list ->
                    rawProducts = list
                    filterProducts(null)
                }.onFailure { e ->
                    Log.e("ProductViewModel", "Error reading products", e)
                }
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
            rawProducts.filter { it.merchantId == profile.uid }
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
        val merchantId = currentUserProfile?.uid

        viewModelScope.launch {
            val result = productRepository.addProduct(product, merchantId = merchantId)
            result.onSuccess { createdId ->
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
                } else {
                    _uiEvent.emit(UiEvent.ShowSuccess("Product draft created successfully! (ID: $createdId)"))
                }
            }.onFailure { e ->
                _uiEvent.emit(UiEvent.ShowError("Failed to create product: ${e.message}"))
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
                val updateData = mapOf(
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
                    "preorderEnd" to product.preorderEnd
                )

                val result = productRepository.updateProduct(product.id, updateData)
                result.onSuccess {
                    _uiEvent.emit(UiEvent.ShowSuccess("Product draft updated successfully!"))
                }.onFailure { e ->
                    _uiEvent.emit(UiEvent.ShowError("Failed to update product: ${e.message}"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to update product: ${e.message}"))
            }
        }
    }

    fun updateProductSequence(productId: String, newSequence: Int) {
        viewModelScope.launch {
            val result = productRepository.updateProduct(productId, mapOf("sequenceNumber" to newSequence))
            result.onSuccess {
                _uiEvent.emit(UiEvent.ShowSuccess("Sequence updated"))
            }.onFailure { e ->
                _uiEvent.emit(UiEvent.ShowError("Failed to update sequence: ${e.message}"))
            }
        }
    }

    fun bulkUpdateProductSequences(updatedProducts: List<Product>) {
        viewModelScope.launch {
            var successCount = 0
            updatedProducts.forEach { product ->
                val res = productRepository.updateProduct(product.id, mapOf("sequenceNumber" to product.sequenceNumber))
                if (res.isSuccess) successCount++
            }
            if (successCount > 0) {
                _uiEvent.emit(UiEvent.ShowSuccess("Saved sequence for $successCount products"))
            } else {
                _uiEvent.emit(UiEvent.ShowError("Failed to update product sequences"))
            }
        }
    }

    fun reviewProductDepartment(productId: String, department: String, approved: Boolean) {
        viewModelScope.launch {
            val result = productRepository.reviewProductDepartment(productId, department, approved)
            result.onSuccess {
                _uiEvent.emit(UiEvent.ShowSuccess("Department '$department' review recorded."))
            }.onFailure { e ->
                _uiEvent.emit(UiEvent.ShowError("Department review failed: ${e.message}"))
            }
        }
    }

    fun approveProduct(
        productId: String,
        commission: Double,
        profitPercent: Double,
        deliveryCharge: Double,
        gstRate: Double
    ) {
        viewModelScope.launch {
            val result = productRepository.grantFinalApproval(
                productId = productId,
                commissionRate = commission,
                taxRate = gstRate
            )
            result.onSuccess {
                _uiEvent.emit(UiEvent.ShowSuccess("Product Final Approval Granted"))
            }.onFailure { e ->
                Log.e("ProductViewModel", "Failed to approve product", e)
                _uiEvent.emit(UiEvent.ShowError("Approval Failed: ${e.message}"))
            }
        }
    }

    fun grantAdminOverride(productId: String, overrideReason: String) {
        viewModelScope.launch {
            val result = productRepository.grantAdminOverride(productId, overrideReason)
            result.onSuccess {
                _uiEvent.emit(UiEvent.ShowSuccess("Master Admin Override Granted! Product is LIVE."))
            }.onFailure { e ->
                _uiEvent.emit(UiEvent.ShowError("Admin Override Failed: ${e.message}"))
            }
        }
    }

    fun publishProduct(productId: String) {
        viewModelScope.launch {
            val result = productRepository.publishProduct(productId)
            result.onSuccess {
                _uiEvent.emit(UiEvent.ShowSuccess("Product published successfully to catalog!"))
            }.onFailure { e ->
                _uiEvent.emit(UiEvent.ShowError("Publish Failed: ${e.message}"))
            }
        }
    }

    fun approveAllProducts(currentUserProfile: UserProfile?) {
        viewModelScope.launch {
            try {
                val pendingList = rawProducts.filter { !it.finalApproval || it.publishingStatus != "PUBLISHED" }
                val targetList = if (pendingList.isNotEmpty()) pendingList else rawProducts

                if (targetList.isEmpty()) {
                    _uiEvent.emit(UiEvent.ShowSuccess("All products are already approved!"))
                    return@launch
                }

                _uiEvent.emit(UiEvent.ShowSuccess("Granting Admin Override for ${targetList.size} products..."))

                var approvedCount = 0
                targetList.forEach { product ->
                    val res = productRepository.grantAdminOverride(product.id, "Bulk Admin Approval")
                    if (res.isSuccess) {
                        productRepository.publishProduct(product.id)
                        approvedCount++
                    }
                }

                _uiEvent.emit(UiEvent.ShowSuccess("Successfully approved & published $approvedCount products!"))
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Failed to approve all products", e)
                _uiEvent.emit(UiEvent.ShowError("Bulk Approval Failed: ${e.message}"))
            }
        }
    }

    fun reservePreorderItem(productId: String, quantity: Int = 1) {
        viewModelScope.launch {
            val result = productRepository.reservePreorderItem(productId, quantity)
            result.onSuccess {
                _uiEvent.emit(UiEvent.ShowSuccess("Pre-order spot reserved!"))
            }.onFailure { e ->
                _uiEvent.emit(UiEvent.ShowError("Pre-order reservation failed: ${e.message}"))
            }
        }
    }

    suspend fun uploadProductImage(uri: Uri): String? {
        return storageRepository.uploadImage(uri, "products/${UUID.randomUUID()}")
    }
}
