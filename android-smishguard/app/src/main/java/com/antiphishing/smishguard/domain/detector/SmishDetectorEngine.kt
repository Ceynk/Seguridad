package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity
import com.antiphishing.smishguard.data.repository.CommunityIndicatorNormalizer
import com.antiphishing.smishguard.domain.model.AnalysisResult
import com.antiphishing.smishguard.domain.model.ThreatFactor
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.util.SafeRegex
import kotlin.math.max
import kotlin.math.min

object SmishDetectorEngine {

    const val DEFAULT_QUARANTINE_THRESHOLD = 50
    const val AGGRESSIVE_THRESHOLD_DELTA = 15
    private const val UMBRAL_MINIMO = 20
    private const val LONGITUD_MAXIMA_MENSAJE = 4000
    private const val LONGITUD_MAXIMA_REGLA_PERSONALIZADA = 2000

    private val PATRON_ENTIDAD_PROTEGIDA = Regex(
        """(?i)\b(?:banco|bancolombia|nequi|daviplata|davivienda|bbva|santander|caixa|dian|pse|scotiabank|colpatria|bancoagrario|bancobogota|banco\s+de\s+bogot[aá]|avvillas|cajasocial|itau|lulobank|nubank|transfiya|efecty|simit|servientrega|coordinadora|interrapidisimo|4-72|correos|dgt|claro|movistar|tigo|wom|colpass|enel|prosperidad\s+social)\b"""
    )

    private val PATRON_CELULAR_PRIVADO = Regex("""^(?:\+?57)?3[0-9]{9}$|^(?:\+?34)?[67][0-9]{8}$""")
    private val PATRON_ESPACIOS = Regex("""\s+""")
    private val PATRON_IP_NEUTRALIZAR = Regex("""\b(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})\b""")

    private val CATEGORIAS_NO_NEUTRALIZABLES = listOf(
        "Suplantación Familiar",
        "Extracción Activa de Credenciales",
        "Extracción de Token",
        "Desvío Telefónico",
        "Falsos Préstamos",
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
        aggressiveMode: Boolean = false,
        communityEntries: Map<String, CommunityBlacklistEntity> = emptyMap(),
        reputationSource: DomainReputationSource = SinReputacionExterna
    ): AnalysisResult {
        return analizarMensaje(
            remitente = sender,
            mensaje = body,
            estaEnListaBlanca = isWhitelisted,
            patronesPersonalizados = customBlockPatterns,
            umbralCuarentena = quarantineThreshold,
            modoAgresivo = aggressiveMode,
            entradasComunidad = communityEntries,
            fuenteReputacion = reputationSource
        )
    }

