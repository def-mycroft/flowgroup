package com.mfme.kernel.data.telemetry

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spans")
data class SpanEntity(
  @PrimaryKey val spanId: String,
  val adapter: String,
  val startNanos: Long,
  val endNanos: Long,
  val envelopeId: Long?,
  val envelopeSha256: String?
)
