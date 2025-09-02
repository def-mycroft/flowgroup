package app.zero.inlet.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "envelope",
    indices = [Index(value = ["sha256"], unique = true)]
)
data class EnvelopeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sha256: String,
    val filename: String,
    val mime: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "created_at_Z") val createdAtZ: String,
    @ColumnInfo(name = "source_package") val sourcePackage: String
)