    fun analizarMensaje(
        remitente: String,
        mensaje: String,
        estaEnListaBlanca: Boolean = false,
        patronesPersonalizados: List<Regex> = emptyList(),
        umbralCuarentena: Int = DEFAULT_QUARANTINE_THRESHOLD,
        modoAgresivo: Boolean = false,
        entradasComunidad: Map<String, CommunityBlacklistEntity> = emptyMap(),
        fuenteReputacion: DomainReputationSource = SinReputacionExterna
    ): AnalysisResult {
        if (estaEnListaBlanca) {
            return AnalysisResult(
                threatScore = 0,
                level = ThreatLevel.SAFE,
                isQuarantined = false,
                factors = emptyList(),
                sanitizedBody = mensaje,
                extractedUrls = emptyList(),
                homoglyphsDetected = emptyList(),
                summaryReason = "Remitente verificado en Lista Blanca.",
                categoriaMensaje = ClasificadorMensajesSms.CATEGORIA_LEGITIMO
            )
        }

        val umbralEfectivo = if (modoAgresivo) {
            max(UMBRAL_MINIMO, umbralCuarentena - AGGRESSIVE_THRESHOLD_DELTA)
        } else {
            umbralCuarentena
        }

        val mensajeAcotado = if (mensaje.length > LONGITUD_MAXIMA_MENSAJE) {
            mensaje.substring(0, LONGITUD_MAXIMA_MENSAJE)
        } else {
            mensaje
        }

        val factores = ArrayList<ThreatFactor>()

        val homoglifos = HomographDetector.findHomoglyphs(mensajeAcotado)
        if (homoglifos.isNotEmpty()) {
            factores.add(
                ThreatFactor(
                    category = "Ataque Homógrafo / Evasión de Filtros",
                    description = "El mensaje contiene caracteres camuflados, invisibles o Leetspeak para eludir la detección.",
                    severityScore = 35,
                    matchedSnippet = homoglifos.joinToString("; ")
                )
            )
        }

        val mensajeNormalizado = HomographDetector.normalizeHomoglyphs(mensajeAcotado)
        val enlacesDetectados = extraerEnlacesUnicos(mensajeAcotado, mensajeNormalizado)

        val factoresEnlaces = UrlThreatAnalyzer.analizarEnlaces(enlacesDetectados)
        factores.addAll(factoresEnlaces)

        val marcaRemitente = DetectorMarcas.buscarEnRemitente(remitente)
        val marcaTexto = DetectorMarcas.buscarEnTexto(mensajeAcotado)
        val enlacesExternos = filtrarEnlacesExternos(enlacesDetectados, remitente, marcaRemitente, factoresEnlaces)

        if (marcaRemitente != null && !marcaRemitente.esExacta) {
            factores.add(
                ThreatFactor(
                    category = "Suplantación de Marca en Remitente",
                    description = "El remitente '$remitente' imita visualmente a la entidad '${marcaRemitente.marca}'.",
                    severityScore = 25,
                    matchedSnippet = remitente
                )
            )
        }

        val todosEnlacesLegitimos = enlacesDetectados.isNotEmpty() &&
            UrlThreatAnalyzer.sonTodosEnlacesLegitimos(enlacesDetectados)
        val tieneEnlaceNoVerificado = enlacesExternos.isNotEmpty()

        val coincidenciaEntidadTexto = PATRON_ENTIDAD_PROTEGIDA.find(mensajeNormalizado)
        val coincidenciaEntidadRemitente = PATRON_ENTIDAD_PROTEGIDA.find(remitente)
        val mencionaEntidad = coincidenciaEntidadTexto != null || coincidenciaEntidadRemitente != null ||
            marcaTexto != null || marcaRemitente != null

        val yaTieneSuplantacionMarcaUrl = factoresEnlaces.any {
            it.category.contains("Suplantación de Marca")
        }

        if (mencionaEntidad && tieneEnlaceNoVerificado && !yaTieneSuplantacionMarcaUrl) {
            val nombreEntidad = coincidenciaEntidadTexto?.value
                ?: coincidenciaEntidadRemitente?.value
                ?: marcaRemitente?.marca
                ?: marcaTexto?.marca
                ?: remitente
            factores.add(
                ThreatFactor(
                    category = "Dominio No Oficial para Entidad Mencionada",
                    description = "El mensaje hace referencia a '$nombreEntidad' pero dirige a un dominio externo no oficial.",
                    severityScore = 25,
                    matchedSnippet = enlacesExternos.firstOrNull()
                )
            )
        }

        val tieneEnlaceMalicioso = factoresEnlaces.any { it.severityScore >= 20 } ||
            (mencionaEntidad && tieneEnlaceNoVerificado)

        var reputacionMaliciosa = false
        for (enlace in enlacesExternos) {
            val dominio = UrlThreatAnalyzer.extraerDominioDeEnlace(enlace)
            if (fuenteReputacion.consultar(dominio) == ReputacionDominio.MALICIOSO) {
                reputacionMaliciosa = true
                factores.add(
                    ThreatFactor(
                        category = "Reputación Externa de Dominio",
                        description = "Una fuente de reputación reporta el dominio $dominio como malicioso.",
                        severityScore = 60,
                        matchedSnippet = dominio
                    )
                )
            }
        }

        val marcaMencionada = marcaRemitente?.marca ?: marcaTexto?.marca
        val entidadSuplantada = if (
            marcaMencionada != null &&
            enlacesExternos.any {
                !DetectorMarcas.dominioEsOficialParaMarca(UrlThreatAnalyzer.extraerDominioDeEnlace(it), marcaMencionada)
            }
        ) marcaMencionada else null
        val remitenteSinEspaciosCtx = remitente.trim().replace(PATRON_ESPACIOS, "")
        val resultadoContextual = AnalizadorContextual.analizar(
            AnalizadorContextual.Entrada(
                textoNormalizado = NormalizadorTexto.normalizar(mensajeAcotado),
                tieneEnlaceExterno = tieneEnlaceNoVerificado,
                entidadSuplantada = entidadSuplantada,
                remitenteImitaMarca = marcaRemitente?.takeIf { !it.esExacta }?.marca,
                tieneRiesgoTecnicoDominio = factoresEnlaces.any { CATEGORIAS_RIESGO_TECNICO.contains(it.category) },
                acortadorDesdeCelular = factoresEnlaces.any { it.category.contains("Enlace Acortado") } &&
                    PATRON_CELULAR_PRIVADO.matches(remitenteSinEspaciosCtx)
            )
        )
        factores.addAll(resultadoContextual.factores)

        val resultadoClasificacion = ClasificadorMensajesSms.clasificarMensaje(
            remitente = remitente,
            mensaje = mensajeAcotado
        )

        val factoresPatronesBrutos = PatternRuleEngine.evaluarPatrones(mensajeNormalizado)
        val tieneIngenieriaSocialFuerte = resultadoContextual.combinacionCritica ||
            detectarIngenieriaSocialFuerte(
                factoresPatronesBrutos = factoresPatronesBrutos,
                tieneEnlaces = enlacesDetectados.isNotEmpty(),
                clasificacionMl = resultadoClasificacion.categoriaPredicha
            )

        val esOtpLegitimo = PatternRuleEngine.esNotificacionOtpLegitima(mensajeNormalizado, tieneEnlaceMalicioso) &&
            !tieneIngenieriaSocialFuerte
        val esTransaccionInformativa = PatternRuleEngine.esNotificacionTransaccionalInformativa(
            mensajeNormalizado,
            tieneEnlaceNoVerificado
        ) && !tieneEnlaceMalicioso && !tieneIngenieriaSocialFuerte
        val esAvisoPresencialLegitimo = PatternRuleEngine.esAvisoSeguridadPresencialLegitimo(
            mensajeNormalizado,
            enlacesDetectados.isNotEmpty()
        ) && !tieneIngenieriaSocialFuerte

        val factoresRemitente = SenderFraudAnalyzer.analizarRemitente(
            remitente = remitente,
            tieneEnlaceMalicioso = tieneEnlaceMalicioso,
            soloEnlacesOficiales = todosEnlacesLegitimos,
            mencionaEntidadEnTexto = mencionaEntidad,
            esOtpLegitimoODefensivo = (esOtpLegitimo || esTransaccionInformativa || esAvisoPresencialLegitimo) &&
                !tieneEnlaceNoVerificado
        )
        factores.addAll(factoresRemitente)

        val factoresPatronesFiltrados = filtrarPatronesContextuales(
            factoresBrutos = factoresPatronesBrutos,
            esOtpLegitimo = esOtpLegitimo,
            esAvisoPresencialLegitimo = esAvisoPresencialLegitimo,
            tieneEnlaceMalicioso = tieneEnlaceMalicioso,
            tieneEnlaces = enlacesDetectados.isNotEmpty(),
            tieneIngenieriaSocialFuerte = tieneIngenieriaSocialFuerte
        )
        factores.addAll(factoresPatronesFiltrados)

        agregarFactoresModeloMl(
            remitente = remitente,
            resultadoClasificacion = resultadoClasificacion,
            factores = factores,
            factoresEnlaces = factoresEnlaces,
            factoresPatrones = factoresPatronesFiltrados,
            factoresRemitente = factoresRemitente,
            tieneEnlaceNoVerificado = tieneEnlaceNoVerificado,
            todosEnlacesLegitimos = todosEnlacesLegitimos,
            esOtpLegitimo = esOtpLegitimo,
            esTransaccionInformativa = esTransaccionInformativa,
            esAvisoPresencialLegitimo = esAvisoPresencialLegitimo
        )

        val puntajeCalculado = calcularRiesgo(
            remitente = remitente,
            mensajeOriginal = mensaje,
            mensajeNormalizado = mensajeNormalizado,
            factores = factores,
            factoresEnlaces = factoresEnlaces,
            factoresPatrones = factoresPatronesFiltrados,
            factoresRemitente = factoresRemitente,
            tieneEnlaceMalicioso = tieneEnlaceMalicioso,
            tieneEnlaceNoVerificado = tieneEnlaceNoVerificado,
            todosEnlacesLegitimos = todosEnlacesLegitimos,
            tieneIngenieriaSocialFuerte = tieneIngenieriaSocialFuerte,
            esOtpLegitimo = esOtpLegitimo,
            esTransaccionInformativa = esTransaccionInformativa,
            esAvisoPresencialLegitimo = esAvisoPresencialLegitimo,
            resultadoClasificacion = resultadoClasificacion,
            patronesPersonalizados = patronesPersonalizados,
            umbralEfectivo = umbralEfectivo,
            entradasComunidad = entradasComunidad
        )

        val tieneEvidenciaCritica = resultadoContextual.combinacionCritica ||
            reputacionMaliciosa ||
            homoglifos.isNotEmpty() ||
            (mencionaEntidad && tieneEnlaceNoVerificado) ||
            factoresEnlaces.any { CATEGORIAS_ENLACE_CRITICO.contains(it.category) } ||
            factoresPatronesFiltrados.any { CATEGORIAS_PATRON_CRITICO.contains(it.category) }

        val puntajeBase = if (
            !tieneEvidenciaCritica &&
            !puntajeCalculado.coincidenciaComunidadMaliciosa &&
            !puntajeCalculado.coincidenciaReglaPersonalizada
        ) {
            min(puntajeCalculado.puntajeAcotado, PUNTAJE_MAXIMO_NO_CRITICO)
        } else {
            puntajeCalculado.puntajeAcotado
        }
        val nivelPorPuntaje = when {
            puntajeBase >= UMBRAL_MALICIOSO -> ThreatLevel.PHISHING
            puntajeBase >= UMBRAL_SOSPECHOSO -> ThreatLevel.SUSPICIOUS
            else -> ThreatLevel.SAFE
        }
        val combinacionVigente = factores.any { it.category == "Combinación de Riesgo" } &&
            (!todosEnlacesLegitimos || resultadoContextual.combinacionCritica)
        val nivelMinimo = when {
            reputacionMaliciosa -> ThreatLevel.PHISHING
            combinacionVigente -> resultadoContextual.nivelMinimo
            else -> ThreatLevel.SAFE
        }
        val nivelAmenaza = if (nivelMinimo.ordinal > nivelPorPuntaje.ordinal) nivelMinimo else nivelPorPuntaje
        val puntajeFinal = when (nivelAmenaza) {
            ThreatLevel.PHISHING -> max(puntajeBase, UMBRAL_MALICIOSO)
            ThreatLevel.SUSPICIOUS -> max(puntajeBase, UMBRAL_SOSPECHOSO)
            ThreatLevel.SAFE -> puntajeBase
        }

        val enCuarentena = puntajeFinal >= umbralEfectivo
        val textoNeutralizado = neutralizarTexto(mensaje, enlacesDetectados)

        val categoriaFinal = when {
            enCuarentena || nivelAmenaza == ThreatLevel.PHISHING -> ClasificadorMensajesSms.CATEGORIA_SMISHING
            resultadoClasificacion.categoriaPredicha == ClasificadorMensajesSms.CATEGORIA_SPAM &&
                resultadoClasificacion.probabilidadSpam >= 0.42 &&
                !tieneEnlaceMalicioso &&
                !tieneIngenieriaSocialFuerte -> ClasificadorMensajesSms.CATEGORIA_SPAM
            nivelAmenaza == ThreatLevel.SUSPICIOUS -> ClasificadorMensajesSms.CATEGORIA_SMISHING
            else -> ClasificadorMensajesSms.CATEGORIA_LEGITIMO
        }

        val resumenMotivo = construirResumenMotivo(
            puntajeCalculado = puntajeCalculado,
            puntajeFinal = puntajeFinal,
            nivelAmenaza = nivelAmenaza,
            categoriaFinal = categoriaFinal,
            factores = factores
        )

        return AnalysisResult(
            threatScore = puntajeFinal,
            level = nivelAmenaza,
            isQuarantined = enCuarentena,
            factors = factores,
            sanitizedBody = textoNeutralizado,
            extractedUrls = enlacesDetectados,
            homoglyphsDetected = homoglifos,
            summaryReason = resumenMotivo,
            categoriaMensaje = categoriaFinal
        )
    }

