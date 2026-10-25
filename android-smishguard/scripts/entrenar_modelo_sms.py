import difflib
import json
import math
import os
import re
import time
import unicodedata
from collections import Counter

DIRECTORIO_ACTUAL = os.path.dirname(os.path.abspath(__file__))
RUTA_DATOS_ENTRENAMIENTO = os.path.join(DIRECTORIO_ACTUAL, "datos_entrenamiento_sms.json")
RUTA_DATOS_VALIDACION = os.path.join(DIRECTORIO_ACTUAL, "datos_validacion_sms.json")
RUTA_DATOS_PRUEBA = os.path.join(DIRECTORIO_ACTUAL, "datos_prueba_independiente.json")
RUTA_REPORTE_EVALUACION = os.path.join(DIRECTORIO_ACTUAL, "reporte_evaluacion_sms.json")
RUTA_CONSISTENCIA = os.path.join(
    DIRECTORIO_ACTUAL,
    "..",
    "app",
    "src",
    "test",
    "resources",
    "casos_consistencia_python_kotlin.json"
)
RUTA_MODELO_SALIDA = os.path.join(
    DIRECTORIO_ACTUAL,
    "..",
    "app",
    "src",
    "main",
    "resources",
    "modelo_tfidf_sms.txt"
)

CLASES = ["LEGITIMO", "SPAM", "SMISHING"]

PALABRAS_VACIAS = {
    "de", "la", "el", "en", "y", "a", "los", "del", "se", "las",
    "un", "una", "con", "al", "lo", "como", "mas", "pero", "sus",
    "le", "ya", "o", "fue", "este", "ha", "si", "porque", "esta",
    "son", "entre", "cuando", "muy", "sin", "sobre", "tambien", "me",
    "hasta", "hay", "donde", "quien", "desde", "todo", "nos", "durante"
}

# Sincronizado exactamente con ClasificadorMensajesSms.kt y UrlThreatAnalyzer.kt (sin wa.me)
DOMINIOS_OFICIALES = {
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
}

MARCAS_PROTEGIDAS_DOMINIO = [
    "bancolombia", "nequi", "daviplata", "davivienda", "bancodebogota", "bancobogota",
    "bancoagrario", "bbva", "santander", "caixabank", "scotiabank", "colpatria",
    "avvillas", "bancocajasocial", "cajasocial", "itau", "lulobank", "nubank",
    "movii", "transfiya", "correos", "dhl", "fedex", "dian", "simit", "runt",
    "colpensiones", "servientrega", "coordinadora", "interrapidisimo", "4-72",
    "efecty", "supergiros", "claro", "movistar", "tigo", "mercadolibre",
    "google", "icloud", "apple", "whatsapp", "microsoft", "netflix", "amazon", "paypal"
]

PALABRAS_RUTA_SENSIBLE = [
    "login", "iniciar-sesion", "verificar", "verificacion", "actualizar", "actualizacion",
    "seguridad", "bloqueo", "desbloqueo", "reactivar", "validar", "validacion",
    "confirmar", "clave", "token", "banca", "sucursal", "acceso", "identidad",
    "anular", "anulacion", "cancelar", "mandamiento", "embargo", "multa",
    "comparendo", "sancion", "pago", "retiro", "subsidio", "desembolso", "aduana",
    "auth", "reembolso", "abono", "ficha", "firmar", "biometria"
]

ACORTADORES_CONOCIDOS = {
    "bit.ly", "tinyurl.com", "t.co", "cutt.ly", "is.gd", "ow.ly", "rb.gy", "goo.gl",
    "shorturl.at", "t.ly", "buff.ly", "shorte.st", "adf.ly", "bc.vc", "s.id", "wa.me",
    "acortar.link", "bitly.ws", "tiny.cc", "v.gd", "qrco.de", "short.gy", "linktr.ee"
}

EXTENSIONES_RIESGO = {
    "xyz", "top", "icu", "tk", "ml", "ga", "cf", "gq", "work", "click", "rest",
    "cam", "ru", "cn", "fit", "link", "buzz", "tokyo", "racing", "date", "men",
    "online", "site", "live", "space", "sbs", "cfd", "bond", "cyou", "qpon", "pw",
    "monster", "autos", "quest", "lat", "vip", "support", "help", "cc", "ws"
}

ALOJAMIENTOS_ABUSADOS = {
    "duckdns.org", "ngrok.io", "ngrok-free.app", "firebaseapp.com", "web.app",
    "pages.dev", "workers.dev", "vercel.app", "000webhostapp.com", "glitch.me",
    "surge.sh", "netlify.app", "render.com", "serveo.net", "loca.lt", "trycloudflare.com",
    "github.io", "weebly.com", "wixsite.com", "blogspot.com", "sites.google.com",
    "onrender.com", "replit.app", "repl.co", "webflow.io", "framer.app", "translate.goog"
}

PREFIJOS_INTERNACIONALES_RIESGO = (
    "+234", "+229", "+225", "+212", "+247", "+675", "+881", "+882", "+880"
)

MAPA_HOMOGLIFOS = {
    "а": "a", "А": "A", "с": "c", "С": "C", "е": "e", "Е": "E",
    "о": "o", "О": "O", "р": "p", "Р": "P", "ѕ": "s", "Ѕ": "S",
    "х": "x", "Х": "X", "у": "y", "У": "Y", "і": "i", "І": "I",
    "ј": "j", "Ј": "J", "ԁ": "d", "ԛ": "q", "α": "a", "Α": "A",
    "ο": "o", "Ο": "O", "ν": "v", "Ν": "N", "ρ": "p", "Ρ": "P",
    "τ": "t", "Τ": "T", "ո": "n", "ѵ": "v", "ѡ": "w", "ӏ": "l",
    "Ӏ": "I", "В": "B", "Н": "H", "К": "K", "М": "M", "Т": "T"
}

