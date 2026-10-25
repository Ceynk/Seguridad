package com.antiphishing.smishguard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiphishing.smishguard.data.local.entity.CustomRuleEntity
import com.antiphishing.smishguard.data.local.entity.WhitelistEntity
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

    val whitelist: StateFlow<List<WhitelistEntity>> = smsRepository.getAllWhitelist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customRules: StateFlow<List<CustomRuleEntity>> = smsRepository.getAllCustomRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communityEntries = smsRepository.getCommunityEntries()
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
                    esMalicioso = false,
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
                        esMalicioso = false,
                        pendienteSincronizacion = true
                    )
                )
                onResult("No se pudo enviar el reporte: ${error.message ?: "error de conexión"}. Se reintentará automáticamente.")
            }
        }
    }
}
