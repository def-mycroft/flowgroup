package com.mfme.kernel.cloud

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.telemetry.NdjsonSink
import com.mfme.kernel.telemetry.ReceiptEmitter
import com.mfme.kernel.telemetry.TelemetryCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CloudVerifyReceiptsTest {
    private lateinit var context: Context
    private lateinit var db: KernelDatabase
    private lateinit var emitter: ReceiptEmitter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, KernelDatabase::class.java).build()
        emitter = ReceiptEmitter(db.receiptDao(), db.spanDao(), NdjsonSink(context))
        DriveServiceFactory.setTokenProviderOverrideForTests(null)
    }

    @After
    fun tearDown() {
        DriveServiceFactory.setTokenProviderOverrideForTests(null)
        db.close()
    }

    @Test
    fun verify_emits_err_when_no_account() = runTest {
        DriveServiceFactory.setTokenProviderOverrideForTests(object : TokenProvider {
            override fun getAccessToken(): String = throw SecurityException("no_account")
            override fun hasAccount(): Boolean = false
        })
        CloudActions.verifyNow(context, emitter, this)
        val receipts = runBlocking(Dispatchers.IO) { db.receiptDao().observeAll().first() }
        assertTrue(receipts.isNotEmpty())
        assertEquals(TelemetryCode.ErrNoAccount.wire, receipts.last().code)
    }

    @Test
    fun verify_emits_ok_when_connected() = runTest {
        DriveServiceFactory.setTokenProviderOverrideForTests(object : TokenProvider {
            override fun getAccessToken(): String = "token"
            override fun hasAccount(): Boolean = true
        })
        CloudActions.verifyNow(context, emitter, this)
        advanceUntilIdle()
        val receipts = runBlocking(Dispatchers.IO) { db.receiptDao().observeAll().first() }
        assertTrue(receipts.isNotEmpty())
        assertEquals(TelemetryCode.OkVerified.wire, receipts.last().code)
    }
}
