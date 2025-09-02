package app.zero.inlet.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "span")
data class SpanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "trace_id") val traceId: String,
    val action: String,
    @ColumnInfo(name = "started_at_Z") val startedAtZ: String,
    @ColumnInfo(name = "ended_at_Z") val endedAtZ: String,
    val status: String
)
