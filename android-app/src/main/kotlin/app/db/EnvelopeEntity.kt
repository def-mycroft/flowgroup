package app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "envelopes",
    indices = [Index(value = ["sha256"], unique = true)]
)
data class EnvelopeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sha256: String,
    val filename: String?,
    val size: Long,
    val mediaType: String,
    val createdAt: Instant,
    val filePath: String?,
    val archivedPath: String,
    val sidecarPath: String
)
