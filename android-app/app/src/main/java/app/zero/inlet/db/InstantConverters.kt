package app.zero.inlet.db

import androidx.room.TypeConverter
import java.time.Instant

class InstantConverters {
    @TypeConverter
    fun fromEpoch(millis: Long): Instant = Instant.ofEpochMilli(millis)

    @TypeConverter
    fun toEpoch(instant: Instant): Long = instant.toEpochMilli()
}
