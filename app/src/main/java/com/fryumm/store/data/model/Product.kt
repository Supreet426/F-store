package com.fryumm.store.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Product(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val description: String = "",
    val shortDescription: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val imageUrl: String = "",
    val images: List<String> = emptyList(),
    val category: String = "",
    val subCategory: String = "",
    val stockQuantity: Int = 0,
    val stockCount: Int = 0,
    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var isAvailable: Boolean = true,
    @get:PropertyName("isInStock")
    @set:PropertyName("isInStock")
    var isInStock: Boolean = true,

    // Canonical Ownership Fields
    val merchantId: String = "",
    val merchantName: String = "",
    val darkStoreId: String? = null,
    val darkHubId: String = "",

    // Revision Control
    val revBilling: Int = 1,
    val revLegal: Int = 1,
    val revQuality: Int = 1,
    val revInventory: Int = 1,
    val approvedRevBilling: Int = 0,
    val approvedRevLegal: Int = 0,
    val approvedRevQuality: Int = 0,
    val approvedRevInventory: Int = 0,

    // Workflow & Approval
    val finalApproval: Boolean = true, // Legacy compatibility default
    val publishingStatus: String = "PUBLISHED", // "DRAFT", "PUBLISHED", "ARCHIVED" (Legacy compatibility default)
    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    var isVerified: Boolean = true, // Legacy compatibility default
    val verifiedBy: String = "",
    val verificationDate: Long = 0,

    // Override Audit
    val isAdminOverride: Boolean = false,
    val overrideReason: String? = null,
    val overrideBy: String? = null,
    val overrideAt: Long? = null,

    // Preorder & Flash Sale Fields
    @get:PropertyName("isPreorderEnabled")
    @set:PropertyName("isPreorderEnabled")
    var isPreorderEnabled: Boolean = false,
    val currentPreorderCount: Int = 0,
    val maxPreorderCapacity: Int = 0,
    val preorderStart: Long? = null,
    val preorderEnd: Long? = null,
    @get:PropertyName("flashSale")
    @set:PropertyName("flashSale")
    var flashSale: Boolean = false,
    val saleStart: Long? = null,
    val saleEnd: Long? = null,
    val discountPercent: Double = 0.0,

    // Warehouse & Taxes
    val aisle: String = "",
    val rack: String = "",
    val barcode: String = "",
    val hsnCode: String = "",
    val taxRate: Double = 5.0,
    val cgstRate: Double = 2.5,
    val sgstRate: Double = 2.5,
    val igstRate: Double = 0.0,
    val isTaxInclusive: Boolean = true,
    val handlingFee: Double = 0.0,
    val commissionRate: Double = 10.0,
    val adminProfitPercent: Double = 10.0,
    val deliveryCharge: Double = 0.0,
    val unit: String = "1 unit",
    val weight: String = "",
    val basePrice: Double = 0.0,
    val merchantRate: Double = 0.0,
    val discountValue: Double = 0.0,
    val minStockLevel: Int = 5,
    val minOrderQuantity: Int = 1,
    val unitType: String = "Weight",
    val measurementUnit: String = "g",
    @get:PropertyName("isComingSoon")
    @set:PropertyName("isComingSoon")
    var isComingSoon: Boolean = false,
    val expiryDate: Long? = null,
    val deliveryChargeIncluded: Boolean = true,
    val freeDeliveryMinAmount: Double = 0.0,
    val freeDeliveryMinQuantity: Int = 0,
    val legalDisclaimer: String = "",
    val fssaiNumber: String = "",
    val nutritionalInfo: String = "",
    val adminFeedback: String? = null,
    @get:PropertyName("isFoodProduct")
    @set:PropertyName("isFoodProduct")
    var isFoodProduct: Boolean = false,
    val variants: List<ProductVariant> = emptyList(),
    val availableCoupons: List<Coupon> = emptyList(),
    val specifications: Map<String, String> = emptyMap(),
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val searchNames: List<String> = emptyList(),
    val sequenceNumber: Int = 999,
    val dailySchedules: Map<String, DaySchedule> = emptyMap(),
    val vacationStart: Long? = null,
    val vacationEnd: Long? = null,
    val substituteProductIds: List<String> = emptyList(),
    val addedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProductVariant(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val stockQuantity: Int = 0,
    val attributes: Map<String, String> = emptyMap()
)

data class Coupon(
    val code: String = "",
    val discount: Double = 0.0,
    val description: String = "",
    val isPercentage: Boolean = false,
    val minOrderAmount: Double = 0.0
)
