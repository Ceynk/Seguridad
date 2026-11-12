package com.antiphishing.smishguard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiphishing.smishguard.data.local.entity.CustomRuleEntity
import com.antiphishing.smishguard.data.local.entity.WhitelistEntity
import com.antiphishing.smishguard.data.local.entity.UserReportEntity
import com.antiphishing.smishguard.data.repository.CallRepository
import com.antiphishing.smishguard.data.repository.CommunityApiClient
import com.antiphishing.smishguard.data.repository.CommunityIndicatorNormalizer
import com.antiphishing.smishguard.data.repository.SecuritySettingsRepository
import com.antiphishing.smishguard.data.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class SettingsViewModel(
    private val settingsRepository: SecuritySettingsRepository,
    private val smsRepository: SmsRepository,
    private val callRepository: CallRepository? = null,
    private val communityApiClient: CommunityApiClient? = null
) : ViewModel() {

    private val _quarantineThreshold = MutableStateFlow(settingsRepository.quarantineThreshold)
    val quarantineThreshold: StateFlow<Int> = _quarantineThreshold.asStateFlow()

    private val _aggressiveMode = MutableStateFlow(settingsRepository.isAggressiveModeEnabled)
    val aggressiveMode: StateFlow<Boolean> = _aggressiveMode.asStateFlow()

    private val _silentDismissNotif = MutableStateFlow(settingsRepository.isSilentDismissNotifEnabled)
    val silentDismissNotif: StateFlow<Boolean> = _silentDismissNotif.asStateFlow()

    private val _blockPrivateCalls = MutableStateFlow(settingsRepository.blockPrivateCalls)
    val blockPrivateCalls: StateFlow<Boolean> = _blockPrivateCalls.asStateFlow()

    private val _blockInvalidCalls = MutableStateFlow(settingsRepository.blockInvalidCalls)
    val blockInvalidCalls: StateFlow<Boolean> = _blockInvalidCalls.asStateFlow()

    private val _internationalCallPolicy = MutableStateFlow(settingsRepository.internationalCallPolicy)
    val internationalCallPolicy: StateFlow<SecuritySettingsRepository.InternationalCallPolicy> =
        _internationalCallPolicy.asStateFlow()

    private val _communitySyncEnabled = MutableStateFlow(settingsRepository.isCommunitySyncEnabled)
    val communitySyncEnabled: StateFlow<Boolean> = _communitySyncEnabled.asStateFlow()

    val whitelist: StateFlow<List<WhitelistEntity>> = smsRepository.getAllWhitelist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customRules: StateFlow<List<CustomRuleEntity>> = smsRepository.getAllCustomRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communityEntries = smsRepository.getCommunityEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userReports: StateFlow<List<UserReportEntity>> = smsRepository.getUserReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuarantineThreshold(value: Int) {
        settingsRepository.quarantineThreshold = value
        _quarantineThreshold.value = value
    }

    fun setAggressiveMode(enabled: Boolean) {
        settingsRepository.isAggressiveModeEnabled = enabled
        _aggressiveMode.value = enabled
    }

    fun setSilentDismissNotif(enabled: Boolean) {
        settingsRepository.isSilentDismissNotifEnabled = enabled
        _silentDismissNotif.value = enabled
    }

    fun setBlockPrivateCalls(enabled: Boolean) {
        settingsRepository.blockPrivateCalls = enabled
        _blockPrivateCalls.value = enabled
    }

    fun setBlockInvalidCalls(enabled: Boolean) {
        settingsRepository.blockInvalidCalls = enabled
        _blockInvalidCalls.value = enabled
    }

    fun setInternationalCallPolicy(policy: SecuritySettingsRepository.InternationalCallPolicy) {
        settingsRepository.internationalCallPolicy = policy
        _internationalCallPolicy.value = policy
    }

    fun setCommunitySyncEnabled(enabled: Boolean) {
        settingsRepository.isCommunitySyncEnabled = enabled
        _communitySyncEnabled.value = enabled
    }

    fun addWhitelistEntry(sender: String, name: String?) {
        viewModelScope.launch {
            smsRepository.addToWhitelist(sender, name)
        }
    }

    fun removeWhitelistEntry(sender: String) {
        viewModelScope.launch {
            smsRepository.removeFromWhitelist(sender)
        }
    }

    fun addCustomRule(
        pattern: String,
        isRegex: Boolean,
        description: String,
        onResult: ((String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val error = smsRepository.addCustomRule(pattern, isRegex, description)
            if (onResult != null) {
                onResult(error)
            }
        }
    }

    fun toggleCustomRule(rule: CustomRuleEntity) {
        viewModelScope.launch {
            val updated = rule.copy(isEnabled = !rule.isEnabled)
            smsRepository.updateCustomRule(updated)
        }
    }

    fun deleteCustomRule(rule: CustomRuleEntity) {
        viewModelScope.launch {
            smsRepository.deleteCustomRule(rule)
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            smsRepository.clearAllRecords()
            val callRepo = callRepository
            if (callRepo != null) {
                callRepo.clearAllCallRecords()
            }
        }
    }

    fun clearQuarantined() {
        viewModelScope.launch {
            smsRepository.clearAllQuarantined()
        }
    }

    fun deleteUserReport(id: String) {
        viewModelScope.launch {
            smsRepository.deleteUserReport(id)
        }
    }

    fun exportUserReports(includeOriginal: Boolean, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val reports = userReports.value
            val builder = StringBuilder()
            builder.append("id,nivel_asignado,nivel_usuario,fecha,huella_mensaje,texto_original\n")
            for (report in reports) {
                val original = if (includeOriginal) {
                    smsRepository.decryptReportBody(report).orEmpty()
                } else {
                    ""
                }
                builder.append(
                    listOf(
                        report.id,
                        report.assignedLevel,
                        report.userLevel,
                        report.timestamp.toString(),
                        report.messageHash,
                        original
                    ).joinToString(",") { csvValue(it) }
                )
                builder.append('\n')
            }
            onResult(builder.toString())
        }
    }

    private fun csvValue(value: String): String {
        return "\"${value.replace("\"", "\"\"")}\""
    }

    fun debeConfirmarReporte(): Boolean {
        return settingsRepository.communityConfirmations < 3
    }

    fun confirmarReporteMostrado() {
        settingsRepository.communityConfirmations = settingsRepository.communityConfirmations + 1
    }

    fun reportarEnComunidad(valor: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val indicador = CommunityIndicatorNormalizer.obtener(valor).firstOrNull()
            if (indicador == null) {
                onResult("Introduce un enlace, dominio o número válido.")
                return@launch
            }
            try {
                val respuesta = withContext(Dispatchers.IO) {
                    communityApiClient?.enviar(indicador)
                }
                val entrada = respuesta ?: com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity(
                    indicador = indicador.valor,
                    tipo = indicador.tipo,
                    reportes = 1,
                    esMalicioso = true,
                    pendienteSincronizacion = true
                )
                smsRepository.saveCommunityEntry(entrada)
                if (respuesta == null) {
                    onResult("Guardado localmente. Se sincronizará cuando el API comunitaria esté configurada.")
                } else {
                    onResult("Reporte enviado y guardado.")
                }
            } catch (error: Exception) {
                smsRepository.saveCommunityEntry(
                    com.antiphishing.smishguard.data.local.entity.CommunityBlacklistEntity(
                        indicador = indicador.valor,
                        tipo = indicador.tipo,
                        reportes = 1,
                        esMalicioso = true,
                        pendienteSincronizacion = true
                    )
                )
                onResult("No se pudo enviar el reporte: ${error.message ?: "error de conexión"}. Se reintentará automáticamente.")
            }
        }
    }

    fun sincronizarComunidad(alTerminar: (String) -> Unit) {
        viewModelScope.launch {
            val cliente = communityApiClient
            if (cliente == null) {
                alTerminar("El cliente comunitario no está disponible.")
                return@launch
            }
            try {
                val total = withContext(Dispatchers.IO) {
                    val pendientes = smsRepository.getPendingCommunityEntries()
                    for (pendiente in pendientes) {
                        val sincronizado = cliente.enviar(
                            com.antiphishing.smishguard.data.repository.CommunityIndicator(
                                pendiente.indicador,
                                pendiente.tipo
                            )
                        )
                        if (sincronizado != null) {
                            smsRepository.saveCommunityEntry(sincronizado)
                        }
                    }
                    val indicadores = cliente.descargar()
                    val inteligencia = cliente.descargarInteligencia()
                    val unidos = indicadores + inteligencia
                    smsRepository.saveCommunityEntries(unidos)
                    unidos.size
                }
                alTerminar("Lista sincronizada ($total registros).")
            } catch (error: Exception) {
                alTerminar("Error al sincronizar: ${error.message ?: "sin conexión"}.")
            }
        }
    }
}
