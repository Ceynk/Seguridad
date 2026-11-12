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
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val pageSize = 50
    private var loadedOffset = 0
    private val _quarantinedMessages = MutableStateFlow<List<SmsMessageData>>(emptyList())
    val quarantinedMessages: StateFlow<List<SmsMessageData>> = _quarantinedMessages
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore
    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages

    val callRecords: StateFlow<List<CallLogUiItem>> =
        (callRepository?.getFilteredCallRecords()?.map { records ->
            records.map { record ->
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

    private var initialized = false

    fun ensureInitialized() {
        if (!initialized) {
            initialized = true
            loadMoreMessages()
        }
    }

    fun loadMoreMessages() {
        if (_isLoadingMore.value) {
            return
        }
        viewModelScope.launch {
            _isLoadingMore.value = true
            val page = smsRepository.getQuarantinedPage(pageSize, loadedOffset)
            if (page.isNotEmpty()) {
                _quarantinedMessages.value = _quarantinedMessages.value + page
                loadedOffset += page.size
            }
            _hasMoreMessages.value = page.size == pageSize
            _isLoadingMore.value = false
        }
    }

    fun restoreMessage(messageId: Long) {
        viewModelScope.launch {
            smsRepository.restoreQuarantinedMessage(messageId)
            reloadMessages()
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            smsRepository.deleteRecord(messageId)
            reloadMessages()
        }
    }

    private suspend fun reloadMessages() {
        loadedOffset = 0
        _quarantinedMessages.value = emptyList()
        _hasMoreMessages.value = true
        val page = smsRepository.getQuarantinedPage(pageSize, loadedOffset)
        _quarantinedMessages.value = page
        loadedOffset = page.size
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
