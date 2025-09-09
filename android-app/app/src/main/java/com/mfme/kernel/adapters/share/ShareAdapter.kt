package com.mfme.kernel.adapters.share

import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import java.time.Instant

/** Extracts a [SharePayload] from an ACTION_SEND [Intent]. */
object ShareAdapter {
    fun fromIntent(context: Context, intent: Intent): SharePayload? {
        if (intent.action != Intent.ACTION_SEND) return null
        val sourceRef = intent.`package`
            ?: intent.getStringExtra(Intent.EXTRA_REFERRER_NAME)
            ?: intent.referrer?.host ?: "unknown"
        val nowUtc = Instant.now()

        intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let { uri ->
            val (name, size, mime) = resolveMeta(context, uri, intent.type)
            return SharePayload.Stream(
                uri = uri,
                mime = mime,
                displayName = name,
                sizeBytes = size,
                sourceRef = sourceRef,
                receivedAtUtc = nowUtc
            )
        }

        val txt = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!txt.isNullOrEmpty()) {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            return SharePayload.Text(
                text = txt,
                subject = subject,
                sourceRef = sourceRef,
                receivedAtUtc = nowUtc
            )
        }
        return null
    }

    private fun resolveMeta(
        context: Context,
        uri: android.net.Uri,
        fallbackMime: String?
    ): Triple<String?, Long?, String?> {
        var name: String? = null
        var size: Long? = null
        val cr = context.contentResolver
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { c ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx != -1) name = c.getString(nameIdx)
                    if (sizeIdx != -1) size = c.getLong(sizeIdx)
                }
            }
        val mime = cr.getType(uri) ?: fallbackMime
        return Triple(name, size, mime)
    }
}
