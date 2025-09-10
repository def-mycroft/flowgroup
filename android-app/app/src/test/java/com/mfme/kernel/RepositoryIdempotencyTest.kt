package com.mfme.kernel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mfme.kernel.data.Envelope
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.KernelRepositoryImpl
import com.mfme.kernel.telemetry.ErrorEmitter
import com.mfme.kernel.telemetry.NdjsonSink
import com.mfme.kernel.telemetry.ReceiptEmitter
import com.mfme.kernel.export.EnvelopeChainer
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class RepositoryIdempotencyTest {
    private lateinit var db: KernelDatabase
    private lateinit var repo: KernelRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KernelDatabase::class.java).build()
        val sink = NdjsonSink(context)
        val receiptEmitter = ReceiptEmitter(db.receiptDao(), db.spanDao(), sink)
        val errorEmitter = ErrorEmitter(receiptEmitter)
        val chainer = EnvelopeChainer(context)
        repo = KernelRepositoryImpl(context, db, Dispatchers.IO, receiptEmitter, errorEmitter, db.spanDao(), chainer)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveEnvelopeIsIdempotentBySha() = runBlocking {
        val env = Envelope(
            sha256 = "abc",
            mime = null,
            text = null,
            filename = null,
            sourcePkgRef = "unknown",
            receivedAtUtc = Instant.now(),
            metaJson = null
        )
        val first = repo.saveEnvelope(env)
        val second = repo.saveEnvelope(env)
        assertTrue(first is SaveResult.Success)
        assertTrue(second is SaveResult.Duplicate)
        val envelopes = repo.observeEnvelopes().first()
        assertEquals(1, envelopes.size)
    }
}
