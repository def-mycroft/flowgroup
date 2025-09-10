package com.mfme.kernel.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Stub worker for Morph 2 background Drive uploads.
 * TODO: implement stream → verify → bind pipeline.
 */
class UploadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        // Implementation pending
        return Result.success()
    }

    companion object {
        const val KEY_SHA256 = "sha256"
    }
}
