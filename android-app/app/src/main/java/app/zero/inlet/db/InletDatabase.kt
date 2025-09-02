package app.zero.inlet.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EnvelopeEntity::class, ReceiptEntity::class, SpanEntity::class],
    version = 1
)
abstract class InletDatabase : RoomDatabase() {
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun spanDao(): SpanDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op for initial version
            }
        }
    }
}
