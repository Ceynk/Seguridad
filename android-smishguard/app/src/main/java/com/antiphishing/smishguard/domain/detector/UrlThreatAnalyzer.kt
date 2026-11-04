package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.domain.model.ThreatFactor
import java.net.URI
import java.util.regex.Pattern
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

data class UrlDecision(
    val url: String,
    val isBlocked: Boolean,
    val threatScore: Int,
    val summaryReason: String,
    val factors: List<ThreatFactor>
)

object UrlThreatAnalyzer {

    private val PATRON_URL_CON_PROTOCOLO = Pattern.compile("(?i)(https?://|hxxps?://|ftp://)[^\\s<>'\"{}|\\\\^`]+")
    private val PATRON_URL_WWW = Pattern.compile("(?i)\\bwww\\.[a-zA-Z0-9\\p{L}\\-\\.]+\\.[a-zA-Z]{2,}(/[^\\s<>'\"{}|\\\\^`]*)?")
    private val PATRON_URL_IP = Pattern.compile("(?i)\\b(?:https?://)?[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}(:[0-9]+)?(/[a-zA-Z0-9\\-\\._~:/?#\\[\\]@!$&'()*+,;=]*)?")
    private val PATRON_DOMINIO_SIN_PROTOCOLO = Pattern.compile(
        "(?i)\\b([a-zA-Z0-9\\p{L}\\-]+(?:\\.[a-zA-Z0-9\\p{L}\\-]+)*\\.(?:com|co|es|net|org|edu|gov|gob|xyz|top|icu|tk|ml|ga|cf|gq|cam|click|fit|link|buzz|online|site|space|fun|host|work|shop|store|app|dev|cloud|live|info|pro|me|io|ly|gy|gl|gle|gd|ee|vc|st|sh|ru|cn|pw|br|mx|cl|pe|uk|goog|club|biz|autos|qpon|at|cc|ws|lat|vip|sbs|cfd|cyou|bond|monster|rest|quest|support|help|so)(?:/[^\\s<>'\"{}|\\\\^`]*)?)\\b"
    )
    private val PATRON_ACORTADOR_SIN_PROTOCOLO = Pattern.compile(
        "(?i)\\b((?:qrco\\.de|s\\.id)(?:/[^\\s<>'\"{}|\\\\^`]*)?)\\b"
    )

    private val PATRON_IP_DIRECTA = Pattern.compile("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$")

