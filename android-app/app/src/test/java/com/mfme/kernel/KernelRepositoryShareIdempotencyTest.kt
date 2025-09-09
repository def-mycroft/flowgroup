package com.mfme.kernel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mfme.kernel.adapters.share.SharePayload
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.KernelRepositoryImpl
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class KernelRepositoryShareIdempotencyTest {
    private lateinit var db: KernelDatabase
    private lateinit var repo: KernelRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KernelDatabase::class.java).build()
        repo = KernelRepositoryImpl(context, db, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun duplicateTextCollapses() = runBlocking {
        val payload = SharePayload.Text(
            text = "hello",
            subject = null,
            sourceRef = "tester",
            receivedAtUtc = Instant.EPOCH
        )
        val first = repo.saveFromShare(payload)
        val second = repo.saveFromShare(payload)
        assertTrue(first is SaveResult.Success)
        assertTrue(second is SaveResult.Duplicate)
        val envelopes = repo.observeEnvelopes().first()
        assertEquals(1, envelopes.size)
        assertEquals("text/plain", envelopes.first().mime)
        assertEquals(Instant.EPOCH, envelopes.first().receivedAtUtc)
    }
}
