package com.antiphishing.smishguard.ui.quarantine

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antiphishing.smishguard.data.repository.CallRepository
import com.antiphishing.smishguard.data.repository.SmsRepository
import com.antiphishing.smishguard.domain.model.SmsMessageData
import com.antiphishing.smishguard.domain.model.ThreatFactor
import com.antiphishing.smishguard.domain.model.ThreatLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class CallLogUiItem(
    val id: Long,
    val phoneNumber: String,
    val timestamp: Long,
    val threatScore: Int,
    val threatLevel: ThreatLevel,
    val isBlocked: Boolean,
    val action: String,
    val countryOrOrigin: String?,
    val summaryReason: String,
    val factors: List<ThreatFactor>
)

class QuarantineViewModel(
    private val smsRepository: SmsRepository,
    private val callRepository: CallRepository? = null
) : ViewModel() {

    val quarantinedMessages: StateFlow<List<SmsMessageData>> =
        smsRepository.getQuarantinedRecords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callRecords: StateFlow<List<CallLogUiItem>> =
        (callRepository?.getAllCallRecords()?.map { records ->
            records
                .filter { it.isBlocked || it.action != "ALLOW" || it.threatScore > 0 }
                .map { record ->
                    val parsedLevel = try {
                        ThreatLevel.valueOf(record.threatLevel)
                    } catch (_: Exception) {
                        ThreatLevel.SAFE
                    }
                    CallLogUiItem(
                        id = record.id,
                        phoneNumber = record.phoneNumber.ifBlank { "Número oculto" },
                        timestamp = record.timestamp,
                        threatScore = record.threatScore,
                        threatLevel = parsedLevel,
                        isBlocked = record.isBlocked,
                        action = record.action,
                        countryOrOrigin = record.countryOrOrigin,
                        summaryReason = record.summaryReason,
                        factors = callRepository.deserializeFactors(record.factorsJson)
                    )
                }
        }?.flowOn(Dispatchers.Default) ?: flowOf(emptyList()))
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

    fun deleteCallRecord(callId: Long) {
        viewModelScope.launch {
            callRepository?.deleteCallRecord(callId)
        }
    }

    fun addSenderToWhitelist(sender: String) {
        viewModelScope.launch {
            smsRepository.addToWhitelist(sender, "Agregado desde Cuarentena")
        }
    }

    fun loadOriginalBody(messageId: Long, onLoaded: (String) -> Unit) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                when (val result = smsRepository.getDecryptedOriginalBody(messageId)) {
                    is SmsRepository.OriginalBodyResult.Available -> result.value
                    SmsRepository.OriginalBodyResult.Missing -> "Registro no encontrado."
                    is SmsRepository.OriginalBodyResult.Unreadable -> "No fue posible descifrar el contenido original."
                }
            }
            onLoaded(text)
        }
    }
}
