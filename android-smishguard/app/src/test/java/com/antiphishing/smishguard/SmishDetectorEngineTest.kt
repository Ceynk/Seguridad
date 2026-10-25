package com.antiphishing.smishguard

import com.antiphishing.smishguard.domain.detector.HomographDetector
import com.antiphishing.smishguard.domain.detector.SmishDetectorEngine
import com.antiphishing.smishguard.domain.model.ThreatLevel
import org.junit.Assert.*
import org.junit.Test

class SmishDetectorEngineTest {

    @Test
    fun palabrasGenericasPorSiSolasNoPonenEnCuarentena() {
        val result = SmishDetectorEngine.analyze(
            sender = "Contacto",
            body = "Mensaje urgente: recuerda verificar tu cuenta y revisar el pago."
        )

        assertTrue(result.threatScore in 0..49)
        assertFalse(result.isQuarantined)
    }

    @Test
    fun mensajeBancarioConUrlMaliciosaYSolicitudDeCredencialesSeAisla() {
        val result = SmishDetectorEngine.analyze(
            sender = "BBVA",
            body = "Su cuenta fue bloqueada. Ingrese sus credenciales en https://bbva-alerta.duckdns.org/login para reactivarla."
        )

        assertTrue(result.isQuarantined)
        assertTrue(result.threatScore >= 60)
    }

    @Test
    fun mensajeDePaqueteriaLegitimoNoSeAisla() {
        val result = SmishDetectorEngine.analyze(
            sender = "Correos",
            body = "Su paquete fue entregado hoy. Consulte el seguimiento en https://correos.es"
        )

        assertFalse(result.isQuarantined)
        assertTrue(result.threatScore < 50)
    }

    @Test
    fun mensajeDePaqueteriaFraudulentoSeAisla() {
        val result = SmishDetectorEngine.analyze(
            sender = "Correos",
            body = "Paquete retenido: pague la tasa de aduana en https://correos-pago.xyz/entrega"
        )

        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Paquetería") })
    }

    @Test
    fun pruebaDeteccionPhishingBancarioConUrlMaliciosa() {
        val sender = "BBVA"
        val body = "BBVA: Su cuenta ha sido bloqueada temporalmente. Ingrese a https://bbva-seguridad.duckdns.org/login para evitar la cancelacion de su tarjeta."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Bancario") })
        assertTrue(result.factors.any { it.category.contains("Alojamiento") || it.category.contains("Marca") })
    }

