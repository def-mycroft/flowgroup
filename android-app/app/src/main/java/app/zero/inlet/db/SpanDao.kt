package app.zero.inlet.db

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface SpanDao {
    @Insert
    fun insert(entity: SpanEntity): Long
}