    private val ACORTADORES_CONOCIDOS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "cutt.ly", "is.gd", "ow.ly", "rb.gy", "goo.gl",
        "shorturl.at", "t.ly", "buff.ly", "shorte.st", "adf.ly", "bc.vc", "s.id", "wa.me",
        "acortar.link", "bitly.ws", "tiny.cc", "v.gd", "qrco.de", "short.gy", "linktr.ee",
        "t.me", "forms.gle"
    )

    private val EXTENSIONES_DE_RIESGO = setOf(
        "xyz", "top", "icu", "tk", "ml", "ga", "cf", "gq", "work", "click", "rest",
        "cam", "ru", "cn", "fit", "link", "buzz", "tokyo", "racing", "date", "men",
        "online", "site", "live", "space", "sbs", "cfd", "bond", "cyou", "qpon", "pw",
        "monster", "autos", "quest", "lat", "vip", "support", "help", "cc", "ws"
    )

    private val ALOJAMIENTOS_GRATUITOS_ABUSADOS = setOf(
        "duckdns.org", "ngrok.io", "ngrok-free.app", "firebaseapp.com", "web.app",
        "pages.dev", "workers.dev", "vercel.app", "000webhostapp.com", "glitch.me",
        "surge.sh", "netlify.app", "render.com", "serveo.net", "loca.lt", "trycloudflare.com",
        "github.io", "weebly.com", "wixsite.com", "blogspot.com", "sites.google.com",
        "onrender.com", "replit.app", "repl.co", "webflow.io", "framer.app", "translate.goog"
    )

    internal val DOMINIOS_OFICIALES_POR_MARCA = mapOf(
        "bancolombia" to setOf("bancolombia.com", "grupobancolombia.com", "transaccionesbancolombia.com", "bancolombia.com.co"),
        "nequi" to setOf("nequi.com.co", "nequi.com"),
        "daviplata" to setOf("daviplata.com", "daviplata.com.co"),
        "davivienda" to setOf("davivienda.com", "davivienda.com.co"),
        "bancodebogota" to setOf("bancodebogota.com", "bancodebogota.com.co"),
        "bancoagrario" to setOf("bancoagrario.gov.co"),
        "bbva" to setOf("bbva.es", "bbva.com", "bbva.com.co"),
        "santander" to setOf("santander.es", "santander.com", "bancosantander.es"),
        "caixa" to setOf("caixabank.es", "caixabank.com", "lacaixa.es"),
        "caixabank" to setOf("caixabank.es", "caixabank.com", "lacaixa.es"),
        "sabadell" to setOf("bancsabadell.com", "bancosabadell.com"),
        "scotiabank" to setOf("scotiabankcolpatria.com", "colpatria.com", "scotiabank.com"),
        "colpatria" to setOf("scotiabankcolpatria.com", "colpatria.com"),
        "avvillas" to setOf("avvillas.com.co"),
        "bancooccidente" to setOf("bancodeoccidente.com.co"),
        "cajasocial" to setOf("bancocajasocial.com"),
        "itau" to setOf("itau.co", "itau.com"),
        "falabella" to setOf("bancofalabella.com.co", "falabella.com.co", "falabella.com"),
        "lulobank" to setOf("lulobank.com"),
        "nubank" to setOf("nu.com.co", "nubank.com.br"),
        "movii" to setOf("movii.com.co"),
        "dale" to setOf("dale.com.co"),
        "transfiya" to setOf("transfiya.com.co", "achcolombia.com.co"),
        "ing" to setOf("ing.es", "ing.com"),
        "bankinter" to setOf("bankinter.com"),
        "abanca" to setOf("abanca.com"),
        "correos" to setOf("correos.es", "correos.com"),
        "dhl" to setOf("dhl.com", "dhl.es", "dhl.com.co"),
        "seur" to setOf("seur.com"),
        "fedex" to setOf("fedex.com"),
        "mrw" to setOf("mrw.es"),
        "dgt" to setOf("dgt.es", "gob.es"),
        "hacienda" to setOf("hacienda.gob.es", "agenciatributaria.es", "agenciatributaria.gob.es", "gob.es"),
        "aeat" to setOf("agenciatributaria.es", "agenciatributaria.gob.es", "gob.es"),
        "seguridad-social" to setOf("seg-social.es", "gob.es"),
        "dian" to setOf("dian.gov.co", "gov.co"),
        "pse" to setOf("pse.com.co", "achcolombia.com.co"),
        "simit" to setOf("fcm.org.co", "simit.org.co", "gov.co"),
        "runt" to setOf("runt.com.co", "runt.gov.co", "gov.co"),
        "prosperidadsocial" to setOf("prosperidadsocial.gov.co", "gov.co"),
        "colpensiones" to setOf("colpensiones.gov.co", "gov.co"),
        "servientrega" to setOf("servientrega.com", "servientrega.com.co"),
        "coordinadora" to setOf("coordinadora.com"),
        "interrapidisimo" to setOf("interrapidisimo.com"),
        "envia" to setOf("envia.co"),
        "4-72" to setOf("4-72.com.co"),
        "efecty" to setOf("efecty.com.co"),
        "supergiros" to setOf("supergiros.com.co"),
        "claro" to setOf("claro.com.co", "miclaro.com.co"),
        "movistar" to setOf("movistar.co", "movistar.com.co", "movistar.es"),
        "tigo" to setOf("tigo.com.co"),
        "wom" to setOf("wom.co"),
        "exito" to setOf("exito.com"),
        "alkosto" to setOf("alkosto.com"),
        "rappi" to setOf("rappi.com.co", "rappi.com"),
        "mercadolibre" to setOf("mercadolibre.com", "mercadolibre.com.co"),
        "google" to setOf("google.com", "google.co", "google.es", "accounts.google.com"),
        "apple" to setOf("apple.com", "icloud.com"),
        "whatsapp" to setOf("whatsapp.com"),
        "microsoft" to setOf("microsoft.com", "live.com", "office.com"),
        "netflix" to setOf("netflix.com"),
        "amazon" to setOf("amazon.com", "amazon.es", "amazon.co"),
        "paypal" to setOf("paypal.com", "paypal.es"),
        "bizum" to setOf("bizum.es")
    )

    private val TODOS_LOS_DOMINIOS_OFICIALES: Set<String> by lazy {
        val conjunto = HashSet<String>()
        for (dominios in DOMINIOS_OFICIALES_POR_MARCA.values) {
            for (dominio in dominios) {
                conjunto.add(dominio)
            }
        }
        conjunto
    }

    private val PALABRAS_FRAUDE_EN_RUTA = listOf(
        "login", "iniciar-sesion", "verificar", "verificacion", "actualizar", "actualizacion",
        "seguridad", "bloqueo", "bloqueada", "desbloqueo", "reactivar", "recuperar",
        "validar", "validacion", "confirmar", "tarjeta", "clave", "token", "banca",
        "sucursal", "acceso", "identidad", "anular", "anulacion", "cancelar",
        "mandamiento", "embargo", "multa", "comparendo", "sancion", "pago", "retiro",
        "subsidio", "bono", "desembolso", "aduana"
    )

    private val PATRON_DOMINIO_LEETSPEAK = Regex("(?i)\\b.*(?:b[4a]nc[0o]|n[3e]qu[1i]|d[4a]v[1i]|cl[4a]v[3e]).*\\b")

    private val CONSONANTES = setOf(
        'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm',
        'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'z'
    )

    private const val MAXIMO_ENLACES = 25

    private val PATRONES_MARCAS_CORTAS: Map<String, Regex> by lazy {
        val mapa = HashMap<String, Regex>()
        for (marca in DOMINIOS_OFICIALES_POR_MARCA.keys) {
            if (marca.length <= 4) {
                val patron = "(?i)(?:^|[-._0-9])" + Regex.escape(marca) + "(?:[-._0-9]|$)"
                mapa[marca] = Regex(patron)
            }
        }
        mapa
    }

    fun detectarEnlaces(texto: String): List<String> {
        val enlaces = LinkedHashSet<String>()

        val buscadorProtocolo = PATRON_URL_CON_PROTOCOLO.matcher(texto)
        while (buscadorProtocolo.find() && enlaces.size < MAXIMO_ENLACES) {
            val candidato = limpiarCandidatoEnlace(buscadorProtocolo.group())
            if (candidato.length > 5) {
                enlaces.add(candidato)
            }
        }

        val buscadorWww = PATRON_URL_WWW.matcher(texto)
        while (buscadorWww.find() && enlaces.size < MAXIMO_ENLACES) {
            val candidato = limpiarCandidatoEnlace(buscadorWww.group())
            if (candidato.length > 4) {
                if (!contieneEnlaceExistente(enlaces, candidato)) {
                    enlaces.add(candidato)
                }
            }
        }

        val buscadorIp = PATRON_URL_IP.matcher(texto)
        while (buscadorIp.find() && enlaces.size < MAXIMO_ENLACES) {
            val candidato = limpiarCandidatoEnlace(buscadorIp.group())
            if (esCandidatoIpv4Valido(candidato)) {
                if (!contieneEnlaceExistente(enlaces, candidato)) {
                    enlaces.add(candidato)
                }
            }
        }

        val buscadorDominio = PATRON_DOMINIO_SIN_PROTOCOLO.matcher(texto)
        while (buscadorDominio.find() && enlaces.size < MAXIMO_ENLACES) {
            val grupo = buscadorDominio.group(1) ?: buscadorDominio.group()
            val candidato = limpiarCandidatoEnlace(grupo)
            if (candidato.length > 4) {
                if (!contieneEnlaceExistente(enlaces, candidato)) {
                    enlaces.add(candidato)
                }
            }
        }

        val buscadorAcortador = PATRON_ACORTADOR_SIN_PROTOCOLO.matcher(texto)
        while (buscadorAcortador.find() && enlaces.size < MAXIMO_ENLACES) {
            val candidato = limpiarCandidatoEnlace(buscadorAcortador.group(1) ?: buscadorAcortador.group())
            if (candidato.length > 3 && !contieneEnlaceExistente(enlaces, candidato)) {
                enlaces.add(candidato)
            }
        }

        return enlaces.toList()
    }

    private fun limpiarCandidatoEnlace(candidato: String): String {
        return candidato.trimEnd('.', ',', ';', ':', ')', ']', '!', '?', '"', '\'')
    }

    private fun contieneEnlaceExistente(enlaces: Set<String>, candidato: String): Boolean {
        for (existente in enlaces) {
            if (existente.contains(candidato)) {
                return true
            }
        }
        return false
    }

    private fun esCandidatoIpv4Valido(candidato: String): Boolean {
        val host = candidato.substringAfter("://").substringBefore("/").substringBefore(":")
        val partes = host.split('.')
        if (partes.size != 4) {
            return false
        }
        for (parte in partes) {
            val numero = parte.toIntOrNull()
            if (numero == null || numero < 0 || numero > 255) {
                return false
            }
        }
        return true
    }

    fun analyzeUrls(urls: List<String>): List<ThreatFactor> {
        return analizarEnlaces(urls)
    }

    fun analizarEnlaces(enlaces: List<String>): List<ThreatFactor> {
        val factores = ArrayList<ThreatFactor>()

        for (enlaceOriginal in enlaces) {
            val enlaceLimpio = enlaceOriginal.replace("hxxps", "https", ignoreCase = true)
                .replace("hxxp", "http", ignoreCase = true)
                .replace("[.]", ".")
                .replace("(.)", ".")
                .replace(" ", "")

            val enlaceConProtocolo = if (!enlaceLimpio.startsWith("http://") && !enlaceLimpio.startsWith("https://")) {
                "http://$enlaceLimpio"
            } else {
                enlaceLimpio
            }

            val dominio = extraerDominioDeEnlace(enlaceOriginal)
            if (dominio.isBlank()) {
                continue
            }

            val dominioNormalizado = HomographDetector.normalizeHomoglyphs(dominio)
            val ruta = extraerRutaDeEnlace(enlaceConProtocolo, enlaceLimpio)

            if (PATRON_IP_DIRECTA.matcher(dominio).matches()) {
                factores.add(
                    ThreatFactor(
                        category = "Dirección IP Directa",
                        description = "El enlace apunta a una dirección IP numérica en lugar de un dominio legítimo.",
                        severityScore = 35,
                        matchedSnippet = enlaceOriginal
                    )
                )
            }

            val extension = dominio.substringAfterLast(".", "")
            if (EXTENSIONES_DE_RIESGO.contains(extension)) {
                factores.add(
                    ThreatFactor(
                        category = "Extensión de Dominio Sospechosa (.$extension)",
                        description = "Utiliza una extensión de dominio comúnmente asociada a campañas de smishing y fraude.",
                        severityScore = 25,
                        matchedSnippet = enlaceOriginal
                    )
                )
            }

            if (esDominioAcortador(dominio)) {
                factores.add(
                    ThreatFactor(
                        category = "Enlace Acortado u Oculto",
                        description = "Utiliza un servicio acortador para ocultar el verdadero destino del enlace.",
                        severityScore = 20,
                        matchedSnippet = enlaceOriginal
                    )
                )
            }

            if (esAlojamientoGratuitoAbusado(dominio)) {
                factores.add(
                    ThreatFactor(
                        category = "Servicio de Alojamiento Gratuito o Túnel",
                        description = "El enlace aloja contenido en una plataforma gratuita o túnel frecuentemente abusado ($dominio).",
                        severityScore = 30,
                        matchedSnippet = enlaceOriginal
                    )
                )
            }

            val esDominioOficial = esDominioOficialGeneral(dominio) || esDominioOficialGeneral(dominioNormalizado)

            var marcaSuplantada = false
            if (!esDominioOficial) {
                for ((marca, dominiosOficialesMarca) in DOMINIOS_OFICIALES_POR_MARCA) {
                    val coincideActual = dominioCoincideConMarca(dominio, marca)
                    val coincideNormalizado = dominioCoincideConMarca(dominioNormalizado, marca)

                    if (coincideActual || coincideNormalizado) {
                        val oficialParaMarca = esDominioOficial(dominio, dominiosOficialesMarca) ||
                            esDominioOficial(dominioNormalizado, dominiosOficialesMarca)
                        if (!oficialParaMarca) {
                            marcaSuplantada = true
                            factores.add(
                                ThreatFactor(
                                    category = "Suplantación de Marca en URL",
                                    description = "El dominio imita la marca oficial '$marca' dentro de una dirección no autorizada ($dominio).",
                                    severityScore = 35,
                                    matchedSnippet = enlaceOriginal
                                )
                            )
                            break
                        }
                    }
                }
            }

            val nivelesDominio = dominio.split('.')
            if (!esDominioOficial && nivelesDominio.size > 4 && !PATRON_IP_DIRECTA.matcher(dominio).matches()) {
                factores.add(
                    ThreatFactor(
                        category = "Cantidad Excesiva de Subdominios",
                        description = "Estructura anómala con múltiples niveles de subdominio para engañar al usuario.",
                        severityScore = 20,
                        matchedSnippet = dominio
                    )
                )
            }

            if (!esDominioOficial && (enlaceOriginal.length > 120 || dominio.length > 36)) {
                factores.add(
                    ThreatFactor(
                        category = "Longitud Inusual de Enlace",
                        description = "El enlace o dominio es inusualmente extenso.",
                        severityScore = 5,
                        matchedSnippet = if (dominio.length > 36) dominio else enlaceOriginal
                    )
                )
            }

            if (enlaceOriginal.contains("@")) {
                factores.add(
                    ThreatFactor(
                        category = "Carácter Anómalo en URL (@)",
                        description = "Uso del carácter '@' para engañar sobre el host real al que se navega.",
                        severityScore = 25,
                        matchedSnippet = "@"
                    )
                )
            }

            val cantidadGuiones = dominio.count { it == '-' }
            if (!esDominioOficial && cantidadGuiones >= 3) {
                factores.add(
                    ThreatFactor(
                        category = "Uso Excesivo de Guiones en Dominio",
                        description = "El dominio contiene múltiples guiones ($cantidadGuiones) concatenando términos sospechosos.",
                        severityScore = 15,
                        matchedSnippet = dominio
                    )
                )
            }

            var tienePalabraFraude = false
            var palabraFraudeEnDominio = false
            for (palabraClave in PALABRAS_FRAUDE_EN_RUTA) {
                val enDominio = dominio.contains(palabraClave) && !marcaSuplantada
                if (ruta.contains(palabraClave) || enDominio) {
                    tienePalabraFraude = true
                    palabraFraudeEnDominio = enDominio
                    break
                }
            }
            if (tienePalabraFraude && !esDominioOficial) {
                factores.add(
                    ThreatFactor(
                        category = "Palabras Clave de Fraude en Enlace",
                        description = "La ruta o dominio contiene palabras frecuentemente asociadas a ingeniería social y captura de credenciales.",
                        severityScore = if (palabraFraudeEnDominio) 20 else 10,
                        matchedSnippet = enlaceOriginal
                    )
                )
            }

            if (PATRON_DOMINIO_LEETSPEAK.matches(dominio) && !esDominioOficial) {
                factores.add(
                    ThreatFactor(
                        category = "Patrón Numérico Sospechoso en Dominio",
                        description = "El dominio sustituye letras por números simulando marcas legítimas (Leetspeak).",
                        severityScore = 20,
                        matchedSnippet = dominio
                    )
                )
            }

            if (!esDominioOficial && enlaceLimpio.startsWith("http://", ignoreCase = true) && !PATRON_IP_DIRECTA.matcher(dominio).matches()) {
                if (tienePalabraFraude || marcaSuplantada) {
                    factores.add(
                        ThreatFactor(
                            category = "Protocolo No Seguro (HTTP)",
                            description = "El enlace no utiliza cifrado seguro HTTPS a pesar de solicitar acciones de autenticación o datos sensibles.",
                            severityScore = 15,
                            matchedSnippet = "http://"
                        )
                    )
                }
            }

            val etiquetaPrincipal = dominio.substringBeforeLast(".")
            val entropia = calcularEntropiaShannon(etiquetaPrincipal)
            val excesoConsonantes = tieneExcesoConsonantes(etiquetaPrincipal)

            val altaEntropia = !esDominioOficial && ((entropia > 3.8 && etiquetaPrincipal.length > 12) || (excesoConsonantes && etiquetaPrincipal.length > 10))
            if (altaEntropia) {
                factores.add(
                    ThreatFactor(
                        category = "Alta Entropía en Dominio",
                        description = "El nombre de dominio presenta una estructura aleatoria indicativa de generación algorítmica (DGA).",
                        severityScore = 15,
                        matchedSnippet = dominio
                    )
                )
            }

            val esIp = PATRON_IP_DIRECTA.matcher(dominio).matches()
            val registrable = obtenerDominioRegistrable(dominio)
            val etiquetaRegistrable = registrable.substringBefore(".")

            if (dominio.contains("xn--")) {
                factores.add(
                    ThreatFactor(
                        category = CATEGORIA_PUNYCODE,
                        description = "El dominio usa codificación punycode (xn--), técnica para imitar letras de una marca.",
                        severityScore = 30,
                        matchedSnippet = dominio
                    )
                )
            }

            if (!esDominioOficial && !esIp && !altaEntropia && pareceDominioAleatorio(etiquetaRegistrable)) {
                factores.add(
                    ThreatFactor(
                        category = CATEGORIA_DOMINIO_ALEATORIO,
                        description = "El nombre '$etiquetaRegistrable' tiene pocas vocales o muchas consonantes seguidas; no parece una marca o palabra real.",
                        severityScore = 20,
                        matchedSnippet = registrable
                    )
                )
            }

            if (!esDominioOficial && !esIp && dominio != registrable) {
                val subdominios = dominio.removeSuffix(".$registrable")
                for (marca in DOMINIOS_OFICIALES_POR_MARCA.keys) {
                    if (marca.length >= 4 && dominioCoincideConMarca(subdominios, marca)) {
                        factores.add(
                            ThreatFactor(
                                category = CATEGORIA_SUBDOMINIO_ENGANOSO,
                                description = "La marca '$marca' aparece solo como subdominio; el dominio real es $registrable.",
                                severityScore = 20,
                                matchedSnippet = dominio
                            )
                        )
                        break
                    }
                }
            }

            if (!esDominioOficial && !enlaceLimpio.contains("://")) {
                factores.add(
                    ThreatFactor(
                        category = CATEGORIA_SIN_ESQUEMA,
                        description = "El enlace se escribió sin http/https, forma común de evadir filtros de enlaces.",
                        severityScore = 5,
                        matchedSnippet = enlaceOriginal
                    )
                )
            }
        }

        return factores
    }

    const val CATEGORIA_PUNYCODE = "Dominio Punycode (posible homógrafo)"
    const val CATEGORIA_DOMINIO_ALEATORIO = "Dominio de Apariencia Aleatoria"
    const val CATEGORIA_SUBDOMINIO_ENGANOSO = "Subdominio Engañoso"
    const val CATEGORIA_SIN_ESQUEMA = "Enlace sin Esquema"

    private val SUFIJOS_DOBLES = setOf(
        "com.co", "org.co", "gov.co", "edu.co", "net.co", "mil.co", "nom.co",
        "com.mx", "gob.mx", "com.br", "com.ar", "gob.es", "com.es", "org.es", "co.uk"
    )

    fun obtenerDominioRegistrable(dominio: String): String {
        val partes = dominio.lowercase().trim('.').split('.')
        if (partes.size <= 2) return partes.joinToString(".")
        val ultimasDos = partes.takeLast(2).joinToString(".")
        val cantidad = if (SUFIJOS_DOBLES.contains(ultimasDos)) 3 else 2
        return partes.takeLast(cantidad).joinToString(".")
    }

    fun pareceDominioAleatorio(etiqueta: String): Boolean {
        val letras = etiqueta.lowercase().filter { it in 'a'..'z' }
        if (letras.length < 5 || letras.contains("xn")) return false
        var vocales = 0
        var consonantesSeguidas = 0
        var maximoSeguidas = 0
        for (letra in letras) {
            if (letra in "aeiouy") {
                vocales++
                consonantesSeguidas = 0
            } else {
                consonantesSeguidas++
                if (consonantesSeguidas > maximoSeguidas) maximoSeguidas = consonantesSeguidas
            }
        }
        val proporcionVocales = vocales.toDouble() / letras.length
        return proporcionVocales < 0.25 || maximoSeguidas >= 5
    }

    fun analyzeSingleUrl(url: String): UrlDecision {
        return analizarEnlaceIndividual(url)
    }

    fun analizarEnlaceIndividual(enlace: String): UrlDecision {
        val enlaceRecortado = enlace.trim()
        if (enlaceRecortado.isEmpty()) {
            return UrlDecision(
                url = enlace,
                isBlocked = false,
                threatScore = 0,
                summaryReason = "URL vacía o no válida.",
                factors = emptyList()
            )
        }

        val homoglifos = HomographDetector.findHomoglyphs(enlaceRecortado)
        val normalizado = HomographDetector.normalizeHomoglyphs(enlaceRecortado)
        val candidatos = listOf(enlaceRecortado, normalizado).distinct()

        val factores = analizarEnlaces(candidatos).toMutableList()

        if (homoglifos.isNotEmpty()) {
            factores.add(
                ThreatFactor(
                    category = "Ataque Homógrafo / Evasión de Filtros",
                    description = "El enlace contiene caracteres no latinos o invisibles para suplantar dominios legítimos.",
                    severityScore = 35,
                    matchedSnippet = homoglifos.joinToString("; ")
                )
            )
        }

        var puntajeTotal = 0
        for (factor in factores) {
            puntajeTotal += factor.severityScore
        }
        val puntajeAcotado = min(100, max(0, puntajeTotal))

        var tieneFactorCritico = false
        for (factor in factores) {
            if (
                factor.category.contains("Dirección IP Directa") ||
                factor.category.contains("Suplantación de Marca") ||
                factor.category.contains("Alojamiento Gratuito") ||
                factor.category.contains("Extensión de Dominio Sospechosa")
            ) {
                tieneFactorCritico = true
                break
            }
        }

        val bloqueado = puntajeAcotado >= 50 || (tieneFactorCritico && puntajeAcotado >= 35)

        val motivo = when {
            bloqueado -> "Enlace de alto riesgo bloqueado preventivamente por SmishGuard."
            puntajeAcotado >= 30 -> "Enlace con características sospechosas detectadas."
            else -> "Enlace verificado sin anomalías conocidas."
        }

        return UrlDecision(
            url = enlaceRecortado,
            isBlocked = bloqueado,
            threatScore = puntajeAcotado,
            summaryReason = motivo,
            factors = factores
        )
    }

    fun extraerDominioDeEnlace(enlaceOriginal: String): String {
        val limpio = enlaceOriginal.replace("hxxps", "https", ignoreCase = true)
            .replace("hxxp", "http", ignoreCase = true)
            .replace("[.]", ".")
            .replace("(.)", ".")
            .replace(" ", "")

        val conProtocolo = if (!limpio.startsWith("http://") && !limpio.startsWith("https://")) {
            "http://$limpio"
        } else {
            limpio
        }

        var hostExtraido = try {
            val host = URI(conProtocolo).host
            if (host == null || host.isBlank()) {
                limpio.substringAfter("://").substringBefore("/").substringBefore(":")
            } else {
                host
            }
        } catch (e: Exception) {
            limpio.substringAfter("://").substringBefore("/").substringBefore(":")
        }.lowercase().trim()

        if (hostExtraido.startsWith("www.")) {
            hostExtraido = hostExtraido.substring(4)
        }
        return hostExtraido
    }

    private fun extraerRutaDeEnlace(enlaceConProtocolo: String, enlaceLimpio: String): String {
        return try {
            URI(enlaceConProtocolo).path ?: ""
        } catch (e: Exception) {
            enlaceLimpio.substringAfter("://").substringAfter("/", "")
        }.lowercase()
    }

    private fun dominioCoincideConMarca(dominio: String, marca: String): Boolean {
        if (marca.length <= 4) {
            val patronGuardado = PATRONES_MARCAS_CORTAS[marca]
            val expresion = if (patronGuardado != null) {
                patronGuardado
            } else {
                val patron = "(?i)(?:^|[-._0-9])" + Regex.escape(marca) + "(?:[-._0-9]|$)"
                Regex(patron)
            }
            return expresion.containsMatchIn(dominio)
        }
        return dominio.contains(marca, ignoreCase = true)
    }

    fun isOfficialDomain(domain: String, officialDomains: Set<String>): Boolean {
        return esDominioOficial(domain, officialDomains)
    }

    fun esDominioOficial(dominio: String, dominiosOficiales: Set<String>): Boolean {
        val dominioLimpio = dominio.lowercase().trim()
        for (oficial in dominiosOficiales) {
            if (dominioLimpio == oficial || dominioLimpio.endsWith(".$oficial")) {
                return true
            }
        }
        return false
    }

    fun esDominioOficialGeneral(dominio: String): Boolean {
        val dominioLimpio = dominio.lowercase().trim()
        if (dominioLimpio.isEmpty()) {
            return false
        }
        if (dominioLimpio.endsWith(".edu.co") || dominioLimpio.endsWith(".gov.co")) {
            return true
        }
        for (oficial in TODOS_LOS_DOMINIOS_OFICIALES) {
            if (dominioLimpio == oficial || dominioLimpio.endsWith(".$oficial")) {
                return true
            }
        }
        return false
    }

    fun esDominioAcortador(dominio: String): Boolean {
        val dominioLimpio = dominio.lowercase().trim()
        for (acortador in ACORTADORES_CONOCIDOS) {
            if (dominioLimpio == acortador || dominioLimpio.endsWith(".$acortador")) {
                return true
            }
        }
        return false
    }

    fun esAlojamientoGratuitoAbusado(dominio: String): Boolean {
        val dominioLimpio = dominio.lowercase().trim()
        for (alojamiento in ALOJAMIENTOS_GRATUITOS_ABUSADOS) {
            if (dominioLimpio == alojamiento || dominioLimpio.endsWith(".$alojamiento")) {
                return true
            }
        }
        return false
    }

    fun esExtensionDeRiesgo(extension: String): Boolean {
        return EXTENSIONES_DE_RIESGO.contains(extension.lowercase().trim())
    }

    fun areAllUrlsLegitimate(urls: List<String>): Boolean {
        return sonTodosEnlacesLegitimos(urls)
    }

    fun sonTodosEnlacesLegitimos(enlaces: List<String>): Boolean {
        if (enlaces.isEmpty()) {
            return true
        }
        for (enlace in enlaces) {
            val dominio = extraerDominioDeEnlace(enlace)
            if (dominio.isBlank()) {
                return false
            }
            if (!esDominioOficialGeneral(dominio)) {
                return false
            }
        }
        return true
    }

    private fun calcularEntropiaShannon(texto: String): Double {
        if (texto.isEmpty()) {
            return 0.0
        }
        val mapaFrecuencia = HashMap<Char, Int>()
        for (i in 0 until texto.length) {
            val caracter = texto[i]
            val conteoActual = mapaFrecuencia[caracter] ?: 0
            mapaFrecuencia[caracter] = conteoActual + 1
        }
        val longitud = texto.length.toDouble()
        var entropia = 0.0
        for (entrada in mapaFrecuencia) {
            val probabilidad = entrada.value / longitud
            entropia -= probabilidad * (ln(probabilidad) / ln(2.0))
        }
        return entropia
    }

    private fun tieneExcesoConsonantes(texto: String): Boolean {
        var consecutivas = 0
        for (i in 0 until texto.length) {
            val caracter = texto[i].lowercaseChar()
            if (CONSONANTES.contains(caracter)) {
                consecutivas++
                if (consecutivas >= 6) {
                    return true
                }
            } else {
                consecutivas = 0
            }
        }
        return false
    }
}
