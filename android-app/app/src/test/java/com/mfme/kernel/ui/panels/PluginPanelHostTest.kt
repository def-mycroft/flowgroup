package com.mfme.kernel.ui.panels

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mfme.kernel.ui.KernelViewModel
import com.mfme.kernel.ui.theme.KernelTheme
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.Envelope
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

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

class PluginPanelHostTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun switchingTabsShowsDifferentPanel() {
        val vm = KernelViewModel(FakeRepo())
        compose.setContent {
            registerBuiltinPanels(vm, devMode = false)
            KernelTheme { PluginPanelHost() }
        }
        compose.onNodeWithText("Camera").assertExists()
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("Receipts").assertExists()
    }
}
