package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.domain.model.ThreatFactor
import java.net.URI
import java.util.regex.Pattern
import kotlin.math.ln

object UrlThreatAnalyzer {

    // Regex universal de URLs: soporta protocolos (http/https/hxxp), www., dominios "desnudos" sin protocolo
    // (muy usados en smishing: "bancolombia-seguro.com/login"), caracteres Unicode (IDN/Punycode/Cirílico) y acortadores.
    // La longitud de etiqueta se acota a 253 (máximo DNS) para limitar el backtracking en textos largos sin espacios.
    // El lookahead tras el TLD evita falsos positivos tipo "algo.mexico" -> "algo.me".
    private val URL_REGEX = Pattern.compile(
        """(?i)\b(?:https?://|hxxps?://|www\.)[^\s<>"'{}|\\^`]{1,2000}|\b[^\s<>"'{}|\\^`/@]{2,253}\.(?:top|xyz|icu|tk|ml|ga|cf|gq|work|click|rest|cam|ru|cn|fit|link|online|site|space|fun|buzz|club|shop|app|io|me|info|cc|to|com|net|org|co|es|mx|pe|ar|cl|ec|us|biz|vip|lat|live|life|pro|sbs|cfd|bond|cyou|qpon|pw|monster|autos|quest|store|website|digital|support|help|page|dev)(?![a-z0-9\-])(?:/[^\s<>"'{}|\\^`]{0,2000})?|\b(?:bit\.ly|tinyurl\.com|t\.co|cutt\.ly|is\.gd|rb\.gy|shorturl\.at|t\.ly|wa\.me)/[^\s<>"'{}|\\^`]{0,2000}"""
    )

    // El lookbehind evita interpretar montos como "$1.200.000.000" o "1,5.200.100.10" como direcciones IP
    private val IP_URL_REGEX = Pattern.compile("""(?i)(?<![\d.,$])\b(?:https?://)?(?:\d{1,3}\.){3}\d{1,3}(?::\d{1,5})?(?:/[^\s]{0,2000})?""")

    private val RAW_IP_PATTERN = Pattern.compile("""^(?:\d{1,3}\.){3}\d{1,3}$""")

