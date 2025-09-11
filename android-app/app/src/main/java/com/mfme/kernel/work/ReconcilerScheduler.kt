package com.mfme.kernel.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object ReconcilerScheduler {
    fun verifyOnce(context: Context) {
        val work = OneTimeWorkRequestBuilder<ReconcilerWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("reconciler:once", ExistingWorkPolicy.REPLACE, work)
    }
}

