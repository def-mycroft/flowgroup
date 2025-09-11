package com.mfme.kernel.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * Schedules unique upload work keyed by sha256.
 */
object UploadScheduler {
    fun enqueue(context: Context, sha256: String) {
        val wifiOnly = try { com.mfme.kernel.cloud.CloudPreferences(context).currentWifiOnly() } catch (_: Throwable) { true }
        val netType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(netType)
            .build()
        val work = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_SHA256 to sha256))
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("upload:$sha256", ExistingWorkPolicy.KEEP, work)
    }
}
