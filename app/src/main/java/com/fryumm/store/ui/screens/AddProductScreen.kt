package com.fryumm.store.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.Coupon
import com.fryumm.store.data.model.Product
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.data.model.DarkStore
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.ui.components.*
import com.fryumm.store.ui.screens.addproduct.*
import com.fryumm.store.ui.theme.FStorePurple
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavHostController, 
    viewModel: StoreViewModel, 
    productToEdit: Product? = null,
    prefilledCategory: String? = null
) {
    var brand by remember { mutableStateOf(productToEdit?.brand ?: "") }
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var searchNames by remember { mutableStateOf(productToEdit?.searchNames ?: emptyList<String>()) }
    var searchNameInput by remember { mutableStateOf("") }
    var description by remember { mutableStateOf(productToEdit?.description ?: "") }
    val websiteCategories = remember {
        listOf(
            "Grocery", "Fruits & Vegetables", "Dairy & Eggs", "Bakery & Biscuits",
            "Beverages", "Snacks & Munchies", "Instant Food", "Meat & Seafood",
            "Personal Care", "Home & Cleaning", "Baby Care", "Pet Care",
            "Electronics", "Pharmacy", "Stationery", "Kitchen & Dining", "Paan Corner"
        )
    }

    var shortDescription by remember { mutableStateOf(productToEdit?.shortDescription ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: prefilledCategory ?: "") }
    var isCustomCategory by remember { 
        mutableStateOf(
            category.isNotBlank() && 
            !websiteCategories.contains(category) && 
            !viewModel.dynamicAppConfig.categorySequence.contains(category)
        ) 
    }
    
    var originalPrice by remember { mutableStateOf(productToEdit?.originalPrice?.toString() ?: "") }
    var basePrice by remember { mutableStateOf(productToEdit?.basePrice?.toString() ?: "") }
    var merchantRate by remember { mutableStateOf(productToEdit?.merchantRate?.toString() ?: "") }
    var taxRate by remember { mutableStateOf(productToEdit?.taxRate?.toString() ?: "0.0") }
    var cgstRate by remember { mutableStateOf(productToEdit?.cgstRate?.toString() ?: "0.0") }
    var sgstRate by remember { mutableStateOf(productToEdit?.sgstRate?.toString() ?: "0.0") }
    var igstRate by remember { mutableStateOf(productToEdit?.igstRate?.toString() ?: "0.0") }
    var hsnCode by remember { mutableStateOf(productToEdit?.hsnCode ?: "") }
    var handlingFee by remember { mutableStateOf(productToEdit?.handlingFee?.toString() ?: "0.0") }
    var deliveryCharge by remember { mutableStateOf(productToEdit?.deliveryCharge?.toString() ?: "0.0") }
    var discountValue by remember { mutableStateOf(productToEdit?.discountValue?.toString() ?: "0.0") }
    var isTaxInclusive by remember { mutableStateOf(productToEdit?.isTaxInclusive ?: true) }

    var barcode by remember { mutableStateOf(productToEdit?.barcode ?: "") }
    var stock by remember { mutableStateOf(productToEdit?.stockQuantity?.toString() ?: "") }
    var minStock by remember { mutableStateOf(productToEdit?.minStockLevel?.toString() ?: "5") }
    var minOrderQuantity by remember { mutableStateOf(productToEdit?.minOrderQuantity?.toString() ?: "1") }
    
    var unitType by remember { mutableStateOf(productToEdit?.unitType ?: "Weight") }
    var measurementUnit by remember { mutableStateOf(productToEdit?.measurementUnit ?: "g") }
    var weightValue by remember { 
        mutableStateOf(
            productToEdit?.weight?.removeSuffix(productToEdit?.measurementUnit ?: "") ?: "500"
        ) 
    }
    
    var aisle by remember { mutableStateOf(productToEdit?.aisle ?: "A1") }
    var rack by remember { mutableStateOf(productToEdit?.rack ?: "R1") }

    var variants by remember { mutableStateOf(productToEdit?.variants ?: emptyList<com.fryumm.store.data.model.ProductVariant>()) }
    var variantTitleInput by remember { mutableStateOf("") }
    var variantPriceInput by remember { mutableStateOf("") }
    var variantStockInput by remember { mutableStateOf("") }
    
    var isFoodProduct by remember { mutableStateOf(productToEdit?.isFoodProduct ?: false) }
    var isComingSoon by remember { mutableStateOf(productToEdit?.isComingSoon ?: false) }
    var expiryDate by remember { mutableStateOf(productToEdit?.expiryDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)) } ?: "") }
    var deliveryChargeIncluded by remember { mutableStateOf(productToEdit?.deliveryChargeIncluded ?: true) }
    var freeDeliveryMinAmount by remember { mutableStateOf(productToEdit?.freeDeliveryMinAmount?.toString() ?: "0.0") }
    var freeDeliveryMinQuantity by remember { mutableStateOf(productToEdit?.freeDeliveryMinQuantity?.toString() ?: "0") }
    var legalDisclaimer by remember { mutableStateOf(productToEdit?.legalDisclaimer ?: "") }
    var nutritionalInfo by remember { mutableStateOf(productToEdit?.nutritionalInfo ?: "") }
    var adminFeedback by remember { mutableStateOf(productToEdit?.adminFeedback ?: "") }
    var specifications by remember { mutableStateOf(productToEdit?.specifications ?: emptyMap<String, String>()) }

    var showCategorySheet by remember { mutableStateOf(false) }

    var productImages by remember { 
        mutableStateOf(
            productToEdit?.let {
                if (it.images.isNotEmpty()) it.images
                else if (it.imageUrl.isNotEmpty()) listOf(it.imageUrl)
                else emptyList()
            } ?: emptyList()
        ) 
    }
    var imageUrlInput by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val isAdmin = viewModel.currentUserProfile?.userRole == UserRole.ADMIN || viewModel.isMasterAdmin
    val isMerchant = viewModel.currentUserProfile?.userRole == UserRole.STORE_OWNER
    val canEditPricing = isAdmin || isMerchant

    val isEditing = productToEdit != null
    var selectedDarkHubId by remember { mutableStateOf(productToEdit?.darkHubId ?: "") }
    var selectedOwnershipType by remember {
        mutableStateOf(
            when {
                productToEdit != null && !productToEdit.merchantId.isNullOrBlank() -> "MERCHANT"
                productToEdit != null && !productToEdit.darkStoreId.isNullOrBlank() -> "DARKSTORE"
                else -> ""
            }
        )
    }

    var selectedMerchantId by remember { mutableStateOf(productToEdit?.merchantId ?: "") }
    var selectedDarkStoreId by remember { mutableStateOf(productToEdit?.darkStoreId ?: "") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    isUploading = true
                    val uploadedUrl = viewModel.uploadProductImage(uri)
                    if (uploadedUrl != null) {
                        productImages = productImages + uploadedUrl
                        Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Upload failed.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    var coupons by remember { mutableStateOf(productToEdit?.availableCoupons ?: emptyList<Coupon>()) }
    var couponCodeInput by remember { mutableStateOf("") }
    var couponDiscountInput by remember { mutableStateOf("") }

    val finalPayable = remember(basePrice, taxRate, handlingFee, discountValue, isTaxInclusive) {
        val base = basePrice.toDoubleOrNull() ?: 0.0
        val tax = taxRate.toDoubleOrNull() ?: 0.0
        val handling = handlingFee.toDoubleOrNull() ?: 0.0
        val disc = discountValue.toDoubleOrNull() ?: 0.0
        
        val netBase = (base - disc).coerceAtLeast(0.0)
        val taxAmount = if (isTaxInclusive) 0.0 else netBase * (tax / 100.0)
        
        val result = netBase + taxAmount + handling
        (Math.round(result * 100.0) / 100.0)
    }

    val discountPercentage = remember(originalPrice, finalPayable) {
        val mrp = originalPrice.toDoubleOrNull() ?: 0.0
        if (mrp > finalPayable && mrp > 0) {
            (((mrp - finalPayable) / mrp) * 100).toInt()
        } else 0
    }

    val isFormValid = brand.isNotBlank() && name.isNotBlank() && category.isNotBlank() &&
            (if (canEditPricing) basePrice.isNotBlank() && merchantRate.isNotBlank() && hsnCode.isNotBlank() else true) && 
            stock.isNotBlank() && (!isFoodProduct || expiryDate.isNotBlank()) && !isUploading && productImages.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productToEdit == null) "Add New Item" else "Update Item", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FStorePurple, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).background(Color.White)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.weight(1f)) {
                if (isAdmin) {
                    item {
                        OwnershipSelectionSection(
                            isAdmin = isAdmin,
                            isEditing = isEditing,
                            selectedDarkHubId = selectedDarkHubId,
                            onDarkHubSelected = { selectedDarkHubId = it },
                            selectedOwnershipType = selectedOwnershipType,
                            onOwnershipTypeChange = { selectedOwnershipType = it },
                            selectedMerchantId = selectedMerchantId,
                            onMerchantSelected = { selectedMerchantId = it },
                            selectedDarkStoreId = selectedDarkStoreId,
                            onDarkStoreSelected = { selectedDarkStoreId = it },
                            merchants = viewModel.allUsers.filter { it.role == "MERCHANT" || it.userRole == UserRole.STORE_OWNER },
                            darkStores = viewModel.darkStores
                        )
                    }
                }

                item {
                    BasicInfoSection(
                        brand = brand,
                        onBrandChange = { brand = it },
                        name = name,
                        onNameChange = { name = it },
                        searchNames = searchNames,
                        onSearchNamesChange = { searchNames = it },
                        searchNameInput = searchNameInput,
                        onSearchNameInputChange = { searchNameInput = it },
                        shortDescription = shortDescription,
                        onShortDescriptionChange = { shortDescription = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        canEditPricing = canEditPricing
                    )
                }

                item {
                    CategoryUnitSection(
                        category = category,
                        onCategoryClick = { showCategorySheet = true },
                        onCategoryChange = { category = it },
                        isCategoryEditable = isCustomCategory,
                        unitType = unitType,
                        onUnitTypeChange = { unitType = it },
                        measurementUnit = measurementUnit,
                        onMeasurementUnitChange = { measurementUnit = it },
                        weightValue = weightValue,
                        onWeightValueChange = { weightValue = it }
                    )
                }

                item {
                    PricingInventorySection(
                        originalPrice = originalPrice,
                        onOriginalPriceChange = { originalPrice = it },
                        basePrice = basePrice,
                        onBasePriceChange = { basePrice = it },
                        merchantRate = merchantRate,
                        onMerchantRateChange = { merchantRate = it },
                        discountValue = discountValue,
                        onDiscountValueChange = { discountValue = it },
                        handlingFee = handlingFee,
                        onHandlingFeeChange = { handlingFee = it },
                        deliveryCharge = deliveryCharge,
                        onDeliveryChargeChange = { deliveryCharge = it },
                        freeDeliveryMinAmount = freeDeliveryMinAmount,
                        onFreeDeliveryMinAmountChange = { freeDeliveryMinAmount = it },
                        freeDeliveryMinQuantity = freeDeliveryMinQuantity,
                        onFreeDeliveryMinQuantityChange = { freeDeliveryMinQuantity = it },
                        hsnCode = hsnCode,
                        onHsnCodeChange = { hsnCode = it },
                        taxRate = taxRate,
                        onTaxRateChange = { taxRate = it },
                        isTaxInclusive = isTaxInclusive,
                        onIsTaxInclusiveChange = { isTaxInclusive = it },
                        cgstRate = cgstRate,
                        onCgstRateChange = { cgstRate = it },
                        sgstRate = sgstRate,
                        onSgstRateChange = { sgstRate = it },
                        igstRate = igstRate,
                        onIgstRateChange = { igstRate = it },
                        stock = stock,
                        onStockChange = { stock = it },
                        minStock = minStock,
                        onMinStockChange = { minStock = it },
                        minOrderQuantity = minOrderQuantity,
                        onMinOrderQuantityChange = { minOrderQuantity = it },
                        barcode = barcode,
                        onBarcodeChange = { barcode = it },
                        finalPayable = finalPayable,
                        discountPercentage = discountPercentage,
                        canEditPricing = canEditPricing,
                        productToEdit = productToEdit,
                        isAdmin = isAdmin
                    )
                }

                item {
                    MediaVariantsSection(
                        productImages = productImages,
                        onRemoveImage = { url -> productImages = productImages.filter { it != url } },
                        isUploading = isUploading,
                        onAddImageClick = { imagePickerLauncher.launch("image/*") },
                        imageUrlInput = imageUrlInput,
                        onImageUrlInputChange = { imageUrlInput = it },
                        onAddImageUrl = {
                            if (imageUrlInput.isNotBlank()) {
                                val finalUrl = if (imageUrlInput.contains("drive.google.com")) {
                                    imageUrlInput.replace("/file/d/", "/uc?export=view&id=").replace("/view?usp=sharing", "")
                                } else imageUrlInput
                                productImages = productImages + finalUrl
                                imageUrlInput = ""
                            }
                        },
                        variants = variants,
                        onVariantsChange = { variants = it },
                        variantTitleInput = variantTitleInput,
                        onVariantTitleInputChange = { variantTitleInput = it },
                        variantPriceInput = variantPriceInput,
                        onVariantPriceInputChange = { variantPriceInput = it },
                        variantStockInput = variantStockInput,
                        onVariantStockInputChange = { variantStockInput = it },
                        coupons = coupons,
                        onCouponsChange = { coupons = it },
                        couponCodeInput = couponCodeInput,
                        onCouponCodeInputChange = { couponCodeInput = it },
                        couponDiscountInput = couponDiscountInput,
                        onCouponDiscountInputChange = { couponDiscountInput = it },
                        isAdmin = isAdmin
                    )
                }

                item {
                    OperationalLegalSection(
                        aisle = aisle,
                        onAisleChange = { aisle = it },
                        rack = rack,
                        onRackChange = { rack = it },
                        isComingSoon = isComingSoon,
                        onIsComingSoonChange = { isComingSoon = it },
                        isFoodProduct = isFoodProduct,
                        onIsFoodProductChange = { isFoodProduct = it },
                        deliveryChargeIncluded = deliveryChargeIncluded,
                        onDeliveryChargeIncludedChange = { deliveryChargeIncluded = it },
                        expiryDate = expiryDate,
                        onExpiryDateChange = { expiryDate = it },
                        nutritionalInfo = nutritionalInfo,
                        onNutritionalInfoChange = { nutritionalInfo = it },
                        legalDisclaimer = legalDisclaimer,
                        onLegalDisclaimerChange = { legalDisclaimer = it },
                        adminFeedback = adminFeedback,
                        onAdminFeedbackChange = { adminFeedback = it },
                        isAdmin = isAdmin,
                        canEditPricing = canEditPricing
                    )
                }

                item {
                    SpecificationsSection(
                        specifications = specifications,
                        onSpecificationsChange = { specifications = it }
                    )
                }
            }

            Button(
                onClick = {
                    if (isAdmin && !isEditing) {
                        if (selectedDarkHubId.isBlank()) {
                            Toast.makeText(context, "MANDATORY: Dark Hub selection is required for all products.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (selectedOwnershipType.isBlank()) {
                            Toast.makeText(context, "MANDATORY: Please select Product Owner (Merchant OR Dark Store).", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (selectedOwnershipType == "MERCHANT" && selectedMerchantId.isBlank()) {
                            Toast.makeText(context, "MANDATORY: Please select a Target Merchant for this product.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (selectedOwnershipType == "DARKSTORE" && selectedDarkStoreId.isBlank()) {
                            Toast.makeText(context, "MANDATORY: Please select a Target Dark Store for this product.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                    }

                    if (!isFormValid) {
                        val missing = mutableListOf<String>()
                        if (brand.isBlank()) missing.add("Brand")
                        if (name.isBlank()) missing.add("Name")
                        if (category.isBlank()) missing.add("Category")
                        if (canEditPricing) {
                            if (basePrice.isBlank()) missing.add("Selling Price")
                            if (merchantRate.isBlank()) missing.add("Merchant Rate")
                            if (hsnCode.isBlank()) missing.add("HSN Code")
                        }
                        if (stock.isBlank()) missing.add("Stock")
                        if (isFoodProduct && expiryDate.isBlank()) missing.add("Expiry Date")
                        if (productImages.isEmpty()) missing.add("Product Images")
                        
                        Toast.makeText(context, "Please fill: ${missing.joinToString()}", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    scope.launch {
                        val parsedExpiry = if (isFoodProduct && expiryDate.isNotBlank()) {
                            try { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(expiryDate)?.time } catch (e: Exception) { null }
                        } else null

                        val resolvedMerchantId: String
                        val resolvedMerchantName: String
                        val resolvedDarkStoreId: String?
                        val resolvedDarkHubId: String

                        if (isEditing) {
                            if (!productToEdit!!.merchantId.isNullOrBlank()) {
                                resolvedMerchantId = productToEdit.merchantId
                                resolvedMerchantName = productToEdit.merchantName
                                resolvedDarkStoreId = null
                                resolvedDarkHubId = productToEdit.darkHubId
                            } else {
                                resolvedMerchantId = ""
                                resolvedMerchantName = ""
                                resolvedDarkStoreId = productToEdit.darkStoreId
                                resolvedDarkHubId = productToEdit.darkHubId
                            }
                        } else if (isAdmin) {
                            resolvedDarkHubId = selectedDarkHubId
                            if (selectedOwnershipType == "MERCHANT") {
                                val selectedMerchant = viewModel.allUsers.find { it.uid == selectedMerchantId }
                                resolvedMerchantId = selectedMerchantId
                                resolvedMerchantName = selectedMerchant?.name ?: "Merchant"
                                resolvedDarkStoreId = null // STRICT XOR EXCLUSION
                            } else {
                                resolvedMerchantId = "" // STRICT XOR EXCLUSION
                                resolvedMerchantName = ""
                                resolvedDarkStoreId = selectedDarkStoreId.ifBlank { selectedDarkHubId }
                            }
                        } else {
                            val isMerchantUser = viewModel.currentUserProfile?.role == "MERCHANT" || viewModel.currentUserProfile?.role == "STORE_OWNER"
                            if (isMerchantUser) {
                                resolvedMerchantId = viewModel.currentUserProfile?.uid ?: ""
                                resolvedMerchantName = viewModel.currentUserProfile?.name ?: "Flash-O Merchant"
                                resolvedDarkStoreId = null // STRICT XOR EXCLUSION
                                resolvedDarkHubId = viewModel.currentUserProfile?.darkStoreId ?: ""
                            } else {
                                val defaultStore = viewModel.darkStores.firstOrNull()
                                resolvedMerchantId = ""
                                resolvedMerchantName = ""
                                resolvedDarkStoreId = viewModel.currentUserProfile?.darkStoreId?.takeIf { it.isNotBlank() } ?: defaultStore?.id ?: "DS_MAIN"
                                resolvedDarkHubId = viewModel.currentUserProfile?.darkStoreId?.takeIf { it.isNotBlank() } ?: defaultStore?.id ?: "DS_MAIN"
                            }
                        }

                        val product = Product(
                            id = productToEdit?.id ?: "",
                            brand = brand,
                            name = name,
                            searchNames = searchNames,
                            description = description,
                            shortDescription = shortDescription,
                            category = category,
                            price = finalPayable,
                            originalPrice = originalPrice.toDoubleOrNull() ?: finalPayable,
                            basePrice = basePrice.toDoubleOrNull() ?: 0.0,
                            merchantRate = merchantRate.toDoubleOrNull() ?: 0.0,
                            taxRate = taxRate.toDoubleOrNull() ?: 0.0,
                            cgstRate = cgstRate.toDoubleOrNull() ?: 0.0,
                            sgstRate = sgstRate.toDoubleOrNull() ?: 0.0,
                            igstRate = igstRate.toDoubleOrNull() ?: 0.0,
                            hsnCode = hsnCode,
                            handlingFee = handlingFee.toDoubleOrNull() ?: 0.0,
                            deliveryCharge = deliveryCharge.toDoubleOrNull() ?: 0.0,
                            discountValue = discountValue.toDoubleOrNull() ?: 0.0,
                            isTaxInclusive = isTaxInclusive,
                            barcode = barcode,
                            stockQuantity = stock.toIntOrNull() ?: 0,
                            minStockLevel = minStock.toIntOrNull() ?: 5,
                            minOrderQuantity = minOrderQuantity.toIntOrNull() ?: 1,
                            unitType = unitType,
                            measurementUnit = measurementUnit,
                            weight = "$weightValue$measurementUnit",
                            aisle = aisle,
                            rack = rack,
                            isInStock = true,
                            isComingSoon = isComingSoon,
                            isFoodProduct = isFoodProduct,
                            expiryDate = parsedExpiry,
                            deliveryChargeIncluded = deliveryChargeIncluded,
                            freeDeliveryMinAmount = freeDeliveryMinAmount.toDoubleOrNull() ?: 0.0,
                            freeDeliveryMinQuantity = freeDeliveryMinQuantity.toIntOrNull() ?: 0,
                            legalDisclaimer = legalDisclaimer,
                            nutritionalInfo = nutritionalInfo,
                            imageUrl = productImages.firstOrNull() ?: "",
                            images = productImages,
                            availableCoupons = coupons,
                            specifications = specifications,
                            isVerified = isAdmin,
                            adminFeedback = adminFeedback,
                            merchantId = resolvedMerchantId,
                            merchantName = resolvedMerchantName,
                            darkStoreId = resolvedDarkStoreId,
                            darkHubId = resolvedDarkHubId,
                            variants = variants,
                            sequenceNumber = productToEdit?.sequenceNumber ?: 999
                        )

                        if (productToEdit == null) {
                            viewModel.addProduct(product)
                        } else {
                            if (isAdmin) {
                                viewModel.updateProductByAdmin(product, adminFeedback)
                            } else {
                                viewModel.updateProduct(product)
                            }
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                enabled = true
            ) {
                Text(if (productToEdit == null) "SAVE PRODUCT" else "UPDATE PRODUCT", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }

    if (showCategorySheet) {
        val catalogCategories = viewModel.dynamicAppConfig.categorySequence
        val existingCategories = remember(viewModel.products) {
            viewModel.products.map { it.category }.distinct().filter { it.isNotBlank() }
        }
        val allCategories = remember(existingCategories, catalogCategories) {
            (websiteCategories + catalogCategories + existingCategories)
                .distinct()
                .filter { it != "Other" }
                .sorted() + "Other"
        }

        CategorySelectorSheet(
            onDismiss = { showCategorySheet = false },
            onCategorySelected = { 
                if (it == "Other") {
                    isCustomCategory = true
                    category = ""
                } else {
                    isCustomCategory = false
                    category = it
                }
            },
            categories = allCategories,
            isAdmin = isAdmin,
            onAddCategory = { viewModel.addCategory(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnershipSelectionSection(
    isAdmin: Boolean,
    isEditing: Boolean,
    selectedDarkHubId: String,
    onDarkHubSelected: (String) -> Unit,
    selectedOwnershipType: String,
    onOwnershipTypeChange: (String) -> Unit,
    selectedMerchantId: String,
    onMerchantSelected: (String) -> Unit,
    selectedDarkStoreId: String,
    onDarkStoreSelected: (String) -> Unit,
    merchants: List<UserProfile>,
    darkStores: List<DarkStore>
) {
    if (!isAdmin) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "1. Dark Hub Assignment (Compulsory)*",
                fontWeight = FontWeight.Bold,
                color = FStorePurple,
                fontSize = 15.sp
            )

            if (isEditing) {
                Text(
                    text = "Dark Hub ID: ${selectedDarkHubId.ifBlank { "Locked" }}",
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                )
            } else {
                var expandedHubDropdown by remember { mutableStateOf(false) }
                val selectedHubStore = darkStores.find { it.id == selectedDarkHubId }

                ExposedDropdownMenuBox(
                    expanded = expandedHubDropdown,
                    onExpandedChange = { expandedHubDropdown = !expandedHubDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedHubStore?.let { "${it.name} (${it.id})" } ?: selectedDarkHubId,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Dark Hub*") },
                        placeholder = { Text("Click to choose Dark Hub...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHubDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedHubDropdown,
                        onDismissRequest = { expandedHubDropdown = false }
                    ) {
                        if (darkStores.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No active dark hubs found") },
                                onClick = { expandedHubDropdown = false }
                            )
                        } else {
                            darkStores.forEach { hub ->
                                DropdownMenuItem(
                                    text = { Text("${hub.name} (${hub.id})") },
                                    onClick = {
                                        onDarkHubSelected(hub.id)
                                        expandedHubDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            Text(
                text = "2. Exclusive Owner (Merchant XOR Dark Store)*",
                fontWeight = FontWeight.Bold,
                color = FStorePurple,
                fontSize = 15.sp
            )

            if (isEditing) {
                Text(
                    text = "Ownership is locked for existing items to maintain catalog integrity.",
                    style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                )
            } else {
                Text(
                    text = "Select EXACTLY ONE owner (Merchant or Dark Store):",
                    style = TextStyle(fontSize = 13.sp, color = Color.DarkGray)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOwnershipTypeChange("MERCHANT") }
                    ) {
                        RadioButton(
                            selected = selectedOwnershipType == "MERCHANT",
                            onClick = { onOwnershipTypeChange("MERCHANT") }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Merchant", fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOwnershipTypeChange("DARKSTORE") }
                    ) {
                        RadioButton(
                            selected = selectedOwnershipType == "DARKSTORE",
                            onClick = { onOwnershipTypeChange("DARKSTORE") }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Dark Store", fontWeight = FontWeight.SemiBold)
                    }
                }

                if (selectedOwnershipType == "MERCHANT") {
                    var expandedMerchantDropdown by remember { mutableStateOf(false) }
                    val selectedMerchant = merchants.find { it.uid == selectedMerchantId }

                    ExposedDropdownMenuBox(
                        expanded = expandedMerchantDropdown,
                        onExpandedChange = { expandedMerchantDropdown = !expandedMerchantDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedMerchant?.let { "${it.name} (${it.email})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Target Merchant*") },
                            placeholder = { Text("Click to choose merchant...") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMerchantDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMerchantDropdown,
                            onDismissRequest = { expandedMerchantDropdown = false }
                        ) {
                            if (merchants.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No active merchants found") },
                                    onClick = { expandedMerchantDropdown = false }
                                )
                            } else {
                                merchants.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text("${m.name} (${m.email})") },
                                        onClick = {
                                            onMerchantSelected(m.uid)
                                            expandedMerchantDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedOwnershipType == "DARKSTORE") {
                    var expandedDarkStoreDropdown by remember { mutableStateOf(false) }
                    val selectedStore = darkStores.find { it.id == selectedDarkStoreId }

                    ExposedDropdownMenuBox(
                        expanded = expandedDarkStoreDropdown,
                        onExpandedChange = { expandedDarkStoreDropdown = !expandedDarkStoreDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedStore?.let { "${it.name} (${it.id})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Target Dark Store*") },
                            placeholder = { Text("Click to choose dark store...") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDarkStoreDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDarkStoreDropdown,
                            onDismissRequest = { expandedDarkStoreDropdown = false }
                        ) {
                            if (darkStores.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No active dark stores found") },
                                    onClick = { expandedDarkStoreDropdown = false }
                                )
                            } else {
                                darkStores.forEach { ds ->
                                    DropdownMenuItem(
                                        text = { Text("${ds.name} (${ds.id})") },
                                        onClick = {
                                            onDarkStoreSelected(ds.id)
                                            expandedDarkStoreDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