    private val CATEGORIAS_RIESGO_TECNICO = setOf(
        "Dirección IP Directa",
        "Alta Entropía en Dominio",
        UrlThreatAnalyzer.CATEGORIA_PUNYCODE,
        UrlThreatAnalyzer.CATEGORIA_DOMINIO_ALEATORIO,
        UrlThreatAnalyzer.CATEGORIA_SUBDOMINIO_ENGANOSO
    )

    private val CATEGORIAS_ENLACE_CRITICO = setOf(
        "Suplantación de Marca en URL",
        "Dirección IP Directa",
        "Servicio de Alojamiento Gratuito o Túnel",
        UrlThreatAnalyzer.CATEGORIA_PUNYCODE,
        UrlThreatAnalyzer.CATEGORIA_SUBDOMINIO_ENGANOSO
    )

    private val CATEGORIAS_PATRON_CRITICO = setOf(
        "Fraude Bancario y Financiero",
        "Suplantación de Organismo Público",
        "Extracción Activa de Credenciales o Clave",
        "Extracción de Token Dinámico u OTP",
        "Falsos Préstamos y Créditos Rápidos (Gota a Gota)",
        "Suplantación de Billetera Digital o Banco (Nequi/Daviplata/Bancolombia)",
        "Fraude Fiscal o Cobro Coactivo (DIAN/Tránsito)",
        "Falso Débito o Alerta PSE",
        "Suplantación de Operador Móvil / Bloqueo IMEI"
    )

