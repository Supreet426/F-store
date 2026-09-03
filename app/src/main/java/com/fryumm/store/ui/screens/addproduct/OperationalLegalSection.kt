package com.fryumm.store.ui.screens.addproduct

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun OperationalLegalSection(
    aisle: String,
    onAisleChange: (String) -> Unit,
    rack: String,
    onRackChange: (String) -> Unit,
    isComingSoon: Boolean,
    onIsComingSoonChange: (Boolean) -> Unit,
    isFoodProduct: Boolean,
    onIsFoodProductChange: (Boolean) -> Unit,
    deliveryChargeIncluded: Boolean,
    onDeliveryChargeIncludedChange: (Boolean) -> Unit,
    expiryDate: String,
    onExpiryDateChange: (String) -> Unit,
    nutritionalInfo: String,
    onNutritionalInfoChange: (String) -> Unit,
    legalDisclaimer: String,
    onLegalDisclaimerChange: (String) -> Unit,
    adminFeedback: String,
    onAdminFeedbackChange: (String) -> Unit,
    isAdmin: Boolean,
    canEditPricing: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (canEditPricing) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = aisle,
                    onValueChange = onAisleChange,
                    label = { Text("Aisle") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = rack,
                    onValueChange = onRackChange,
                    label = { Text("Rack") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Text(
            "Compliance & Safety",
            fontWeight = FontWeight.Bold,
            color = FStorePurple,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Is Coming Soon? (Show but disable orders)", modifier = Modifier.weight(1f))
            Switch(checked = isComingSoon, onCheckedChange = onIsComingSoonChange)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Is Food Product?", modifier = Modifier.weight(1f))
            Switch(checked = isFoodProduct, onCheckedChange = onIsFoodProductChange)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Delivery Charge Included in Price?", modifier = Modifier.weight(1f))
            Switch(checked = deliveryChargeIncluded, onCheckedChange = onDeliveryChargeIncludedChange)
        }

        if (isFoodProduct) {
            OutlinedTextField(
                value = expiryDate,
                onValueChange = onExpiryDateChange,
                label = { Text("Expiry Date (DD/MM/YYYY)*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Required for food products") }
            )
            OutlinedTextField(
                value = nutritionalInfo,
                onValueChange = onNutritionalInfoChange,
                label = { Text("Nutritional Info") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = legalDisclaimer,
            onValueChange = onLegalDisclaimerChange,
            label = { Text("Legal Disclaimer") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (isAdmin) {
            Text(
                "Admin Verification & Notes",
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
            OutlinedTextField(
                value = adminFeedback,
                onValueChange = onAdminFeedbackChange,
                label = { Text("What did you change? (Visible to Merchant)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. Corrected category, Adjusted price as per MRP") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Red,
                    focusedLabelColor = Color.Red
                )
            )
        }
    }
}
