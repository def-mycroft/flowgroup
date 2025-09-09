package com.mfme.kernel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.KernelRepositoryImpl
import com.mfme.kernel.data.SaveResult
import com.mfme.kernel.util.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest
import java.time.Instant

class KernelRepositoryCameraAdapterTest {
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
    fun duplicateCameraCaptureCollapsesAndKeepsUtc() = runBlocking {
        val bytes = "photo".toByteArray()
        val meta = mapOf("filename" to "p.jpg", "tsUtc" to Instant.EPOCH)
        val expectedSha = toHex(MessageDigest.getInstance("SHA-256").digest(bytes))
        val first = repo.saveFromCamera(bytes, meta)
        val second = repo.saveFromCamera(bytes, meta)
        assertTrue(first is SaveResult.Success)
        assertTrue(second is SaveResult.Duplicate)
        val env = repo.observeEnvelopes().first().first()
        assertEquals(Instant.EPOCH, env.receivedAtUtc)
        assertEquals(expectedSha, env.sha256)
    }
}
