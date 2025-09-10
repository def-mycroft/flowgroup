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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE receipts ADD COLUMN ok INTEGER NOT NULL DEFAULT 1")
        db.execSQL(
            "UPDATE receipts SET ok = CASE WHEN code IN ('OK_NEW','OK_DUPLICATE') THEN 1 ELSE 0 END"
        )
        db.execSQL(
            "UPDATE receipts SET code = CASE code " +
                "WHEN 'OK_NEW' THEN 'ok_new' " +
                "WHEN 'OK_DUPLICATE' THEN 'ok_duplicate' " +
                "WHEN 'ERR_PERMISSION' THEN 'permission_denied' " +
                "WHEN 'ERR_INVALID_INPUT' THEN 'empty_input' " +
                "WHEN 'ERR_STORAGE' THEN 'storage_quota' " +
                "WHEN 'ERR_UNAVAILABLE' THEN 'device_unavailable' " +
                "WHEN 'ERR_IO' THEN 'device_unavailable' " +
                "WHEN 'ERR_UNKNOWN' THEN 'unknown' " +
                "ELSE code END"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS cloud_binding (envelopeId INTEGER NOT NULL PRIMARY KEY, driveFileId TEXT NOT NULL, uploadedAtUtc INTEGER NOT NULL, md5 TEXT, bytes INTEGER, UNIQUE(driveFileId))"
        )
    }
}
