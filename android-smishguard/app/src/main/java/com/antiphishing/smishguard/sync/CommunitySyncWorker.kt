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
            application.smsRepository.sincronizarConServidor(application.communityApiClient)
            application.smsRepository.getCommunityEntriesByIndicator().values.maxOfOrNull { it.actualizadoEn }?.let {
                application.settingsRepository.communitySyncCursor = it
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
