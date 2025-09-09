package com.mfme.kernel.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Preserve old receipts table if present
        try {
            db.execSQL("ALTER TABLE receipts RENAME TO receipts_legacy")
        } catch (_: Throwable) {
            // table might not exist
        }
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS receipts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, adapter TEXT NOT NULL, code TEXT NOT NULL, tsUtcIso TEXT NOT NULL, envelopeId INTEGER, envelopeSha256 TEXT, message TEXT, spanId TEXT NOT NULL, receiptSha256 TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS spans (spanId TEXT PRIMARY KEY NOT NULL, adapter TEXT NOT NULL, startNanos INTEGER NOT NULL, endNanos INTEGER NOT NULL, envelopeId INTEGER, envelopeSha256 TEXT)"
        )
    }
}
