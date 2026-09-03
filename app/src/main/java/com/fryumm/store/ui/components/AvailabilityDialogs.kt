package com.fryumm.store.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.data.model.DaySchedule
import com.fryumm.store.data.model.TimeRange
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilitySchedulingDialog(
    title: String,
    initialDailySchedules: Map<String, DaySchedule>,
    initialVacationStart: Long?,
    initialVacationEnd: Long?,
    onDismiss: () -> Unit,
    onSave: (Map<String, DaySchedule>, Long?, Long?) -> Unit
) {
    var dailySchedules by remember { mutableStateOf(initialDailySchedules) }
    var vacationStart by remember { mutableStateOf(initialVacationStart) }
    var vacationEnd by remember { mutableStateOf(initialVacationEnd) }
    
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("Operating Hours", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FStorePurple)
                        Text("Set when visibility is active each day.", fontSize = 11.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                    }

                    items(daysOfWeek) { day ->
                        val schedule = dailySchedules[day] ?: DaySchedule()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if(schedule.isEnabled) Color(0xFFF1F8E9) else Color(0xFFF5F5F5)),
                            border = BorderStroke(0.5.dp, if(schedule.isEnabled) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color.LightGray)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(day, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Switch(
                                        checked = schedule.isEnabled,
                                        onCheckedChange = { 
                                            val newMap = dailySchedules.toMutableMap()
                                            newMap[day] = schedule.copy(isEnabled = it)
                                            dailySchedules = newMap
                                        },
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }
                                
                                if (schedule.isEnabled) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        val range = schedule.timeRanges.firstOrNull() ?: TimeRange()
                                        OutlinedTextField(
                                            value = range.startTime,
                                            onValueChange = { t ->
                                                val newMap = dailySchedules.toMutableMap()
                                                newMap[day] = schedule.copy(timeRanges = listOf(range.copy(startTime = t)))
                                                dailySchedules = newMap
                                            },
                                            label = { Text("From") },
                                            modifier = Modifier.weight(1f),
                                            placeholder = { Text("08:00") },
                                            textStyle = TextStyle(fontSize = 12.sp)
                                        )
                                        Text("to", fontSize = 12.sp)
                                        OutlinedTextField(
                                            value = range.endTime,
                                            onValueChange = { t ->
                                                val newMap = dailySchedules.toMutableMap()
                                                newMap[day] = schedule.copy(timeRanges = listOf(range.copy(endTime = t)))
                                                dailySchedules = newMap
                                            },
                                            label = { Text("To") },
                                            modifier = Modifier.weight(1f),
                                            placeholder = { Text("22:00") },
                                            textStyle = TextStyle(fontSize = 12.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("Vacation / Long Absence", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
                        Text("Hide everything for a specific date range.", fontSize = 11.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start Date", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = vacationStart?.let { sdf.format(Date(it)) } ?: "",
                                    onValueChange = { },
                                    readOnly = true,
                                    placeholder = { Text("DD/MM/YYYY") },
                                    modifier = Modifier.fillMaxWidth().clickable { 
                                        vacationStart = System.currentTimeMillis()
                                    },
                                    trailingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("End Date", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = vacationEnd?.let { sdf.format(Date(it)) } ?: "",
                                    onValueChange = { },
                                    readOnly = true,
                                    placeholder = { Text("DD/MM/YYYY") },
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        vacationEnd = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L)
                                    },
                                    trailingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                        
                        if (vacationStart != null || vacationEnd != null) {
                            TextButton(onClick = { vacationStart = null; vacationEnd = null }) {
                                Text("Clear Vacation Dates", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(dailySchedules, vacationStart, vacationEnd)
            }, colors = ButtonDefaults.buttonColors(containerColor = FStorePurple)) {
                Text("SAVE SETTINGS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
