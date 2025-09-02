package app.zero.inlet.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipt",
    foreignKeys = [
        ForeignKey(
            entity = EnvelopeEntity::class,
            parentColumns = ["sha256"],
            childColumns = ["envelope_sha256"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("envelope_sha256")]
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "envelope_sha256") val envelopeSha256: String,
    @ColumnInfo(name = "created_at_Z") val createdAtZ: String
)
