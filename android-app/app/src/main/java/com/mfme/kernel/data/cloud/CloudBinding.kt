package com.mfme.kernel.data.cloud

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "cloud_binding",
    indices = [Index("driveFileId", unique = true)]
)
data class CloudBinding(
    @PrimaryKey val envelopeId: Long,
    val driveFileId: String,
    val uploadedAtUtc: Instant,
    val md5: String?,
    val bytes: Long?
)