    const val UMBRAL_MALICIOSO = 60
    const val UMBRAL_SOSPECHOSO = 35
    private const val PUNTAJE_MAXIMO_NO_CRITICO = 55

    private fun filtrarEnlacesExternos(
        enlaces: List<String>,
        remitente: String,
        marcaRemitente: DetectorMarcas.MarcaDetectada?,
        factoresEnlaces: List<ThreatFactor>
    ): List<String> {
        val remitenteLetras = NormalizadorTexto.soloLetras(remitente)
        val sinRiesgoEnUrl = factoresEnlaces.none { it.severityScore >= 15 }
        val externos = ArrayList<String>()
        for (enlace in enlaces) {
            val dominio = UrlThreatAnalyzer.extraerDominioDeEnlace(enlace)
            if (dominio.isBlank() || UrlThreatAnalyzer.esDominioOficialGeneral(dominio)) continue
            val etiqueta = UrlThreatAnalyzer.obtenerDominioRegistrable(dominio).substringBefore(".")
            val coherenteConRemitente = sinRiesgoEnUrl && marcaRemitente == null &&
                remitenteLetras.length >= 3 && etiqueta == remitenteLetras
            if (!coherenteConRemitente) externos.add(enlace)
        }
        return externos
    }

    private fun extraerEnlacesUnicos(mensajeOriginal: String, mensajeNormalizado: String): List<String> {
        val enlacesCombinados = LinkedHashSet<String>()
        enlacesCombinados.addAll(UrlThreatAnalyzer.detectarEnlaces(mensajeOriginal))
        enlacesCombinados.addAll(UrlThreatAnalyzer.detectarEnlaces(mensajeNormalizado))
        return enlacesCombinados.toList()
    }

