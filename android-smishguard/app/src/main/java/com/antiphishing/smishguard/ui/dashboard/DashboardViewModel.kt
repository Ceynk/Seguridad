package com.antiphishing.smishguard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiphishing.smishguard.data.local.entity.CallRecordEntity
import com.antiphishing.smishguard.data.repository.CallRepository
import com.antiphishing.smishguard.data.repository.SecuritySettingsRepository
import com.antiphishing.smishguard.data.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val isProtectionActive: Boolean = true,
    val isSilentDismissEnabled: Boolean = true
)

class DashboardViewModel(
    private val smsRepository: SmsRepository,
    private val settingsRepository: SecuritySettingsRepository,
    callRepository: CallRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            isProtectionActive = settingsRepository.isProtectionEnabled,
            isSilentDismissEnabled = settingsRepository.isSilentDismissNotifEnabled
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val totalScanned = smsRepository.getTotalAnalyzedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val quarantinedCount = smsRepository.getActiveQuarantinedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val phishingCount = smsRepository.getPhishingDetectedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentQuarantined = smsRepository.getRecentQuarantinedRecords(4)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedCallsCount: StateFlow<Int> = (callRepository?.getBlockedCallsCount() ?: flowOf(0))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentFlaggedCalls: StateFlow<List<CallRecordEntity>> =
        (callRepository?.getRecentFlaggedCalls(5) ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun syncSettings() {
        _uiState.value = DashboardUiState(
            isProtectionActive = settingsRepository.isProtectionEnabled,
            isSilentDismissEnabled = settingsRepository.isSilentDismissNotifEnabled
        )
    }

    fun toggleProtection(enabled: Boolean) {
        settingsRepository.isProtectionEnabled = enabled
        _uiState.value = _uiState.value.copy(isProtectionActive = enabled)
    }
}
