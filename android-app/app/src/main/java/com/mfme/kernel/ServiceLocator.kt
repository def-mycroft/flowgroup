package com.mfme.kernel

import android.content.Context
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.di.AppModule
import com.mfme.kernel.export.VaultConfig

object ServiceLocator {
    @Volatile private var db: KernelDatabase? = null
    @Volatile private var repo: KernelRepository? = null
    @Volatile private var vaultCfg: VaultConfig? = null

    fun vaultConfig(appContext: Context): VaultConfig =
        vaultCfg ?: synchronized(this) { VaultConfig(appContext).also { vaultCfg = it } }

    fun repository(appContext: Context): KernelRepository {
        return repo ?: synchronized(this) {
            val database = db ?: AppModule.provideDatabase(appContext).also { db = it }
            val emitter = AppModule.provideReceiptEmitter(appContext, database)
            val errorEmitter = AppModule.provideErrorEmitter(emitter)
            val config = vaultConfig(appContext)
            AppModule.provideRepository(appContext, database, emitter, errorEmitter, config).also { repo = it }
        }
    }
}

