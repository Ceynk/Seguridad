package com.antiphishing.smishguard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiphishing.smishguard.data.repository.SecuritySettingsRepository
import com.antiphishing.smishguard.data.repository.SmsRepository
import com.antiphishing.smishguard.domain.model.SmsMessageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val isProtectionActive: Boolean = true
)

class DashboardViewModel(
    private val smsRepository: SmsRepository,
    private val settingsRepository: SecuritySettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(isProtectionActive = settingsRepository.isProtectionEnabled)
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val totalScanned = smsRepository.getTotalAnalyzedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val quarantinedCount = smsRepository.getActiveQuarantinedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val phishingCount = smsRepository.getPhishingDetectedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentQuarantined = smsRepository.getQuarantinedRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleProtection(enabled: Boolean) {
        settingsRepository.isProtectionEnabled = enabled
        _uiState.value = _uiState.value.copy(isProtectionActive = enabled)
    }
}
