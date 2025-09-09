package com.mfme.kernel.telemetry

import java.security.MessageDigest

object Hashing {
  fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
