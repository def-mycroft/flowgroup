package com.mfme.kernel.adapters.share

import android.net.Uri
import java.time.Instant

/** Payload normalized from an ACTION_SEND intent. */
sealed interface SharePayload {
    val sourceRef: String
    val receivedAtUtc: Instant

    data class Text(
        val text: String,
        val subject: String?,
        override val sourceRef: String,
        override val receivedAtUtc: Instant
    ) : SharePayload

    data class Stream(
        val uri: Uri,
        val mime: String?,
        val displayName: String?,
        val sizeBytes: Long?,
        override val sourceRef: String,
        override val receivedAtUtc: Instant
    ) : SharePayload
}
