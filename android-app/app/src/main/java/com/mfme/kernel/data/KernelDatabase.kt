package com.mfme.kernel.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mfme.kernel.data.telemetry.ReceiptEntity
import com.mfme.kernel.data.telemetry.SpanEntity
import com.mfme.kernel.data.telemetry.ReceiptDao
import com.mfme.kernel.data.telemetry.SpanDao

@Database(
    entities = [Envelope::class, ReceiptEntity::class, SpanEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KernelDatabase : RoomDatabase() {
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun spanDao(): SpanDao
}
