package com.antiphishing.smishguard.domain.detector

import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

data class ResultadoClasificacionSms(
    val categoriaPredicha: String,
    val probabilidadLegitimo: Double,
    val probabilidadSpam: Double,
    val probabilidadSmishing: Double,
    val terminosRelevantes: List<String>,
    val tieneEvidenciaEspecificaSmishing: Boolean = false
)

data class PesosTermino(
    val termino: String,
    val valorIdf: Double,
    val pesoLegitimo: Double,
    val pesoSpam: Double,
    val pesoSmishing: Double
)

object ClasificadorMensajesSms {

    const val CATEGORIA_LEGITIMO = "LEGITIMO"
    const val CATEGORIA_SPAM = "SPAM"
    const val CATEGORIA_SMISHING = "SMISHING"

    private const val ARCHIVO_MODELO = "modelo_tfidf_sms.txt"

    private val PALABRAS_VACIAS = setOf(
        "de", "la", "el", "en", "y", "a", "los", "del", "se", "las",
        "un", "una", "con", "al", "lo", "como", "mas", "pero", "sus",
        "le", "ya", "o", "fue", "este", "ha", "si", "porque", "esta",
        "son", "entre", "cuando", "muy", "sin", "sobre", "tambien", "me",
        "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante"
    )

    private val DOMINIOS_OFICIALES = setOf(
        "bancolombia.com", "grupobancolombia.com", "transaccionesbancolombia.com", "bancolombia.com.co",
        "nequi.com.co", "nequi.com", "daviplata.com", "daviplata.com.co",
        "davivienda.com", "davivienda.com.co", "bancodebogota.com", "bancodebogota.com.co",
        "bancoagrario.gov.co", "bbva.es", "bbva.com", "bbva.com.co",
        "santander.es", "santander.com", "bancosantander.es",
        "caixabank.es", "caixabank.com", "lacaixa.es",
        "bancsabadell.com", "bancosabadell.com",
        "scotiabankcolpatria.com", "colpatria.com", "scotiabank.com",
        "avvillas.com.co", "bancodeoccidente.com.co", "bancocajasocial.com",
        "itau.co", "itau.com", "bancofalabella.com.co", "falabella.com.co", "falabella.com",
        "lulobank.com", "nu.com.co", "nubank.com.br", "movii.com.co", "dale.com.co",
        "transfiya.com.co", "achcolombia.com.co", "ing.es", "ing.com", "bankinter.com", "abanca.com",
        "correos.es", "correos.com", "dhl.com", "dhl.es", "dhl.com.co", "seur.com", "fedex.com", "mrw.es",
        "dgt.es", "gob.es", "hacienda.gob.es", "agenciatributaria.es", "agenciatributaria.gob.es", "seg-social.es",
        "dian.gov.co", "gov.co", "pse.com.co", "fcm.org.co", "simit.org.co", "runt.com.co", "runt.gov.co",
        "prosperidadsocial.gov.co", "colpensiones.gov.co",
        "servientrega.com", "servientrega.com.co", "coordinadora.com", "interrapidisimo.com",
        "envia.co", "4-72.com.co", "efecty.com.co", "supergiros.com.co",
        "claro.com.co", "miclaro.com.co", "movistar.co", "movistar.com.co", "movistar.es",
        "tigo.com.co", "wom.co", "exito.com", "alkosto.com", "rappi.com.co", "rappi.com",
        "mercadolibre.com", "mercadolibre.com.co",
        "google.com", "google.co", "google.es", "accounts.google.com",
        "apple.com", "icloud.com", "whatsapp.com",
        "microsoft.com", "live.com", "office.com", "netflix.com",
        "amazon.com", "amazon.es", "amazon.co", "paypal.com", "paypal.es", "bizum.es"
    )

