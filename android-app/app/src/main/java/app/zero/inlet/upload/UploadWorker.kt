package app.zero.inlet.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Simplified worker representing an upload task.
 * The worker does no real work and simply succeeds.
 */
class UploadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = Result.success()
}