    @Test
    fun pruebaDeteccionEstafaPaqueteriaConTldDeRiesgo() {
        val sender = "+34600123456"
        val body = "CORREOS: Su paquete no pudo ser entregado por falta de pago de tasa de aduana (2.64 EUR). Realice el pago urgente en: https://entrega-correos.xyz/pago"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.factors.any { it.category.contains("Paquetería") })
        assertTrue(result.factors.any { it.category.contains("Extensión de Dominio Sospechosa") })
    }

    @Test
    fun pruebaDeteccionAtaqueHomografoConCaracteresCirlicos() {
        val rawText = "Inicie sesion en https://раураl.top/auth para validar su cuenta"
        val homoglyphs = HomographDetector.findHomoglyphs(rawText)

        assertTrue(homoglyphs.isNotEmpty())

        val result = SmishDetectorEngine.analyze(sender = "Soporte", body = rawText)
        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.factors.any { it.category.contains("Homógrafo") || it.category.contains("Homografo") })
    }

    @Test
    fun pruebaDeteccionFalsaMultaDgtConIpDirecta() {
        val sender = "DGT"
        val body = "DGT: Sancion administrativa por exceso de velocidad. Ultimo aviso de pago antes de recargo: http://192.168.1.50/dgt"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.factors.any { it.category.contains("Organismo Público") })
        assertTrue(result.factors.any { it.category.contains("Dirección IP Directa") })
    }

    @Test
    fun pruebaDeteccionEstafaSuplantacionFamiliar() {
        val sender = "+34699887766"
        val body = "Hola mama, se me ha roto el movil y este es mi nuevo numero. Escribeme a whatsapp urgente que tengo una emergencia"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertTrue(result.level == ThreatLevel.PHISHING || result.level == ThreatLevel.SUSPICIOUS)
        assertTrue(result.factors.any { it.category.contains("Familiar") })
    }

    @Test
    fun pruebaSmsLegitimoGoogleOtpEsSeguro() {
        val sender = "Google"
        val body = "G-918234 es tu codigo de verificacion de Google."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertFalse(result.isQuarantined)
    }

    @Test
    fun pruebaSmsLegitimoBancolombiaClaveDinamicaSinFalsoPositivo() {
        val sender = "Bancolombia"
        val body = "Bancolombia le informa que su clave dinamica es 849201. Valida por 5 minutos. No la comparta con nadie."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertEquals(0, result.threatScore)
        assertFalse(result.isQuarantined)
        assertTrue(result.factors.isEmpty())
    }

    @Test
    fun pruebaSmsLegitimoNequiOtpSinFalsoPositivo() {
        val sender = "Nequi"
        val body = "Nequi: Tu codigo de verificacion es 391-482 para ingresar a tu cuenta. No compartas este codigo con nadie."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertEquals(0, result.threatScore)
        assertFalse(result.isQuarantined)
        assertTrue(result.factors.isEmpty())
    }

    @Test
    fun pruebaSmsLegitimoDaviviendaConSubdominioOficialSinFalsoPositivo() {
        val sender = "Davivienda"
        val body = "Davivienda: Realice sus consultas de seguridad en nuestro portal oficial https://notificaciones.davivienda.com"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertFalse(result.isQuarantined)
        assertFalse(result.factors.any { it.category.contains("Suplantación de Marca") })
    }

    @Test
    fun pruebaSmsLegitimoBancolombiaConSubdominioOficialSinFalsoPositivo() {
        val sender = "Bancolombia"
        val body = "Bancolombia: Ingrese a la sucursal virtual https://sucursalpersonas.transaccionesbancolombia.com para consultar su saldo."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertFalse(result.isQuarantined)
        assertFalse(result.factors.any { it.category.contains("Suplantación de Marca") })
    }

    @Test
    fun pruebaDeteccionUrlMaliciosaSinProtocoloYSinBarraFinal() {
        val sender = "Alerta"
        val body = "Su cuenta ha sido bloqueada preventivamente. Ingrese a bancolombia-reactivar.xyz para recuperarla urgente."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
        assertTrue(result.extractedUrls.isNotEmpty())
        assertTrue(result.factors.any { it.category.contains("Extensión de Dominio Sospechosa") || it.category.contains("Marca") })
    }

    @Test
    fun pruebaDeteccionLeetspeakEnPalabrasSensibles() {
        val sender = "Soporte"
        val body = "Su cuenta b4nc0l0mb14 ha sido bl0qu34d4. Ingrese a https://alerta-cuenta.xyz"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
    }

    @Test
    fun pruebaDeteccionEspaciadoArtificialEvasivo() {
        val sender = "Soporte"
        val body = "b a n c o l o m b i a : c u e n t a   b l o q u e a d a . Visite https://portal-falso.top"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
    }

    @Test
    fun pruebaDeteccionFalsoPrestamoGotaAGota() {
        val sender = "+573009988776"
        val body = "Credito pre-aprobado por $3.000.000 sin fiador ni buro. Descargue el apk inmediato en: https://prestamo-ya.top/app.apk"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Préstamos") || it.category.contains("Prestamo") })
    }

    @Test
    fun pruebaDeteccionSuplantacionOperadorMovilImei() {
        val sender = "Claro"
        val body = "Claro: Su linea sera suspendida por falta de registro de IMEI. Evite el bloqueo y registre su equipo en: https://claro-imei.xyz"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Operador Móvil") || it.category.contains("Extensión") })
    }

    @Test
    fun pruebaCelularPrivadoEnviandoAlertaBancariaEsSospechoso() {
        val sender = "+573108889900"
        val body = "Bancolombia: Notificacion de seguridad. Ingrese a https://bancolombia-login.top para verificar su estado."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.factors.any { it.category.contains("Celular Privada") })
    }

    @Test
    fun pruebaRemitenteEnListaBlancaRetornaSeguro() {
        val sender = "+34611223344"
        val body = "Hola! Te envio este link con descuento: http://oferta-loca.xyz/promo"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body, isWhitelisted = true)

        assertEquals(ThreatLevel.SAFE, result.level)
        assertEquals(0, result.threatScore)
        assertFalse(result.isQuarantined)
    }

    @Test
    fun pruebaReglaPersonalizadaActivaCuarentena() {
        val sender = "Promo"
        val body = "Gana dinero facil desde casa sin esfuerzo"
        val customRule = listOf(Regex("(?i)gana dinero facil"))

        val result = SmishDetectorEngine.analyze(
            sender = sender,
            body = body,
            customBlockPatterns = customRule
        )

        assertTrue(result.factors.any { it.category.contains("Personalizada") })
        assertTrue(result.threatScore >= 40)
    }

    @Test
    fun pruebaDeteccionPhishingNequiConAlojamientoAbusado() {
        val sender = "Nequi"
        val body = "Nequi: Su cuenta ha sido bloqueada preventivamente por movimientos inusuales. Reactívela aquí: https://nequi-desbloqueo.pages.dev/auth"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Nequi") || it.category.contains("Billetera") })
        assertTrue(result.factors.any { it.category.contains("Alojamiento") || it.category.contains("Marca") })
    }

    @Test
    fun pruebaDeteccionFraudeDianConEmbargo() {
        val sender = "DIAN-Alerta"
        val body = "DIAN: Mandamiento de pago. Su cuenta bancaria presenta embargo por mora tributaria. Descargue la resolucion en: https://dian-tramites.xyz/pago"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.factors.any { it.category.contains("Fiscal") || it.category.contains("DIAN") })
        assertTrue(result.factors.any { it.category.contains("Extensión de Dominio Sospechosa") })
    }

    @Test
    fun pruebaDeteccionFalsoDebitoPseConIpDirecta() {
        val sender = "PSE"
        val body = "PSE: Transaccion aprobada por $450.000 COP. Si no reconoce este debito cancele de inmediato en: http://192.168.1.80/pse"

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.threatScore >= 60)
        assertTrue(result.factors.any { it.category.contains("PSE") || it.category.contains("Débito") })
        assertTrue(result.factors.any { it.category.contains("Dirección IP Directa") })
    }

    @Test
    fun pruebaDeteccionRoboTokenOtpDinamico() {
        val sender = "+573009876543"
        val body = "Banco: Alerta de seguridad. Para anular transferencia reenvié de inmediato el código dinámico de 6 dígitos que acaba de recibir."

        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        assertTrue(result.level == ThreatLevel.PHISHING || result.level == ThreatLevel.SUSPICIOUS)
        assertTrue(result.factors.any { it.category.contains("Token") || it.category.contains("OTP") })
    }

    @Test
    fun pruebaDeteccionEvasionConCaracteresInvisiblesAnchoCero() {
        val body = "N\u200Be\u200Bq\u200Bu\u200Bi: Su cuenta ha sido b\u200Bl\u200Bo\u200Bq\u200Bu\u200Be\u200Ba\u200Bd\u200Ba. Entre en: https://nequi-login.top/auth"

        val result = SmishDetectorEngine.analyze(sender = "Soporte", body = body)

        assertTrue(result.factors.any { it.category.contains("Homógrafo") || it.category.contains("Homografo") })
        assertEquals(ThreatLevel.PHISHING, result.level)
    }

    @Test
    fun pruebaDefangCompletoDeUrlsIpsYDominios() {
        val rawText = "Visite http://192.168.1.100 y https://banco-fraude.xyz/login para verificar."
        val extracted = listOf("http://192.168.1.100", "https://banco-fraude.xyz/login")

        val defanged = SmishDetectorEngine.defangText(rawText, extracted)

        assertFalse(defanged.contains("http://"))
        assertFalse(defanged.contains("https://"))
        assertTrue(defanged.contains("hxxp://") || defanged.contains("hxxps://"))
        assertTrue(defanged.contains("192[.]168[.]1[.]100"))
        assertTrue(defanged.contains("banco-fraude[.]xyz/login"))
    }

    @Test
    fun pruebaSinEmojisEnResultadosDelMotor() {
        val sender = "Nequi"
        val body = "Nequi: Cuenta bloqueada. Ingrese a https://nequi-desbloqueo.pages.dev"
        val result = SmishDetectorEngine.analyze(sender = sender, body = body)

        val emojiPattern = Regex("""[\uD83C-\uDBFF][\uDC00-\uDFFF]|[\u2600-\u27BF]""")
        assertFalse("summaryReason tiene emoji: ${result.summaryReason}", emojiPattern.containsMatchIn(result.summaryReason))
        for (factor in result.factors) {
            assertFalse("category tiene emoji: ${factor.category}", emojiPattern.containsMatchIn(factor.category))
            assertFalse("description tiene emoji: '${factor.description}'", emojiPattern.containsMatchIn(factor.description))
        }
    }

    @Test
    fun pruebaNoFalsoPositivoMarcasCortasIngYPse() {
        val legitimateUrls = listOf(
            "https://booking.com/hotel/123",
            "https://hosting.com/pricing",
            "https://billing.example.com",
            "https://shopping.test.org",
            "https://eclipse.org/download",
            "https://synapse.org/info"
        )

        for (url in legitimateUrls) {
            val factors = com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer.analyzeUrls(listOf(url))
            val brandFactors = factors.filter { it.category.contains("Suplantación de Marca") }
            assertTrue("URL $url no debería activar factor de suplantación de marca: $brandFactors", brandFactors.isEmpty())
        }
    }

    @Test
    fun pruebaDeteccionSuplantacionMarcaCortaCuandoEstaDelimitada() {
        val maliciousUrls = listOf(
            "https://ing-direct-alerta.xyz/login",
            "https://banco-ing.xyz/auth",
            "https://pse-pagos-enlinea.top/verificar"
        )

        for (url in maliciousUrls) {
            val factors = com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer.analyzeUrls(listOf(url))
            assertTrue(
                "URL $url debería activar factor de suplantación de marca",
                factors.any { it.category.contains("Suplantación de Marca") }
            )
        }
    }

    @Test
    fun pruebaDeteccionMultipleEspaciadoArtificial() {
        val text = "Estimado cliente, b a n c o l o m b i a : c u e n t a  b l o q u e a d a . Visite https://alerta.xyz"
        val homoglyphs = HomographDetector.findHomoglyphs(text)

        assertTrue(
            "Debe detectar espaciado artificial en texto con múltiples palabras",
            homoglyphs.any { it.contains("Espaciado artificial") }
        )
    }

    @Test
    fun pruebaAtaqueConOtpFalsoYIngenieriaSocialNoSeNeutraliza() {
        val body = "Su cuenta Nequi ha sido bloqueada. Ingrese a https://nequi-desbloqueo.xyz para recuperar acceso. Codigo de seguridad 9182. Valido por 5 minutos, no lo comparta con nadie."
        val result = SmishDetectorEngine.analyze(sender = "Nequi", body = body)

        assertEquals(ThreatLevel.PHISHING, result.level)
        assertTrue(result.isQuarantined)
        assertTrue(result.threatScore >= 60)
    }

    @Test
    fun pruebaReglaPersonalizadaAislaMensajePorSiSola() {
        val customRule = listOf(Regex("""\bpremio\s+exclusivo\b""", RegexOption.IGNORE_CASE))
        val body = "Felicidades, ha ganado un premio exclusivo por su fidelidad. Reclame hoy."

        val result = SmishDetectorEngine.analyze(
            sender = "+573001112233",
            body = body,
            customBlockPatterns = customRule,
            quarantineThreshold = 50
        )

        assertTrue(result.isQuarantined)
        assertTrue(result.factors.any { it.category.contains("Personalizada") })
    }

    @Test
    fun pruebaDefangTextNeutralizaIpsYProtocolosCompletamente() {
        val raw = "Visite http://192.168.1.10/login y www.malware.com/payload"
        val urls = listOf("http://192.168.1.10/login", "www.malware.com/payload")
        val defanged = SmishDetectorEngine.defangText(raw, urls)

        assertFalse("No debe contener http://", defanged.contains("http://"))
        assertFalse("No debe contener www.", defanged.contains("www."))
        assertTrue("Debe contener hxxp://", defanged.contains("hxxp://"))
        assertTrue("Debe neutralizar la IP con corchetes", defanged.contains("192[.]168[.]1[.]10"))
    }
}