    private val MARCAS_PROTEGIDAS_DOMINIO = listOf(
        "bancolombia", "nequi", "daviplata", "davivienda", "bancodebogota", "bancobogota",
        "bancoagrario", "bbva", "santander", "caixabank", "scotiabank", "colpatria",
        "avvillas", "bancocajasocial", "cajasocial", "itau", "lulobank", "nubank",
        "movii", "transfiya", "correos", "dhl", "fedex", "dian", "simit", "runt",
        "colpensiones", "servientrega", "coordinadora", "interrapidisimo", "4-72",
        "efecty", "supergiros", "claro", "movistar", "tigo", "mercadolibre",
        "google", "icloud", "apple", "whatsapp", "microsoft", "netflix", "amazon", "paypal"
    )

    private val PALABRAS_RUTA_SENSIBLE = listOf(
        "login", "iniciar-sesion", "verificar", "verificacion", "actualizar", "actualizacion",
        "seguridad", "bloqueo", "desbloqueo", "reactivar", "validar", "validacion",
        "confirmar", "clave", "token", "banca", "sucursal", "acceso", "identidad",
        "anular", "anulacion", "cancelar", "mandamiento", "embargo", "multa",
        "comparendo", "sancion", "pago", "retiro", "subsidio", "desembolso", "aduana",
        "auth", "reembolso", "abono", "ficha", "firmar", "biometria"
    )

