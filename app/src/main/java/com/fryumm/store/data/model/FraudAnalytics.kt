package com.fryumm.store.data.model

data class FraudAnalytics(
    val totalFlaggedUsers: Int,
    val totalRiskAmount: Double,
    val multipleAccounts: List<FraudIncident>,
    val referralFraud: List<FraudIncident>,
    val couponAbuse: List<FraudIncident>,
    val highReturners: List<FraudIncident>,
    val codAbuse: List<FraudIncident>,
    val gpsAnomaly: List<FraudIncident>,
    val paymentSuspicion: List<FraudIncident>
)

data class FraudIncident(
    val customerId: String,
    val customerName: String,
    val reason: String,
    val riskLevel: RiskLevel,
    val evidence: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}
