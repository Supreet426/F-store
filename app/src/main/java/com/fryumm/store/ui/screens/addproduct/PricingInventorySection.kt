package com.fryumm.store.ui.screens.addproduct

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.Product
import com.fryumm.store.ui.components.PriceBreakdownRow
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun PricingInventorySection(
    originalPrice: String,
    onOriginalPriceChange: (String) -> Unit,
    basePrice: String,
    onBasePriceChange: (String) -> Unit,
    merchantRate: String,
    onMerchantRateChange: (String) -> Unit,
    discountValue: String,
    onDiscountValueChange: (String) -> Unit,
    handlingFee: String,
    onHandlingFeeChange: (String) -> Unit,
    deliveryCharge: String,
    onDeliveryChargeChange: (String) -> Unit,
    freeDeliveryMinAmount: String,
    onFreeDeliveryMinAmountChange: (String) -> Unit,
    freeDeliveryMinQuantity: String,
    onFreeDeliveryMinQuantityChange: (String) -> Unit,
    hsnCode: String,
    onHsnCodeChange: (String) -> Unit,
    taxRate: String,
    onTaxRateChange: (String) -> Unit,
    isTaxInclusive: Boolean,
    onIsTaxInclusiveChange: (Boolean) -> Unit,
    cgstRate: String,
    onCgstRateChange: (String) -> Unit,
    sgstRate: String,
    onSgstRateChange: (String) -> Unit,
    igstRate: String,
    onIgstRateChange: (String) -> Unit,
    stock: String,
    onStockChange: (String) -> Unit,
    minStock: String,
    onMinStockChange: (String) -> Unit,
    minOrderQuantity: String,
    onMinOrderQuantityChange: (String) -> Unit,
    barcode: String,
    onBarcodeChange: (String) -> Unit,
    finalPayable: Double,
    discountPercentage: Int,
    canEditPricing: Boolean,
    productToEdit: Product?,
    isAdmin: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (canEditPricing) {
            Text("Tax & GST Compliance*", fontWeight = FontWeight.Bold, color = FStorePurple)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = hsnCode,
                        onValueChange = onHsnCodeChange,
                        label = { Text("HSN Code*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        placeholder = { Text("e.g. 1905, 0401") },
                        supportingText = { Text("Required for GST billing") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF9800),
                            focusedLabelColor = Color(0xFFFF9800)
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = taxRate,
                            onValueChange = {
                                onTaxRateChange(it)
                                val total = it.toDoubleOrNull() ?: 0.0
                                onCgstRateChange((total / 2).toString())
                                onSgstRateChange((total / 2).toString())
                                onIgstRateChange("0.0")
                            },
                            label = { Text("Total GST (%)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            suffix = { Text("%") }
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isTaxInclusive,
                                onCheckedChange = onIsTaxInclusiveChange
                            )
                            Text("Price includes Tax", fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cgstRate,
                            onValueChange = onCgstRateChange,
                            label = { Text("CGST %") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = sgstRate,
                            onValueChange = onSgstRateChange,
                            label = { Text("SGST %") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = igstRate,
                            onValueChange = onIgstRateChange,
                            label = { Text("IGST %") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            Text("Pricing & Bill Breakdown", fontWeight = FontWeight.Bold, color = FStorePurple)

            Card(
                colors = CardDefaults.cardColors(containerColor = FStorePurple.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FStorePurple.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "TOTAL AMOUNT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Gray
                            )
                            Text(
                                "₹$finalPayable",
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                color = FStorePurple
                            )
                        }
                        if (discountPercentage > 0) {
                            Surface(color = Color(0xFF2E7D32), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    " $discountPercentage% OFF ",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = FStorePurple.copy(alpha = 0.1f)
                    )

                    val mrp = originalPrice.toDoubleOrNull() ?: 0.0
                    val base = basePrice.toDoubleOrNull() ?: 0.0
                    val disc = discountValue.toDoubleOrNull() ?: 0.0
                    val tax = taxRate.toDoubleOrNull() ?: 0.0
                    val hFee = handlingFee.toDoubleOrNull() ?: 0.0
                    val dCharge = deliveryCharge.toDoubleOrNull() ?: 0.0
                    val cTax = cgstRate.toDoubleOrNull() ?: 0.0
                    val sTax = sgstRate.toDoubleOrNull() ?: 0.0

                    PriceBreakdownRow("MRP", "₹$mrp")
                    PriceBreakdownRow("Selling Price", "₹$base")
                    if (disc > 0) PriceBreakdownRow("Store Discount", "-₹$disc", Color(0xFF2E7D32))

                    if (cTax > 0) PriceBreakdownRow(
                        "CGST ($cTax%)",
                        "+₹${String.format("%.2f", (base - disc) * (cTax / 100))}"
                    )
                    if (sTax > 0) PriceBreakdownRow(
                        "SGST ($sTax%)",
                        "+₹${String.format("%.2f", (base - disc) * (sTax / 100))}"
                    )
                    if (tax > 0 && cTax == 0.0 && sTax == 0.0) PriceBreakdownRow(
                        "GST ($tax%)",
                        "+₹${String.format("%.2f", (base - disc) * (tax / 100))}"
                    )

                    if (hFee > 0) PriceBreakdownRow("Handling Fee", "+₹$hFee")
                    if (dCharge > 0) Text(
                        "Delivery charge of ₹$dCharge will be added at checkout",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (!isAdmin && productToEdit != null) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Note: Changing prices will hide this product until Admin re-verifies it.",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedTextField(
                value = originalPrice,
                onValueChange = onOriginalPriceChange,
                label = { Text("MRP (Max Retail Price)*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                prefix = { Text("₹ ") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = basePrice,
                    onValueChange = onBasePriceChange,
                    label = { Text("Selling Price*") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text("₹ ") }
                )
                OutlinedTextField(
                    value = merchantRate,
                    onValueChange = onMerchantRateChange,
                    label = { Text("Merchant Rate*") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text("₹ ") },
                    supportingText = { Text("Payout per unit") }
                )
            }
            
            OutlinedTextField(
                value = discountValue,
                onValueChange = onDiscountValueChange,
                label = { Text("Discount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                prefix = { Text("₹ ") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = handlingFee,
                    onValueChange = onHandlingFeeChange,
                    label = { Text("Handling Fee") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text("₹ ") }
                )
                OutlinedTextField(
                    value = deliveryCharge,
                    onValueChange = onDeliveryChargeChange,
                    label = { Text("Delivery Charge") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text("₹ ") },
                    supportingText = { Text("Manual adjustment") }
                )
            }

            Text(
                "Free Delivery Thresholds (Specific to this item)",
                fontWeight = FontWeight.Bold,
                color = FStorePurple,
                fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = freeDeliveryMinAmount,
                    onValueChange = onFreeDeliveryMinAmountChange,
                    label = { Text("Free if Value ≥") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text("₹ ") },
                    supportingText = { Text("Set 0 to disable") }
                )
                OutlinedTextField(
                    value = freeDeliveryMinQuantity,
                    onValueChange = onFreeDeliveryMinQuantityChange,
                    label = { Text("Free if Qty ≥") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    suffix = { Text("pcs") },
                    supportingText = { Text("Set 0 to disable") }
                )
            }
        }

        Text(
            "Inventory & Measurements",
            fontWeight = FontWeight.Bold,
            color = FStorePurple,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = stock,
                onValueChange = onStockChange,
                label = { Text("Stock Count*") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = barcode,
                onValueChange = onBarcodeChange,
                label = { Text("Barcode ID") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { Icon(Icons.Default.QrCodeScanner, null) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = minStock,
                onValueChange = onMinStockChange,
                label = { Text("Min Alert Lvl") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = minOrderQuantity,
                onValueChange = onMinOrderQuantityChange,
                label = { Text("Min Order Qty") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                supportingText = { Text("Min units per order") }
            )
        }
    }
}
