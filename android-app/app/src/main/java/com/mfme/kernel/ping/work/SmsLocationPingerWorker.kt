package com.mfme.kernel.ping.work

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mfme.kernel.adapters.sms.SmsSendAdapter
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.di.AppModule
import com.mfme.kernel.export.VaultConfig
import com.mfme.kernel.ping.data.LocationPingPreferences
import com.mfme.kernel.telemetry.ReceiptEmitter
import com.mfme.kernel.telemetry.TelemetryCode
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SmsLocationPingerWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db: KernelDatabase = AppModule.provideDatabase(applicationContext)
        val emitter: ReceiptEmitter = AppModule.provideReceiptEmitter(applicationContext, db)
        val repo = AppModule.provideRepository(
            context = applicationContext,
            db = db,
            emitter = emitter,
            errorEmitter = AppModule.provideErrorEmitter(emitter),
            config = VaultConfig(applicationContext),
        )
        val sms = SmsSendAdapter(applicationContext, repo)

        val prefs = LocationPingPreferences(applicationContext)
        val model = prefs.flow.first()

        // Config guards
        val recipients = model.recipients.filter { isValidE164(it) }
        if (!model.enabled || recipients.isEmpty()) {
            // No-op success with a span-like receipt entry for observability
            emitter.emitV2(true, TelemetryCode.OkDuplicate.wire, "pinger", spanId = null, envelopeId = null, envelopeSha256 = null, message = "disabled_or_empty")
            return Result.success()
        }

        // Rate limit: max 3 sends within any 15-minute (900s) window across all recipients
        val nowEpoch = Instant.now().epochSecond
        val windowStart = nowEpoch - 900
        val recent = model.sentHistoryEpochSec.filter { it >= windowStart }
        if (recent.size >= 3) {
            emitter.emitV2(true, TelemetryCode.OkDuplicate.wire, "pinger", null, null, null, "rate_limited_window")
            return Result.success()
        }

        val fix = getLastKnownLocation()
        if (fix == null) {
            emitter.emitV2(false, TelemetryCode.DeviceUnavailable.wire, "pinger", null, null, null, "no_location")
            return Result.success()
        }

        val body = renderBody(fix.latitude, fix.longitude)

        // Send to all recipients
        var anySuccess = false
        recipients.forEach { to ->
            try {
                val result = sms.send(to, body)
                anySuccess = anySuccess || result is com.mfme.kernel.data.SaveResult.Success || result is com.mfme.kernel.data.SaveResult.Duplicate
            } catch (se: SecurityException) {
                emitter.emitV2(false, TelemetryCode.PermissionDenied.wire, "pinger", null, null, null, se.message)
            } catch (t: Throwable) {
                emitter.emitV2(false, TelemetryCode.DeviceUnavailable.wire, "pinger", null, null, null, t.message)
            }
        }

        // Update send history only if at least one send path went through repository
        if (anySuccess) {
            val updated = (recent + nowEpoch).sorted().takeLast(10) // Keep last few entries
            prefs.updateSentHistory(updated)
        }

        return Result.success()
    }

    private fun isValidE164(number: String): Boolean =
        number.matches(Regex("^\\+[1-9]\\d{1,14}$"))

    private fun getLastKnownLocation(): Location? {
        val lm = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var best: Location? = null
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || (loc.time > (best?.time ?: 0))) best = loc
            } catch (_: SecurityException) {
                return null
            }
        }
        return best
    }

    private fun renderBody(lat: Double, lon: Double): String {
        val nowLocal = Instant.now().atZone(ZoneId.systemDefault())
        val ts = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(nowLocal)
        val link = "https://maps.google.com/?q=${lat},${lon}&z=16"
        return "$link @ $ts"
    }
}

