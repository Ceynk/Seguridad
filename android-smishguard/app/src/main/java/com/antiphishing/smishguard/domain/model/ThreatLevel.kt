package com.antiphishing.smishguard.domain.model

enum class ThreatLevel(val label: String, val colorHex: Long) {
    SAFE("Seguro", 0xFF00E676),
    SUSPICIOUS("Sospechoso", 0xFFFFD600),
    PHISHING("Amenaza de Smishing", 0xFFFF1744);

    fun isDangerous(): Boolean = this == PHISHING || this == SUSPICIOUS
}
