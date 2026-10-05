package com.antiphishing.smishguard.domain.model

data class AnalysisResult(
    val threatScore: Int,
    val level: ThreatLevel,
    val isQuarantined: Boolean,
    val factors: List<ThreatFactor>,
    val sanitizedBody: String,
    val extractedUrls: List<String>,
    val homoglyphsDetected: List<String>,
    val summaryReason: String
)
