package app.zero.inlet.db

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface ReceiptDao {
    @Insert
    suspend fun insert(entity: Receipt): Long
}
