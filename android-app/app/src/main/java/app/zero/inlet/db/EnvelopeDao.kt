package app.zero.inlet.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EnvelopeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(entity: EnvelopeEntity): Long

    @Query("SELECT * FROM envelope WHERE sha256 = :sha")
    fun findBySha(sha: String): EnvelopeEntity?

    @Query("SELECT COUNT(*) FROM envelope")
    fun count(): Int
}