CARACTERES_INVISIBLES = {
    "\u200B", "\u200C", "\u200D", "\u200E", "\u200F",
    "\u202A", "\u202B", "\u202C", "\u202D", "\u202E",
    "\u2060", "\uFEFF", "\u00AD", "\u034F"
}

REEMPLAZOS_LEET = [
    (re.compile(r"(?i)\bb4nc0\b"), "banco"),
    (re.compile(r"(?i)\bb4nc0l0mb14\b"), "bancolombia"),
    (re.compile(r"(?i)\bn3qu1\b"), "nequi"),
    (re.compile(r"(?i)\bd4v1pl4t4\b"), "daviplata"),
    (re.compile(r"(?i)\bd14n\b"), "dian"),
    (re.compile(r"(?i)\bbl0qu34d[ao]\b"), "bloqueada"),
    (re.compile(r"(?i)\br3str1ng1d[ao]\b"), "restringido"),
    (re.compile(r"(?i)\bcl4v3\b"), "clave"),
    (re.compile(r"(?i)\bcr3d1t0\b"), "credito"),
    (re.compile(r"(?i)\bp4qu3t3\b"), "paquete"),
    (re.compile(r"(?i)\bs3gur1d4d\b"), "seguridad"),
    (re.compile(r"(?i)\bv3r1f1c4r\b"), "verificar"),
    (re.compile(r"(?i)\b4ctu4l1z4r\b"), "actualizar"),
]

PALABRAS_SENSIBLES_ESPACIADO = {
    "banco", "nequi", "daviplata", "bancolombia", "clave", "bloqueo", "bloqueada",
    "tarjeta", "dian", "aduana", "multa", "urgente", "pse", "dinero", "premio"
}

PATRON_ESPACIADO_ARTIFICIAL = re.compile(r"(?i)\b([a-z0-9])\s+([a-z0-9])\s+([a-z0-9])\s+([a-z0-9])(?:\s+([a-z0-9]))*\b")

PATRON_ENLACE = re.compile(
    r"(?i)(?:https?://|hxxps?://|ftp://)[^\s<>'\"{}|\\^`]+|\bwww\.[a-z0-9\-]+(?:\.[a-z0-9\-]+)+(?:/[^\s<>'\"{}|\\^`]*)?|\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(?::\d+)?(?:/[^\s<>'\"{}|\\^`]*)?|\b[a-z0-9\-]+(?:\.[a-z0-9\-]+)+\.(?:com|co|es|net|org|edu|gov|gob|xyz|top|icu|tk|ml|ga|cf|gq|cam|click|fit|link|buzz|online|site|space|fun|host|work|shop|store|app|dev|cloud|live|info|pro|me|io|ly|gy|gl|at|cc|ws|lat|vip|sbs|cfd|cyou|bond|monster|rest|quest|support|help|so)(?:/[^\s<>'\"{}|\\^`]*)?"
)

PATRON_MASCARA_OTP = re.compile(r"\bG-\d{4,8}\b|\b\d{3}-\d{3}\b|\b\d{4,8}\b")


def normalizar_evasion(texto):
    caracteres = []
    for ch in texto:
        if ch not in CARACTERES_INVISIBLES:
            caracteres.append(MAPA_HOMOGLIFOS.get(ch, ch))
    procesado = "".join(caracteres)

    def colapsar_si_sensible(coincidencia):
        fragmento = coincidencia.group(0)
        compacto = re.sub(r"\s+", "", fragmento)
        if compacto.lower() in PALABRAS_SENSIBLES_ESPACIADO:
            return compacto
        return fragmento

    procesado = PATRON_ESPACIADO_ARTIFICIAL.sub(colapsar_si_sensible, procesado)
    for patron, reemplazo in REEMPLAZOS_LEET:
        procesado = patron.sub(reemplazo, procesado)
    return procesado


def quitar_acentos(texto):
    normalizado = unicodedata.normalize("NFD", texto)
    return "".join(c for c in normalizado if unicodedata.category(c) != "Mn")


def detectar_enlaces(texto):
    vistos = []
    for coincidencia in PATRON_ENLACE.finditer(texto):
        candidato = coincidencia.group(0).rstrip(".,;:)]!?\"'")
        if len(candidato) <= 4:
            continue
        if re.match(r"^\d+\.\d+\.\d+\.\d+", candidato):
            host_ip = candidato.split("/", 1)[0].split(":", 1)[0]
            octetos = host_ip.split(".")
            if len(octetos) != 4 or any(not o.isdigit() or int(o) > 255 for o in octetos):
                continue
        if candidato not in vistos:
            vistos.append(candidato)
    return vistos


def extraer_dominio_y_ruta(enlace):
    limpio = enlace.lower().strip().rstrip(".,;:)]!?\"'")
    limpio = limpio.replace("hxxps://", "https://").replace("hxxp://", "http://")
    if "://" in limpio:
        sin_esquema = limpio.split("://", 1)[1]
    else:
        sin_esquema = limpio
    if "/" in sin_esquema:
        parte_host, ruta = sin_esquema.split("/", 1)
        ruta = "/" + ruta
    else:
        parte_host, ruta = sin_esquema, ""
    dominio = parte_host.split(":", 1)[0]
    if dominio.startswith("www."):
        dominio = dominio[4:]
    return dominio, ruta