    private fun detectarIngenieriaSocialFuerte(
        factoresPatronesBrutos: List<ThreatFactor>,
        tieneEnlaces: Boolean,
        clasificacionMl: String
    ): Boolean {
        for (factor in factoresPatronesBrutos) {
            if (CATEGORIAS_NO_NEUTRALIZABLES.any { factor.category.contains(it) }) {
                return true
            }
            if (
                factor.category.contains("Falsas Recompensas") &&
                (tieneEnlaces || clasificacionMl == ClasificadorMensajesSms.CATEGORIA_SMISHING)
            ) {
                return true
            }
        }
        return false
    }

    private fun filtrarPatronesContextuales(
        factoresBrutos: List<ThreatFactor>,
        esOtpLegitimo: Boolean,
        esAvisoPresencialLegitimo: Boolean,
        tieneEnlaceMalicioso: Boolean,
        tieneEnlaces: Boolean,
        tieneIngenieriaSocialFuerte: Boolean
    ): List<ThreatFactor> {
        val filtrados = ArrayList<ThreatFactor>()
        var yaIncluyoReglaBancariaSinEnlace = false

        val ordenados = if (!tieneEnlaces && !tieneIngenieriaSocialFuerte) {
            factoresBrutos.sortedByDescending { it.severityScore }
        } else {
            factoresBrutos
        }

        for (factor in ordenados) {
            val esCategoriaBancaria = factor.category.contains("Billetera") || factor.category.contains("Bancario")
            if ((esOtpLegitimo || esAvisoPresencialLegitimo) && !tieneEnlaceMalicioso && esCategoriaBancaria) {
                continue
            }
            if (!tieneEnlaces && !tieneIngenieriaSocialFuerte && esCategoriaBancaria) {
                if (yaIncluyoReglaBancariaSinEnlace) {
                    continue
                }
                yaIncluyoReglaBancariaSinEnlace = true
            }
            filtrados.add(factor)
        }
        return filtrados
    }

