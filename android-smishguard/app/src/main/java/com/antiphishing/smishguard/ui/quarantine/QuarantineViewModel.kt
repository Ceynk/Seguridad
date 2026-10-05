package com.antiphishing.smishguard.ui.quarantine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiphishing.smishguard.data.repository.SmsRepository
import com.antiphishing.smishguard.domain.model.SmsMessageData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuarantineViewModel(
    private val smsRepository: SmsRepository
) : ViewModel() {

    val quarantinedMessages: StateFlow<List<SmsMessageData>> =
        smsRepository.getQuarantinedRecords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreMessage(messageId: Long) {
        viewModelScope.launch {
            smsRepository.restoreQuarantinedMessage(messageId)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            smsRepository.deleteRecord(messageId)
        }
    }

    fun addSenderToWhitelist(sender: String) {
        viewModelScope.launch {
            smsRepository.addToWhitelist(sender, "Agregado desde Bóveda")
        }
    }
}
