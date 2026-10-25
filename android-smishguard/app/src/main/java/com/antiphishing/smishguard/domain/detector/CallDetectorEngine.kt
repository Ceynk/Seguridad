package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.domain.model.CallAction
import com.antiphishing.smishguard.domain.model.CallAnalysisResult
import com.antiphishing.smishguard.domain.model.ThreatFactor
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.util.SafeRegex
import kotlin.math.max
import kotlin.math.min

object CallDetectorEngine {

    const val DEFAULT_BLOCK_THRESHOLD = 60
    const val AGGRESSIVE_THRESHOLD_DELTA = 15
    private const val MIN_THRESHOLD = 20

    private val WANGIRI_HIGH_RISK_DESTINATIONS = linkedMapOf(
        "247" to "Isla Ascensión (Fraude Wangiri frecuente)",
        "229" to "Benín (Fraude de tarificación internacional)",
        "225" to "Costa de Marfil (Estafas telefónicas)",
        "234" to "Nigeria (Campañas de fraude y extorsión)",
        "212" to "Marruecos (Wangiri / Ping calls)",
        "675" to "Papúa Nueva Guinea (Llamadas de tarificación no autorizada)",
        "881" to "Telefonía Satelital Global (Altísimo costo por minuto)",
        "882" to "Redes Satelitales Internacionales (Cobro especial)",
        "883" to "Redes Internacionales Compartidas (Cobro especial)",
        "231" to "Liberia (Wangiri)",
        "232" to "Sierra Leona (Wangiri)",
        "269" to "Comoras (Wangiri)",
        "387" to "Bosnia y Herzegovina (Prefijo abusado en devoluciones de llamada)",
        "355" to "Albania (Fraude telefónico)",
        "371" to "Letonia (Numeración de tarificación especial)",
        "880" to "Bangladés (Spam masivo y Wangiri)",
        "222" to "Mauritania (Wangiri)",
        "252" to "Somalia (Wangiri)",
        "960" to "Maldivas (Wangiri)",
        "682" to "Islas Cook (Tarificación especial)",
        "690" to "Tokelau (Tarificación especial)"
    )

    private val PREMIUM_RATE_PATTERNS = listOf(
        Regex("""^(?:\+34|0034)?(?:905|907|803|806|807)[0-9]{6}$"""),
        Regex("""^(?:\+57|0057)?(?:01900|01901|901)[0-9]{6,7}$"""),
        Regex("""^(?:\+52|0052)?(?:01900|900)[0-9]{7}$""")
    )

    private val EMERGENCY_SHORT_NUMBERS = setOf(
        "123", "112", "911", "132", "144", "155", "106", "122", "165", "060", "091", "092", "141", "016", "024"
    )

    private val CLEAN_CHARS = Regex("""[\s\-().]""")

