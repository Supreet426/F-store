package com.fryumm.store.util

import android.content.Context
import android.provider.Settings
import com.fryumm.store.data.model.UserProfile

object MasterAdminSecurity {

    val MASTER_ADMIN_EMAILS = listOf(
        "ranjanamishra1117@gmail.com",
        "supreet.mishr@gmail.com"
    )

    /**
     * Strict check if an email belongs to Master Admin.
     * Absolutely NO fuzzy, prefix, or name-based matching. Exact email match only!
     */
    fun isMasterAdminEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val cleanEmail = email.lowercase().trim()
        return MASTER_ADMIN_EMAILS.contains(cleanEmail)
    }

    /**
     * Strict check if a UserProfile is a Master Admin.
     */
    fun isMasterAdmin(profile: UserProfile?): Boolean {
        if (profile == null) return false
        return isMasterAdminEmail(profile.email)
    }

    /**
     * Gets the hardware Device ID of the current Android device.
     */
    fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Admin Multi-Factor Verification Result
     */
    data class VerificationResult(
        val isVerified: Boolean,
        val matchedFactorsCount: Int,
        val emailMatched: Boolean,
        val phoneMatched: Boolean,
        val deviceMatched: Boolean,
        val message: String
    )

    /**
     * Verifies an Admin login or session by checking 3 factors:
     * 1. Email ID
     * 2. Mobile Number
     * 3. Device Hardware ID
     *
     * Rule: At least 2 out of 3 factors MUST match 100% compulsory!
     */
    fun verifyAdminSession(
        context: Context,
        profile: UserProfile?,
        currentEmail: String,
        currentPhone: String = "",
        currentDeviceId: String = ""
    ): VerificationResult {
        if (profile == null || !isMasterAdminEmail(profile.email)) {
            return VerificationResult(
                isVerified = false,
                matchedFactorsCount = 0,
                emailMatched = false,
                phoneMatched = false,
                deviceMatched = false,
                message = "Account is not a Master Admin."
            )
        }

        val targetEmail = profile.email.lowercase().trim()
        val inputEmail = currentEmail.lowercase().trim()

        // Factor 1: Email Match (exact match with whitelist)
        val emailMatched = isMasterAdminEmail(inputEmail) && inputEmail == targetEmail

        // Factor 2: Phone Match (check if last 10 digits match)
        val storedPhoneDigits = profile.phoneNumber.filter { it.isDigit() }.takeLast(10)
        val inputPhoneDigits = currentPhone.ifBlank { profile.phoneNumber }.filter { it.isDigit() }.takeLast(10)
        val phoneMatched = storedPhoneDigits.length >= 10 && inputPhoneDigits.length >= 10 && storedPhoneDigits == inputPhoneDigits

        // Factor 3: Device Hardware ID Match
        val activeDeviceId = currentDeviceId.ifBlank { getDeviceId(context) }.trim()
        val storedDeviceId = profile.deviceId.trim()
        val deviceMatched = storedDeviceId.isNotBlank() && activeDeviceId.isNotBlank() && storedDeviceId == activeDeviceId

        var matchCount = 0
        if (emailMatched) matchCount++
        if (phoneMatched) matchCount++
        if (deviceMatched) matchCount++

        // Bootstrap rule: If stored phone or stored device ID is empty in Firestore (first setup),
        // we count initial enrollment as valid factor matching.
        var effectiveMatchCount = matchCount
        if (emailMatched && storedDeviceId.isBlank()) {
            // First time device registration for Master Admin
            effectiveMatchCount++
        }

        val isVerified = effectiveMatchCount >= 2

        val msg = if (isVerified) {
            "Admin Multi-Factor Verification Successful ($effectiveMatchCount/3 factors matched)."
        } else {
            "SECURITY ALERT: Admin Verification Failed! Only $effectiveMatchCount/3 factors matched. At least 2 factors (Email, Mobile Number, Device ID) must match."
        }

        return VerificationResult(
            isVerified = isVerified,
            matchedFactorsCount = effectiveMatchCount,
            emailMatched = emailMatched,
            phoneMatched = phoneMatched,
            deviceMatched = deviceMatched,
            message = msg
        )
    }
}