def coincide_dominio_en_conjunto(dominio, conjunto):
    for base in conjunto:
        if dominio == base or dominio.endswith("." + base):
            return True
    return False


def extraer_tokens_mensaje(remitente, mensaje):
    mensaje_norm = normalizar_evasion(mensaje)
    enlaces = detectar_enlaces(mensaje_norm)

    marcadores_estructurales = []
    if len(enlaces) > 0:
        marcadores_estructurales.append("_tiene_enlace_")
        for enlace in enlaces:
            dominio, ruta = extraer_dominio_y_ruta(enlace)
            if not dominio:
                continue

            es_oficial = coincide_dominio_en_conjunto(dominio, DOMINIOS_OFICIALES)
            es_edu_gob = (
                dominio.endswith(".edu.co")
                or dominio.endswith(".gov.co")
                or dominio.endswith(".gob.es")
                or dominio.endswith(".edu")
            )

            if es_oficial or es_edu_gob:
                marcadores_estructurales.append("_enlace_verificado_")
            else:
                marcadores_estructurales.append("_enlace_externo_")

            if coincide_dominio_en_conjunto(dominio, ACORTADORES_CONOCIDOS):
                marcadores_estructurales.append("_enlace_acortado_")

            if coincide_dominio_en_conjunto(dominio, ALOJAMIENTOS_ABUSADOS):
                marcadores_estructurales.append("_enlace_alojamiento_gratuito_")

            partes_dominio = dominio.split(".")
            extension = partes_dominio[-1] if len(partes_dominio) > 1 else ""
            if extension in EXTENSIONES_RIESGO:
                marcadores_estructurales.append("_enlace_extension_riesgo_")

            if re.match(r"^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$", dominio):
                marcadores_estructurales.append("_enlace_ip_directa_")

            if ruta.endswith(".apk") or ruta.endswith("/apk"):
                marcadores_estructurales.append("_enlace_ejecutable_apk_")

            if not es_oficial and not es_edu_gob:
                if any(marca in dominio for marca in MARCAS_PROTEGIDAS_DOMINIO):
                    marcadores_estructurales.append("_enlace_suplanta_marca_")

                texto_url = dominio + ruta
                if any(palabra in texto_url for palabra in PALABRAS_RUTA_SENSIBLE):
                    marcadores_estructurales.append("_enlace_ruta_sensible_")

                if dominio.count("-") >= 2 or len(partes_dominio) >= 4:
                    marcadores_estructurales.append("_enlace_estructura_compleja_")
    else:
        marcadores_estructurales.append("_sin_enlace_")

    remitente_limpio = re.sub(r"\s+", "", remitente.strip())
    if any(remitente_limpio.startswith(pref) for pref in PREFIJOS_INTERNACIONALES_RIESGO):
        marcadores_estructurales.append("_remitente_internacional_riesgo_")
    elif re.match(r"^(?:\+?57)?3\d{9}$|^(?:\+?34)?[67]\d{8}$", remitente_limpio):
        marcadores_estructurales.append("_remitente_celular_")
    elif re.match(r"^\d{4,6}$", remitente_limpio):
        marcadores_estructurales.append("_remitente_codigo_corto_")

    texto_sin_enlaces = mensaje_norm
    for enlace in enlaces:
        texto_sin_enlaces = texto_sin_enlaces.replace(enlace, " ")

    texto_limpio = quitar_acentos(texto_sin_enlaces.lower())

    if re.search(r"\$\s*\d+|\b\d+(?:\.\d{3})+\b|\b(?:cop|eur|usd)\s*\d+|\b\d+\s*(?:cop|eur|usd|pesos)\b", texto_limpio):
        marcadores_estructurales.append("_menciona_monto_")

    if re.search(r"\b\d{1,2}\s*%\b|\bdescuento[s]?\b|\boferta[s]?\b|\bpromo(?:cion(?:es)?)?\b|\b2x1\b|\b3x2\b|\bliquidacion\b|\brebajas\b|\btienda[s]?\s+fisica[s]?\b", texto_limpio):
        marcadores_estructurales.append("_patron_promocional_")

    if re.search(r"\bno\s+(?:lo\s+|la\s+)?(?:comparta[s]?|entregue[s]?|revele[s]?)\b|\bnunca\s+(?:lo\s+|la\s+)?(?:comparta[s]?|entregue[s]?)\b|\bjamas\s+revele[s]?\b|\bvalida?\s+por\s+\d+\s+min|\bvigencia\s+de\s+\d+\s+min|\bexpira\s+en\s+\d+\s+min|\bun\s+solo\s+uso\b", texto_limpio):
        marcadores_estructurales.append("_aviso_defensivo_otp_")

    if re.search(r"\b(?<!no\s)(?<!nunca\s)(?<!jamas\s)(?:reenvie|responda|responde|envie|envianos|entregue|digite|digita|ingrese|ingresa|dictenos|escriba)\b.*\b(?:codigo|otp|clave|pin|credenciales|usuario|contrase|cvv|16\s+digitos|4\s+digitos|6\s+digitos)\b", texto_limpio):
        marcadores_estructurales.append("_solicita_credencial_u_otp_")

    if re.search(r"\b(?:llame\s+(?:ya|urgente|de\s+inmediato|antes\s+de)|comuniquese\s+(?:ya|urgente|hoy|de\s+inmediato)|escrib(?:eme|a|enos)\s+(?:urgente\s+)?(?:a\s+|por\s+|al\s+)?whatsapp|este\s+es\s+mi\s+(?:nuevo\s+numero|numero\s+nuevo)|(?:prestame|pasame|consigname|transfiere(?:me)?|consigne)\s+(?:urgente\s+|por\s+favor\s+)?(?:\$|\d+|a\s+la\s+cuenta|al\s+nequi|al\s+daviplata))\b", texto_limpio):
        marcadores_estructurales.append("_solicita_contacto_o_giro_")

    texto_alfanumerico = re.sub(r"[^a-z0-9\s]", " ", texto_limpio)
    palabras_brutas = texto_alfanumerico.split()

    palabras_filtradas = []
    for palabra in palabras_brutas:
        if palabra.isdigit():
            if 4 <= len(palabra) <= 8:
                palabras_filtradas.append("_numero_corto_")
            elif 9 <= len(palabra) <= 12:
                palabras_filtradas.append("_telefono_en_texto_")
            continue
        if len(palabra) >= 2 and palabra not in PALABRAS_VACIAS:
            palabras_filtradas.append(palabra)

    tokens = list(palabras_filtradas)
    for indice in range(len(palabras_filtradas) - 1):
        tokens.append(f"{palabras_filtradas[indice]}_{palabras_filtradas[indice + 1]}")

    tokens.extend(marcadores_estructurales)
    return tokens


