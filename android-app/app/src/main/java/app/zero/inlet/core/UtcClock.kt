package app.zero.inlet.core

import java.time.Instant

object UtcClock {
    fun nowZ(): String {
        val iso = Instant.now().toString()
        return if (iso.endsWith("Z")) iso else "${iso}Z"
    }
}
