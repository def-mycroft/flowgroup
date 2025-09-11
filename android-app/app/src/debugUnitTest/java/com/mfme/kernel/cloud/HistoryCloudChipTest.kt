package com.mfme.kernel.cloud

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.mfme.kernel.data.Envelope
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.SaveResult
import com.mfme.kernel.export.VaultConfig
import com.mfme.kernel.ui.HistoryScreen
import com.mfme.kernel.ui.KernelViewModel
import com.mfme.kernel.ui.theme.KernelTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class FakeRepo2 : KernelRepository {
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

@RunWith(RobolectricTestRunner::class)
class HistoryCloudChipTest {
    @get:Rule val compose = createComposeRule()

    @After
    fun tearDown() {
        DriveServiceFactory.setTokenProviderOverrideForTests(null)
    }

    @Test
    fun chip_shows_not_connected_when_no_account() {
        DriveServiceFactory.setTokenProviderOverrideForTests(object : TokenProvider {
            override fun getAccessToken(): String = throw SecurityException("no_account")
            override fun hasAccount(): Boolean = false
        })
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val vm = KernelViewModel(FakeRepo2(), VaultConfig(context))
        compose.setContent { KernelTheme { HistoryScreen(vm) } }
        compose.onNodeWithText("Drive: Not connected").assertExists()
    }

    @Test
    fun chip_shows_connected_when_account_present() {
        DriveServiceFactory.setTokenProviderOverrideForTests(object : TokenProvider {
            override fun getAccessToken(): String = "token"
            override fun hasAccount(): Boolean = true
        })
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val vm = KernelViewModel(FakeRepo2(), VaultConfig(context))
        compose.setContent { KernelTheme { HistoryScreen(vm) } }
        compose.onNodeWithText("Drive: Connected").assertExists()
    }
}

