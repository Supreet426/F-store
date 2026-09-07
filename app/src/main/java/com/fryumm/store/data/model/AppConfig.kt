package com.fryumm.store.data.model

data class DynamicAppConfig(
    val priorityBrands: List<String> = emptyList(),
    val sponsoredBrands: List<SponsoredBrand> = emptyList(), // Feature 1
    val promotedSellers: List<String> = emptyList(), // Feature 3
    val categorySequence: List<String> = emptyList(),
    val homeSectionOrder: List<String> = emptyList(), // Removed defaults that cause flickering/glitches
    val cartSuggestions: List<RecommendationItem> = emptyList(), // Unlimited suggestions managed by Admin
    val timeBasedCategories: Map<String, List<String>> = emptyMap(), // Feature 2 (e.g. "07:00-10:00" -> ["Dairy", "Bakery"])
    val searchRetargeting: Map<String, String> = emptyMap(), // Feature 4 (e.g. "Organic Honey" -> "Dabur Honey")
    val bannerAds: List<BannerAd> = emptyList(),
    val productAds: List<ProductAd> = emptyList(),
    val videoAds: List<VideoAd> = emptyList(),
    val flashSale: FlashSale? = null,
    val flashOPass: FlashOPassConfig? = null,
    val smartCollections: List<SmartCollection> = emptyList(),
    val comingSoonCategories: List<String> = emptyList(),
    val searchBoostFactor: Double = 1.5,
    
    // Header Deals Button Config
    val dealsButtonText: String = "Flash-O Deals",
    val dealsButtonAction: String = "flash_sale", // flash_sale, trending, or category name
    val dealsButtonIcon: String = "https://cdn-icons-png.flaticon.com/512/706/706164.png",
    
    val popupNotification: PopupNotification = PopupNotification(),

    val referralConfig: ReferralConfig = ReferralConfig(),

    // Support Configuration
    val supportNumber: String = "9318312478",
    val supportEmail: String = "ranjanamishra1117@gmail.com",
    val supportWhatsApp: String = "9318312478",
    val isLiveChatEnabled: Boolean = true,

    // Merchant Referral Admin Config
    val merchantReferralMessage: String = "Order fresh food from Flash-O! Use my code {CODE} for exclusive offers.",
    val appDownloadLink: String = "https://play.google.com/store/apps/details?id=com.flash-o.customer",
    val referralDiscountPercent: Double = 50.0, // 50% off for referred customers
    val maxReferralDiscount: Double = 100.0,
    val weatherImpact: Double = 0.0,
    val eventImpact: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class AppUpdateInfo(
    val appId: String = "", // "customer", "partner", "store", "web", etc.
    val appName: String = "",
    val appDescription: String = "",
    val iconUrl: String = "",
    val category: String = "Ecosystem App",
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val downloadUrl: String = "",
    val playStoreUrl: String = "",
    val directApkUrl: String = "", // Google Drive or direct APK URL
    val githubUrl: String = "",
    val websiteUrl: String = "",
    val releaseNotes: String = "",
    val isMandatory: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getEffectiveDownloadUrl(): String {
        return directApkUrl.ifEmpty { downloadUrl.ifEmpty { playStoreUrl.ifEmpty { websiteUrl } } }
    }
}


data class FlashOPassTier(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val price: Double = 0.0,
    val minOrderForFreeDelivery: Double = 0.0,
    val durationDays: Int = 30, // New: Duration of the pass in days
    val benefits: List<String> = emptyList(),
    val imageUrl: String = "",
    val isActive: Boolean = true
)

data class FlashOPassConfig(
    val title: String = "Flash-O Pass",
    val tiers: List<FlashOPassTier> = listOf(
        FlashOPassTier(
            id = "basic_99",
            title = "Flash-O Pass 99",
            subtitle = "Free delivery on ₹199+",
            price = 99.0,
            minOrderForFreeDelivery = 199.0,
            benefits = listOf("Free Delivery on 1000+ items", "Standard Support")
        ),
        FlashOPassTier(
            id = "premium_199",
            title = "Flash-O Premium 199",
            subtitle = "Free delivery on ₹99+",
            price = 199.0,
            minOrderForFreeDelivery = 99.0,
            benefits = listOf("Free Delivery on 1000+ items", "Priority Delivery", "Max Discounts")
        )
    ),
    val isActive: Boolean = true
)

data class FlashSale(
    val title: String = "Flash Sale",
    val startTime: Long = 0,
    val endTime: Long = 0,
    val productIds: List<String> = emptyList(),
    val discountPercentage: Int = 0,
    val isActive: Boolean = true
)

data class SmartCollection(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "BESTSELLER", // BESTSELLER, TRENDING, NEW_ARRIVAL
    val productIds: List<String> = emptyList(),
    val isActive: Boolean = true
)

data class SponsoredBrand(
    val name: String = "",
    val bidAmount: Double = 0.0,
    val expiryDate: Long = 0,
    val isActive: Boolean = true
)

data class BannerAd(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val actionUrl: String = "",
    val priority: Int = 0,
    val isActive: Boolean = true
)

data class ProductAd(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val label: String = "Featured",
    val isActive: Boolean = true
)

data class VideoAd(
    val id: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val actionUrl: String = "",
    val isActive: Boolean = true
)

data class RecommendationItem(
    val id: String = "",
    val name: String = "",
    val type: String = "PRODUCT", // "PRODUCT" or "BRAND"
    val imageUrl: String = ""
)

data class ReferralConfig(
    val referrerFirstOrderReward: Double = 15.0,
    val referrerSecondOrderReward: Double = 10.0,
    val refereePassDurationDays: Int = 30,
    val milestones: List<ReferralMilestone> = listOf(
        ReferralMilestone(count = 100, bonus = 500.0, label = "Silver"),
        ReferralMilestone(count = 300, bonus = 2000.0, label = "Gold"),
        ReferralMilestone(count = 500, bonus = 5000.0, label = "Platinum")
    ),
    val isActive: Boolean = true
)

data class ReferralMilestone(
    val count: Int = 0,
    val bonus: Double = 0.0,
    val label: String = ""
)

data class PopupNotification(
    val title: String = "",
    val message: String = "",
    val imageUrl: String = "",
    val actionUrl: String = "",
    val isActive: Boolean = false
)
