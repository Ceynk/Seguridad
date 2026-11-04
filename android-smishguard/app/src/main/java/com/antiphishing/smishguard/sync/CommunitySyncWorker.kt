package com.antiphishing.smishguard.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.data.repository.CommunityIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommunitySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val application = applicationContext as? SmishGuardApp ?: return@withContext Result.failure()
        return@withContext try {
            for (pendiente in application.smsRepository.getPendingCommunityEntries()) {
                val sincronizado = application.communityApiClient.enviar(
                    CommunityIndicator(pendiente.indicador, pendiente.tipo)
                )
                if (sincronizado != null) {
                    application.smsRepository.saveCommunityEntry(sincronizado)
                }
            }
            val entradas = application.communityApiClient.descargar(
                application.settingsRepository.communitySyncCursor
            )
            val inteligencia = application.communityApiClient.descargarInteligencia(
                application.settingsRepository.communitySyncCursor
            )
            application.smsRepository.saveCommunityEntries(entradas + inteligencia)
            (entradas + inteligencia).maxOfOrNull { it.actualizadoEn }?.let {
                application.settingsRepository.communitySyncCursor = it
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
