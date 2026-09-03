package com.fryumm.store.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fryumm.store.data.model.UserProfile
import com.fryumm.store.data.model.UserRole
import com.fryumm.store.data.repository.NotificationRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) : AndroidViewModel(application) {

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowSuccess(val message: String) : UiEvent()
    }

    val masterAdmins = listOf("ranjanamishra1117@gmail.com", "supreet.mishr@gmail.com")

    var currentUserProfile by mutableStateOf<UserProfile?>(null)
        private set

    val isMasterAdmin: Boolean
        get() = currentUserProfile?.email?.lowercase()?.trim() in masterAdmins

    var isAuthLoading by mutableStateOf(true)
        private set

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 64)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        setupSession()
    }

    fun setupSession(onSessionReady: (() -> Unit)? = null) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            isAuthLoading = false
            onSessionReady?.invoke()
            return
        }

        viewModelScope.launch {
            try {
                val collections = listOf("users", "sellers", "riders", "staff", "customers")
                var doc: com.google.firebase.firestore.DocumentSnapshot? = null
                for (coll in collections) {
                    try {
                        val d = firestore.collection(coll).document(currentUser.uid).get().await()
                        if (d.exists()) {
                            doc = d
                            break
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error checking collection $coll for UID ${currentUser.uid}", e)
                    }
                }

                if (doc != null && doc.exists()) {
                    val profile = try { doc.toObject(UserProfile::class.java) } catch (e: Exception) { null }
                    currentUserProfile = profile?.let { 
                        it.copy(
                            email = it.email.ifEmpty { currentUser.email ?: "" },
                            name = it.name.ifEmpty { currentUser.displayName ?: "User" },
                            photoUrl = it.photoUrl.ifEmpty { currentUser.photoUrl?.toString() ?: "" }
                        )
                    } ?: UserProfile(
                        uid = currentUser.uid, 
                        name = currentUser.displayName ?: "User", 
                        email = currentUser.email ?: "",
                        photoUrl = currentUser.photoUrl?.toString() ?: ""
                    )
                } else {
                    val displayName = currentUser.displayName.takeIf { !it.isNullOrBlank() }
                        ?: currentUser.email?.substringBefore("@")
                        ?: "Partner User"
                    val newProfile = UserProfile(
                        uid = currentUser.uid, 
                        email = currentUser.email ?: "", 
                        name = displayName, 
                        photoUrl = currentUser.photoUrl?.toString() ?: "",
                        role = if (currentUser.email?.lowercase() in masterAdmins) "ADMIN" else "STORE_OWNER"
                    )
                    try {
                        val targetCollection = if (newProfile.role == "ADMIN") "users" else "sellers"
                        firestore.collection(targetCollection).document(currentUser.uid).set(newProfile).await()
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error saving new profile to Firestore", e)
                    }
                    currentUserProfile = newProfile
                }
                
                currentUserProfile?.let { profile ->
                    if (profile.uid.isNotEmpty()) {
                        val tokenRole = when(profile.userRole) {
                            UserRole.ADMIN -> "admin"
                            UserRole.RIDER -> "delivery"
                            else -> "seller"
                        }
                        notificationRepository.updateToken(profile.uid, tokenRole)
                    }
                }
                onSessionReady?.invoke()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Auth System Error", e)
                if (currentUserProfile == null) {
                    currentUserProfile = UserProfile(
                        uid = currentUser.uid,
                        email = currentUser.email ?: "",
                        name = currentUser.displayName ?: "User",
                        photoUrl = currentUser.photoUrl?.toString() ?: "",
                        role = if (currentUser.email?.lowercase() in masterAdmins) "ADMIN" else "STORE_OWNER"
                    )
                }
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun login(email: String, pass: String, onSuccess: (() -> Unit)? = null) {
        isAuthLoading = true
        val cleanInput = email.trim()
        
        viewModelScope.launch {
            try {
                var targetEmail = cleanInput
                
                if (!cleanInput.contains("@")) {
                    val collections = listOf("users", "sellers", "riders", "staff", "customers")
                    var resolvedEmail: String? = null
                    for (coll in collections) {
                        try {
                            val byPhone = firestore.collection(coll)
                                .whereEqualTo("phoneNumber", cleanInput)
                                .limit(1).get().await()
                            if (!byPhone.isEmpty) {
                                resolvedEmail = byPhone.documents[0].getString("email")
                                if (!resolvedEmail.isNullOrBlank()) break
                            }
                            
                            val byMobile = firestore.collection(coll)
                                .whereEqualTo("mobile", cleanInput)
                                .limit(1).get().await()
                            if (!byMobile.isEmpty) {
                                resolvedEmail = byMobile.documents[0].getString("email")
                                if (!resolvedEmail.isNullOrBlank()) break
                            }

                            val byName = firestore.collection(coll)
                                .whereEqualTo("name", cleanInput)
                                .limit(1).get().await()
                            if (!byName.isEmpty) {
                                resolvedEmail = byName.documents[0].getString("email")
                                if (!resolvedEmail.isNullOrBlank()) break
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Username/Phone Lookup Error in $coll", e)
                        }
                    }
                    targetEmail = resolvedEmail ?: "${cleanInput.lowercase()}@flash-o.com"
                }

                auth.signInWithEmailAndPassword(targetEmail, pass).await()
                setupSession(onSuccess)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login Exception", e)
                val msg = e.localizedMessage ?: e.message ?: ""
                val friendlyMessage = when {
                    msg.contains("invalid-credential", ignoreCase = true) ||
                    msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                    msg.contains("wrong-password", ignoreCase = true) ->
                        "Incorrect Email/Username or Password. Please check your details."
                    msg.contains("user-not-found", ignoreCase = true) ->
                        "No account found with this Email/Username. Please register first."
                    msg.contains("invalid-email", ignoreCase = true) ->
                        "Please enter a valid email address."
                    msg.contains("user-disabled", ignoreCase = true) ->
                        "This account has been disabled. Please contact support."
                    msg.contains("too-many-requests", ignoreCase = true) ->
                        "Too many failed login attempts. Please try again later."
                    else -> "Login failed: $msg"
                }
                _uiEvent.emit(UiEvent.ShowError(friendlyMessage))
                isAuthLoading = false
            }
        }
    }

    fun loginWithGoogle(credential: AuthCredential, onSuccess: (() -> Unit)? = null) {
        isAuthLoading = true
        viewModelScope.launch {
            try { 
                auth.signInWithCredential(credential).await()
                setupSession(onSuccess)
            } catch (e: Exception) { 
                Log.e("AuthViewModel", "Google Sign-In Error", e)
                val msg = e.localizedMessage ?: e.message ?: ""
                val friendlyMessage = when {
                    msg.contains("user-disabled", ignoreCase = true) ->
                        "This Google account has been disabled."
                    else -> "Google Sign-In failed: $msg"
                }
                _uiEvent.emit(UiEvent.ShowError(friendlyMessage))
                isAuthLoading = false 
            }
        }
    }

    fun register(
        email: String,
        pass: String,
        name: String,
        phone: String,
        customRole: UserRole? = null,
        staffDepartment: String? = null,
        deviceId: String = "",
        onSuccess: (() -> Unit)? = null
    ) {
        // STRICT SECURITY RULE 1: Absolute Admin Lock. Absolutely no new Admin roles permitted!
        if (customRole == UserRole.ADMIN) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("SECURITY VIOLATION: Creating new Admin accounts is strictly prohibited! No new Admin roles can ever be created.")) }
            return
        }

        if (phone.length < 10) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Invalid Phone Number. Must be at least 10 digits.")) }
            return
        }
        
        isAuthLoading = true
        viewModelScope.launch {
            try {
                val cleanPhone = phone.trim()
                val cleanEmail = email.trim().lowercase()
                val cleanDeviceId = deviceId.trim()
                val roleStr = customRole?.name ?: "STORE_OWNER"

                var isDuplicateDetected = false
                val duplicateFlags = mutableListOf<String>()

                if (roleStr == "CUSTOMER") {
                    // FLEXIBLE CUSTOMER REGISTRATION POLICY:
                    // Registration is NEVER blocked for customers (users change phones, share devices, forget emails).
                    // Instead, if duplicate Device ID, Mobile, or Email is detected, we log an Admin Fraud Alert
                    // and freeze Referral Rewards pending Staff verification, while allowing normal orders without friction.
                    val existingPhone1 = firestore.collection("customers").whereEqualTo("phoneNumber", cleanPhone).get().await()
                    val existingPhone2 = firestore.collection("customers").whereEqualTo("mobile", cleanPhone).get().await()
                    val existingEmail = if (cleanEmail.isNotEmpty()) firestore.collection("customers").whereEqualTo("email", cleanEmail).get().await() else null
                    val existingDevice = if (cleanDeviceId.isNotEmpty()) firestore.collection("customers").whereEqualTo("deviceId", cleanDeviceId).get().await() else null

                    if (!existingPhone1.isEmpty || !existingPhone2.isEmpty) duplicateFlags.add("SHARED_MOBILE")
                    if (existingEmail != null && !existingEmail.isEmpty) duplicateFlags.add("SHARED_EMAIL")
                    if (existingDevice != null && !existingDevice.isEmpty) duplicateFlags.add("SHARED_DEVICE_ID")

                    if (duplicateFlags.isNotEmpty()) {
                        isDuplicateDetected = true
                    }
                } else {
                    // Multi-Role Support: Same Mobile/Email/Device ID IS allowed across different roles (Merchant, Rider, Staff).
                    // We only check for duplicate within the specific target role collection.
                    val targetCollection = when (roleStr) {
                        "RIDER" -> "riders"
                        "OTHER" -> "staff"
                        else -> "sellers"
                    }
                    val existing1 = firestore.collection(targetCollection).whereEqualTo("phoneNumber", cleanPhone).get().await()
                    val existing2 = firestore.collection(targetCollection).whereEqualTo("mobile", cleanPhone).get().await()
                    if (!existing1.isEmpty || !existing2.isEmpty) {
                        _uiEvent.emit(UiEvent.ShowError("An account with Mobile Number $cleanPhone already exists in $targetCollection!"))
                        isAuthLoading = false
                        return@launch
                    }
                }

                val result = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
                val user = result.user
                if (user != null) {
                    val targetCollection = when (roleStr) {
                        "RIDER" -> "riders"
                        "OTHER" -> "staff"
                        "CUSTOMER" -> "customers"
                        else -> "sellers"
                    }
                    val newProfile = UserProfile(
                        uid = user.uid, 
                        email = cleanEmail, 
                        name = name, 
                        role = roleStr,
                        phoneNumber = cleanPhone,
                        staffDepartment = staffDepartment ?: if (roleStr == "OTHER") "General Operations" else ""
                    )
                    firestore.collection(targetCollection).document(user.uid).set(newProfile).await()
                    
                    // If Customer role, write additional CustomerProfile record with Device ID and Referral status
                    if (roleStr == "CUSTOMER") {
                        val freezeReason = if (isDuplicateDetected) {
                            "Multi-Account Registration Alert: Shared ${duplicateFlags.joinToString(", ")} detected on registration. Referral rewards held for Staff verification."
                        } else ""

                        val customerProfileData = mapOf(
                            "id" to user.uid,
                            "name" to name,
                            "email" to cleanEmail,
                            "mobile" to cleanPhone,
                            "deviceId" to cleanDeviceId,
                            "joinedDate" to System.currentTimeMillis(),
                            "isDuplicateDeviceDetected" to isDuplicateDetected,
                            "registrationDuplicateFlags" to duplicateFlags,
                            "referralEarningsFrozen" to isDuplicateDetected,
                            "referralVerificationStatus" to if (isDuplicateDetected) "FROZEN_PENDING_VERIFICATION" else "VERIFIED",
                            "referralEarningsFreezeReason" to freezeReason
                        )
                        firestore.collection("customers").document(user.uid).set(customerProfileData, com.google.firebase.firestore.SetOptions.merge()).await()

                        if (isDuplicateDetected) {
                            // Log Fraud Alert Incident for Admin / Staff Dashboard
                            val incidentMap = mapOf(
                                "id" to "ALERT_${System.currentTimeMillis()}",
                                "userId" to user.uid,
                                "userName" to name,
                                "userRole" to "CUSTOMER",
                                "incidentType" to "MULTI_ACCOUNT_REGISTRATION_ALERT",
                                "riskLevel" to "MEDIUM",
                                "description" to "Customer Registered with existing parameters: ${duplicateFlags.joinToString(", ")}. Account ACTIVE for orders; Referral rewards pending Staff review.",
                                "deviceId" to cleanDeviceId,
                                "timestamp" to System.currentTimeMillis(),
                                "isResolved" to false
                            )
                            firestore.collection("fraud_incidents").document(incidentMap["id"] as String).set(incidentMap).await()
                        }
                    }

                    currentUserProfile = newProfile
                    setupSession(onSuccess)
                    _uiEvent.emit(UiEvent.ShowSuccess("Account Created Successfully for $cleanPhone!"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Registration Failed: ${e.localizedMessage ?: e.message}"))
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Please enter a valid email address above to reset password")) }
            return
        }
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(cleanEmail).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Password reset link sent to $cleanEmail"))
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password reset error", e)
                _uiEvent.emit(UiEvent.ShowError("Password reset failed: ${e.localizedMessage ?: e.message}"))
            }
        }
    }

    fun logout(onLoggedOut: (() -> Unit)? = null) {
        auth.signOut()
        currentUserProfile = null
        onLoggedOut?.invoke()
    }
}
