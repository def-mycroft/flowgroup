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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(viewModel: KernelViewModel) {
    val receipts by viewModel.receipts.collectAsState()
    val envelopes by viewModel.envelopes.collectAsState()
    val formatter = DateTimeFormatter.ISO_INSTANT
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Receipts", style = MaterialTheme.typography.titleMedium) }
        items(receipts, key = { it.id }) { r ->
            Column { 
                Text("ts: ${'$'}{formatter.format(r.tsUtc)}")
                Text("${'$'}{r.status}/${'$'}{r.code}")
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { Text("Envelopes", style = MaterialTheme.typography.titleMedium) }
        items(envelopes, key = { it.id }) { e ->
            Column {
                Text("sha256: ${'$'}{e.sha256}")
                Text("mime: ${'$'}{e.mime ?: ""}")
                Text("filename: ${'$'}{e.filename ?: ""}")
            }
        }
    }
}
