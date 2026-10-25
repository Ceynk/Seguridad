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

    private val PROTOCOL_URL_REGEX = Pattern.compile("(?i)(https?://|hxxps?://|ftp://)[^\\s<>'\"{}|\\\\^`]+")
    private val WWW_URL_REGEX = Pattern.compile("(?i)\\bwww\\.[a-zA-Z0-9\\p{L}\\-\\.]+\\.[a-zA-Z]{2,}(/[^\\s<>'\"{}|\\\\^`]*)?")
    private val IP_URL_REGEX = Pattern.compile("(?i)\\b(?:https?://)?[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}(:[0-9]+)?(/[a-zA-Z0-9\\-\\._~:/?#\\[\\]@!$&'()*+,;=]*)?")
    private val BARE_DOMAIN_REGEX = Pattern.compile(
        "(?i)\\b([a-zA-Z0-9\\p{L}\\-]+(?:\\.[a-zA-Z0-9\\p{L}\\-]+)*\\.(?:com|co|es|net|org|xyz|top|icu|tk|ml|ga|cf|gq|cam|click|fit|link|buzz|online|site|space|fun|host|work|shop|store|app|dev|cloud|live|info|pro|me|io)(?:/[^\\s<>'\"{}|\\\\^`]*)?)\\b"
    )

    private val RAW_IP_PATTERN = Pattern.compile("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$")

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
        val result = mutableSetOf<String>()
        for (domainSet in OFFICIAL_BRAND_DOMAINS.values) {
            for (domain in domainSet) {
                result.add(domain)
            }
        }
        result
    }

    private val FRAUD_PATH_KEYWORDS = listOf(
        "login", "iniciar-sesion", "verificar", "actualizar", "seguridad",
        "bloqueo", "bloqueada", "desbloqueo", "reactivar", "recuperar",
        "validar", "confirmar", "tarjeta", "clave", "token", "banca",
        "mandamiento", "embargo", "multa", "sancion", "pago", "retiro"
    )

    private val LEETSPEAK_DOMAIN_PATTERN = Regex("(?i)\\b.*(?:b[4a]nc[0o]|n[3e]qu[1i]|d[4a]v[1i]|cl[4a]v[3e]).*\\b")

    private val CONSONANTS = setOf(
        'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm',
        'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'z'
    )

    private const val MAX_URLS = 25

    private val SHORT_BRAND_REGEX: Map<String, Regex> by lazy {
        val result = mutableMapOf<String, Regex>()
        for (brand in OFFICIAL_BRAND_DOMAINS.keys) {
            if (brand.length <= 3) {
                val pattern = "(?i)(?:^|[-._0-9])" + Regex.escape(brand) + "(?:[-._0-9]|$)"
                result[brand] = Regex(pattern)
            }
        }
        result
    }

    fun extractUrls(text: String): List<String> {
        val urls = LinkedHashSet<String>()

        fun cleanCandidate(candidate: String): String {
            return candidate.trimEnd('.', ',', ';', ':', ')', ']', '!', '?', '"', '\'')
        }

        val protoMatcher = PROTOCOL_URL_REGEX.matcher(text)
        while (protoMatcher.find() && urls.size < MAX_URLS) {
            val candidate = cleanCandidate(protoMatcher.group())
            if (candidate.length > 5) {
                urls.add(candidate)
            }
        }

        val wwwMatcher = WWW_URL_REGEX.matcher(text)
        while (wwwMatcher.find() && urls.size < MAX_URLS) {
            val candidate = cleanCandidate(wwwMatcher.group())
            if (candidate.length > 4) {
                urls.add(candidate)
            }
        }

        val ipMatcher = IP_URL_REGEX.matcher(text)
        while (ipMatcher.find() && urls.size < MAX_URLS) {
            val candidate = cleanCandidate(ipMatcher.group())
            if (isValidIpv4Candidate(candidate)) {
                var alreadyContained = false
                for (existing in urls) {
                    if (existing.contains(candidate)) {
                        alreadyContained = true
                        break
                    }
                }
                if (!alreadyContained) {
                    urls.add(candidate)
                }
            }
        }

        val bareMatcher = BARE_DOMAIN_REGEX.matcher(text)
        while (bareMatcher.find() && urls.size < MAX_URLS) {
            val candidate = cleanCandidate(bareMatcher.group(1) ?: bareMatcher.group())
            if (candidate.length > 4) {
                var alreadyContained = false
                for (existing in urls) {
                    if (existing.contains(candidate)) {
                        alreadyContained = true
                        break
                    }
                }
                if (!alreadyContained) {
                    urls.add(candidate)
                }
            }
        }

        return urls.toList()
    }

    private fun isValidIpv4Candidate(candidate: String): Boolean {
        val host = candidate.substringAfter("://").substringBefore("/").substringBefore(":")
        val parts = host.split('.')
        if (parts.size != 4) {
            return false
        }
        for (part in parts) {
            val num = part.toIntOrNull()
            if (num == null || num < 0 || num > 255) {
                return false
            }
        }
        return true
    }

    fun analyzeUrls(urls: List<String>): List<ThreatFactor> {
        val factors = mutableListOf<ThreatFactor>()

        for (rawUrl in urls) {
            val sanitized = rawUrl.replace("hxxps", "https", ignoreCase = true)
                .replace("hxxp", "http", ignoreCase = true)
                .replace("[.]", ".")
                .replace("(.)", ".")
                .replace(" ", "")

            val normalizedUrl = if (!sanitized.startsWith("http://") && !sanitized.startsWith("https://")) {
                "http://$sanitized"
            } else {
                sanitized
            }

            val rawHost = try {
                val host = URI(normalizedUrl).host
                if (host == null || host.isBlank()) {
                    sanitized.substringAfter("://").substringBefore("/").substringBefore(":")
                } else {
                    host
                }
            } catch (e: Exception) {
                sanitized.substringAfter("://").substringBefore("/").substringBefore(":")
            }.lowercase()

            if (rawHost.isBlank()) {
                continue
            }

            val domain = rawHost
            val normalizedDomain = HomographDetector.normalizeHomoglyphs(domain)
            val path = try {
                URI(normalizedUrl).path ?: ""
            } catch (e: Exception) {
                sanitized.substringAfter("://").substringAfter("/", "")
            }.lowercase()

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

            val tld = domain.substringAfterLast(".", "")
            if (HIGH_RISK_TLDS.contains(tld)) {
                factors.add(
                    ThreatFactor(
                        category = "Extensión de Dominio Sospechosa (.$tld)",
                        description = "Utiliza una extensión de dominio comúnmente asociada a campañas de smishing y fraude.",
                        severityScore = 25,
                        matchedSnippet = rawUrl
                    )
                )
            }

            var isShortener = false
            for (shortener in KNOWN_SHORTENERS) {
                if (domain == shortener || domain.endsWith(".$shortener")) {
                    isShortener = true
                    break
                }
            }
            if (isShortener) {
                factors.add(
                    ThreatFactor(
                        category = "Enlace Acortado u Oculto",
                        description = "Utiliza un servicio acortador para ocultar el verdadero destino del enlace.",
                        severityScore = 20,
                        matchedSnippet = rawUrl
                    )
                )
            }

            var isAbusedHost = false
            for (abused in ABUSED_FREE_HOSTS) {
                if (domain == abused || domain.endsWith(".$abused")) {
                    isAbusedHost = true
                    break
                }
            }
            if (isAbusedHost) {
                factors.add(
                    ThreatFactor(
                        category = "Servicio de Alojamiento Gratuito o Túnel",
                        description = "El enlace aloja contenido en una plataforma gratuita o túnel frecuentemente abusado ($domain).",
                        severityScore = 30,
                        matchedSnippet = rawUrl
                    )
                )
            }

            var isOfficialDomain = false
            for (official in ALL_OFFICIAL_DOMAINS) {
                if (domain == official || domain.endsWith(".$official") || normalizedDomain == official || normalizedDomain.endsWith(".$official")) {
                    isOfficialDomain = true
                    break
                }
            }

            var brandSpoofed = false
            if (!isOfficialDomain) {
                for ((brand, officialDomains) in OFFICIAL_BRAND_DOMAINS) {
                    val matchesCurrent = domainMatchesBrand(domain, brand)
                    val matchesNormalized = domainMatchesBrand(normalizedDomain, brand)

                    if (matchesCurrent || matchesNormalized) {
                        val isOfficialForBrand = isOfficialDomain(domain, officialDomains) || isOfficialDomain(normalizedDomain, officialDomains)
                        if (!isOfficialForBrand) {
                            brandSpoofed = true
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
            }

            val domainLabels = domain.split('.')
            if (!isOfficialDomain && domainLabels.size > 4 && !RAW_IP_PATTERN.matcher(domain).matches()) {
                factors.add(
                    ThreatFactor(
                        category = "Cantidad Excesiva de Subdominios",
                        description = "Estructura anómala con múltiples niveles de subdominio para engañar al usuario.",
                        severityScore = 20,
                        matchedSnippet = domain
                    )
                )
            }

            if (!isOfficialDomain && (rawUrl.length > 85 || domain.length > 32)) {
                factors.add(
                    ThreatFactor(
                        category = "Longitud Inusual de Enlace",
                        description = "El enlace o dominio es excesivamente extenso, técnica utilizada para ofuscar la URL.",
                        severityScore = 15,
                        matchedSnippet = if (domain.length > 32) domain else rawUrl
                    )
                )
            }

            if (rawUrl.contains("@")) {
                factors.add(
                    ThreatFactor(
                        category = "Carácter Anómalo en URL (@)",
                        description = "Uso del carácter '@' para engañar sobre el host real al que se navega.",
                        severityScore = 25,
                        matchedSnippet = "@"
                    )
                )
            }
            val hyphenCount = domain.count { it == '-' }
            if (!isOfficialDomain && hyphenCount >= 3) {
                factors.add(
                    ThreatFactor(
                        category = "Uso Excesivo de Guiones en Dominio",
                        description = "El dominio contiene múltiples guiones ($hyphenCount) concatenando términos sospechosos.",
                        severityScore = 15,
                        matchedSnippet = domain
                    )
                )
            }

            var hasFraudKeyword = false
            for (kw in FRAUD_PATH_KEYWORDS) {
                if (path.contains(kw) || (domain.contains(kw) && !brandSpoofed)) {
                    hasFraudKeyword = true
                    break
                }
            }
            if (hasFraudKeyword && !isOfficialDomain) {
                factors.add(
                    ThreatFactor(
                        category = "Palabras Clave de Fraude en Enlace",
                        description = "La ruta o dominio contiene palabras frecuentemente asociadas a ingeniería social y captura de credenciales.",
                        severityScore = 20,
                        matchedSnippet = rawUrl
                    )
                )
            }

            if (LEETSPEAK_DOMAIN_PATTERN.matches(domain) && !isOfficialDomain) {
                factors.add(
                    ThreatFactor(
                        category = "Patrón Numérico Sospechoso en Dominio",
                        description = "El dominio sustituye letras por números simulando marcas legítimas (Leetspeak).",
                        severityScore = 20,
                        matchedSnippet = domain
                    )
                )
            }

            if (!isOfficialDomain && normalizedUrl.startsWith("http://") && !RAW_IP_PATTERN.matcher(domain).matches()) {
                if (hasFraudKeyword || brandSpoofed) {
                    factors.add(
                        ThreatFactor(
                            category = "Protocolo No Seguro (HTTP)",
                            description = "El enlace no utiliza cifrado seguro HTTPS a pesar de solicitar acciones de autenticación o datos sensibles.",
                            severityScore = 15,
                            matchedSnippet = "http://"
                        )
                    )
                }
            }

            val subLabel = domain.substringBeforeLast(".")
            val entropy = calculateShannonEntropy(subLabel)
            val hasHighConsonants = hasExcessiveConsonants(subLabel)

            if (!isOfficialDomain && ((entropy > 3.8 && subLabel.length > 12) || (hasHighConsonants && subLabel.length > 10))) {
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

        return factors
    }

    fun analyzeSingleUrl(url: String): UrlDecision {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return UrlDecision(
                url = url,
                isBlocked = false,
                threatScore = 0,
                summaryReason = "URL vacía o no válida.",
                factors = emptyList()
            )
        }

        val homoglyphs = HomographDetector.findHomoglyphs(trimmed)
        val normalized = HomographDetector.normalizeHomoglyphs(trimmed)
        val candidateUrls = listOf(trimmed, normalized).distinct()

        val factors = analyzeUrls(candidateUrls).toMutableList()

        if (homoglyphs.isNotEmpty()) {
            factors.add(
                ThreatFactor(
                    category = "Ataque Homógrafo / Evasión de Filtros",
                    description = "El enlace contiene caracteres no latinos o invisibles para suplantar dominios legítimos.",
                    severityScore = 35,
                    matchedSnippet = homoglyphs.joinToString("; ")
                )
            )
        }

        var totalScore = 0
        for (f in factors) {
            totalScore += f.severityScore
        }
        val boundedScore = min(100, max(0, totalScore))

        val hasCriticalFactor = factors.any {
            it.category.contains("Dirección IP Directa") ||
            it.category.contains("Suplantación de Marca") ||
            it.category.contains("Alojamiento Gratuito") ||
            it.category.contains("Extensión de Dominio Sospechosa")
        }

        val isBlocked = boundedScore >= 50 || (hasCriticalFactor && boundedScore >= 35)

        val reason = when {
            isBlocked -> "Enlace de alto riesgo bloqueado preventivamente por SmishGuard ($boundedScore% de amenaza)."
            boundedScore >= 30 -> "Enlace con características sospechosas detectadas."
            else -> "Enlace verificado sin anomalías conocidas."
        }

        return UrlDecision(
            url = trimmed,
            isBlocked = isBlocked,
            threatScore = boundedScore,
            summaryReason = reason,
            factors = factors
        )
    }

    private fun domainMatchesBrand(domain: String, brand: String): Boolean {
        if (brand.length <= 3) {
            val cachedRegex = SHORT_BRAND_REGEX[brand]
            val regex = if (cachedRegex != null) {
                cachedRegex
            } else {
                val pattern = "(?i)(?:^|[-._0-9])" + Regex.escape(brand) + "(?:[-._0-9]|$)"
                Regex(pattern)
            }
            return regex.containsMatchIn(domain)
        }
        return domain.contains(brand, ignoreCase = true)
    }

    fun isOfficialDomain(domain: String, officialDomains: Set<String>): Boolean {
        val cleanDomain = domain.lowercase().trim()
        for (official in officialDomains) {
            if (cleanDomain == official || cleanDomain.endsWith(".$official")) {
                return true
            }
        }
        return false
    }

    fun areAllUrlsLegitimate(urls: List<String>): Boolean {
        if (urls.isEmpty()) {
            return true
        }

        for (rawUrl in urls) {
            val cleanUrl = rawUrl.replace("hxxps", "https", ignoreCase = true)
                .replace("hxxp", "http", ignoreCase = true)
                .replace("[.]", ".")
            val normalized = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                "http://$cleanUrl"
            } else {
                cleanUrl
            }

            val domain = try {
                val host = URI(normalized).host
                if (host == null || host.isBlank()) {
                    cleanUrl.substringAfter("://").substringBefore("/").substringBefore(":")
                } else {
                    host
                }
            } catch (e: Exception) {
                cleanUrl.substringAfter("://").substringBefore("/").substringBefore(":")
            }.lowercase()

            if (domain.isBlank()) {
                return false
            }

            var isOfficial = false
            for (official in ALL_OFFICIAL_DOMAINS) {
                if (domain == official || domain.endsWith(".$official")) {
                    isOfficial = true
                    break
                }
            }
            if (!isOfficial) {
                return false
            }
        }
        return true
    }

    private fun calculateShannonEntropy(str: String): Double {
        if (str.isEmpty()) {
            return 0.0
        }
        val freqMap = mutableMapOf<Char, Int>()
        for (i in 0 until str.length) {
            val c = str[i]
            val currentCount = freqMap[c]
            if (currentCount != null) {
                freqMap[c] = currentCount + 1
            } else {
                freqMap[c] = 1
            }
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
                if (consecutive >= 6) {
                    return true
                }
            } else {
                consecutive = 0
            }
        }
        return false
    }
}
