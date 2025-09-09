package com.mfme.kernel.data.telemetry

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val ok: Boolean,
  val code: String,
  val adapter: String,
  val tsUtcIso: String,
  val envelopeId: Long?,
  val envelopeSha256: String?,
  val message: String?,
  val spanId: String,
  val receiptSha256: String
)
