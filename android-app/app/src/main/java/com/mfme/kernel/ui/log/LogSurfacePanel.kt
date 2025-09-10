package com.mfme.kernel.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mfme.kernel.data.Envelope
import com.mfme.kernel.data.telemetry.ReceiptEntity
import com.mfme.kernel.ui.KernelViewModel
import com.mfme.kernel.ui.theme.KernelTheme
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Builds a markdown preview from receipts and envelopes. */
fun buildMarkdownPreview(receipts: List<ReceiptEntity>, envelopes: List<Envelope>): String {
    val date = LocalDate.now(ZoneOffset.UTC)
    val sb = StringBuilder("# ${date}\n")
    receipts.forEach { r ->
        sb.append("- receipt ${r.code} ${r.tsUtcIso}\n")
    }
    envelopes.forEach { e ->
        sb.append("- envelope ${e.sha256}\n")
    }
    return sb.toString()
}

/** Panel showing recent receipts/envelopes with a markdown preview. */
@Composable
fun LogSurfacePanel(viewModel: KernelViewModel) {
    val receipts by viewModel.receipts.collectAsState()
    val envelopes by viewModel.envelopes.collectAsState()
    var show by remember { mutableStateOf(false) }
    val preview = remember(receipts, envelopes) { buildMarkdownPreview(receipts, envelopes) }

    val tokens = KernelTheme.tokens
    Column(Modifier.fillMaxSize().padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(receipts, key = { it.id }) { r ->
                Text("${r.code} · ${r.adapter}")
            }
            items(envelopes, key = { it.id }) { e ->
                Text("env ${e.sha256}")
            }
        }
        Button(onClick = { show = !show }) { Text("Preview Markdown") }
        if (show) {
            Text(preview)
        }
    }
}