    private val KNOWN_SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "cutt.ly", "is.gd", "ow.ly", "rb.gy", "goo.gl",
        "shorturl.at", "t.ly", "buff.ly", "shorte.st", "adf.ly", "bc.vc", "s.id", "wa.me",
        "acortar.link", "bitly.ws", "tiny.cc", "v.gd", "qrco.de", "short.gy", "linktr.ee"
    )

    private val HIGH_RISK_TLDS = setOf(
        "xyz", "top", "icu", "tk", "ml", "ga", "cf", "gq", "work", "click", "rest",
        "cam", "ru", "cn", "fit", "link", "buzz", "tokyo", "racing", "date", "men",
        "online", "site", "live", "space", "sbs", "cfd", "bond", "cyou", "qpon", "pw",
        "monster", "autos", "quest", "lat", "vip", "support", "help"
    )

    private val ABUSED_FREE_HOSTS = setOf(
        "duckdns.org", "ngrok.io", "ngrok-free.app", "firebaseapp.com", "web.app",
        "pages.dev", "workers.dev", "vercel.app", "000webhostapp.com", "glitch.me",
        "surge.sh", "netlify.app", "render.com", "serveo.net", "loca.lt", "trycloudflare.com",
        "github.io", "weebly.com", "wixsite.com", "blogspot.com", "sites.google.com",
        "onrender.com", "replit.app", "repl.co", "webflow.io", "framer.app", "translate.goog"
    )

    // Registro de marcas y sus dominios base oficiales reconocidos (eTLD+1 y subdominios)
    private val OFFICIAL_BRAND_DOMAINS = mapOf(
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
        "servientrega" to setOf("servientrega.com", "servientrega.com.co"),
        "coordinadora" to setOf("coordinadora.com"),
        "interrapidisimo" to setOf("interrapidisimo.com"),
        "mercadolibre" to setOf("mercadolibre.com", "mercadolibre.com.co"),
        "google" to setOf("google.com", "google.co", "google.es", "accounts.google.com"),
        "apple" to setOf("apple.com", "icloud.com"),
        "whatsapp" to setOf("whatsapp.com", "wa.me"),
        "microsoft" to setOf("microsoft.com", "live.com", "office.com"),
        "netflix" to setOf("netflix.com"),
        "amazon" to setOf("amazon.com", "amazon.es", "amazon.co"),
        "paypal" to setOf("paypal.com", "paypal.es"),
        "bizum" to setOf("bizum.es")
    )

    private val ALL_OFFICIAL_DOMAINS: Set<String> by lazy {
        OFFICIAL_BRAND_DOMAINS.values.flatten().toSet()
    }

    private val CONSONANTS = setOf(
        'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm',
        'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'z'
    )

    private const val MAX_URLS = 25

    // Regex precompiladas para marcas cortas (evita compilar en cada análisis)
    private val SHORT_BRAND_REGEX: Map<String, Regex> by lazy {
        OFFICIAL_BRAND_DOMAINS.keys.filter { it.length <= 3 }.associateWith { brand ->
            Regex("""(?i)(?:^|[-._0-9])${Regex.escape(brand)}(?:[-._0-9]|$)""")
        }
    }

    fun extractUrls(text: String): List<String> {
        val urls = LinkedHashSet<String>()
        val matcher = URL_REGEX.matcher(text)
        while (matcher.find() && urls.size < MAX_URLS) {
            val candidate = matcher.group().trimEnd('.', ',', ';', ':', ')', ']', '!', '?')
            if (candidate.length > 3) urls.add(candidate)
        }
        val ipMatcher = IP_URL_REGEX.matcher(text)
        while (ipMatcher.find() && urls.size < MAX_URLS) {
            val found = ipMatcher.group().trimEnd('.', ',', ';', ':', ')', ']', '!', '?')
            if (!isValidIpv4Inside(found)) continue
            if (urls.any { it.contains(found) }) continue
            urls.add(found)
        }
        return urls.toList()
    }

    private fun isValidIpv4Inside(candidate: String): Boolean {
        val host = candidate.substringAfter("://").substringBefore("/").substringBefore(":")
        val parts = host.split('.')
        return parts.size == 4 && parts.all { p -> p.isNotEmpty() && (p.toIntOrNull() ?: 256) in 0..255 }
    }

    fun analyzeUrls(urls: List<String>): List<ThreatFactor> {
        val factors = mutableListOf<ThreatFactor>()

        for (rawUrl in urls) {
            val sanitized = rawUrl.replace("hxxp", "http", ignoreCase = true)
                .replace("[.]", ".")
                .replace("(.)", ".")
                .replace(" ", "")

            val normalizedUrl = if (!sanitized.startsWith("http://") && !sanitized.startsWith("https://")) {
                "http://$sanitized"
            } else sanitized

            val domain = try {
                val h = URI(normalizedUrl).host
                if (h.isNullOrBlank()) {
                    sanitized.substringAfter("://").substringBefore("/").substringBefore(":")
                } else h
            } catch (e: Exception) {
                sanitized.substringAfter("://").substringBefore("/").substringBefore(":")
            }.lowercase()

            if (domain.isBlank()) continue

            // 1. Detección de IP directa
            if (RAW_IP_PATTERN.matcher(domain).matches()) {
                factors.add(
                    ThreatFactor(
                        category = "Dirección IP Directa",
                        description = "El enlace apunta a una dirección IP numérica en lugar de un dominio legítimo.",
                        severityScore = 35,
                        matchedSnippet = rawUrl
                    )
                )
            }

            // 2. TLDs de alto riesgo
            val tld = domain.substringAfterLast(".", "")
            if (HIGH_RISK_TLDS.contains(tld)) {
                factors.add(
                    ThreatFactor(
                        category = "Extensión de Dominio Sospechosa (.$tld)",
                        description = "Utiliza una extensión de dominio frecuentemente asociada a campañas de smishing y malware.",
                        severityScore = 25,
                        matchedSnippet = rawUrl
                    )
                )
            }

            // 3. Enlaces Acortados
            if (KNOWN_SHORTENERS.any { domain == it || domain.endsWith(".$it") }) {
                factors.add(
                    ThreatFactor(
                        category = "Enlace Acortado u Oculto",
                        description = "Utiliza un servicio acortador para camuflar el verdadero destino final.",
                        severityScore = 20,
                        matchedSnippet = rawUrl
                    )
                )
            }

            // 4. Servicios de hosting gratuito o túneles de desarrollo
            if (ABUSED_FREE_HOSTS.any { domain == it || domain.endsWith(".$it") }) {
                factors.add(
                    ThreatFactor(
                        category = "Servicio de Alojamiento Gratuito o Túnel",
                        description = "El enlace aloja contenido en un servicio gratuito frecuentemente abusado ($domain).",
                        severityScore = 30,
                        matchedSnippet = rawUrl
                    )
                )
            }

            // 5. Suplantación de Marca en URL (con verificación jerárquica y soporte de homógrafos normalizados)
            val normalizedDomain = HomographDetector.normalizeHomoglyphs(domain)
            for ((brand, officialDomains) in OFFICIAL_BRAND_DOMAINS) {
                if (domainMatchesBrand(domain, brand) || domainMatchesBrand(normalizedDomain, brand)) {
                    val isOfficial = isOfficialDomain(domain, officialDomains) || isOfficialDomain(normalizedDomain, officialDomains)
                    if (!isOfficial) {
                        factors.add(
                            ThreatFactor(
                                category = "Suplantación de Marca en URL",
                                description = "El dominio imita la marca oficial '$brand' dentro de una dirección no autorizada ($domain).",
                                severityScore = 35,
                                matchedSnippet = rawUrl
                            )
                        )
                        break
                    }
                }
            }

            // 6. Entropía y generación algorítmica de dominios (DGA)
            val subLabel = domain.substringBeforeLast(".")
            val entropy = calculateShannonEntropy(subLabel)
            val hasHighConsonantDensity = hasExcessiveConsonants(subLabel)

            if ((entropy > 3.8 && subLabel.length > 12) || (hasHighConsonantDensity && subLabel.length > 10)) {
                // Verificar que no sea un dominio oficial
                val isAnyOfficial = ALL_OFFICIAL_DOMAINS.any { domain == it || domain.endsWith(".$it") }
                if (!isAnyOfficial) {
                    factors.add(
                        ThreatFactor(
                            category = "Alta Entropía en Dominio",
                            description = "El nombre de dominio presenta una estructura aleatoria indicativa de generación algorítmica (DGA).",
                            severityScore = 15,
                            matchedSnippet = domain
                        )
                    )
                }
            }
        }

        return factors
    }

    /**
     * Verifica si el dominio contiene una referencia a la marca especificada.
     * Para marcas cortas (<= 3 caracteres como "ing" o "pse"), se requiere que estén
     * delimitadas por puntos, guiones, dígitos o extremos para no generar falsos positivos
     * con palabras comunes del idioma (booking, hosting, eclipse, response, etc.).
     */
    private fun domainMatchesBrand(domain: String, brand: String): Boolean {
        if (brand.length <= 3) {
            val regex = SHORT_BRAND_REGEX[brand]
                ?: Regex("""(?i)(?:^|[-._0-9])${Regex.escape(brand)}(?:[-._0-9]|$)""")
            return regex.containsMatchIn(domain)
        }
        return domain.contains(brand, ignoreCase = true)
    }

    /**
     * Comprueba si un dominio pertenece legítimamente a la entidad oficial (incluyendo cualquier subdominio válido).
     */
    fun isOfficialDomain(domain: String, officialDomains: Set<String>): Boolean {
        val cleanDomain = domain.lowercase().trim()
        return officialDomains.any { official ->
            cleanDomain == official || cleanDomain.endsWith(".$official")
        }
    }

    /**
     * Verifica si una lista de URLs extraídas contiene exclusivamente dominios oficiales reconocidos.
     */
    fun areAllUrlsLegitimate(urls: List<String>): Boolean {
        if (urls.isEmpty()) return true

        for (rawUrl in urls) {
            val cleanUrl = rawUrl.replace("hxxp", "http", ignoreCase = true).replace("[.]", ".")
            val normalized = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) "http://$cleanUrl" else cleanUrl
            val domain = try {
                val h = URI(normalized).host
                if (h.isNullOrBlank()) {
                    cleanUrl.substringAfter("://").substringBefore("/").substringBefore(":")
                } else h
            } catch (e: Exception) {
                cleanUrl.substringAfter("://").substringBefore("/").substringBefore(":")
            }.lowercase()

            if (domain.isBlank()) return false
            val isOfficial = ALL_OFFICIAL_DOMAINS.any { domain == it || domain.endsWith(".$it") }
            if (!isOfficial) return false
        }
        return true
    }

    private fun calculateShannonEntropy(str: String): Double {
        if (str.isEmpty()) return 0.0
        val freqMap = mutableMapOf<Char, Int>()
        for (i in 0 until str.length) {
            val c = str[i]
            freqMap[c] = (freqMap[c] ?: 0) + 1
        }
        val len = str.length.toDouble()
        var entropy = 0.0
        for (entry in freqMap) {
            val p = entry.value / len
            entropy -= p * (ln(p) / ln(2.0))
        }
        return entropy
    }

    private fun hasExcessiveConsonants(str: String): Boolean {
        var consecutive = 0
        for (i in 0 until str.length) {
            val c = str[i].lowercaseChar()
            if (CONSONANTS.contains(c)) {
                consecutive++
                if (consecutive >= 6) return true
            } else {
                consecutive = 0
            }
        }
        return false
    }
}
