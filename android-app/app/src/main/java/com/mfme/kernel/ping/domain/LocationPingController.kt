package com.mfme.kernel.ping.domain

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.mfme.kernel.adapters.sms.SmsSendAdapter
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.SaveResult
import com.mfme.kernel.di.AppModule
import com.mfme.kernel.export.VaultConfig
import com.mfme.kernel.ping.data.LocationPingPreferences
import com.mfme.kernel.telemetry.ReceiptEmitter
import com.mfme.kernel.telemetry.TelemetryCode
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LocationPingController(private val context: Context) {
    private val prefs = LocationPingPreferences(context)

    suspend fun testNow(): Result {
        val db: KernelDatabase = AppModule.provideDatabase(context)
        val emitter: ReceiptEmitter = AppModule.provideReceiptEmitter(context, db)
        val repo = AppModule.provideRepository(context, db, emitter, AppModule.provideErrorEmitter(emitter), VaultConfig(context))
        val sms = SmsSendAdapter(context, repo)

        val model = prefs.flow.first()
        val recipient = model.selfTestNumber?.takeIf { isValidE164(it) }
            ?: model.recipients.firstOrNull { isValidE164(it) }
            ?: return Result.Error("empty_input")

        val fix = getLastKnownLocation() ?: run {
            emitter.emitV2(false, TelemetryCode.DeviceUnavailable.wire, "ping_test", "", null, null, "no_location")
            return Result.Error("no_location")
        }
        val body = renderBody(fix.latitude, fix.longitude)
        return try {
            when (sms.send(recipient, body)) {
                is SaveResult.Success, is SaveResult.Duplicate -> Result.Ok
                is SaveResult.Error -> Result.Error("send_error")
            }
        } catch (se: SecurityException) {
            emitter.emitV2(false, TelemetryCode.PermissionDenied.wire, "ping_test", "", null, null, se.message)
            Result.Error("permission_denied")
        } catch (_: Throwable) {
            Result.Error("send_error")
        }
    }

    private fun isValidE164(number: String): Boolean =
        number.matches(Regex("^\\+[1-9]\\d{1,14}$"))

    private fun getLastKnownLocation(): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
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

    sealed interface Result {
        data object Ok : Result
        data class Error(val reason: String) : Result
    }
}
