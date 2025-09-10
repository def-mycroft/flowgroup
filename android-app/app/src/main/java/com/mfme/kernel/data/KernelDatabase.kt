package com.mfme.kernel.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mfme.kernel.data.cloud.CloudBinding
import com.mfme.kernel.data.cloud.CloudBindingDao
import com.mfme.kernel.data.telemetry.ReceiptEntity
import com.mfme.kernel.data.telemetry.SpanEntity
import com.mfme.kernel.data.telemetry.ReceiptDao
import com.mfme.kernel.data.telemetry.SpanDao

@Database(
    entities = [Envelope::class, ReceiptEntity::class, SpanEntity::class, CloudBinding::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KernelDatabase : RoomDatabase() {
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun spanDao(): SpanDao
    abstract fun cloudBindingDao(): CloudBindingDao
}
