package com.mfme.kernel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
@Composable
fun HistoryScreen(viewModel: KernelViewModel) {
    val receipts by viewModel.receipts.collectAsState()
    val envelopes by viewModel.envelopes.collectAsState()
    var filter by remember { mutableStateOf(0) }

    val filtered = when (filter) {
        1 -> receipts.filter { !it.ok }
        2 -> receipts.filter { it.ok }
        else -> receipts
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Receipts", style = MaterialTheme.typography.titleMedium)
            Column(horizontalAlignment = Alignment.Start) {
                TextButton(onClick = { filter = 0 }) { Text("All") }
                TextButton(onClick = { filter = 1 }) { Text("Errors") }
                TextButton(onClick = { filter = 2 }) { Text("Ok") }
            }
        }
        items(filtered, key = { it.id }) { r ->
            Column {
                val glyph = if (r.ok) "✅" else "⚠️"
                Text("$glyph ${r.code} · ${r.adapter} · ${r.tsUtcIso}")
                r.envelopeSha256?.let { Text(it) }
                r.message?.let { Text(it) }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { Text("Envelopes", style = MaterialTheme.typography.titleMedium) }
        items(envelopes, key = { it.id }) { e ->
            Column {
                Text("sha256: ${e.sha256}")
                Text("mime: ${e.mime ?: ""}")
                Text("filename: ${e.filename ?: ""}")
            }
        }
    }
}