def verificar_independencia_conjuntos(entrenamiento, validacion, prueba):
    conjuntos = [("entrenamiento", entrenamiento), ("validacion", validacion), ("prueba", prueba)]
    vistos_exactos = {}
    vistos_plantilla = {}
    vistos_familia = {}

    for nombre, lista in conjuntos:
        for item in lista:
            exacto = item["mensaje"].strip().lower()
            if exacto in vistos_exactos:
                raise ValueError(f"Duplicado exacto entre {vistos_exactos[exacto]} y {nombre}")
            vistos_exactos[exacto] = nombre

            sin_url = PATRON_ENLACE.sub(" <url> ", exacto)
            plantilla = " ".join(re.sub(r"[^a-z<>_\s]", " ", re.sub(r"\d+", " <num> ", quitar_acentos(sin_url))).split())
            if plantilla in vistos_plantilla:
                raise ValueError(f"Plantilla repetida entre {vistos_plantilla[plantilla]} y {nombre}")
            vistos_plantilla[plantilla] = nombre

            familia = item.get("familia_plantilla", "")
            if familia:
                if familia in vistos_familia and vistos_familia[familia] != nombre:
                    raise ValueError(f"Familia de plantilla '{familia}' compartida entre {vistos_familia[familia]} y {nombre}")
                vistos_familia[familia] = nombre

    # Revisar cuasi-duplicados estructurales contra prueba independiente
    desarrollo_norm = []
    for item in (entrenamiento + validacion):
        sin_url = PATRON_ENLACE.sub(" <url> ", item["mensaje"].lower())
        norm = " ".join(re.sub(r"[^a-z<>_\s]", " ", re.sub(r"\d+", " <num> ", quitar_acentos(sin_url))).split())
        desarrollo_norm.append((norm, set(norm.split())))

    max_sim = 0.0
    for item_p in prueba:
        sin_url_p = PATRON_ENLACE.sub(" <url> ", item_p["mensaje"].lower())
        norm_p = " ".join(re.sub(r"[^a-z<>_\s]", " ", re.sub(r"\d+", " <num> ", quitar_acentos(sin_url_p))).split())
        set_p = set(norm_p.split())
        comp = difflib.SequenceMatcher(None, norm_p, "")
        for norm_d, set_d in desarrollo_norm:
            comp.set_seq2(norm_d)
            sim = comp.ratio() if comp.quick_ratio() >= 0.68 else 0.0
            if sim > max_sim:
                max_sim = sim
            union = len(set_p | set_d)
            jaccard = (len(set_p & set_d) / union) if union > 0 else 0.0
            if sim >= 0.72 or jaccard >= 0.65:
                raise ValueError(f"Fuga estructural entre desarrollo y prueba (sim={sim:.2f}, jaccard={jaccard:.2f})")
    return round(max_sim, 4)


def construir_vocabulario(ejemplos_entrenamiento, maximo_terminos=1500, frecuencia_minima=2):
    """Calcula vocabulario e IDF exclusivamente sobre datos de entrenamiento."""
    frecuencia_documentos = Counter()
    total_documentos = len(ejemplos_entrenamiento)

    for ejemplo in ejemplos_entrenamiento:
        tokens_unicos = set(extraer_tokens_mensaje(ejemplo["remitente"], ejemplo["mensaje"]))
        for token in tokens_unicos:
            frecuencia_documentos[token] += 1

    # Orden determinista por frecuencia descendente y luego orden alfabético
    candidatos = [
        (termino, conteo)
        for termino, conteo in frecuencia_documentos.items()
        if conteo >= frecuencia_minima
    ]
    candidatos.sort(key=lambda par: (-par[1], par[0]))
    terminos_seleccionados = candidatos[:maximo_terminos]

    vocabulario = {}
    valores_idf = {}
    for indice, (termino, conteo) in enumerate(terminos_seleccionados):
        vocabulario[termino] = indice
        valores_idf[termino] = math.log((1.0 + total_documentos) / (1.0 + conteo)) + 1.0

    return vocabulario, valores_idf


