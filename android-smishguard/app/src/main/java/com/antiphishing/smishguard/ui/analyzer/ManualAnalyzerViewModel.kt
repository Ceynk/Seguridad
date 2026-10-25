package com.antiphishing.smishguard.ui.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiphishing.smishguard.data.repository.SecuritySettingsRepository
import com.antiphishing.smishguard.data.repository.SmsRepository
import com.antiphishing.smishguard.domain.detector.SmishDetectorEngine
import com.antiphishing.smishguard.domain.detector.UrlDecision
import com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer
import com.antiphishing.smishguard.domain.model.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PresetSmishTemplate(
    val title: String,
    val sender: String,
    val text: String
)

class ManualAnalyzerViewModel(
    private val smsRepository: SmsRepository,
    private val settingsRepository: SecuritySettingsRepository? = null
) : ViewModel() {

    private val _senderInput = MutableStateFlow("")
    val senderInput: StateFlow<String> = _senderInput.asStateFlow()

    private val _bodyInput = MutableStateFlow("")
    val bodyInput: StateFlow<String> = _bodyInput.asStateFlow()

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult: StateFlow<AnalysisResult?> = _analysisResult.asStateFlow()

    private val _blockedDecision = MutableStateFlow<UrlDecision?>(null)
    val blockedDecision: StateFlow<UrlDecision?> = _blockedDecision.asStateFlow()

    private val _safeUrlToOpen = MutableStateFlow<String?>(null)
    val safeUrlToOpen: StateFlow<String?> = _safeUrlToOpen.asStateFlow()

    val presets = listOf(
        PresetSmishTemplate(
            title = "Billetera Digital (Nequi)",
            sender = "Nequi",
            text = "Nequi: Su cuenta ha sido bloqueada preventivamente por movimientos sospechosos. Ingrese a https://nequi-desbloqueo.pages.dev/login para restablecer su acceso antes de 24 horas."
        ),
        PresetSmishTemplate(
            title = "Fraude Fiscal (DIAN)",
            sender = "DIAN-Alerta",
            text = "DIAN: Notificación coactiva. Su cuenta bancaria presenta orden de embargo por mora. Descargue la resolución y evite sanciones en: https://dian-tramites.xyz/mandamiento"
        ),
        PresetSmishTemplate(
            title = "Falso Débito (PSE)",
            sender = "PSE-Notifica",
            text = "PSE: Transacción aprobada por $450.000 COP en MercadoLibre. Si no reconoce este débito, solicite la anulación inmediata en: http://192.168.1.85/pse-anulacion"
        ),
        PresetSmishTemplate(
            title = "Subsidio (Daviplata)",
            sender = "Daviplata",
            text = "Daviplata: Beneficiario seleccionado para desembolso de subsidio por $320.000 COP. Registre sus datos para cobro: https://daviplata-subsidios.top/retiro"
        ),
        PresetSmishTemplate(
            title = "Fraude Bancario (Bancolombia)",
            sender = "Bancolombia",
            text = "Bancolombia: Alerta de seguridad. Clave dinámica requerida para autorizar transferencia por $1.200.000 COP. Si no fue usted cancele en: https://bancolombia-sucursal.duckdns.org"
        ),
        PresetSmishTemplate(
            title = "Ataque Homógrafo (Punycode)",
            sender = "Soporte-Seguro",
            text = "Alerta de seguridad: Inicie sesión para verificar su tarjeta en https://раураl.top/auth antes de 24 horas."
        ),
        PresetSmishTemplate(
            title = "Estafa Paquetería (Envío)",
            sender = "+573001234567",
            text = "Su envío no pudo ser entregado debido a dirección incompleta. Actualice los datos de entrega en: https://seguimiento-entrega.xyz/track"
        ),
        PresetSmishTemplate(
            title = "Suplantación Familiar",
            sender = "+573109998877",
            text = "Hola mamá, se me ha roto el teléfono y este es mi nuevo número. Escríbeme a WhatsApp urgente que tengo una emergencia."
        ),
        PresetSmishTemplate(
            title = "SMS Legítimo (Google 2FA)",
            sender = "Google",
            text = "G-849201 es tu código de verificación de Google. No lo compartas con nadie."
        )
    )

    fun setSharedInput(sender: String, body: String) {
        _senderInput.value = sender
        _bodyInput.value = body
        analyze()
    }

    fun onSenderChange(sender: String) {
        _senderInput.value = sender
    }

    fun onBodyChange(body: String) {
        _bodyInput.value = body
    }

    fun loadPreset(preset: PresetSmishTemplate) {
        _senderInput.value = preset.sender
        _bodyInput.value = preset.text
        analyze()
    }

    fun analyze() {
        val sender = _senderInput.value
        val body = _bodyInput.value
        if (body.isBlank()) {
            return
        }

        viewModelScope.launch {
            val isWhitelisted = smsRepository.isSenderWhitelisted(sender)
            val customPatterns = smsRepository.getActiveCustomRegexes()
            val communityEntries = smsRepository.getCommunityEntriesByIndicator()
            val threshold = settingsRepository?.quarantineThreshold ?: SmishDetectorEngine.DEFAULT_QUARANTINE_THRESHOLD
            val aggressive = settingsRepository?.isAggressiveModeEnabled ?: false
            val result = withContext(Dispatchers.Default) {
                SmishDetectorEngine.analyze(
                    sender = sender,
                    body = body,
                    isWhitelisted = isWhitelisted,
                    customBlockPatterns = customPatterns,
                    quarantineThreshold = threshold,
                    aggressiveMode = aggressive,
                    communityEntries = communityEntries
                )
            }
            _analysisResult.value = result
        }
    }

    fun attemptOpenUrl(rawUrl: String) {
        val decision = UrlThreatAnalyzer.analyzeSingleUrl(rawUrl)
        if (decision.isBlocked) {
            _blockedDecision.value = decision
        } else {
            _safeUrlToOpen.value = rawUrl
        }
    }

    fun showBlockedDecision(decision: UrlDecision) {
        _blockedDecision.value = decision
    }

    fun dismissBlockedDecision() {
        _blockedDecision.value = null
    }

    fun consumeSafeUrl() {
        _safeUrlToOpen.value = null
    }

    fun clear() {
        _senderInput.value = ""
        _bodyInput.value = ""
        _analysisResult.value = null
        _blockedDecision.value = null
        _safeUrlToOpen.value = null
    }
}
