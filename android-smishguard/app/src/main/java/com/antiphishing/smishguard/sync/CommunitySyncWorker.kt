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
        if (!application.settingsRepository.isCommunitySyncEnabled) {
            return@withContext Result.success()
        }
        return@withContext try {
            for (eliminado in application.smsRepository.getPendingDeletedCommunityIndicators()) {
                try {
                    if (application.communityApiClient.eliminar(eliminado)) {
                        application.smsRepository.eliminarEntradaComunidad(eliminado, pendienteServidor = false)
                    }
                } catch (_: Exception) {
                }
            }
            for (pendiente in application.smsRepository.getPendingCommunityEntries()) {
                try {
                    val sincronizado = application.communityApiClient.enviar(
                        CommunityIndicator(pendiente.indicador, pendiente.tipo)
                    )
                    if (sincronizado != null) {
                        application.smsRepository.saveCommunityEntry(
                            sincronizado,
                            indicadorOriginal = pendiente.indicador
                        )
                    }
                } catch (_: Exception) {
                }
            }
            val entradas = application.communityApiClient.descargar()
            val inteligencia = application.communityApiClient.descargarInteligencia()
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