def vectorizar_mensaje(remitente, mensaje, vocabulario, valores_idf):
    tokens = extraer_tokens_mensaje(remitente, mensaje)
    conteo_tokens = Counter(tokens)
    vector_disperso = {}
    suma_cuadrados = 0.0

    for termino, frecuencia in conteo_tokens.items():
        if termino in vocabulario:
            indice = vocabulario[termino]
            peso_tf = 1.0 + math.log(frecuencia)
            valor = peso_tf * valores_idf[termino]
            vector_disperso[indice] = valor
            suma_cuadrados += valor * valor

    norma = math.sqrt(suma_cuadrados)
    if norma > 0.0:
        for indice in list(vector_disperso.keys()):
            vector_disperso[indice] /= norma

    return vector_disperso


def calcular_softmax(puntajes):
    maximo = max(puntajes)
    exponenciales = [math.exp(p - maximo) for p in puntajes]
    suma = sum(exponenciales)
    return [valor / suma for valor in exponenciales]


def entrenar_regresion_logistica(
    ejemplos_entrenamiento,
    vocabulario,
    valores_idf,
    epocas_maximas=450,
    tasa_aprendizaje=0.85,
    regularizacion=0.002,
    tolerancia_gradiente=1e-4
):
    cantidad_terminos = len(vocabulario)
    cantidad_clases = len(CLASES)

    pesos = [[0.0 for _ in range(cantidad_clases)] for _ in range(cantidad_terminos)]
    interceptos = [0.0 for _ in range(cantidad_clases)]

    datos_vectorizados = []
    conteo_clases = Counter()
    for ejemplo in ejemplos_entrenamiento:
        vector = vectorizar_mensaje(ejemplo["remitente"], ejemplo["mensaje"], vocabulario, valores_idf)
        indice_clase = CLASES.index(ejemplo["clase"])
        datos_vectorizados.append((vector, indice_clase))
        conteo_clases[indice_clase] += 1

    total_ejemplos = float(len(datos_vectorizados))
    pesos_clase = {
        c: total_ejemplos / (cantidad_clases * max(1, conteo_clases[c]))
        for c in range(cantidad_clases)
    }

    perdida_inicial = None
    perdida_final = None
    epocas_ejecutadas = 0

    for epoca in range(epocas_maximas):
        epocas_ejecutadas = epoca + 1
        gradiente_pesos = [[0.0 for _ in range(cantidad_clases)] for _ in range(cantidad_terminos)]
        gradiente_interceptos = [0.0 for _ in range(cantidad_clases)]
        suma_perdida = 0.0

        for vector, clase_real in datos_vectorizados:
            puntajes = list(interceptos)
            for indice_termino, valor in vector.items():
                fila_pesos = pesos[indice_termino]
                for c in range(cantidad_clases):
                    puntajes[c] += valor * fila_pesos[c]

            probabilidades = calcular_softmax(puntajes)
            ponderacion = pesos_clase[clase_real]
            prob_real = max(1e-15, probabilidades[clase_real])
            suma_perdida -= ponderacion * math.log(prob_real)

            for c in range(cantidad_clases):
                objetivo = 1.0 if c == clase_real else 0.0
                error = (probabilidades[c] - objetivo) * ponderacion
                gradiente_interceptos[c] += error
                for indice_termino, valor in vector.items():
                    gradiente_pesos[indice_termino][c] += error * valor

        penalizacion_l2 = 0.0
        norma_grad_cuadrado = 0.0
        factor_tasa = tasa_aprendizaje / (1.0 + 0.0025 * epoca)

        for c in range(cantidad_clases):
            grad_int = gradiente_interceptos[c] / total_ejemplos
            norma_grad_cuadrado += grad_int * grad_int
            interceptos[c] -= factor_tasa * grad_int

        for indice_termino in range(cantidad_terminos):
            for c in range(cantidad_clases):
                w_actual = pesos[indice_termino][c]
                penalizacion_l2 += 0.5 * regularizacion * w_actual * w_actual
                grad_total = (gradiente_pesos[indice_termino][c] / total_ejemplos) + regularizacion * w_actual
                norma_grad_cuadrado += grad_total * grad_total
                pesos[indice_termino][c] -= factor_tasa * grad_total

        perdida_epoca = (suma_perdida / total_ejemplos) + penalizacion_l2
        if perdida_inicial is None:
            perdida_inicial = perdida_epoca
        perdida_final = perdida_epoca

        if math.sqrt(norma_grad_cuadrado) < tolerancia_gradiente:
            break

    info_entrenamiento = {
        "epocas_ejecutadas": epocas_ejecutadas,
        "perdida_inicial": round(perdida_inicial or 0.0, 5),
        "perdida_final": round(perdida_final or 0.0, 5),
        "regularizacion": regularizacion
    }
    return pesos, interceptos, info_entrenamiento


def clasificar_con_umbrales(probabilidades, umbral_smishing=0.46, umbral_spam=0.45):
    prob_leg, prob_spam, prob_smish = probabilidades[0], probabilidades[1], probabilidades[2]
    if prob_smish >= umbral_smishing and prob_smish >= prob_leg and prob_smish >= prob_spam:
        return "SMISHING"
    if prob_spam >= umbral_spam and prob_spam >= prob_leg and prob_spam > prob_smish:
        return "SPAM"
    if prob_smish > prob_leg and prob_smish > prob_spam and prob_smish >= 0.42:
        return "SMISHING"
    if prob_spam > prob_leg and prob_spam >= prob_smish:
        return "SPAM"
    return "LEGITIMO"


