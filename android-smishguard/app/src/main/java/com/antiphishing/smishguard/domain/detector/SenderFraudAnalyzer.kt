package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.domain.model.ThreatFactor

object SenderFraudAnalyzer {

    private val REPUTABLE_ALPHANUMERIC_SENDERS = setOf(
        "bancolombia", "nequi", "daviplata", "davivienda", "banco", "bbva", "santander",
        "caixa", "caixabank", "correos", "dhl", "seur", "dgt", "aeat", "hacienda",
        "seguridad", "seg-social", "netflix", "bizum", "soporte", "alerta", "verificacion",
        "google", "whatsapp", "microsoft", "amazon", "apple", "pse", "dian"
    )

    private val HIGH_RISK_PREFIXES = listOf(
        "+234",
        "+229",
        "+225",
        "+212",
        "+247",
        "+675",
        "+881",
        "+882",
        "+7",
        "+880"
    )

    private val MOBILE_PHONE_REGEX = Regex("""^(?:\+?57)?3[0-9]{9}$|^(?:\+?34)?[67][0-9]{8}$""")
    private val SHORTCODE_REGEX = Regex("""^[0-9]{4,6}$""")

    fun analyzeSender(
        sender: String,
        hasMaliciousUrl: Boolean,
        hasOfficialUrlOnly: Boolean,
        mentionsFinancialEntityInBody: Boolean,
        isLegitimateOtpOrDefensive: Boolean
    ): List<ThreatFactor> {
        val factors = mutableListOf<ThreatFactor>()
        val cleanSender = sender.trim().lowercase()

        var isAlphanumeric = false
        for (i in 0 until cleanSender.length) {
            if (cleanSender[i].isLetter()) {
                isAlphanumeric = true
                break
            }
        }

        var matchesKnownBrand = false
        for (brand in REPUTABLE_ALPHANUMERIC_SENDERS) {
            if (cleanSender.contains(brand)) {
                matchesKnownBrand = true
                break
            }
        }

        if (isAlphanumeric && matchesKnownBrand) {
            if (hasMaliciousUrl) {
                factors.add(
                    ThreatFactor(
                        category = "Remitente Alfanumérico Suplantado",
                        description = "El mensaje utiliza un remitente alfanumérico ($sender) comúnmente suplantado para distribuir enlaces fraudulentos.",
                        severityScore = 25,
                        matchedSnippet = sender
                    )
                )
            }
        }

        val normalizedPhone = sender.replace("\\s+".toRegex(), "")
        if (MOBILE_PHONE_REGEX.matches(normalizedPhone) && mentionsFinancialEntityInBody && !isLegitimateOtpOrDefensive) {
            factors.add(
                ThreatFactor(
                    category = "Uso de Línea Celular Privada para Notificación Bancaria",
                    description = "Las entidades bancarias y oficiales no emiten comunicaciones de seguridad desde líneas celulares privadas ($sender).",
                    severityScore = 35,
                    matchedSnippet = sender
                )
            )
        }

        for (prefix in HIGH_RISK_PREFIXES) {
            if (sender.startsWith(prefix)) {
                factors.add(
                    ThreatFactor(
                        category = "Prefijo Telefónico Internacional de Riesgo",
                        description = "El mensaje proviene de una numeración internacional con alta incidencia de campañas de smishing y extorsión ($prefix).",
                        severityScore = 30,
                        matchedSnippet = sender
                    )
                )
                break
            }
        }

        var plusCount = 0
        var allDigits = true
        for (i in 0 until sender.length) {
            val ch = sender[i]
            if (ch == '+') {
                plusCount++
            }
            if (!ch.isDigit()) {
                allDigits = false
            }
        }

        val isTooShortAndNonDigit = (sender.length in 1..3) && !allDigits
        if (plusCount > 1 || isTooShortAndNonDigit) {
            factors.add(
                ThreatFactor(
                    category = "Identificador de Remitente Anómalo",
                    description = "El número o identificador del remitente presenta una estructura no conforme a los estándares de telecomunicación.",
                    severityScore = 15,
                    matchedSnippet = sender
                )
            )
        }

        return factors
    }

    fun isCertifiedShortcode(sender: String): Boolean {
        return SHORTCODE_REGEX.matches(sender.trim())
    }
}
