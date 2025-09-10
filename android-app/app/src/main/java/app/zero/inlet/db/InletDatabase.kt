package app.zero.inlet.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Envelope::class, Receipt::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(InstantConverters::class)
abstract class InletDatabase : RoomDatabase() {
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun receiptDao(): ReceiptDao
}

