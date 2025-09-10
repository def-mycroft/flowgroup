package com.mfme.kernel.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mfme.kernel.ServiceLocator
import com.mfme.kernel.export.ObsidianExporter
import kotlinx.coroutines.flow.first

/**
 * Periodic worker that re-materializes the current day's log in the vault.
 * Current implementation is a placeholder.
 */
class VaultRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val cfg = ServiceLocator.vaultConfig(applicationContext)
        val uri = cfg.treeUri.first() ?: return Result.success()
        // In a complete implementation we would re-render recent envelope briefs and logs.
        // Here we simply instantiate the exporter to ensure dependencies resolve.
        ObsidianExporter(applicationContext, uri)
        return Result.success()
    }
}

