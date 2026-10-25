package com.fryumm.store.ui.screens.useractivity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.*
import com.fryumm.store.ui.theme.FStorePurple
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit

@Composable
fun UserProfileDetailsDialog(user: UserProfile, viewModel: StoreViewModel, onDismiss: () -> Unit) {
    var isEditMode by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(user.name) }
    var editPhone by remember { mutableStateOf(user.phoneNumber) }
    var editEmail by remember { mutableStateOf(user.email) }
    var editDept by remember { mutableStateOf(user.staffDepartment) }
    var showBlockReasonDialog by remember { mutableStateOf(false) }
    var blockReasonInput by remember { mutableStateOf("") }

    if (showBlockReasonDialog) {
        AlertDialog(
            onDismissRequest = { showBlockReasonDialog = false },
            title = { Text("Block Account Reason", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = blockReasonInput,
                    onValueChange = { blockReasonInput = it },
                    label = { Text("Reason for blocking") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateUserStatus(user.uid, false, blockReasonInput)
                    showBlockReasonDialog = false
                    onDismiss()
                }) { Text("BLOCK") }
            },
            dismissButton = { TextButton(onClick = { showBlockReasonDialog = false }) { Text("CANCEL") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isEditMode) "Edit User Profile" else if (user.userRole == UserRole.OTHER) "Staff Profile" else "User Profile", fontWeight = FontWeight.Black)
                IconButton(onClick = { isEditMode = !isEditMode }) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = "Toggle Edit Mode",
                        tint = FStorePurple
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditMode) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Mobile Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (user.userRole == UserRole.OTHER) {
                        OutlinedTextField(
                            value = editDept,
                            onValueChange = { editDept = it },
                            label = { Text("Staff Department / Scope") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(if (user.email.isNotEmpty()) user.email else "No Email", color = Color.Gray, fontSize = 14.sp)
                    if (user.phoneNumber.isNotEmpty()) {
                        Text("Phone: ${user.phoneNumber}", fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    
                    Surface(
                        color = if (user.userRole == UserRole.ADMIN) Color(0xFFE8EAF6) else Color(0xFFE0F2F1),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ROLE: ${user.userRole.name}",
                            fontWeight = FontWeight.Bold,
                            color = if (user.userRole == UserRole.ADMIN) Color(0xFF1A237E) else Color(0xFF00695C),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp
                        )
                    }

                    if (user.userRole == UserRole.OTHER && user.staffDepartment.isNotEmpty()) {
                        Text("Department / Scope: ${user.staffDepartment}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FStorePurple)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Account Status: ", fontSize = 13.sp)
                        Text(
                            if (user.active) "ACTIVE" else "BLOCKED", 
                            fontWeight = FontWeight.Bold, 
                            color = if (user.active) Color(0xFF2E7D32) else Color.Red,
                            fontSize = 13.sp
                        )
                    }

                    if (!user.active && !user.blockReason.isNullOrEmpty()) {
                        Text("Reason: ${user.blockReason}", fontSize = 12.sp, color = Color.Red)
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    // Actions
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (user.active) {
                            OutlinedButton(
                                onClick = { showBlockReasonDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("BLOCK", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { 
                                    viewModel.updateUserStatus(user.uid, true)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("UNBLOCK", fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (user.email.isNotEmpty()) viewModel.sendPasswordResetEmail(user.email)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RESET PASS", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEditMode) {
                Button(
                    onClick = {
                        val updatedProfile = user.copy(
                            name = editName,
                            phoneNumber = editPhone,
                            email = editEmail,
                            staffDepartment = editDept
                        )
                        viewModel.updateUserProfileDetails(updatedProfile)
                        isEditMode = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SAVE CHANGES")
                }
            } else {
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE") }
            }
        },
        dismissButton = {
            if (isEditMode) {
                TextButton(onClick = { isEditMode = false }) { Text("CANCEL") }
            }
        }
    )
}

@Composable
fun RoleChangeDialog(
    userName: String,
    currentRole: String,
    onDismiss: () -> Unit,
    onConfirmNewRole: (String) -> Unit
) {
    var selectedRole by remember { mutableStateOf(currentRole.ifEmpty { "CUSTOMER" }) }
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("CUSTOMER", "MERCHANT", "RIDER", "OTHER", "ADMIN")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Role for $userName", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select new role for this user:", fontSize = 13.sp, color = Color.Gray)
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Role: $selectedRole", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        roles.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r, fontWeight = if (r == selectedRole) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedRole = r
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmNewRole(selectedRole); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("UPDATE ROLE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun OrderAnalyticsDetailsDialog(order: Order, onDismiss: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order Analytics: #${order.id}", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AnalyticsDetailRow("Order Time", sdf.format(Date(order.timestamp)))
                AnalyticsDetailRow("Delivery Time", order.deliveryTime?.let { sdf.format(Date(it)) } ?: "Not Delivered")
                AnalyticsDetailRow("Coupon", order.couponCode ?: "None")
                AnalyticsDetailRow("Discount", "₹${order.discountAmount}")
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text("Items", fontWeight = FontWeight.Bold, color = FStorePurple)
                order.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.product.name} x ${item.quantity}", fontSize = 12.sp)
                        Text("₹${item.product.price * item.quantity}", fontSize = 12.sp)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                AnalyticsDetailRow("Estimated Profit", "₹${order.estimatedProfit.toInt()}", isHighlight = true)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE") }
        }
    )
}

@Composable
fun AnalyticsDetailRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = if (isHighlight) FontWeight.Black else FontWeight.Medium, color = if (isHighlight) Color(0xFF4CAF50) else Color.Black)
    }
}

@Composable
fun AddUserDialog(
    initialRole: UserRole, 
    isMasterAdmin: Boolean, 
    onDismiss: () -> Unit, 
    onConfirm: (String, String, String, String, UserRole, String?) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(initialRole) }
    var staffDept by remember { mutableStateOf("Helpline & Customer Support") }
    var roleExpanded by remember { mutableStateOf(false) }
    var deptExpanded by remember { mutableStateOf(false) }

    val availableRoles = listOf(UserRole.STORE_OWNER, UserRole.RIDER, UserRole.OTHER)

    val departments = listOf(
        "Helpline & Customer Support",
        "Cancellation Management",
        "Website Management",
        "Advertisement & Marketing",
        "Technical Operations"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add User", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { if(it.length <= 10) phone = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Initial Password") }, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { roleExpanded = true }, modifier = Modifier.fillMaxWidth()) { 
                        Text("Role: ${if (role == UserRole.OTHER) "Staff / Tech Worker" else role.name}")
                        Icon(Icons.Default.ArrowDropDown, null) 
                    }
                    DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        availableRoles.forEach { r -> 
                            DropdownMenuItem(
                                text = { Text(if (r == UserRole.OTHER) "Staff / Tech Worker" else r.name) }, 
                                onClick = { role = r; roleExpanded = false }
                            ) 
                        }
                    }
                }

                if (role == UserRole.OTHER) {
                    Box {
                        OutlinedButton(onClick = { deptExpanded = true }, modifier = Modifier.fillMaxWidth()) { 
                            Text("Dept: $staffDept", fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, null) 
                        }
                        DropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                            departments.forEach { d -> 
                                DropdownMenuItem(
                                    text = { Text(d, fontSize = 12.sp) }, 
                                    onClick = { staffDept = d; deptExpanded = false }
                                ) 
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Button(onClick = { 
                if(email.isNotBlank() && password.length >= 6 && phone.length == 10) {
                    onConfirm(email, password, name, phone, role, if (role == UserRole.OTHER) staffDept else null) 
                }
            }) { Text("CREATE") } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