def predecir_mensaje(remitente, mensaje, vocabulario, valores_idf, pesos, interceptos, umbral_smishing=0.46, umbral_spam=0.45):
    vector = vectorizar_mensaje(remitente, mensaje, vocabulario, valores_idf)
    puntajes = list(interceptos)
    for indice_termino, valor in vector.items():
        for c in range(len(CLASES)):
            puntajes[c] += valor * pesos[indice_termino][c]
    probabilidades = calcular_softmax(puntajes)
    categoria = clasificar_con_umbrales(probabilidades, umbral_smishing, umbral_spam)
    return categoria, probabilidades


def seleccionar_hiperparametros_en_validacion(entrenamiento, validacion, vocabulario, valores_idf):
    """Ajusta regularización y umbrales únicamente usando el conjunto de validación."""
    mejor_puntaje = -1.0
    mejor_config = None

    for reg in (0.001, 0.002, 0.004, 0.007):
        pesos, interceptos, info = entrenar_regresion_logistica(
            entrenamiento, vocabulario, valores_idf, regularizacion=reg
        )
        probs_val = [
            predecir_mensaje(x["remitente"], x["mensaje"], vocabulario, valores_idf, pesos, interceptos)[1]
            for x in validacion
        ]
        reales_val = [x["clase"] for x in validacion]

        for u_smish in (0.42, 0.46, 0.50, 0.54):
            for u_spam in (0.42, 0.46, 0.50):
                preds_val = [clasificar_con_umbrales(p, u_smish, u_spam) for p in probs_val]
                metricas = calcular_metricas(reales_val, preds_val)
                # Penalizar falsos positivos donde un LEGITIMO termina en SMISHING
                fp_criticos = metricas["matriz_confusion"][0][2]
                valor_objetivo = (
                    0.55 * metricas["macro_f1"]
                    + 0.45 * metricas["por_clase"]["SMISHING"]["f1"]
                    - 0.03 * fp_criticos
                )
                if valor_objetivo > mejor_puntaje:
                    mejor_puntaje = valor_objetivo
                    mejor_config = {
                        "regularizacion": reg,
                        "umbral_smishing": u_smish,
                        "umbral_spam": u_spam,
                        "metricas_validacion": metricas,
                        "pesos": pesos,
                        "interceptos": interceptos,
                        "info_entrenamiento": info
                    }

    return mejor_config


def exportar_modelo(ruta_salida, vocabulario, valores_idf, pesos, interceptos, umbral_smishing, umbral_spam, corte_poda=0.01):
    os.makedirs(os.path.dirname(ruta_salida), exist_ok=True)
    terminos_ordenados = sorted(vocabulario.items(), key=lambda item: item[1])

    lineas = [
        f"CONFIG|{umbral_smishing:.4f}|{umbral_spam:.4f}",
        "INTERCEPTOS|" + "|".join(f"{valor:.6f}" for valor in interceptos)
    ]

    for termino, indice in terminos_ordenados:
        pesos_termino = pesos[indice]
        if max(abs(p) for p in pesos_termino) < corte_poda:
            continue
        idf = valores_idf[termino]
        lineas.append(
            f"TERMINO|{termino}|{idf:.6f}|"
            f"{pesos_termino[0]:.6f}|{pesos_termino[1]:.6f}|{pesos_termino[2]:.6f}"
        )

    with open(ruta_salida, "w", encoding="utf-8", newline="\n") as archivo:
        archivo.write("\n".join(lineas) + "\n")


def cargar_modelo_exportado(ruta_modelo):
    """Recarga exactamente el archivo de texto que leerá Kotlin para garantizar equivalencia numérica."""
    vocabulario = {}
    valores_idf = {}
    pesos = []
    interceptos = [0.0, 0.0, 0.0]
    umbral_smishing = 0.46
    umbral_spam = 0.46

    with open(ruta_modelo, "r", encoding="utf-8") as archivo:
        for linea in archivo:
            partes = linea.strip().split("|")
            if not partes:
                continue
            if partes[0] == "CONFIG" and len(partes) >= 3:
                umbral_smishing = float(partes[1])
                umbral_spam = float(partes[2])
            elif partes[0] == "INTERCEPTOS" and len(partes) >= 4:
                interceptos = [float(partes[1]), float(partes[2]), float(partes[3])]
            elif partes[0] == "TERMINO" and len(partes) >= 6:
                termino = partes[1]
                indice = len(vocabulario)
                vocabulario[termino] = indice
                valores_idf[termino] = float(partes[2])
                pesos.append([float(partes[3]), float(partes[4]), float(partes[5])])

    return vocabulario, valores_idf, pesos, interceptos, umbral_smishing, umbral_spam


def ocultar_codigos_sensibles(texto):
    """Oculta códigos OTP y secuencias numéricas cortas en los informes de evaluación."""
    return PATRON_MASCARA_OTP.sub("[CODIGO_OCULTO]", texto)


