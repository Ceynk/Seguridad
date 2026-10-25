package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.domain.model.ThreatFactor

object SenderFraudAnalyzer {

    private val REMITENTES_ALFANUMERICOS_CONOCIDOS = setOf(
        "bancolombia", "nequi", "daviplata", "davivienda", "banco", "bancobogota", "bbva", "santander",
        "caixa", "caixabank", "correos", "dhl", "seur", "dgt", "aeat", "hacienda",
        "seguridad", "seg-social", "netflix", "bizum", "soporte", "alerta", "verificacion",
        "google", "whatsapp", "microsoft", "amazon", "apple", "pse", "dian", "simit",
        "servientrega", "coordinadora", "interrapidisimo", "4-72", "claro", "movistar",
        "tigo", "wom", "efecty", "transfiya", "colpass", "enel", "prosperidad"
    )

    private val PREFIJOS_DE_RIESGO = listOf(
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

    private val PATRON_TELEFONO_CELULAR = Regex("""^(?:\+?57)?3[0-9]{9}$|^(?:\+?34)?[67][0-9]{8}$""")
    private val PATRON_CODIGO_CORTO = Regex("""^[0-9]{4,6}$""")
    private val PATRON_ESPACIOS = Regex("""\s+""")

    fun analyzeSender(
        sender: String,
        hasMaliciousUrl: Boolean,
        hasOfficialUrlOnly: Boolean,
        mentionsFinancialEntityInBody: Boolean,
        isLegitimateOtpOrDefensive: Boolean
    ): List<ThreatFactor> {
        return analizarRemitente(
            remitente = sender,
            tieneEnlaceMalicioso = hasMaliciousUrl,
            soloEnlacesOficiales = hasOfficialUrlOnly,
            mencionaEntidadEnTexto = mentionsFinancialEntityInBody,
            esOtpLegitimoODefensivo = isLegitimateOtpOrDefensive
        )
    }

    fun analizarRemitente(
        remitente: String,
        tieneEnlaceMalicioso: Boolean,
        soloEnlacesOficiales: Boolean,
        mencionaEntidadEnTexto: Boolean,
        esOtpLegitimoODefensivo: Boolean
    ): List<ThreatFactor> {
        val factores = ArrayList<ThreatFactor>()
        val remitenteLimpio = remitente.trim().lowercase()

        var tieneLetras = false
        for (caracter in remitenteLimpio) {
            if (caracter.isLetter()) {
                tieneLetras = true
                break
            }
        }

        var coincideMarcaConocida = false
        for (marca in REMITENTES_ALFANUMERICOS_CONOCIDOS) {
            if (remitenteLimpio.contains(marca)) {
                coincideMarcaConocida = true
                break
            }
        }

        if (tieneLetras && coincideMarcaConocida) {
            if (tieneEnlaceMalicioso && !soloEnlacesOficiales) {
                factores.add(
                    ThreatFactor(
                        category = "Remitente Alfanumérico Suplantado",
                        description = "El mensaje utiliza un remitente alfanumérico ($remitente) comúnmente suplantado para distribuir enlaces fraudulentos.",
                        severityScore = 25,
                        matchedSnippet = remitente
                    )
                )
            }
        }

        val telefonoNormalizado = remitente.replace(PATRON_ESPACIOS, "")
        if (PATRON_TELEFONO_CELULAR.matches(telefonoNormalizado) && mencionaEntidadEnTexto && !esOtpLegitimoODefensivo) {
            factores.add(
                ThreatFactor(
                    category = "Uso de Línea Celular Privada para Notificación Bancaria",
                    description = "Las entidades bancarias y oficiales no emiten comunicaciones de seguridad desde líneas celulares privadas ($remitente).",
                    severityScore = 35,
                    matchedSnippet = remitente
                )
            )
        }

        var prefijoDetectado: String? = null
        for (prefijo in PREFIJOS_DE_RIESGO) {
            if (remitente.startsWith(prefijo)) {
                prefijoDetectado = prefijo
                break
            }
        }
        if (prefijoDetectado != null) {
            factores.add(
                ThreatFactor(
                    category = "Prefijo Telefónico Internacional de Riesgo",
                    description = "El mensaje proviene de una numeración internacional con alta incidencia de campañas de smishing y extorsión ($prefijoDetectado).",
                    severityScore = 30,
                    matchedSnippet = remitente
                )
            )
        }

        var cantidadMas = 0
        var todosDigitos = true
        for (caracter in remitente) {
            if (caracter == '+') {
                cantidadMas++
            }
            if (!caracter.isDigit()) {
                todosDigitos = false
            }
        }

        val esDemasiadoCortoYNoNumerico = (remitente.length in 1..3) && !todosDigitos
        if (cantidadMas > 1 || esDemasiadoCortoYNoNumerico) {
            factores.add(
                ThreatFactor(
                    category = "Identificador de Remitente Anómalo",
                    description = "El número o identificador del remitente presenta una estructura no conforme a los estándares de telecomunicación.",
                    severityScore = 15,
                    matchedSnippet = remitente
                )
            )
        }

        return factores
    }

    fun isCertifiedShortcode(sender: String): Boolean {
        return esCodigoCortoCertificado(sender)
    }

    fun esCodigoCortoCertificado(remitente: String): Boolean {
        return PATRON_CODIGO_CORTO.matches(remitente.trim())
    }
}
