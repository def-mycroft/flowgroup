package com.mfme.kernel.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Envelope::class, Receipt::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KernelDatabase : RoomDatabase() {
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun receiptDao(): ReceiptDao
}
