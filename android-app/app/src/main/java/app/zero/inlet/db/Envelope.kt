package app.zero.inlet.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "envelope",
    indices = [Index(value = ["sha256"], unique = true)]
)
data class Envelope(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sha256: String,
    val media_type: String,
    val filename: String,
    val bytes_path: String,
    val text: String,
    val size_bytes: Long,
    val created_at_utc: Instant,
    val source_pkg: String
)