    private fun agregarFactoresModeloMl(
        remitente: String,
        resultadoClasificacion: ResultadoClasificacionSms,
        factores: MutableList<ThreatFactor>,
        factoresEnlaces: List<ThreatFactor>,
        factoresPatrones: List<ThreatFactor>,
        factoresRemitente: List<ThreatFactor>,
        tieneEnlaceNoVerificado: Boolean,
        todosEnlacesLegitimos: Boolean,
        esOtpLegitimo: Boolean,
        esTransaccionInformativa: Boolean,
        esAvisoPresencialLegitimo: Boolean
    ) {
        val tieneEvidencia = resultadoClasificacion.tieneEvidenciaEspecificaSmishing ||
            factoresEnlaces.isNotEmpty() ||
            factoresPatrones.isNotEmpty()

        if (
            resultadoClasificacion.categoriaPredicha != ClasificadorMensajesSms.CATEGORIA_SMISHING ||
            resultadoClasificacion.probabilidadSmishing < 0.45 ||
            !tieneEvidencia ||
            esOtpLegitimo ||
            esTransaccionInformativa ||
            esAvisoPresencialLegitimo ||
            todosEnlacesLegitimos
        ) {
            return
        }

        val puntajeMl = when {
            resultadoClasificacion.probabilidadSmishing >= 0.75 -> 38
            resultadoClasificacion.probabilidadSmishing >= 0.68 -> 30
            else -> 22
        }

        val fragmentoTerminos = if (resultadoClasificacion.terminosRelevantes.isNotEmpty()) {
            resultadoClasificacion.terminosRelevantes.joinToString(", ")
        } else {
            "patron linguistico de fraude"
        }

        factores.add(
            ThreatFactor(
                category = "Modelo ML: Patrón de Smishing Detectado",
                description = "El clasificador estadístico TF-IDF identificó un patrón lingüístico compatible con smishing.",
                severityScore = puntajeMl,
                matchedSnippet = fragmentoTerminos
            )
        )

        val remitenteSinEspacios = remitente.trim().replace(PATRON_ESPACIOS, "")
        val esCelularPrivado = PATRON_CELULAR_PRIVADO.matches(remitenteSinEspacios)
        val yaTieneFactorCelular = factoresRemitente.any { it.category.contains("Celular Privada") }

        if (esCelularPrivado && tieneEnlaceNoVerificado && !yaTieneFactorCelular) {
            factores.add(
                ThreatFactor(
                    category = "Enlace Externo desde Línea Celular Privada",
                    description = "El mensaje proviene de una línea móvil particular e incluye un enlace no verificado asociado a patrones de fraude.",
                    severityScore = 15,
                    matchedSnippet = remitente
                )
            )
        }
    }

