package com.fryumm.store.ui.screens.addproduct

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fryumm.store.data.model.Coupon
import com.fryumm.store.data.model.ProductVariant
import com.fryumm.store.ui.theme.FStorePurple
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaVariantsSection(
    productImages: List<String>,
    onRemoveImage: (String) -> Unit,
    isUploading: Boolean,
    onAddImageClick: () -> Unit,
    imageUrlInput: String,
    onImageUrlInputChange: (String) -> Unit,
    onAddImageUrl: () -> Unit,
    variants: List<ProductVariant>,
    onVariantsChange: (List<ProductVariant>) -> Unit,
    variantTitleInput: String,
    onVariantTitleInputChange: (String) -> Unit,
    variantPriceInput: String,
    onVariantPriceInputChange: (String) -> Unit,
    variantStockInput: String,
    onVariantStockInputChange: (String) -> Unit,
    coupons: List<Coupon>,
    onCouponsChange: (List<Coupon>) -> Unit,
    couponCodeInput: String,
    onCouponCodeInputChange: (String) -> Unit,
    couponDiscountInput: String,
    onCouponDiscountInputChange: (String) -> Unit,
    isAdmin: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Product Gallery (3-8 Images Recommended)",
            fontWeight = FontWeight.Bold,
            color = FStorePurple
        )

        if (productImages.isNotEmpty() || isUploading) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(productImages) { imgUrl ->
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = imgUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { onRemoveImage(imgUrl) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (isUploading) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = FStorePurple,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                if (productImages.size < 8 && !isUploading) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(Color.Transparent, RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                .clickable { onAddImageClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray)
                                Text("Add More", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { onAddImageClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Text("Upload Product Images (Min 3)", color = Color.Gray)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text("Add Image via URL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = imageUrlInput,
                    onValueChange = onImageUrlInputChange,
                    label = { Text("Paste Image/Google Drive Link") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 12.sp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onAddImageUrl,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
            Text(
                "Supports direct links and public Google Drive shared links.",
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3E5F5), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                "SKU Variants (Weights/Sizes)",
                fontWeight = FontWeight.Bold,
                color = FStorePurple,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = variantTitleInput,
                    onValueChange = onVariantTitleInputChange,
                    label = { Text("Variant (500g)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = variantPriceInput,
                    onValueChange = onVariantPriceInputChange,
                    label = { Text("Price (₹)") },
                    modifier = Modifier.weight(0.7f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = variantStockInput,
                    onValueChange = onVariantStockInputChange,
                    label = { Text("Stock") },
                    modifier = Modifier.weight(0.6f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (variantTitleInput.isNotBlank() && variantPriceInput.isNotBlank()) {
                            val p = variantPriceInput.toDoubleOrNull() ?: 0.0
                            val s = variantStockInput.toIntOrNull() ?: 10
                            val newVar = ProductVariant(
                                id = UUID.randomUUID().toString(),
                                title = variantTitleInput,
                                price = p,
                                stockQuantity = s
                            )
                            onVariantsChange(variants + newVar)
                            onVariantTitleInputChange("")
                            onVariantPriceInputChange("")
                            onVariantStockInputChange("")
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = FStorePurple,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }

            if (variants.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    variants.forEach { v ->
                        InputChip(
                            selected = true,
                            onClick = { onVariantsChange(variants.filter { it.id != v.id }) },
                            label = { Text("${v.title} - ₹${v.price} (${v.stockQuantity} in stock)") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        if (isAdmin) {
            Text(
                "Offers & Coupons",
                fontWeight = FontWeight.Bold,
                color = FStorePurple,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = couponCodeInput,
                        onValueChange = { onCouponCodeInputChange(it.uppercase()) },
                        label = { Text("Code") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = couponDiscountInput,
                        onValueChange = onCouponDiscountInputChange,
                        label = { Text("₹ Off") },
                        modifier = Modifier.weight(0.6f),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (couponCodeInput.isNotBlank() && couponDiscountInput.isNotBlank()) {
                                val discount = couponDiscountInput.toDoubleOrNull() ?: 0.0
                                val newCoupon = Coupon(
                                    code = couponCodeInput,
                                    discount = discount,
                                    description = "Get ₹$discount off on this product"
                                )
                                onCouponsChange(coupons + newCoupon)
                                onCouponCodeInputChange("")
                                onCouponDiscountInputChange("")
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = FStorePurple,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                }

                if (coupons.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Active Coupons:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (coupon in coupons) {
                            AssistChip(
                                onClick = { onCouponsChange(coupons.filter { it.code != coupon.code }) },
                                label = { Text("${coupon.code} (₹${coupon.discount})") },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
