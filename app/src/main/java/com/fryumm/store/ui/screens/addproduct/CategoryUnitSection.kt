package com.fryumm.store.ui.screens.addproduct

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.theme.FStorePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryUnitSection(
    category: String,
    onCategoryClick: () -> Unit,
    unitType: String,
    onUnitTypeChange: (String) -> Unit,
    measurementUnit: String,
    onMeasurementUnitChange: (String) -> Unit,
    weightValue: String,
    onWeightValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Category Placement*", fontWeight = FontWeight.Bold, color = FStorePurple)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { },
                label = { Text("Category Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick() },
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Select category...") },
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledBorderColor = Color.Gray,
                    disabledLabelColor = FStorePurple,
                    disabledPlaceholderColor = Color.Gray,
                    disabledTrailingIconColor = FStorePurple
                ),
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = FStorePurple)
                }
            )

            Text(
                "Tap to choose from existing categories or create new one.",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        Text("Measurements", fontWeight = FontWeight.Bold, color = FStorePurple)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            var unitExpanded by remember { mutableStateOf(false) }
            val unitTypes = listOf("Weight", "Volume", "Unit/Count", "Length")

            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = unitType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sold by") },
                    modifier = Modifier.menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = unitExpanded,
                    onDismissRequest = { unitExpanded = false }
                ) {
                    unitTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                onUnitTypeChange(type)
                                val newUnit = when (type) {
                                    "Weight" -> "g"
                                    "Volume" -> "ml"
                                    "Length" -> "m"
                                    else -> "pc"
                                }
                                onMeasurementUnitChange(newUnit)
                                unitExpanded = false
                            }
                        )
                    }
                }
            }

            var measureExpanded by remember { mutableStateOf(false) }
            val units = when (unitType) {
                "Weight" -> listOf("g", "kg", "oz", "lb")
                "Volume" -> listOf("ml", "l", "fl oz")
                "Length" -> listOf("cm", "m", "inch", "ft")
                else -> listOf("pc", "pack", "unit", "set", "box")
            }

            ExposedDropdownMenuBox(
                expanded = measureExpanded,
                onExpandedChange = { measureExpanded = it },
                modifier = Modifier.weight(0.8f)
            ) {
                OutlinedTextField(
                    value = measurementUnit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    modifier = Modifier.menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = measureExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = measureExpanded,
                    onDismissRequest = { measureExpanded = false }
                ) {
                    units.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u) },
                            onClick = {
                                onMeasurementUnitChange(u)
                                measureExpanded = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = weightValue,
            onValueChange = onWeightValueChange,
            label = { Text("Quantity Value*") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("e.g. 500") }
        )
    }
}
