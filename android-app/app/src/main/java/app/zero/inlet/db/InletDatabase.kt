package app.zero.inlet.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Envelope::class, Receipt::class, Span::class],
    version = 2
)
@TypeConverters(InstantConverters::class)
abstract class InletDatabase : RoomDatabase() {
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun spanDao(): SpanDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS envelope_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sha256 TEXT NOT NULL,
                        media_type TEXT NOT NULL,
                        filename TEXT NOT NULL,
                        bytes_path TEXT NOT NULL,
                        text TEXT NOT NULL,
                        size_bytes INTEGER NOT NULL,
                        created_at_utc INTEGER NOT NULL,
                        source_pkg TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO envelope_new (id, sha256, media_type, filename, bytes_path, text, size_bytes, created_at_utc, source_pkg)
                    SELECT id, sha256, mime, filename, '', '', size_bytes,
                        COALESCE(strftime('%s', created_at_Z) * 1000, strftime('%s','now') * 1000),
                        source_package
                    FROM envelope
                """)
                db.execSQL("DROP TABLE envelope")
                db.execSQL("ALTER TABLE envelope_new RENAME TO envelope")

                db.execSQL("DROP TABLE IF EXISTS receipt")
                db.execSQL("CREATE TABLE receipt (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    envelopeSha TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at_utc INTEGER NOT NULL
                )")

                db.execSQL("DROP TABLE IF EXISTS span")
                db.execSQL("CREATE TABLE span (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    envelopeSha TEXT NOT NULL,
                    start_nanos INTEGER NOT NULL,
                    end_nanos INTEGER NOT NULL
                )")
            }
        }
    }
}