def calcular_metricas(reales, predichos):
    matriz = [[0 for _ in range(len(CLASES))] for _ in range(len(CLASES))]
    for real, pred in zip(reales, predichos):
        matriz[CLASES.index(real)][CLASES.index(pred)] += 1

    metricas_por_clase = {}
    suma_f1 = 0.0
    for idx, nombre_clase in enumerate(CLASES):
        vp = matriz[idx][idx]
        fp = sum(matriz[fila][idx] for fila in range(len(CLASES)) if fila != idx)
        fn = sum(matriz[idx][col] for col in range(len(CLASES)) if col != idx)

        precision = vp / (vp + fp) if (vp + fp) > 0 else 0.0
        recall = vp / (vp + fn) if (vp + fn) > 0 else 0.0
        f1 = (2.0 * precision * recall) / (precision + recall) if (precision + recall) > 0 else 0.0
        suma_f1 += f1
        metricas_por_clase[nombre_clase] = {
            "precision": round(precision, 4),
            "recall": round(recall, 4),
            "f1": round(f1, 4),
            "soporte": sum(matriz[idx])
        }

    correctas = sum(matriz[i][i] for i in range(len(CLASES)))
    total = len(reales)
    return {
        "exactitud": round(correctas / float(total), 4) if total > 0 else 0.0,
        "macro_f1": round(suma_f1 / len(CLASES), 4),
        "total_ejemplos": total,
        "predicciones_correctas": correctas,
        "predicciones_incorrectas": total - correctas,
        "por_clase": metricas_por_clase,
        "matriz_confusion": matriz
    }


def evaluar_subconjuntos_criticos(datos_prueba, predicciones):
    legitimos_con_enlace = []
    smishing_sin_enlace = []
    remitentes_celular = []

    for item, pred in zip(datos_prueba, predicciones):
        tiene_url = len(detectar_enlaces(normalizar_evasion(item["mensaje"]))) > 0
        if item["clase"] == "LEGITIMO" and tiene_url:
            legitimos_con_enlace.append(pred == "LEGITIMO")
        if item["clase"] == "SMISHING" and not tiene_url:
            smishing_sin_enlace.append(pred == "SMISHING")
        if item["remitente"].startswith("+"):
            remitentes_celular.append(pred == item["clase"])

    def tasa(aciertos):
        return f"{sum(aciertos)}/{len(aciertos)} ({(100.0 * sum(aciertos) / max(1, len(aciertos))):.1f}%)"

    return {
        "legitimos_con_enlace_acertados": tasa(legitimos_con_enlace),
        "smishing_sin_enlace_detectados": tasa(smishing_sin_enlace),
        "remitentes_numericos_acertados": tasa(remitentes_celular)
    }


