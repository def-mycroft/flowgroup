package com.mfme.kernel.ui.log

import java.time.LocalDate

sealed class VaultWriteResult {
    object OkPreviewOnly : VaultWriteResult()
}

/** Port for writing markdown logs to a vault. */
interface VaultWriterPort {
    suspend fun write(day: LocalDate, text: String): VaultWriteResult
}
