package com.mfme.kernel.data.telemetry

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReceiptDao {
  @Insert
  suspend fun insert(e: ReceiptEntity): Long

  @Query("SELECT * FROM receipts ORDER BY id DESC LIMIT :limit OFFSET :offset")
  suspend fun pageNewestFirst(limit: Int, offset: Int): List<ReceiptEntity>

  @Query("SELECT COUNT(*) FROM receipts WHERE envelopeId = :envId")
  suspend fun countForEnvelope(envId: Long): Int

  @Query("SELECT * FROM receipts ORDER BY id DESC")
  fun observeAll(): kotlinx.coroutines.flow.Flow<List<ReceiptEntity>>
}

@Dao
interface SpanDao {
  @Insert
  suspend fun insert(e: SpanEntity)

  @Query("UPDATE spans SET envelopeId=:envId, envelopeSha256=:sha WHERE spanId=:spanId")
  suspend fun bindEnvelope(spanId: String, envId: Long?, sha: String?)
}
