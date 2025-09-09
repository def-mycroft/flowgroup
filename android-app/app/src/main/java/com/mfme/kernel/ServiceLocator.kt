package com.mfme.kernel

import android.content.Context
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.di.AppModule

object ServiceLocator {
    @Volatile private var db: KernelDatabase? = null
    @Volatile private var repo: KernelRepository? = null

    fun repository(appContext: Context): KernelRepository {
        return repo ?: synchronized(this) {
            val database = db ?: AppModule.provideDatabase(appContext).also { db = it }
            AppModule.provideRepository(database).also { repo = it }
        }
    }
}
