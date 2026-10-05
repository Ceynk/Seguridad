package com.antiphishing.smishguard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiphishing.smishguard.data.local.entity.CustomRuleEntity
import com.antiphishing.smishguard.data.local.entity.WhitelistEntity
import com.antiphishing.smishguard.data.repository.CallRepository
import com.antiphishing.smishguard.data.repository.SecuritySettingsRepository
import com.antiphishing.smishguard.data.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SecuritySettingsRepository,
    private val smsRepository: SmsRepository,
    private val callRepository: CallRepository? = null
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
            onResult?.invoke(error)
        }
    }

    fun toggleCustomRule(rule: CustomRuleEntity) {
        viewModelScope.launch {
            smsRepository.updateCustomRule(rule.copy(isEnabled = !rule.isEnabled))
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
            callRepository?.clearAllCallRecords()
        }
    }

    fun clearQuarantined() {
        viewModelScope.launch {
            smsRepository.clearAllQuarantined()
        }
    }
}
