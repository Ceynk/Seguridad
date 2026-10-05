package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.domain.model.AnalysisResult
import com.antiphishing.smishguard.domain.model.ThreatFactor
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.util.SafeRegex
import kotlin.math.max
import kotlin.math.min

object SmishDetectorEngine {

    const val DEFAULT_QUARANTINE_THRESHOLD = 50

    /** Reducción del umbral cuando el usuario activa el modo heurístico agresivo. */
    const val AGGRESSIVE_THRESHOLD_DELTA = 15
    private const val MIN_THRESHOLD = 20
    private const val MAX_BODY_LENGTH = 4000
    private const val MAX_CUSTOM_RULE_INPUT = 2000

    private val FINANCIAL_ENTITY_REGEX = Regex(
        """(?i)\b(?:banco|bancolombia|nequi|daviplata|davivienda|bbva|santander|caixa|dian|pse|scotiabank|colpatria|bancoagrario)\b"""
    )

    private val IP_DEFANG_REGEX = Regex("""\b(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})\b""")

    /**
     * Categorías de ingeniería social que NUNCA deben neutralizarse por parecer un OTP.
     * Evita la evasión trivial de añadir "válido por 5 minutos" o "no lo comparta" a un fraude.
     */
    private val NON_NEUTRALIZABLE_CATEGORIES = listOf(
        "Suplantación Familiar",
        "Extracción Activa de Credenciales",
        "Extracción de Token",
        "Falsos Préstamos",
        "Falsas Recompensas",
        "Estafa de Paquetería",
        "Suplantación de Organismo Público",
        "Fraude Fiscal",
        "Suplantación de Operador"
    )