    private val ACORTADORES_CONOCIDOS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "cutt.ly", "is.gd", "ow.ly", "rb.gy", "goo.gl",
        "shorturl.at", "t.ly", "buff.ly", "shorte.st", "adf.ly", "bc.vc", "s.id", "wa.me",
        "acortar.link", "bitly.ws", "tiny.cc", "v.gd", "qrco.de", "short.gy", "linktr.ee"
    )

    private val EXTENSIONES_RIESGO = setOf(
        "xyz", "top", "icu", "tk", "ml", "ga", "cf", "gq", "work", "click", "rest",
        "cam", "ru", "cn", "fit", "link", "buzz", "tokyo", "racing", "date", "men",
        "online", "site", "live", "space", "sbs", "cfd", "bond", "cyou", "qpon", "pw",
        "monster", "autos", "quest", "lat", "vip", "support", "help", "cc", "ws"
    )

    private val ALOJAMIENTOS_ABUSADOS = setOf(
        "duckdns.org", "ngrok.io", "ngrok-free.app", "firebaseapp.com", "web.app",
        "pages.dev", "workers.dev", "vercel.app", "000webhostapp.com", "glitch.me",
        "surge.sh", "netlify.app", "render.com", "serveo.net", "loca.lt", "trycloudflare.com",
        "github.io", "weebly.com", "wixsite.com", "blogspot.com", "sites.google.com",
        "onrender.com", "replit.app", "repl.co", "webflow.io", "framer.app", "translate.goog"
    )

    private val PREFIJOS_INTERNACIONALES_RIESGO = listOf(
        "+234", "+229", "+225", "+212", "+247", "+675", "+881", "+882", "+880"
    )

    private val MAPA_HOMOGLIFOS = mapOf(
        'а' to 'a', 'А' to 'A', 'с' to 'c', 'С' to 'C', 'е' to 'e', 'Е' to 'E',
        'о' to 'o', 'О' to 'O', 'р' to 'p', 'Р' to 'P', 'ѕ' to 's', 'Ѕ' to 'S',
        'х' to 'x', 'Х' to 'X', 'у' to 'y', 'У' to 'Y', 'і' to 'i', 'І' to 'I',
        'ј' to 'j', 'Ј' to 'J', 'ԁ' to 'd', 'ԛ' to 'q', 'α' to 'a', 'Α' to 'A',
        'ο' to 'o', 'Ο' to 'O', 'ν' to 'v', 'Ν' to 'N', 'ρ' to 'p', 'Ρ' to 'P',
        'τ' to 't', 'Τ' to 'T', 'ո' to 'n', 'ѵ' to 'v', 'ѡ' to 'w', 'ӏ' to 'l',
        'Ӏ' to 'I', 'В' to 'B', 'Н' to 'H', 'К' to 'K', 'М' to 'M', 'Т' to 'T'
    )

    private val CARACTERES_INVISIBLES = setOf(
        '\u200B', '\u200C', '\u200D', '\u200E', '\u200F',
        '\u202A', '\u202B', '\u202C', '\u202D', '\u202E',
        '\u2060', '\uFEFF', '\u00AD', '\u034F'
    )

    private val REEMPLAZOS_LEET = listOf(
        Regex("""(?i)\bb4nc0\b""") to "banco",
        Regex("""(?i)\bb4nc0l0mb14\b""") to "bancolombia",
        Regex("""(?i)\bn3qu1\b""") to "nequi",
        Regex("""(?i)\bd4v1pl4t4\b""") to "daviplata",
        Regex("""(?i)\bd14n\b""") to "dian",
        Regex("""(?i)\bbl0qu34d[ao]\b""") to "bloqueada",
        Regex("""(?i)\br3str1ng1d[ao]\b""") to "restringido",
        Regex("""(?i)\bcl4v3\b""") to "clave",
        Regex("""(?i)\bcr3d1t0\b""") to "credito",
        Regex("""(?i)\bp4qu3t3\b""") to "paquete",
        Regex("""(?i)\bs3gur1d4d\b""") to "seguridad",
        Regex("""(?i)\bv3r1f1c4r\b""") to "verificar",
        Regex("""(?i)\b4ctu4l1z4r\b""") to "actualizar"
    )

    private val PALABRAS_SENSIBLES_ESPACIADO = setOf(
        "banco", "nequi", "daviplata", "bancolombia", "clave", "bloqueo", "bloqueada",
        "tarjeta", "dian", "aduana", "multa", "urgente", "pse", "dinero", "premio"
    )

    private val PATRON_ESPACIADO_ARTIFICIAL = Regex(
        """(?i)\b([a-z0-9])\s+([a-z0-9])\s+([a-z0-9])\s+([a-z0-9])(?:\s+([a-z0-9]))*\b"""
    )

    private val PATRON_ENLACE = Regex(
        """(?i)(?:https?://|hxxps?://|ftp://)[^\s<>'"{}|\\^`]+|\bwww\.[a-z0-9\-]+(?:\.[a-z0-9\-]+)+(?:/[^\s<>'"{}|\\^`]*)?|\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(?::\d+)?(?:/[^\s<>'"{}|\\^`]*)?|\b[a-z0-9\-]+(?:\.[a-z0-9\-]+)+\.(?:com|co|es|net|org|edu|gov|gob|xyz|top|icu|tk|ml|ga|cf|gq|cam|click|fit|link|buzz|online|site|space|fun|host|work|shop|store|app|dev|cloud|live|info|pro|me|io|ly|gy|gl|at|cc|ws|lat|vip|sbs|cfd|cyou|bond|monster|rest|quest|support|help|so)(?:/[^\s<>'"{}|\\^`]*)?"""
    )

    private val PATRON_INICIO_IPV4 = Regex("""^\d+\.\d+\.\d+\.\d+""")
    private val PATRON_CELULAR = Regex("""^(?:\+?57)?3\d{9}$|^(?:\+?34)?[67]\d{8}$""")
    private val PATRON_CODIGO_CORTO = Regex("""^\d{4,6}$""")
    private val PATRON_IP = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")
    private val PATRON_MONTO = Regex("""\$\s*\d+|\b\d+(?:\.\d{3})+\b|\b(?:cop|eur|usd)\s*\d+|\b\d+\s*(?:cop|eur|usd|pesos)\b""")
    private val PATRON_PROMOCIONAL = Regex("""\b\d{1,2}\s*%\b|\bdescuento[s]?\b|\boferta[s]?\b|\bpromo(?:cion(?:es)?)?\b|\b2x1\b|\b3x2\b|\bliquidacion\b|\brebajas\b|\btienda[s]?\s+fisica[s]?\b""")
    private val PATRON_AVISO_OTP = Regex("""\bno\s+(?:lo\s+|la\s+)?(?:comparta[s]?|entregue[s]?|revele[s]?)\b|\bnunca\s+(?:lo\s+|la\s+)?(?:comparta[s]?|entregue[s]?)\b|\bjamas\s+revele[s]?\b|\bvalida?\s+por\s+\d+\s+min|\bvigencia\s+de\s+\d+\s+min|\bexpira\s+en\s+\d+\s+min|\bun\s+solo\s+uso\b""")
    private val PATRON_SOLICITUD_CREDENCIAL = Regex("""\b(?<!no\s)(?<!nunca\s)(?<!jamas\s)(?:reenvie|responda|responde|envie|envianos|entregue|digite|digita|ingrese|ingresa|dictenos|escriba)\b.*\b(?:codigo|otp|clave|pin|credenciales|usuario|contrase|cvv|16\s+digitos|4\s+digitos|6\s+digitos)\b""")
    private val PATRON_SOLICITUD_CONTACTO_O_GIRO = Regex("""\b(?:llame\s+(?:ya|urgente|de\s+inmediato|antes\s+de)|comuniquese\s+(?:ya|urgente|hoy|de\s+inmediato)|escrib(?:eme|a|enos)\s+(?:urgente\s+)?(?:a\s+|por\s+|al\s+)?whatsapp|este\s+es\s+mi\s+(?:nuevo\s+numero|numero\s+nuevo)|(?:prestame|pasame|consigname|transfiere(?:me)?|consigne)\s+(?:urgente\s+|por\s+favor\s+)?(?:\$|\d+|a\s+la\s+cuenta|al\s+nequi|al\s+daviplata))\b""")
    private val PATRON_NO_ALFANUMERICO = Regex("""[^a-z0-9\s]""")
    private val PATRON_ESPACIOS = Regex("""\s+""")

    private val ETIQUETAS_ESTRUCTURALES = mapOf(
        "_enlace_suplanta_marca_" to "suplantacion de marca en enlace",
        "_enlace_extension_riesgo_" to "extension de dominio riesgosa",
        "_enlace_ruta_sensible_" to "ruta sensible de captura",
        "_enlace_estructura_compleja_" to "estructura compleja de dominio",
        "_enlace_alojamiento_gratuito_" to "alojamiento gratuito abusado",
        "_enlace_acortado_" to "enlace acortado",
        "_enlace_ip_directa_" to "direccion ip directa",
        "_enlace_ejecutable_apk_" to "descarga de archivo apk",
        "_solicita_credencial_u_otp_" to "solicitud de clave o codigo",
        "_solicita_contacto_o_giro_" to "solicitud urgente de contacto o giro",
        "_telefono_en_texto_" to "linea telefonica de contacto en texto"
    )

    @Volatile
    private var modeloCargado = false
    private var umbralSmishing = 0.46
    private var umbralSpam = 0.46
    private var interceptoLegitimo = 0.0
    private var interceptoSpam = 0.0
    private var interceptoSmishing = 0.0
    private val mapaTerminos = HashMap<String, PesosTermino>()

    fun precargarModelo() {
        cargarModeloSiEsNecesario()
    }

    fun estaModeloCargado(): Boolean {
        cargarModeloSiEsNecesario()
        return mapaTerminos.isNotEmpty()
    }

    @Synchronized
    private fun cargarModeloSiEsNecesario() {
        if (modeloCargado) {
            return
        }

        val flujoEntrada = ClasificadorMensajesSms::class.java.classLoader?.getResourceAsStream(ARCHIVO_MODELO)
            ?: ClasificadorMensajesSms::class.java.getResourceAsStream("/$ARCHIVO_MODELO")
        if (flujoEntrada == null) {
            modeloCargado = true
            return
        }

        BufferedReader(InputStreamReader(flujoEntrada, Charsets.UTF_8)).use { lector ->
            var lineaActual = lector.readLine()
            while (lineaActual != null) {
                procesarLineaModelo(lineaActual.trim())
                lineaActual = lector.readLine()
            }
        }
        modeloCargado = true
    }

    private fun procesarLineaModelo(textoLinea: String) {
        if (textoLinea.isEmpty()) {
            return
        }
        val partes = textoLinea.split("|")
        when (partes[0]) {
            "CONFIG" -> {
                if (partes.size >= 3) {
                    umbralSmishing = partes[1].toDoubleOrNull() ?: 0.46
                    umbralSpam = partes[2].toDoubleOrNull() ?: 0.46
                }
            }
            "INTERCEPTOS" -> {
                if (partes.size >= 4) {
                    interceptoLegitimo = partes[1].toDoubleOrNull() ?: 0.0
                    interceptoSpam = partes[2].toDoubleOrNull() ?: 0.0
                    interceptoSmishing = partes[3].toDoubleOrNull() ?: 0.0
                }
            }
            "TERMINO" -> {
                if (partes.size >= 6) {
                    val termino = partes[1]
                    mapaTerminos[termino] = PesosTermino(
                        termino = termino,
                        valorIdf = partes[2].toDoubleOrNull() ?: 1.0,
                        pesoLegitimo = partes[3].toDoubleOrNull() ?: 0.0,
                        pesoSpam = partes[4].toDoubleOrNull() ?: 0.0,
                        pesoSmishing = partes[5].toDoubleOrNull() ?: 0.0
                    )
                }
            }
        }
    }

    fun clasificarMensaje(
        remitente: String,
        mensaje: String,
        enlacesDetectados: List<String> = emptyList()
    ): ResultadoClasificacionSms {
        cargarModeloSiEsNecesario()

        if (mapaTerminos.isEmpty()) {
            return ResultadoClasificacionSms(
                categoriaPredicha = CATEGORIA_LEGITIMO,
                probabilidadLegitimo = 1.0,
                probabilidadSpam = 0.0,
                probabilidadSmishing = 0.0,
                terminosRelevantes = emptyList()
            )
        }

        val tokens = extraerTokensMensaje(remitente, mensaje, enlacesDetectados)
        val valoresTfIdf = vectorizarTokens(tokens)

        var puntajeLegitimo = interceptoLegitimo
        var puntajeSpam = interceptoSpam
        var puntajeSmishing = interceptoSmishing

        val contribucionesLexicas = ArrayList<Pair<String, Double>>()
        val contribucionesEstructurales = ArrayList<Pair<String, Double>>()

        for ((termino, valorNormalizado) in valoresTfIdf) {
            val infoTermino = mapaTerminos[termino] ?: continue
            puntajeLegitimo += valorNormalizado * infoTermino.pesoLegitimo
            puntajeSpam += valorNormalizado * infoTermino.pesoSpam
            val aporteSmishing = valorNormalizado * infoTermino.pesoSmishing
            puntajeSmishing += aporteSmishing

            if (aporteSmishing > 0.08) {
                if (!termino.startsWith("_")) {
                    contribucionesLexicas.add(Pair(termino.replace("_", " "), aporteSmishing))
                } else {
                    val etiqueta = ETIQUETAS_ESTRUCTURALES[termino]
                    if (etiqueta != null) {
                        contribucionesEstructurales.add(Pair(etiqueta, aporteSmishing))
                    }
                }
            }
        }

        val probabilidades = calcularProbabilidades(puntajeLegitimo, puntajeSpam, puntajeSmishing)
        val probLegitimo = probabilidades[0]
        val probSpam = probabilidades[1]
        val probSmishing = probabilidades[2]
        val categoria = clasificarConUmbrales(probLegitimo, probSpam, probSmishing)

        val terminosPrincipales = seleccionarTerminosRelevantes(contribucionesLexicas, contribucionesEstructurales)
        val tieneEvidenciaEspecifica = contribucionesLexicas.isNotEmpty() || contribucionesEstructurales.isNotEmpty()

        return ResultadoClasificacionSms(
            categoriaPredicha = categoria,
            probabilidadLegitimo = probLegitimo,
            probabilidadSpam = probSpam,
            probabilidadSmishing = probSmishing,
            terminosRelevantes = terminosPrincipales,
            tieneEvidenciaEspecificaSmishing = tieneEvidenciaEspecifica
        )
    }

    private fun vectorizarTokens(tokens: List<String>): Map<String, Double> {
        val conteoTokens = HashMap<String, Int>()
        for (token in tokens) {
            conteoTokens[token] = (conteoTokens[token] ?: 0) + 1
        }

        val valoresTfIdf = HashMap<String, Double>()
        var sumaCuadrados = 0.0

        for ((termino, frecuencia) in conteoTokens) {
            val infoTermino = mapaTerminos[termino] ?: continue
            val pesoTf = 1.0 + ln(frecuencia.toDouble())
            val valor = pesoTf * infoTermino.valorIdf
            valoresTfIdf[termino] = valor
            sumaCuadrados += valor * valor
        }

        val norma = sqrt(sumaCuadrados)
        if (norma > 0.0) {
            val claves = valoresTfIdf.keys.toList()
            for (clave in claves) {
                valoresTfIdf[clave] = (valoresTfIdf[clave] ?: 0.0) / norma
            }
        }
        return valoresTfIdf
    }

    private fun seleccionarTerminosRelevantes(
        contribucionesLexicas: MutableList<Pair<String, Double>>,
        contribucionesEstructurales: MutableList<Pair<String, Double>>
    ): List<String> {
        contribucionesLexicas.sortByDescending { it.second }
        contribucionesEstructurales.sortByDescending { it.second }

        val resultado = ArrayList<String>()
        for (par in contribucionesLexicas) {
            if (resultado.size < 3) {
                resultado.add(par.first)
            }
        }
        for (par in contribucionesEstructurales) {
            if (resultado.size < 3) {
                resultado.add(par.first)
            }
        }
        return resultado
    }

    private fun clasificarConUmbrales(
        probLegitimo: Double,
        probSpam: Double,
        probSmishing: Double
    ): String {
        if (probSmishing >= umbralSmishing && probSmishing >= probLegitimo && probSmishing >= probSpam) {
            return CATEGORIA_SMISHING
        }
        if (probSpam >= umbralSpam && probSpam >= probLegitimo && probSpam > probSmishing) {
            return CATEGORIA_SPAM
        }
        if (probSmishing > probLegitimo && probSmishing > probSpam && probSmishing >= 0.42) {
            return CATEGORIA_SMISHING
        }
        if (probSpam > probLegitimo && probSpam >= probSmishing) {
            return CATEGORIA_SPAM
        }
        return CATEGORIA_LEGITIMO
    }

    fun extraerTokensMensaje(
        remitente: String,
        mensaje: String,
        enlacesExternos: List<String> = emptyList()
    ): List<String> {
        val mensajeNorm = normalizarEvasion(mensaje)
        val enlaces = if (enlacesExternos.isNotEmpty()) {
            enlacesExternos
        } else {
            detectarEnlacesModelo(mensajeNorm)
        }

        val marcadoresEstructurales = extraerMarcadoresEnlaces(enlaces)
        agregarMarcadoresRemitente(remitente, marcadoresEstructurales)

        var textoSinEnlaces = mensajeNorm
        for (enlace in enlaces) {
            textoSinEnlaces = textoSinEnlaces.replace(enlace, " ")
        }

        val textoLimpio = quitarAcentos(textoSinEnlaces.lowercase())
        agregarMarcadoresSemantica(textoLimpio, marcadoresEstructurales)

        val palabrasFiltradas = extraerPalabrasBase(textoLimpio)
        val tokens = ArrayList<String>(palabrasFiltradas.size * 2 + marcadoresEstructurales.size)
        tokens.addAll(palabrasFiltradas)
        for (indice in 0 until (palabrasFiltradas.size - 1)) {
            tokens.add(palabrasFiltradas[indice] + "_" + palabrasFiltradas[indice + 1])
        }
        tokens.addAll(marcadoresEstructurales)
        return tokens
    }

    fun normalizarEvasion(texto: String): String {
        val sinInvisibles = StringBuilder(texto.length)
        for (ch in texto) {
            if (!CARACTERES_INVISIBLES.contains(ch)) {
                sinInvisibles.append(MAPA_HOMOGLIFOS[ch] ?: ch)
            }
        }
        var procesado = PATRON_ESPACIADO_ARTIFICIAL.replace(sinInvisibles.toString()) { coincidencia ->
            val compacto = coincidencia.value.replace(PATRON_ESPACIOS, "")
            if (PALABRAS_SENSIBLES_ESPACIADO.contains(compacto.lowercase())) {
                compacto
            } else {
                coincidencia.value
            }
        }
        for ((patron, reemplazo) in REEMPLAZOS_LEET) {
            procesado = procesado.replace(patron, reemplazo)
        }
        return procesado
    }

    fun detectarEnlacesModelo(texto: String): List<String> {
        val vistos = ArrayList<String>()
        for (coincidencia in PATRON_ENLACE.findAll(texto)) {
            val candidato = coincidencia.value.trimEnd('.', ',', ';', ':', ')', ']', '!', '?', '"', '\'')
            if (candidato.length <= 4) {
                continue
            }
            if (PATRON_INICIO_IPV4.containsMatchIn(candidato) && !esIpv4Valido(candidato)) {
                continue
            }
            if (!vistos.contains(candidato)) {
                vistos.add(candidato)
            }
        }
        return vistos
    }

    private fun esIpv4Valido(candidato: String): Boolean {
        val hostIp = candidato.substringBefore("/").substringBefore(":")
        val octetos = hostIp.split(".")
        if (octetos.size != 4) {
            return false
        }
        for (octeto in octetos) {
            val valor = octeto.toIntOrNull()
            if (valor == null || valor < 0 || valor > 255) {
                return false
            }
        }
        return true
    }

    private fun extraerDominioYRuta(enlace: String): Pair<String, String> {
        val limpio = enlace.lowercase()
            .trim()
            .trimEnd('.', ',', ';', ':', ')', ']', '!', '?', '"', '\'')
            .replace("hxxps://", "https://")
            .replace("hxxp://", "http://")
        val sinEsquema = if (limpio.contains("://")) limpio.substringAfter("://") else limpio
        val parteHost: String
        val ruta: String
        if (sinEsquema.contains("/")) {
            parteHost = sinEsquema.substringBefore("/")
            ruta = "/" + sinEsquema.substringAfter("/")
        } else {
            parteHost = sinEsquema
            ruta = ""
        }
        var dominio = parteHost.substringBefore(":")
        if (dominio.startsWith("www.")) {
            dominio = dominio.substring(4)
        }
        return Pair(dominio, ruta)
    }

    private fun coincideDominioEnConjunto(dominio: String, conjunto: Set<String>): Boolean {
        for (base in conjunto) {
            if (dominio == base || dominio.endsWith(".$base")) {
                return true
            }
        }
        return false
    }

    private fun extraerMarcadoresEnlaces(enlaces: List<String>): MutableList<String> {
        val marcadores = ArrayList<String>()
        if (enlaces.isEmpty()) {
            marcadores.add("_sin_enlace_")
            return marcadores
        }

        marcadores.add("_tiene_enlace_")
        for (enlace in enlaces) {
            val (dominio, ruta) = extraerDominioYRuta(enlace)
            if (dominio.isEmpty()) {
                continue
            }

            val esOficial = coincideDominioEnConjunto(dominio, DOMINIOS_OFICIALES)
            val esEduGob = dominio.endsWith(".edu.co") ||
                dominio.endsWith(".gov.co") ||
                dominio.endsWith(".gob.es") ||
                dominio.endsWith(".edu")

            if (esOficial || esEduGob) {
                marcadores.add("_enlace_verificado_")
            } else {
                marcadores.add("_enlace_externo_")
            }

            if (coincideDominioEnConjunto(dominio, ACORTADORES_CONOCIDOS)) {
                marcadores.add("_enlace_acortado_")
            }
            if (coincideDominioEnConjunto(dominio, ALOJAMIENTOS_ABUSADOS)) {
                marcadores.add("_enlace_alojamiento_gratuito_")
            }

            val partesDominio = dominio.split(".")
            val extension = if (partesDominio.size > 1) partesDominio.last() else ""
            if (EXTENSIONES_RIESGO.contains(extension)) {
                marcadores.add("_enlace_extension_riesgo_")
            }
            if (PATRON_IP.matches(dominio)) {
                marcadores.add("_enlace_ip_directa_")
            }
            if (ruta.endsWith(".apk") || ruta.endsWith("/apk")) {
                marcadores.add("_enlace_ejecutable_apk_")
            }

            if (!esOficial && !esEduGob) {
                if (MARCAS_PROTEGIDAS_DOMINIO.any { dominio.contains(it) }) {
                    marcadores.add("_enlace_suplanta_marca_")
                }
                val textoUrl = dominio + ruta
                if (PALABRAS_RUTA_SENSIBLE.any { textoUrl.contains(it) }) {
                    marcadores.add("_enlace_ruta_sensible_")
                }
                val guiones = dominio.count { it == '-' }
                if (guiones >= 2 || partesDominio.size >= 4) {
                    marcadores.add("_enlace_estructura_compleja_")
                }
            }
        }
        return marcadores
    }

    private fun agregarMarcadoresRemitente(remitente: String, marcadores: MutableList<String>) {
        val remitenteLimpio = remitente.trim().replace(PATRON_ESPACIOS, "")
        if (PREFIJOS_INTERNACIONALES_RIESGO.any { remitenteLimpio.startsWith(it) }) {
            marcadores.add("_remitente_internacional_riesgo_")
        } else if (PATRON_CELULAR.matches(remitenteLimpio)) {
            marcadores.add("_remitente_celular_")
        } else if (PATRON_CODIGO_CORTO.matches(remitenteLimpio)) {
            marcadores.add("_remitente_codigo_corto_")
        }
    }

    private fun agregarMarcadoresSemantica(textoLimpio: String, marcadores: MutableList<String>) {
        if (PATRON_MONTO.containsMatchIn(textoLimpio)) {
            marcadores.add("_menciona_monto_")
        }
        if (PATRON_PROMOCIONAL.containsMatchIn(textoLimpio)) {
            marcadores.add("_patron_promocional_")
        }
        if (PATRON_AVISO_OTP.containsMatchIn(textoLimpio)) {
            marcadores.add("_aviso_defensivo_otp_")
        }
        if (PATRON_SOLICITUD_CREDENCIAL.containsMatchIn(textoLimpio)) {
            marcadores.add("_solicita_credencial_u_otp_")
        }
        if (PATRON_SOLICITUD_CONTACTO_O_GIRO.containsMatchIn(textoLimpio)) {
            marcadores.add("_solicita_contacto_o_giro_")
        }
    }

    private fun extraerPalabrasBase(textoLimpio: String): List<String> {
        val textoAlfanumerico = PATRON_NO_ALFANUMERICO.replace(textoLimpio, " ")
        val palabrasBrutas = textoAlfanumerico.trim().split(PATRON_ESPACIOS)
        val palabrasFiltradas = ArrayList<String>()

        for (palabra in palabrasBrutas) {
            if (palabra.isEmpty()) {
                continue
            }
            if (palabra.all { it.isDigit() }) {
                if (palabra.length in 4..8) {
                    palabrasFiltradas.add("_numero_corto_")
                } else if (palabra.length in 9..12) {
                    palabrasFiltradas.add("_telefono_en_texto_")
                }
                continue
            }
            if (palabra.length >= 2 && !PALABRAS_VACIAS.contains(palabra)) {
                palabrasFiltradas.add(palabra)
            }
        }
        return palabrasFiltradas
    }

    fun quitarAcentos(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val constructor = StringBuilder(normalizado.length)
        for (caracter in normalizado) {
            if (Character.getType(caracter) != Character.NON_SPACING_MARK.toInt()) {
                constructor.append(caracter)
            }
        }
        return constructor.toString()
    }

    private fun calcularProbabilidades(
        puntajeLegitimo: Double,
        puntajeSpam: Double,
        puntajeSmishing: Double
    ): DoubleArray {
        val maximo = maxOf(puntajeLegitimo, puntajeSpam, puntajeSmishing)
        val expLegitimo = exp(puntajeLegitimo - maximo)
        val expSpam = exp(puntajeSpam - maximo)
        val expSmishing = exp(puntajeSmishing - maximo)
        val suma = expLegitimo + expSpam + expSmishing

        return doubleArrayOf(
            expLegitimo / suma,
            expSpam / suma,
            expSmishing / suma
        )
    }
}
