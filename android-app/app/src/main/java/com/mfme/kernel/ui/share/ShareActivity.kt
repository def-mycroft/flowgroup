package com.mfme.kernel.ui.share

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
import com.mfme.kernel.R
import com.mfme.kernel.ServiceLocator
import com.mfme.kernel.adapters.share.ShareAdapter
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShareActivity : ComponentActivity() {
    private val dwellMs: Long by lazy {
        resources.getInteger(R.integer.share_success_dwell_ms).toLong()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hostState = SnackbarHostState()
        setContent {
            val host = remember { hostState }
            BackHandler(enabled = true) { }
            Box(modifier = Modifier.fillMaxSize()) {
                SnackbarHost(hostState = host, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        val payload = ShareAdapter.fromIntent(this, intent)
        if (payload == null) {
            lifecycleScope.launch {
                hostState.showSnackbar(getString(R.string.share_no_content))
                delay(dwellMs)
                finish()
            }
            return
        }

        val repo: KernelRepository = ServiceLocator.repository(applicationContext)
        lifecycleScope.launch {
            val result = repo.saveFromShare(payload)
            val msg = when (result) {
                is SaveResult.Success, is SaveResult.Duplicate -> R.string.share_saved
                is SaveResult.Error -> R.string.share_save_failed
            }
            hostState.showSnackbar(getString(msg))
            delay(dwellMs)
            finish()
        }
    }
}
