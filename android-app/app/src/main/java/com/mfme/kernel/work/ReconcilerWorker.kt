package com.mfme.kernel.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.zero.core.cloud.DriveAdapter
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.cloud.CloudBinding
import com.mfme.kernel.di.AppModule
import com.mfme.kernel.telemetry.ReceiptEmitter
import com.mfme.kernel.telemetry.TelemetryCode
import java.time.Instant

/**
 * Periodic worker that verifies cloud bindings against remote Drive state.
 * TODO: inject real DriveAdapter implementation.
 */
class ReconcilerWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db: KernelDatabase = AppModule.provideDatabase(applicationContext)
        val emitter: ReceiptEmitter = AppModule.provideReceiptEmitter(applicationContext, db)
        val span = emitter.begin("reconciler")
        val envelopeDao = db.envelopeDao()
        val bindingDao = db.cloudBindingDao()
        val drive: DriveAdapter = driveAdapter()

        // Tombstone orphan bindings
        bindingDao.findOrphans().forEach { bindingDao.deleteByEnvelopeId(it.envelopeId) }

        val envelopes = envelopeDao.getAll()
        for (env in envelopes) {
            val binding = bindingDao.findByEnvelopeId(env.id)
            if (binding == null) {
                val ref = drive.findBySha256(env.sha256, "").getOrNull()
                if (ref != null) {
                    val newBinding = CloudBinding(env.id, ref.id, Instant.now(), ref.md5, ref.bytes)
                    bindingDao.upsert(newBinding)
                    emitter.emitV2(true, TelemetryCode.OkRebound.wire, "reconciler", span.spanId, env.id, env.sha256, null)
                } else {
                    emitter.emitV2(false, TelemetryCode.ErrorNotFound.wire, "reconciler", span.spanId, env.id, env.sha256, null)
                }
            } else {
                val meta = drive.getMetadata(binding.driveFileId).getOrNull()
                if (meta == null) {
                    val ref = drive.findBySha256(env.sha256, "").getOrNull()
                    if (ref != null) {
                        val rebound = CloudBinding(env.id, ref.id, Instant.now(), ref.md5, ref.bytes)
                        bindingDao.upsert(rebound)
                        emitter.emitV2(true, TelemetryCode.OkRebound.wire, "reconciler", span.spanId, env.id, env.sha256, null)
                    } else {
                        emitter.emitV2(false, TelemetryCode.ErrorNotFound.wire, "reconciler", span.spanId, env.id, env.sha256, null)
                    }
                } else {
                    emitter.emitV2(true, TelemetryCode.OkAlreadyBound.wire, "reconciler", span.spanId, env.id, env.sha256, null)
                }
            }
        }

        emitter.end(span)
        return Result.success()
    }

    private suspend fun driveAdapter(): DriveAdapter = TODO("Provide DriveAdapter")
}
