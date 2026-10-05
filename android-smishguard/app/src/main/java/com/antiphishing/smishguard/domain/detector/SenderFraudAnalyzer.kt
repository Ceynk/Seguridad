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
        "+234", // Nigeria
        "+229", // Benín
        "+225", // Costa de Marfil
        "+212", // Marruecos
        "+247", // Isla Ascensión
        "+675", // Papúa Nueva Guinea
        "+881", // Satelital
        "+882", // Satelital
        "+7",   // Rusia / Kazajistán
        "+880"  // Bangladés
    )

    // Expresión regular para números celulares estándar (Colombia: +57 3xx, España: +34 6xx/7xx, etc.)
    private val MOBILE_PHONE_REGEX = Regex("""^(?:\+?57)?3[0-9]{9}$|^(?:\+?34)?[67][0-9]{8}$""")

    // Expresión regular para códigos cortos bancarios/empresariales legítimos (ej: 85454, 89000, etc.)
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

        // 1. Remitente alfanumérico que imita marcas reconocidas
        val isAlphanumeric = cleanSender.any { it.isLetter() }
        val matchesKnownBrand = REPUTABLE_ALPHANUMERIC_SENDERS.any { cleanSender.contains(it) }

        if (isAlphanumeric && matchesKnownBrand) {
            // Solo se marca como suplantación si viene acompañado de URLs maliciosas o solicitudes sospechosas
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
            // Si no tiene URLs maliciosas y es un OTP legítimo, NO se penaliza (es el canal oficial)
        }

        // 2. Número celular privado haciéndose pasar por entidad bancaria / oficial
        // Si el remitente es un teléfono móvil particular pero en el cuerpo simula ser un banco o institución
        if (MOBILE_PHONE_REGEX.matches(sender.replace("\\s+".toRegex(), "")) && mentionsFinancialEntityInBody && !isLegitimateOtpOrDefensive) {
            factors.add(
                ThreatFactor(
                    category = "Uso de Línea Celular Privada para Notificación Bancaria",
                    description = "Las entidades bancarias y oficiales no emiten comunicaciones de seguridad desde líneas celulares privadas ($sender).",
                    severityScore = 35,
                    matchedSnippet = sender
                )
            )
        }

        // 3. Prefijos telefónicos internacionales de alto riesgo (fraude transfronterizo)
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

        // 4. Identificador con anomalías estructurales
        if (sender.count { it == '+' } > 1 || (sender.length in 1..3 && !sender.all { it.isDigit() })) {
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

    /**
     * Retorna verdadero si el remitente parece ser un código corto empresarial (ej. 85454).
     */
    fun isCertifiedShortcode(sender: String): Boolean {
        return SHORTCODE_REGEX.matches(sender.trim())
    }
}
