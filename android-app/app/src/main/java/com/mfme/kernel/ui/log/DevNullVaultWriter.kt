package com.mfme.kernel.ui.log

import java.time.LocalDate

/** Preview only writer that discards all output. */
class DevNullVaultWriter : VaultWriterPort {
    override suspend fun write(day: LocalDate, text: String): VaultWriteResult = VaultWriteResult.OkPreviewOnly
}
