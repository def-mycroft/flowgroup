package app.zero.inlet.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "span")
data class Span(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val envelopeSha: String,
    val start_nanos: Long,
    val end_nanos: Long
)
