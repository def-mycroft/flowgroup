package app.zero.inlet.core

import java.security.MessageDigest

object Sha256Hasher {
    fun hash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
