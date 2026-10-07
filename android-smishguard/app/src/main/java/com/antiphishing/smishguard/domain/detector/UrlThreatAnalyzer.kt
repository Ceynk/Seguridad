package com.antiphishing.smishguard.domain.detector

import com.antiphishing.smishguard.domain.model.ThreatFactor
import java.net.URI
import java.util.regex.Pattern
import kotlin.math.ln

object UrlThreatAnalyzer {

    private val URL_REGEX = Pattern.compile(
        """(?i)\b(?:https?://|hxxps?://|www\.)[^\s<>"'{}|\\^`]{1,2000}|\b[^\s<>"'{}|\\^`/@]{2,253}\.(?:top|xyz|icu|tk|ml|ga|cf|gq|work|click|rest|cam|ru|cn|fit|link|online|site|space|fun|buzz|club|shop|app|io|me|info|cc|to|com|net|org|co|es|mx|pe|ar|cl|ec|us|biz|vip|lat|live|life|pro|sbs|cfd|bond|cyou|qpon|pw|monster|autos|quest|store|website|digital|support|help|page|dev)(?![a-z0-9\-])(?:/[^\s<>"'{}|\\^`]{0,2000})?|\b(?:bit\.ly|tinyurl\.com|t\.co|cutt\.ly|is\.gd|rb\.gy|shorturl\.at|t\.ly|wa\.me)/[^\s<>"'{}|\\^`]{0,2000}"""
    )

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
        val matcher = URL_REGEX.matcher(text)
        while (matcher.find() && urls.size < MAX_URLS) {
            val candidate = matcher.group().trimEnd('.', ',', ';', ':', ')', ']', '!', '?')
            if (candidate.length > 3) {
                urls.add(candidate)
            }
        }
        val ipMatcher = IP_URL_REGEX.matcher(text)
        while (ipMatcher.find() && urls.size < MAX_URLS) {
            val found = ipMatcher.group().trimEnd('.', ',', ';', ':', ')', ']', '!', '?')
            if (!isValidIpv4Inside(found)) {
                continue
            }
            var alreadyContained = false
            for (existing in urls) {
                if (existing.contains(found)) {
                    alreadyContained = true
                    break
                }
            }
            if (!alreadyContained) {
                urls.add(found)
            }
        }
        return urls.toList()
    }

    private fun isValidIpv4Inside(candidate: String): Boolean {
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
            val sanitized = rawUrl.replace("hxxp", "http", ignoreCase = true)
                .replace("[.]", ".")
                .replace("(.)", ".")
                .replace(" ", "")

            val normalizedUrl = if (!sanitized.startsWith("http://") && !sanitized.startsWith("https://")) {
                "http://$sanitized"
            } else {
                sanitized
            }

            val domain = try {
                val host = URI(normalizedUrl).host
                if (host == null || host.isBlank()) {
                    sanitized.substringAfter("://").substringBefore("/").substringBefore(":")
                } else {
                    host
                }
            } catch (e: Exception) {
                sanitized.substringAfter("://").substringBefore("/").substringBefore(":")
            }.lowercase()

            if (domain.isBlank()) {
                continue
            }

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
                        description = "Utiliza una extensión de dominio frecuentemente asociada a campañas de smishing y malware.",
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
                        description = "Utiliza un servicio acortador para camuflar el verdadero destino final.",
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
                        description = "El enlace aloja contenido en un servicio gratuito frecuentemente abusado ($domain).",
                        severityScore = 30,
                        matchedSnippet = rawUrl
                    )
                )
            }

            val normalizedDomain = HomographDetector.normalizeHomoglyphs(domain)
            for ((brand, officialDomains) in OFFICIAL_BRAND_DOMAINS) {
                val matchesCurrent = domainMatchesBrand(domain, brand)
                val matchesNormalized = domainMatchesBrand(normalizedDomain, brand)

                if (matchesCurrent || matchesNormalized) {
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

            val subLabel = domain.substringBeforeLast(".")
            val entropy = calculateShannonEntropy(subLabel)
            val hasHighConsonantDensity = hasExcessiveConsonants(subLabel)

            if ((entropy > 3.8 && subLabel.length > 12) || (hasHighConsonantDensity && subLabel.length > 10)) {
                var isAnyOfficial = false
                for (official in ALL_OFFICIAL_DOMAINS) {
                    if (domain == official || domain.endsWith(".$official")) {
                        isAnyOfficial = true
                        break
                    }
                }
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
            val cleanUrl = rawUrl.replace("hxxp", "http", ignoreCase = true).replace("[.]", ".")
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
