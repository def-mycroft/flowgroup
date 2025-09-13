package com.mfme.kernel.cloud

import android.content.Context
import com.mfme.kernel.telemetry.ReceiptEmitter
import com.mfme.kernel.telemetry.TelemetryCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Helper actions for Cloud UI flows so they can be exercised in tests without UI.
 */
object CloudActions {
    private const val BRIEF_TAG = "brief:imbued-sycamore property:property-drive-connect-visible"

    fun verifyNow(context: Context, emitter: ReceiptEmitter, scope: CoroutineScope, background: Boolean = true) {
        // Always emit a receipt; never silent.
        val adapter = DriveServiceFactory.getAdapter(context)
        val span = kotlinx.coroutines.runBlocking { emitter.begin("cloud_verify") }
        if (adapter == null) {
            kotlinx.coroutines.runBlocking {
                emitter.emitV2(
                    ok = TelemetryCode.ErrNoAccount.ok,
                    codeWire = TelemetryCode.ErrNoAccount.wire,
                    adapter = "cloud_verify",
                    spanId = span.spanId,
                    envelopeId = null,
                    envelopeSha256 = null,
                    message = BRIEF_TAG
                )
                emitter.end(span)
            }
            return
        }

        kotlinx.coroutines.runBlocking {
            emitter.emitV2(
                ok = TelemetryCode.OkVerifyQueued.ok,
                codeWire = TelemetryCode.OkVerifyQueued.wire,
                adapter = "cloud_verify",
                spanId = span.spanId,
                envelopeId = null,
                envelopeSha256 = null,
                message = BRIEF_TAG
            )
            emitter.end(span)
        }

        // Probe for verification. In production we do this in the background; in tests we can run inline.
        val probeWork: suspend () -> Unit = {
            val verifySpan = emitter.begin("cloud_verify")
            val result = DriveServiceFactory.getAdapter(context)?.probe()
            if (result?.isSuccess == true) {
                emitter.emitV2(
                    ok = TelemetryCode.OkVerified.ok,
                    codeWire = TelemetryCode.OkVerified.wire,
                    adapter = "cloud_verify",
                    spanId = verifySpan.spanId,
                    envelopeId = null,
                    envelopeSha256 = null,
                    message = BRIEF_TAG
                )
            } else {
                emitter.emitV2(
                    ok = TelemetryCode.ErrVerifyFailed().ok,
                    codeWire = TelemetryCode.ErrVerifyFailed().wire,
                    adapter = "cloud_verify",
                    spanId = verifySpan.spanId,
                    envelopeId = null,
                    envelopeSha256 = null,
                    message = BRIEF_TAG
                )
            }
            emitter.end(verifySpan)
        }
        if (background) {
            scope.launch { kotlin.runCatching { probeWork() } }
        } else {
            kotlinx.coroutines.runBlocking { kotlin.runCatching { probeWork() } }
        }
    }
}
