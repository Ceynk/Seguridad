package com.antiphishing.smishguard.domain.model

data class SmsMessageData(
    val id: Long = 0,
    val sender: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val threatScore: Int = 0,
    val threatLevel: ThreatLevel = ThreatLevel.SAFE,
    val isQuarantined: Boolean = false,
    val threatFactors: List<ThreatFactor> = emptyList(),
    val extractedUrls: List<String> = emptyList(),
    val wasRestored: Boolean = false
)
