package app.zero.inlet.db

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface ReceiptDao {
    @Insert
    fun insert(entity: ReceiptEntity): Long
}
