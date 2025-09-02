package app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import app.archive.ShareArchiver
import app.db.EnvelopeEntity
import app.db.EnvelopeRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val snackbar = SnackbarHostState()
    private val repo by lazy { EnvelopeRepository.get(this) }
    private val archiver by lazy { ShareArchiver(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        setContent {
            val items = repo.observeAll().collectAsState(initial = emptyList())
            Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                EnvelopeList(items.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleShareIntent(it) }
    }

    private fun handleShareIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
            val type = intent.type ?: "application/octet-stream"
            lifecycleScope.launch {
                val env = archiver.archive(uri, uri.lastPathSegment, type, target = "local")
                val entity = EnvelopeEntity(
                    sha256 = env.contentHashSha256,
                    filename = env.filename,
                    size = env.sizeBytes,
                    mediaType = env.mediaType,
                    createdAt = env.createdAtUtc,
                    filePath = env.filePath?.toString(),
                    archivedPath = env.archivedPath?.toString() ?: "",
                    sidecarPath = env.sidecarPath?.toString() ?: ""
                )
                repo.insert(entity)
                snackbar.showSnackbar("Saved")
            }
        }
    }
}

@Composable
fun EnvelopeList(envelopes: List<EnvelopeEntity>) {
    if (envelopes.isEmpty()) {
        Text("No entries")
    } else {
        LazyColumn(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            items(envelopes) { env ->
                Card { Text(env.filename ?: env.sha256) }
            }
        }
    }
}
