package com.mfme.kernel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mfme.kernel.data.Envelope
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.Receipt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class RoomSchemaTest {
    private lateinit var db: KernelDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KernelDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun envelopeInstantRoundTrip() = runBlocking {
        val instant = Instant.now()
        val env = Envelope(
            sha256 = "sha",
            mime = null,
            text = null,
            filename = null,
            sourcePkgRef = "unknown",
            receivedAtUtc = instant,
            metaJson = null
        )
        db.envelopeDao().insert(env)
        val loaded = db.envelopeDao().findBySha("sha")!!
        assertEquals(instant.toEpochMilli(), loaded.receivedAtUtc.toEpochMilli())
    }

    @Test
    fun receiptsOrderedDesc() = runBlocking {
        val now = Instant.now()
        db.receiptDao().insert(
            Receipt("a", "ok", "a", null, now.minusSeconds(10))
        )
        db.receiptDao().insert(
            Receipt("b", "ok", "b", null, now)
        )
        val receipts = db.receiptDao().observeAll().first()
        assertEquals("b", receipts.first().envelopeSha256)
    }
}
