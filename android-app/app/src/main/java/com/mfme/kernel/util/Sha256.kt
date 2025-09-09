package com.mfme.kernel.util

import java.security.MessageDigest

fun sha256(bytes: ByteArray): String {
    val d = MessageDigest.getInstance("SHA-256").digest(bytes)
    return d.joinToString("") { "%02x".format(it) }
}