    fun analyze(
        sender: String,
        body: String,
        isWhitelisted: Boolean = false,
        customBlockPatterns: List<Regex> = emptyList(),
        quarantineThreshold: Int = DEFAULT_QUARANTINE_THRESHOLD,
        aggressiveMode: Boolean = false
    ): AnalysisResult {
        // 1. Verificación de Lista Blanca
        if (isWhitelisted) {
            return AnalysisResult(
                threatScore = 0,
                level = ThreatLevel.SAFE,
                isQuarantined = false,
                factors = emptyList(),
                sanitizedBody = body,
                extractedUrls = emptyList(),
                homoglyphsDetected = emptyList(),
                summaryReason = "Remitente verificado en Lista Blanca."
            )
        }

        val effectiveThreshold = if (aggressiveMode) {
            max(MIN_THRESHOLD, quarantineThreshold - AGGRESSIVE_THRESHOLD_DELTA)
        } else quarantineThreshold

        // Acotar la longitud de entrada para proteger el motor contra ReDoS y sobreconsumo de CPU
        val boundedBody = if (body.length > MAX_BODY_LENGTH) body.substring(0, MAX_BODY_LENGTH) else body
        val factors = mutableListOf<ThreatFactor>()

        // 2. Detección y normalización de Homoglifos, caracteres invisibles y Leetspeak
        val homoglyphs = HomographDetector.findHomoglyphs(boundedBody)
        if (homoglyphs.isNotEmpty()) {
            factors.add(
                ThreatFactor(
                    category = "Ataque Homógrafo / Evasión de Filtros",
                    description = "El mensaje contiene caracteres camuflados, invisibles o Leetspeak para eludir la detección.",
                    severityScore = 35,
                    matchedSnippet = homoglyphs.joinToString("; ")
                )
            )
        }
        val normalizedBody = HomographDetector.normalizeHomoglyphs(boundedBody)

        // 3. Extracción y análisis exhaustivo de URLs
        val extractedUrls = UrlThreatAnalyzer.extractUrls(boundedBody)
        val urlFactors = UrlThreatAnalyzer.analyzeUrls(extractedUrls)
        factors.addAll(urlFactors)

        val hasMaliciousUrl = urlFactors.isNotEmpty()
        val allUrlsLegitimate = extractedUrls.isNotEmpty() && UrlThreatAnalyzer.areAllUrlsLegitimate(extractedUrls)
        val hasUnverifiedUrl = extractedUrls.isNotEmpty() && !allUrlsLegitimate

        // 4. Evaluación de patrones de ingeniería social (antes del remitente para poder
        //    invalidar la heurística de "OTP legítimo" cuando hay tácticas de fraude duras)
        val rawPatternFactors = PatternRuleEngine.evaluatePatterns(normalizedBody)
        val hasHardSocialEngineering = rawPatternFactors.any { f ->
            NON_NEUTRALIZABLE_CATEGORIES.any { f.category.contains(it) }
        }

        // 5. Verificación de contexto: ¿Es un OTP o notificación defensiva legítima?
        val looksLikeOtp = PatternRuleEngine.isLegitimateOtpNotification(normalizedBody, hasMaliciousUrl)
        val isLegitimateOtp = looksLikeOtp && !hasHardSocialEngineering
        val mentionsFinancial = FINANCIAL_ENTITY_REGEX.containsMatchIn(normalizedBody)

        // 6. Análisis del remitente
        val senderFactors = SenderFraudAnalyzer.analyzeSender(
            sender = sender,
            hasMaliciousUrl = hasMaliciousUrl,
            hasOfficialUrlOnly = allUrlsLegitimate,
            mentionsFinancialEntityInBody = mentionsFinancial,
            isLegitimateOtpOrDefensive = isLegitimateOtp && !hasUnverifiedUrl
        )
        factors.addAll(senderFactors)

        val filteredPatternFactors = mutableListOf<ThreatFactor>()
        for (pFactor in rawPatternFactors) {
            // Mitigación de Falsos Positivos: Si es un OTP legítimo sin URLs maliciosas,
            // no penalizar la mención de banco/clave dinámica
            if (isLegitimateOtp && !hasMaliciousUrl) {
                if (pFactor.category.contains("Billetera") || pFactor.category.contains("Bancario")) {
                    // Es un OTP genuino informando una clave o código; se descarta como factor de amenaza
                    continue
                }
            }
            filteredPatternFactors.add(pFactor)
        }
        factors.addAll(filteredPatternFactors)

        // 7. Cálculo de Puntuación Ponderada y Contextual
        var totalScore = factors.sumOf { it.severityScore }

        // Correlación de ataque: Si convergen vector URL malicioso y patrones de ingeniería social
        if (hasMaliciousUrl && filteredPatternFactors.isNotEmpty()) {
            totalScore = (totalScore * 1.35).toInt()
        }

        // Correlación de ataque: Celular privado enviando URLs y haciéndose pasar por banco
        if (senderFactors.any { it.category.contains("Celular Privada") } && hasMaliciousUrl) {
            totalScore = (totalScore * 1.4).toInt()
        }

        // ATENUADORES DE CONFIANZA (Trust Mitigators):
        // Solo se confía plenamente en un OTP si no hay URL maliciosa, ni URL no verificada,
        // ni prefijo internacional de riesgo, ni tácticas de ingeniería social duras.
        val trustOtp = isLegitimateOtp && !hasMaliciousUrl && !hasUnverifiedUrl &&
            !senderFactors.any { it.category.contains("Internacional") }
        if (trustOtp) {
            totalScore = 0
            factors.clear()
        } else if (allUrlsLegitimate && !hasMaliciousUrl && !hasHardSocialEngineering) {
            // Si todas las URLs pertenecen a dominios oficiales reconocidos, amortiguar significativamente
            totalScore = (totalScore * 0.25).toInt()
        }

        // 8. Reglas Personalizadas del Usuario: se aplican DESPUÉS de los atenuadores porque
        //    expresan una decisión explícita del usuario y deben aislar el mensaje por sí solas.
        var customRuleMatched = false
        if (customBlockPatterns.isNotEmpty()) {
            val safeBody = if (body.length > MAX_CUSTOM_RULE_INPUT) body.substring(0, MAX_CUSTOM_RULE_INPUT) else body
            val safeNorm = if (normalizedBody.length > MAX_CUSTOM_RULE_INPUT) normalizedBody.substring(0, MAX_CUSTOM_RULE_INPUT) else normalizedBody
            for (customRegex in customBlockPatterns) {
                val matched = SafeRegex.containsMatch(customRegex, safeBody) == true ||
                    SafeRegex.containsMatch(customRegex, safeNorm) == true ||
                    SafeRegex.containsMatch(customRegex, sender) == true
                if (matched) {
                    customRuleMatched = true
                    factors.add(
                        ThreatFactor(
                            category = "Regla Personalizada de Bloqueo",
                            description = "Coincidencia con una regla definida manualmente por el usuario (${customRegex.pattern}).",
                            severityScore = 40,
                            matchedSnippet = customRegex.pattern
                        )
                    )
                    break
                }
            }
        }
        if (customRuleMatched) {
            totalScore = max(totalScore + 40, effectiveThreshold)
        }

        val boundedScore = min(100, max(0, totalScore))

        val threatLevel = when {
            boundedScore >= 60 -> ThreatLevel.PHISHING
            boundedScore >= 35 -> ThreatLevel.SUSPICIOUS
            else -> ThreatLevel.SAFE
        }

        val isQuarantined = boundedScore >= effectiveThreshold
        val sanitizedBody = defangText(body, extractedUrls)

        val summaryReason = when {
            trustOtp && boundedScore == 0 -> "Mensaje de autenticación o código de seguridad verificado (Legítimo)."
            customRuleMatched && threatLevel != ThreatLevel.PHISHING -> "Mensaje aislado por una regla personalizada del usuario."
            threatLevel == ThreatLevel.PHISHING -> "Alto riesgo de Smishing detectado (${factors.size} factores de riesgo identificados)."
            threatLevel == ThreatLevel.SUSPICIOUS -> "Mensaje sospechoso con posibles elementos de coerción o anomalías."
            else -> "No se han detectado factores maliciosos conocidos."
        }

        return AnalysisResult(
            threatScore = boundedScore,
            level = threatLevel,
            isQuarantined = isQuarantined,
            factors = factors,
            sanitizedBody = sanitizedBody,
            extractedUrls = extractedUrls,
            homoglyphsDetected = homoglyphs,
            summaryReason = summaryReason
        )
    }

    fun defangText(text: String, extractedUrls: List<String> = emptyList()): String {
        var result = text

        // Neutralizar primero las URLs extraídas exactas (ordenadas por longitud descendente para evitar colisiones de prefijo)
        val sortedUrls = extractedUrls.sortedByDescending { it.length }
        for (rawUrl in sortedUrls) {
            val defangedUrl = rawUrl
                .replace("https://", "hxxps://", ignoreCase = true)
                .replace("http://", "hxxp://", ignoreCase = true)
                .replace(".", "[.]")
            result = result.replace(rawUrl, defangedUrl)
        }

        result = result
            .replace("https://", "hxxps://", ignoreCase = true)
            .replace("http://", "hxxp://", ignoreCase = true)
            .replace("www.", "w[w]w[.]", ignoreCase = true)

        // Neutralizar direcciones IP numéricas
        result = IP_DEFANG_REGEX.replace(result) { match ->
            "${match.groupValues[1]}[.]${match.groupValues[2]}[.]${match.groupValues[3]}[.]${match.groupValues[4]}"
        }

        return result
    }
}
