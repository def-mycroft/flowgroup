package app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvelopeDao {
    @Query("SELECT * FROM envelopes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<EnvelopeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: EnvelopeEntity)
}
