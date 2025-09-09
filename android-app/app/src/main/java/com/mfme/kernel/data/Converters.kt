package com.mfme.kernel.data

import androidx.room.TypeConverter
import java.time.Instant
import com.mfme.kernel.data.telemetry.ReceiptCode

class Converters {
    @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let { Instant.ofEpochMilli(it) }

    @TypeConverter fun receiptCodeToString(v: ReceiptCode?): String? = v?.name
    @TypeConverter fun stringToReceiptCode(v: String?): ReceiptCode? = v?.let { ReceiptCode.valueOf(it) }
}
