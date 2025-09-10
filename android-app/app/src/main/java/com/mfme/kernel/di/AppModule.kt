package com.mfme.kernel.di

import android.content.Context
import androidx.room.Room
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.KernelRepositoryImpl
import com.mfme.kernel.data.MIGRATION_1_2
import com.mfme.kernel.data.MIGRATION_2_3
import com.mfme.kernel.data.MIGRATION_3_4
import com.mfme.kernel.telemetry.ErrorEmitter
import com.mfme.kernel.telemetry.NdjsonSink
import com.mfme.kernel.telemetry.ReceiptEmitter
import com.mfme.kernel.export.EnvelopeChainer
import com.mfme.kernel.export.ObsidianExporter
import kotlinx.coroutines.Dispatchers

object AppModule {
    fun provideDatabase(context: Context): KernelDatabase =
        Room.databaseBuilder(context, KernelDatabase::class.java, "kernel.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    fun provideReceiptEmitter(context: Context, db: KernelDatabase): ReceiptEmitter {
        val sink = NdjsonSink(context)
        return ReceiptEmitter(db.receiptDao(), db.spanDao(), sink)
    }

    fun provideErrorEmitter(receiptEmitter: ReceiptEmitter): ErrorEmitter =
        ErrorEmitter(receiptEmitter)

    fun provideEnvelopeChainer(context: Context): EnvelopeChainer {
        val exporter = ObsidianExporter()
        return EnvelopeChainer(context, exporter)
    }

    fun provideRepository(
        context: Context,
        db: KernelDatabase,
        emitter: ReceiptEmitter,
        errorEmitter: ErrorEmitter
    ): KernelRepository {
        val chainer = provideEnvelopeChainer(context)
        return KernelRepositoryImpl(context, db, Dispatchers.IO, emitter, errorEmitter, db.spanDao(), chainer)
    }
}
