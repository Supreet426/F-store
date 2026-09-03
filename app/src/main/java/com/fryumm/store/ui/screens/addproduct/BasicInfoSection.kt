package com.fryumm.store.ui.screens.addproduct

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.theme.FStorePurple

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BasicInfoSection(
    brand: String,
    onBrandChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    searchNames: List<String>,
    onSearchNamesChange: (List<String>) -> Unit,
    searchNameInput: String,
    onSearchNameInputChange: (String) -> Unit,
    shortDescription: String,
    onShortDescriptionChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    canEditPricing: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("Product Naming & Brand", fontWeight = FontWeight.Bold, color = FStorePurple)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = brand,
                onValueChange = onBrandChange,
                label = { Text("Brand Name*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. Amul, Haldiram's") }
            )
        }
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Product Name*") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("e.g. Gold Milk, Classic Salted Chips") }
        )

        if (canEditPricing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "Search Keywords (Regional Names/Nicknames)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = FStorePurple
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchNameInput,
                        onValueChange = onSearchNameInputChange,
                        label = { Text("Add Keyword (e.g. Doodh, Milk)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(fontSize = 14.sp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (searchNameInput.isNotBlank() && !searchNames.contains(searchNameInput.trim())) {
                                onSearchNamesChange(searchNames + searchNameInput.trim())
                                onSearchNameInputChange("")
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

                if (searchNames.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        searchNames.forEach { keyword ->
                            InputChip(
                                selected = false,
                                onClick = { onSearchNamesChange(searchNames - keyword) },
                                label = { Text(keyword, fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = FStorePurple.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }
                Text(
                    "Add multiple names to help customers find this product in any language.",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            OutlinedTextField(
                value = shortDescription,
                onValueChange = onShortDescriptionChange,
                label = { Text("Short Description (1-2 lines)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. Fresh organic cow milk delivered within 10 mins.") }
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Detailed Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
