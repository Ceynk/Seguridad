package com.antiphishing.smishguard.domain.model

enum class ThreatLevel(val label: String, val colorHex: Long) {
    SAFE("Seguro", 0xFF0F766E),
    SUSPICIOUS("Sospechoso", 0xFF9A5B06),
    PHISHING("Amenaza de Smishing", 0xFFA61B29);
}
