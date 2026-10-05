package com.antiphishing.smishguard.domain.model

data class ThreatFactor(
    val category: String,
    val description: String,
    val severityScore: Int,
    val matchedSnippet: String? = null
)
