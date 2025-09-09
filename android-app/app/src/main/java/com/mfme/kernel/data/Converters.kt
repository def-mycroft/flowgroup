package com.mfme.kernel.data

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let { Instant.ofEpochMilli(it) }
}