    fun analyzeCall(
        incomingNumber: String,
        isWhitelisted: Boolean = false,
        userNumber: String? = null,
        customBlockPatterns: List<Regex> = emptyList(),
        blockThreshold: Int = DEFAULT_BLOCK_THRESHOLD,
        aggressiveMode: Boolean = false,
        networkVerificationFailed: Boolean = false
    ): CallAnalysisResult {
        val cleanNumber = incomingNumber.trim().replace(CLEAN_CHARS, "")
        val effectiveThreshold = if (aggressiveMode) {
            max(MIN_THRESHOLD, blockThreshold - AGGRESSIVE_THRESHOLD_DELTA)
        } else {
            blockThreshold
        }

        if (isWhitelisted) {
            return CallAnalysisResult(
                phoneNumber = incomingNumber,
                threatScore = 0,
                level = ThreatLevel.SAFE,
                isBlocked = false,
                action = CallAction.ALLOW,
                factors = emptyList(),
                countryOrOrigin = null,
                summaryReason = "Número verificado en Lista Blanca de confianza."
            )
        }

        if (EMERGENCY_SHORT_NUMBERS.contains(cleanNumber)) {
            return CallAnalysisResult(
                phoneNumber = incomingNumber,
                threatScore = 0,
                level = ThreatLevel.SAFE,
                isBlocked = false,
                action = CallAction.ALLOW,
                factors = emptyList(),
                countryOrOrigin = "Servicio de Emergencia Oficial",
                summaryReason = "Número de emergencia o auxilio público legítimo."
            )
        }

        val factors = mutableListOf<ThreatFactor>()
        var detectedCountryOrOrigin: String? = null

        if (cleanNumber.isEmpty()) {
            val emptyScore = if (aggressiveMode) 70 else 30
            factors.add(
                ThreatFactor(
                    category = "Número Oculto o Privado",
                    description = "La llamada no presenta identificador. Es una táctica habitual en extorsión y vishing.",
                    severityScore = emptyScore,
                    matchedSnippet = null
                )
            )
            detectedCountryOrOrigin = "Identificador oculto"
        }

        if (networkVerificationFailed) {
            factors.add(
                ThreatFactor(
                    category = "Identificador Falsificado (Verificación de Red Fallida)",
                    description = "La red del operador reporta que el número mostrado no superó la verificación STIR/SHAKEN: probable Caller ID Spoofing.",
                    severityScore = 50,
                    matchedSnippet = incomingNumber
                )
            )
        }

        val internationalDigits = if (cleanNumber.startsWith("+")) {
            cleanNumber.substring(1)
        } else if (cleanNumber.startsWith("00")) {
            cleanNumber.substring(2)
        } else {
            null
        }

        if (internationalDigits != null) {
            val riskDestination = WANGIRI_HIGH_RISK_DESTINATIONS.entries
                .firstOrNull { internationalDigits.startsWith(it.key) }
            if (riskDestination != null) {
                val (prefix, description) = riskDestination
                detectedCountryOrOrigin = description
                factors.add(
                    ThreatFactor(
                        category = "Riesgo Crítico de Wangiri (Llamada Perdida Fraudulenta)",
                        description = "Llamada proveniente de destino internacional de alto riesgo ($description). Diseñada para inducir devolución de llamada y generar cargos excesivos.",
                        severityScore = 65,
                        matchedSnippet = "+$prefix"
                    )
                )
            }
        }

        if (PREMIUM_RATE_PATTERNS.any { it.matches(cleanNumber) }) {
            factors.add(
                ThreatFactor(
                    category = "Línea de Tarificación Adicional (Sobrecoste)",
                    description = "El número pertenece a un rango de tarificación especial con cobro adicional elevado.",
                    severityScore = 60,
                    matchedSnippet = cleanNumber
                )
            )
        }

        if (cleanNumber.isNotEmpty()) {
            val plusCount = cleanNumber.count { it == '+' }
            val hasInvalidChars = cleanNumber.any { !it.isDigit() && it != '+' }

            val hasMultiplePlus = plusCount > 1 || cleanNumber.lastIndexOf('+') > 0
            if (hasMultiplePlus || hasInvalidChars) {
                factors.add(
                    ThreatFactor(
                        category = "Identificador de Llamada Malformado (Posible Spoofing)",
                        description = "El identificador de la llamada contiene caracteres ilegales o estructura incompatible con ITU-T E.164.",
                        severityScore = 40,
                        matchedSnippet = incomingNumber
                    )
                )
            }

            val digitsOnly = cleanNumber.filter(Char::isDigit)
            if (digitsOnly.length < 5 && !EMERGENCY_SHORT_NUMBERS.contains(digitsOnly)) {
                factors.add(
                    ThreatFactor(
                        category = "Número de Longitud Excesivamente Corta",
                        description = "El número tiene una cantidad de dígitos insuficiente para un abonado telefónico estándar.",
                        severityScore = 35,
                        matchedSnippet = incomingNumber
                    )
                )
            } else if (digitsOnly.length > 15) {
                factors.add(
                    ThreatFactor(
                        category = "Número Excede Longitud Máxima E.164",
                        description = "El número supera el estándar internacional de 15 dígitos.",
                        severityScore = 35,
                        matchedSnippet = incomingNumber
                    )
                )
            }
        }

        if (userNumber != null && userNumber.isNotBlank() && cleanNumber.isNotEmpty()) {
            val cleanUser = userNumber.trim().replace(CLEAN_CHARS, "")
            if (cleanUser.length >= 8 && cleanNumber.length >= 8) {
                val userPrefix = cleanUser.substring(0, 6)
                val incomingPrefix = cleanNumber.substring(0, 6)
                if (userPrefix == incomingPrefix && cleanUser != cleanNumber) {
                    factors.add(
                        ThreatFactor(
                            category = "Neighbor Spoofing (Suplantación de Proximidad)",
                            description = "El número imita el prefijo local o de operador del usuario para generar falsa confianza.",
                            severityScore = 25,
                            matchedSnippet = incomingPrefix
                        )
                    )
                }
            }
        }

        if (cleanNumber.isNotEmpty()) {
            val matchingRule = customBlockPatterns.firstOrNull {
                SafeRegex.containsMatch(it, cleanNumber) == true
            }
            if (matchingRule != null) {
                factors.add(
                    ThreatFactor(
                        category = "Regla de Bloqueo Telefónico Personalizada",
                        description = "Coincidencia con un filtro de llamadas configurado por el usuario (${matchingRule.pattern}).",
                        severityScore = 100,
                        matchedSnippet = matchingRule.pattern
                    )
                )
            }
        }

        val totalScore = factors.sumOf { it.severityScore }
        val boundedScore = min(100, max(0, totalScore))

        val threatLevel = when {
            boundedScore >= 60 -> ThreatLevel.PHISHING
            boundedScore >= 30 -> ThreatLevel.SUSPICIOUS
            else -> ThreatLevel.SAFE
        }

        val isBlocked = boundedScore >= effectiveThreshold

        val action = when {
            isBlocked -> CallAction.BLOCK
            threatLevel != ThreatLevel.SAFE -> CallAction.WARN
            else -> CallAction.ALLOW
        }

        val summaryReason = when (action) {
            CallAction.BLOCK -> "Llamada maliciosa bloqueada automáticamente ($boundedScore% de riesgo detectado)."
            CallAction.WARN -> "Llamada sospechosa: procedencia o numeración con anomalías."
            CallAction.ALLOW -> "Número verificado sin anomalías de riesgo."
        }

        val displayPhone = if (incomingNumber.isBlank()) {
            "Número oculto"
        } else {
            incomingNumber
        }

        return CallAnalysisResult(
            phoneNumber = displayPhone,
            threatScore = boundedScore,
            level = threatLevel,
            isBlocked = isBlocked,
            action = action,
            factors = factors,
            countryOrOrigin = detectedCountryOrOrigin,
            summaryReason = summaryReason
        )
    }
}
