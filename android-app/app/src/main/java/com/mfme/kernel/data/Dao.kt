package com.mfme.kernel.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvelopeDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(envelope: Envelope): Long

    @Query("SELECT * FROM envelopes WHERE sha256 = :sha LIMIT 1")
    suspend fun findBySha(sha: String): Envelope?

    @Query("SELECT * FROM envelopes ORDER BY receivedAtUtc DESC")
    fun observeAll(): Flow<List<Envelope>>

    @Query("SELECT * FROM envelopes")
    suspend fun getAll(): List<Envelope>
}
