package com.mfme.kernel.ui.gestures

import com.mfme.kernel.data.Envelope
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.SaveResult
import com.mfme.kernel.ui.KernelViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeRepo : KernelRepository {
    override fun observeReceipts(): Flow<List<com.mfme.kernel.data.telemetry.ReceiptEntity>> = flowOf(emptyList())
    override fun observeEnvelopes(): Flow<List<Envelope>> = flowOf(emptyList())
    override suspend fun saveEnvelope(env: Envelope): SaveResult = SaveResult.Success(0)
    override suspend fun saveFromCamera(bytes: ByteArray, meta: Map<String, Any?>): SaveResult = SaveResult.Success(0)
    override suspend fun saveFromMic(bytes: ByteArray, meta: Map<String, Any?>): SaveResult = SaveResult.Success(0)
    override suspend fun saveFromFile(uri: android.net.Uri, meta: Map<String, Any?>): SaveResult = SaveResult.Success(0)
    override suspend fun saveFromLocation(json: String): SaveResult = SaveResult.Success(0)
    override suspend fun saveFromSensors(json: String): SaveResult = SaveResult.Success(0)
    override suspend fun saveSmsOut(phone: String, body: String, at: java.time.Instant): SaveResult = SaveResult.Success(0)
    override suspend fun ingestSmsIn(sender: String, body: String, at: java.time.Instant): SaveResult = SaveResult.Success(0)
    override suspend fun saveFromShare(payload: com.mfme.kernel.adapters.share.SharePayload): SaveResult = SaveResult.Success(0)
}

class NoopGestureAdapterTest {
    @Test
    fun coalescesRapidMarks() = runTest {
        val vm = KernelViewModel(FakeRepo())
        var count = 0
        val job = launch { vm.gestures.collect { count++ } }
        var now = 0L
        val adapter = NoopGestureAdapter(vm, windowMs = 300) { now }
        adapter.on(GestureIntent.Mark)
        adapter.on(GestureIntent.Mark)
        assertEquals(1, count)
        now += 400
        adapter.on(GestureIntent.Mark)
        assertEquals(2, count)
        job.cancel()
    }
}
