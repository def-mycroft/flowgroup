package com.mfme.kernel.util

import java.io.InputStream
import java.security.MessageDigest

/** Computes SHA-256 digests. */
fun sha256OfStream(input: InputStream, bufferSize: Int = 64 * 1024): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    val buf = ByteArray(bufferSize)
    while (true) {
        val read = input.read(buf)
        if (read < 0) break
        md.update(buf, 0, read)
    }
    return md.digest()
}

fun sha256OfUtf8(text: String): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))

fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
