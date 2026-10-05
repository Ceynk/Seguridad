package com.antiphishing.smishguard.domain.model

enum class CallAction {
    ALLOW,
    WARN,
    BLOCK
}

data class CallAnalysisResult(
    val phoneNumber: String,
    val threatScore: Int,
    val level: ThreatLevel,
    val isBlocked: Boolean,
    val action: CallAction,
    val factors: List<ThreatFactor>,
    val countryOrOrigin: String?,
    val summaryReason: String
)
