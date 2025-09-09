package com.mfme.kernel.data

/** Result of attempting to persist an envelope derived from a share. */
sealed class SaveResult {
    data class Success(val envelopeId: Long) : SaveResult()
    data class Duplicate(val envelopeId: Long) : SaveResult()
    data class Error(val cause: Throwable) : SaveResult()
}
