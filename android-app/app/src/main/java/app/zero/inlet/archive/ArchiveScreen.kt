package app.zero.inlet.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.zero.inlet.db.Envelope
import app.zero.inlet.repo.EnvelopeRepository
import app.zero.inlet.repo.ServiceLocator

@Composable
fun ArchiveScreen(repo: EnvelopeRepository = ServiceLocator.repository) {
    val envelopes by repo.observeNewest().collectAsState(initial = emptyList())
    if (envelopes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No captures yet.")
        }
    } else {
        LazyColumn {
            items(envelopes) { env ->
                Text(env.text.take(40))
            }
        }
    }
}
