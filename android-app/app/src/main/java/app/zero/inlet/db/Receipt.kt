package app.zero.inlet.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "receipt")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val envelopeSha: String,
    val status: String,
    val created_at_utc: Instant
)
