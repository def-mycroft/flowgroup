package app.zero.inlet.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvelopeDao {
    @Upsert
    suspend fun upsert(envelope: Envelope): Long

    @Query("SELECT * FROM envelope WHERE sha256 = :sha")
    suspend fun findBySha(sha: String): Envelope?

    @Query("SELECT * FROM envelope ORDER BY created_at_utc DESC")
    fun observeNewest(): Flow<List<Envelope>>
}
