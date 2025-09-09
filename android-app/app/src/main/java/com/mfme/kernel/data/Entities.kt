package com.mfme.kernel.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "envelopes", indices = [Index("sha256", unique = true)])
data class Envelope(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sha256: String,
    val mime: String?,
    val text: String?,
    val filename: String?,
    val sourcePkgRef: String,
    val receivedAtUtc: Instant,
    val metaJson: String?
)