    private fun construirResumenMotivo(
        puntajeCalculado: ResultadoCalculoRiesgo,
        puntajeFinal: Int,
        nivelAmenaza: ThreatLevel,
        categoriaFinal: String,
        factores: List<ThreatFactor>
    ): String {
        val motivosPrincipales = factores.take(3).joinToString("; ") { it.category }
        return when {
            puntajeCalculado.confianzaOtp && puntajeFinal == 0 ->
                "Nivel Legítimo: mensaje de autenticación o código de seguridad verificado."
            puntajeCalculado.coincidenciaComunidadMaliciosa ->
                "Nivel Malicioso: indicador confirmado como malicioso por la comunidad."
            puntajeCalculado.coincidenciaComunidadReporte ->
                "Nivel ${nivelAmenaza.label}: indicador reportado por la comunidad."
            puntajeCalculado.coincidenciaReglaPersonalizada && nivelAmenaza != ThreatLevel.PHISHING ->
                "Nivel ${nivelAmenaza.label}: mensaje aislado por una regla personalizada del usuario."
            nivelAmenaza == ThreatLevel.PHISHING ->
                "Nivel Malicioso (${factores.size} motivos): $motivosPrincipales."
            nivelAmenaza == ThreatLevel.SUSPICIOUS ->
                "Nivel Sospechoso (${factores.size} motivos): $motivosPrincipales."
            categoriaFinal == ClasificadorMensajesSms.CATEGORIA_SPAM ->
                "Nivel Legítimo (promoción comercial): sin indicadores críticos de fraude."
            else ->
                "Nivel Legítimo: no se detectaron combinaciones de riesgo."
        }
    }

    data class ResultadoCalculoRiesgo(
        val puntajeAcotado: Int,
        val confianzaOtp: Boolean,
        val coincidenciaReglaPersonalizada: Boolean,
        val coincidenciaComunidadMaliciosa: Boolean,
        val coincidenciaComunidadReporte: Boolean
    )

    fun calcularRiesgo(
        remitente: String,
        mensajeOriginal: String,
        mensajeNormalizado: String,
        factores: MutableList<ThreatFactor>,
        factoresEnlaces: List<ThreatFactor>,
        factoresPatrones: List<ThreatFactor>,
        factoresRemitente: List<ThreatFactor>,
        tieneEnlaceMalicioso: Boolean,
        tieneEnlaceNoVerificado: Boolean,
        todosEnlacesLegitimos: Boolean,
        tieneIngenieriaSocialFuerte: Boolean,
        esOtpLegitimo: Boolean,
        esTransaccionInformativa: Boolean,
        esAvisoPresencialLegitimo: Boolean = false,
        resultadoClasificacion: ResultadoClasificacionSms,
        patronesPersonalizados: List<Regex>,
        umbralEfectivo: Int,
        entradasComunidad: Map<String, CommunityBlacklistEntity>
    ): ResultadoCalculoRiesgo {
        var puntajeTotal = 0
        for (factor in factores) {
            puntajeTotal += factor.severityScore
        }

        val tieneSenalesTexto = factoresPatrones.isNotEmpty() ||
            (resultadoClasificacion.categoriaPredicha == ClasificadorMensajesSms.CATEGORIA_SMISHING &&
                resultadoClasificacion.probabilidadSmishing >= 0.70)

        if (tieneEnlaceMalicioso && tieneSenalesTexto) {
            puntajeTotal = (puntajeTotal * 1.35).toInt()
        }

        val tieneCelularPrivado = factoresRemitente.any { it.category.contains("Celular Privada") }
        val tienePrefijoRiesgo = factoresRemitente.any { it.category.contains("Internacional") }

        if (tieneCelularPrivado && tieneEnlaceMalicioso) {
            puntajeTotal = (puntajeTotal * 1.4).toInt()
        }

        val sinRiesgoExterno = !tieneEnlaceMalicioso && !tieneEnlaceNoVerificado && !tienePrefijoRiesgo
        val confianzaOtp = esOtpLegitimo && sinRiesgoExterno
        val confianzaTransaccion = esTransaccionInformativa && sinRiesgoExterno
        val confianzaPresencial = esAvisoPresencialLegitimo && sinRiesgoExterno

        if (confianzaOtp || confianzaTransaccion || confianzaPresencial) {
            puntajeTotal = 0
            factores.clear()
        } else if (todosEnlacesLegitimos && factoresEnlaces.isEmpty() && !tieneIngenieriaSocialFuerte) {
            puntajeTotal = (puntajeTotal * 0.25).toInt()
        } else if (
            resultadoClasificacion.categoriaPredicha == ClasificadorMensajesSms.CATEGORIA_SPAM &&
            resultadoClasificacion.probabilidadSpam >= 0.60 &&
            !tieneEnlaceMalicioso &&
            !tieneIngenieriaSocialFuerte &&
            factoresPatrones.size <= 1
        ) {
            puntajeTotal = min(puntajeTotal, 30)
        }

        var coincidenciaReglaPersonalizada = false
        if (patronesPersonalizados.isNotEmpty()) {
            val textoSeguro = mensajeOriginal.take(LONGITUD_MAXIMA_REGLA_PERSONALIZADA)
            val normalizadoSeguro = mensajeNormalizado.take(LONGITUD_MAXIMA_REGLA_PERSONALIZADA)

            for (reglaUsuario in patronesPersonalizados) {
                val coincideTexto = SafeRegex.containsMatch(reglaUsuario, textoSeguro) == true
                val coincideNorm = SafeRegex.containsMatch(reglaUsuario, normalizadoSeguro) == true
                val coincideRemitente = SafeRegex.containsMatch(reglaUsuario, remitente) == true

                if (coincideTexto || coincideNorm || coincideRemitente) {
                    coincidenciaReglaPersonalizada = true
                    factores.add(
                        ThreatFactor(
                            category = "Regla Personalizada de Bloqueo",
                            description = "Coincidencia con una regla definida manualmente por el usuario (${reglaUsuario.pattern}).",
                            severityScore = 40,
                            matchedSnippet = reglaUsuario.pattern
                        )
                    )
                    break
                }
            }
        }

        if (coincidenciaReglaPersonalizada) {
            puntajeTotal = max(puntajeTotal + 40, umbralEfectivo)
        }

        var coincidenciaComunidadMaliciosa = false
        var coincidenciaComunidadReporte = false
        val indicadoresEnMensaje = CommunityIndicatorNormalizer.obtener("$remitente $mensajeOriginal")
        for (itemIndicador in indicadoresEnMensaje) {
            val indicador = itemIndicador.valor
            val entrada = entradasComunidad[indicador] ?: continue
            if (entrada.esMalicioso || entrada.reportes >= 5) {
                coincidenciaComunidadMaliciosa = true
                factores.add(
                    ThreatFactor(
                        category = "Lista negra de la comunidad",
                        description = "La comunidad confirmó este indicador como malicioso.",
                        severityScore = 100,
                        matchedSnippet = indicador
                    )
                )
            } else {
                coincidenciaComunidadReporte = true
                factores.add(
                    ThreatFactor(
                        category = "Reporte de la comunidad",
                        description = "Este indicador fue reportado por la comunidad y todavía está en evaluación.",
                        severityScore = 25,
                        matchedSnippet = indicador
                    )
                )
            }
        }

        if (coincidenciaComunidadMaliciosa) {
            puntajeTotal = 100
        } else if (coincidenciaComunidadReporte) {
            puntajeTotal += 25
        }

        val puntajeAcotado = min(100, max(0, puntajeTotal))
        return ResultadoCalculoRiesgo(
            puntajeAcotado = puntajeAcotado,
            confianzaOtp = confianzaOtp,
            coincidenciaReglaPersonalizada = coincidenciaReglaPersonalizada,
            coincidenciaComunidadMaliciosa = coincidenciaComunidadMaliciosa,
            coincidenciaComunidadReporte = coincidenciaComunidadReporte
        )
    }