def ejecutar_entrenamiento():
    with open(RUTA_DATOS_ENTRENAMIENTO, "r", encoding="utf-8") as f:
        datos_entrenamiento = json.load(f)
    with open(RUTA_DATOS_VALIDACION, "r", encoding="utf-8") as f:
        datos_validacion = json.load(f)
    with open(RUTA_DATOS_PRUEBA, "r", encoding="utf-8") as f:
        datos_prueba = json.load(f)

    max_similitud = verificar_independencia_conjuntos(datos_entrenamiento, datos_validacion, datos_prueba)

    # 1. Vocabulario e IDF calculados exclusivamente sobre entrenamiento
    vocabulario_base, idf_base = construir_vocabulario(datos_entrenamiento)

    # 2. Ajuste de regularización y umbrales sobre validación (nunca sobre prueba)
    mejor = seleccionar_hiperparametros_en_validacion(
        datos_entrenamiento, datos_validacion, vocabulario_base, idf_base
    )

    exportar_modelo(
        RUTA_MODELO_SALIDA,
        vocabulario_base,
        idf_base,
        mejor["pesos"],
        mejor["interceptos"],
        mejor["umbral_smishing"],
        mejor["umbral_spam"]
    )

    # 3. Recargar el modelo exportado exacto para evaluar en igualdad de condiciones con Android
    vocab_exp, idf_exp, pesos_exp, inter_exp, u_smish, u_spam = cargar_modelo_exportado(RUTA_MODELO_SALIDA)

    inicio_inferencia = time.perf_counter()
    reales_prueba = []
    predichos_prueba = []
    falsos_positivos_smishing = []
    falsos_negativos_smishing = []
    otras_confusiones = []

    for item in datos_prueba:
        clase_pred, probs = predecir_mensaje(
            item["remitente"], item["mensaje"], vocab_exp, idf_exp, pesos_exp, inter_exp, u_smish, u_spam
        )
        reales_prueba.append(item["clase"])
        predichos_prueba.append(clase_pred)

        if clase_pred != item["clase"]:
            registro_error = {
                "remitente": item["remitente"],
                "mensaje_enmascarado": ocultar_codigos_sensibles(item["mensaje"]),
                "clase_real": item["clase"],
                "clase_predicha": clase_pred,
                "probabilidades": {
                    "LEGITIMO": round(probs[0], 4),
                    "SPAM": round(probs[1], 4),
                    "SMISHING": round(probs[2], 4)
                }
            }
            if clase_pred == "SMISHING" and item["clase"] != "SMISHING":
                falsos_positivos_smishing.append(registro_error)
            elif item["clase"] == "SMISHING" and clase_pred != "SMISHING":
                falsos_negativos_smishing.append(registro_error)
            else:
                otras_confusiones.append(registro_error)

    tiempo_total_ms = (time.perf_counter() - inicio_inferencia) * 1000.0
    tiempo_promedio_ms = tiempo_total_ms / max(1, len(datos_prueba))

    metricas_prueba = calcular_metricas(reales_prueba, predichos_prueba)
    subconjuntos = evaluar_subconjuntos_criticos(datos_prueba, predichos_prueba)
    tamano_bytes = os.path.getsize(RUTA_MODELO_SALIDA)

    # 4. Exportar casos de consistencia para prueba automatizada Python <-> Kotlin
    os.makedirs(os.path.dirname(RUTA_CONSISTENCIA), exist_ok=True)
    muestra_consistencia = []
    indices_muestra = [0, 8, 13, 16, 20, 26, 35, 37, 45, 47, 70, 72, 80, 86, 88, 92, 96, 97, 99, 104]
    for idx in indices_muestra:
        if idx < len(datos_prueba):
            caso = datos_prueba[idx]
            cat_c, probs_c = predecir_mensaje(
                caso["remitente"], caso["mensaje"], vocab_exp, idf_exp, pesos_exp, inter_exp, u_smish, u_spam
            )
            muestra_consistencia.append({
                "remitente": caso["remitente"],
                "mensaje": caso["mensaje"],
                "categoria_python": cat_c,
                "prob_legitimo": round(probs_c[0], 6),
                "prob_spam": round(probs_c[1], 6),
                "prob_smishing": round(probs_c[2], 6)
            })
    with open(RUTA_CONSISTENCIA, "w", encoding="utf-8", newline="\n") as f_cons:
        json.dump(muestra_consistencia, f_cons, indent=2, ensure_ascii=False)

    reporte = {
        "nota_metodologica": "Corpus sintético controlado con separación estricta por familias de plantillas. No sustituye calibración probabilística sobre tráfico real de operadoras.",
        "distribucion_datos": {
            "entrenamiento": dict(Counter(x["clase"] for x in datos_entrenamiento)),
            "validacion": dict(Counter(x["clase"] for x in datos_validacion)),
            "prueba_independiente": dict(Counter(x["clase"] for x in datos_prueba)),
            "maxima_similitud_estructural_desarrollo_vs_prueba": max_similitud
        },
        "modelo": {
            "terminos_vocabulario_entrenamiento": len(vocabulario_base),
            "terminos_exportados": len(vocab_exp),
            "tamano_archivo_bytes": tamano_bytes,
            "tamano_archivo_kb": round(tamano_bytes / 1024.0, 2),
            "regularizacion_seleccionada": mejor["regularizacion"],
            "umbral_smishing_validacion": u_smish,
            "umbral_spam_validacion": u_spam,
            "convergencia": mejor["info_entrenamiento"],
            "tiempo_promedio_inferencia_python_ms": round(tiempo_promedio_ms, 4)
        },
        "evaluacion_validacion": mejor["metricas_validacion"],
        "evaluacion_prueba_independiente": metricas_prueba,
        "pruebas_especificas_subconjuntos": subconjuntos,
        "analisis_errores_prueba": {
            "falsos_positivos_smishing": falsos_positivos_smishing,
            "falsos_negativos_smishing": falsos_negativos_smishing,
            "confusiones_legitimo_spam": otras_confusiones
        }
    }

    with open(RUTA_REPORTE_EVALUACION, "w", encoding="utf-8", newline="\n") as f_rep:
        json.dump(reporte, f_rep, indent=2, ensure_ascii=False)

    print("=== ENTRENAMIENTO Y EVALUACION COMPLETADOS ===")
    print(f"Modelo exportado: {RUTA_MODELO_SALIDA} ({tamano_bytes / 1024.0:.2f} KB, {len(vocab_exp)} términos)")
    print(f"Tiempo promedio por SMS (Python): {tiempo_promedio_ms:.4f} ms")
    print(f"Hiperparámetros (validación): reg={mejor['regularizacion']}, umbral_smishing={u_smish}, umbral_spam={u_spam}")
    print("\n--- Métricas en Conjunto de Prueba Independiente (105 mensajes, 0 plantillas compartidas) ---")
    print(f"Exactitud: {metricas_prueba['exactitud']:.4f} | Macro-F1: {metricas_prueba['macro_f1']:.4f}")
    print(f"Correctas: {metricas_prueba['predicciones_correctas']} | Incorrectas: {metricas_prueba['predicciones_incorrectas']}")
    print("Orden de clases en matriz de confusión:", CLASES)
    for fila_nombre, fila_vals in zip(CLASES, metricas_prueba["matriz_confusion"]):
        print(f"  Real {fila_nombre:10s} -> {fila_vals}")
    for clase, vals in metricas_prueba["por_clase"].items():
        print(f"  {clase:10s}: Precision={vals['precision']:.4f}, Recall={vals['recall']:.4f}, F1={vals['f1']:.4f} (n={vals['soporte']})")
    print("\n--- Subconjuntos Críticos ---")
    for k, v in subconjuntos.items():
        print(f"  {k}: {v}")
    print(f"\nFalsos positivos hacia SMISHING: {len(falsos_positivos_smishing)}")
    for fp in falsos_positivos_smishing:
        print(f"  [FP] ({fp['clase_real']} -> SMISHING) {fp['mensaje_enmascarado']}")
    print(f"Falsos negativos de SMISHING: {len(falsos_negativos_smishing)}")
    for fn in falsos_negativos_smishing:
        print(f"  [FN] (SMISHING -> {fn['clase_predicha']}) {fn['mensaje_enmascarado']}")


if __name__ == "__main__":
    ejecutar_entrenamiento()
