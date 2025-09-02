package app.zero.inlet.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import app.zero.inlet.repo.EnvelopeRepository
import app.zero.inlet.repo.ServiceLocator
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val snackbarHostState = SnackbarHostState()
        setContent {
            val host = remember { snackbarHostState }
            BackHandler(enabled = true) { }
            Box(modifier = Modifier.fillMaxSize()) {
                SnackbarHost(hostState = host, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrBlank()) {
            lifecycleScope.launch {
                snackbarHostState.showSnackbar("No text found")
                delay(resources.getInteger(app.zero.inlet.R.integer.share_success_dwell_ms).toLong())
                finish()
            }
            return
        }
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size > 1_000_000) {
            lifecycleScope.launch {
                snackbarHostState.showSnackbar("Save failed")
                delay(resources.getInteger(app.zero.inlet.R.integer.share_error_dwell_ms).toLong())
                finish()
            }
            return
        }
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val source = intent.getStringExtra("source_package")
            ?: intent.getStringExtra(Intent.EXTRA_REFERRER_NAME)
            ?: intent.referrer?.host
            ?: "unknown"
        val receivedAt = Instant.now()
        val repo = ServiceLocator.repository
        lifecycleScope.launch {
            when (repo.saveEnvelope(text, subject, source, receivedAt)) {
                is EnvelopeRepository.EnvelopeResult.Success,
                is EnvelopeRepository.EnvelopeResult.Duplicate -> {
                    snackbarHostState.showSnackbar("Saved")
                    delay(resources.getInteger(app.zero.inlet.R.integer.share_success_dwell_ms).toLong())
                }
                is EnvelopeRepository.EnvelopeResult.Error -> {
                    snackbarHostState.showSnackbar("Save failed")
                    delay(resources.getInteger(app.zero.inlet.R.integer.share_error_dwell_ms).toLong())
                }
            }
            finish()
        }
    }
}