    fun defangText(text: String, extractedUrls: List<String> = emptyList()): String {
        return neutralizarTexto(text, extractedUrls)
    }

    fun neutralizarTexto(texto: String, enlacesExtraidos: List<String> = emptyList()): String {
        var resultado = texto

        val enlacesOrdenados = enlacesExtraidos.sortedByDescending { it.length }
        for (enlaceOriginal in enlacesOrdenados) {
            val enlaceNeutralizado = enlaceOriginal
                .replace("https://", "hxxps://", ignoreCase = true)
                .replace("http://", "hxxp://", ignoreCase = true)
                .replace(".", "[.]")
            resultado = resultado.replace(enlaceOriginal, enlaceNeutralizado)
        }

        resultado = resultado
            .replace("https://", "hxxps://", ignoreCase = true)
            .replace("http://", "hxxp://", ignoreCase = true)
            .replace("www.", "w[w]w[.]", ignoreCase = true)
            .replace("www[.]", "w[w]w[.]", ignoreCase = true)

        resultado = PATRON_IP_NEUTRALIZAR.replace(resultado) { coincidencia ->
            "${coincidencia.groupValues[1]}[.]${coincidencia.groupValues[2]}[.]${coincidencia.groupValues[3]}[.]${coincidencia.groupValues[4]}"
        }

        return resultado
    }
}
