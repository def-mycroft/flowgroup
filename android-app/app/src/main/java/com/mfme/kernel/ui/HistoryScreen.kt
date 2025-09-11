package com.mfme.kernel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mfme.kernel.ui.theme.KernelTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mfme.kernel.di.AppModule
import kotlinx.coroutines.runBlocking
@Composable
fun HistoryScreen(viewModel: KernelViewModel) {
    val context = LocalContext.current
    val receipts by viewModel.receipts.collectAsState()
    val envelopes by viewModel.envelopes.collectAsState()
    val error by viewModel.error.collectAsState()
    var filter by remember { mutableStateOf(0) }

    val filtered = when (filter) {
        1 -> receipts.filter { !it.ok }
        2 -> receipts.filter { it.ok }
        3 -> receipts.filter { it.adapter == "sms_in" || it.adapter == "sms_out" }
        else -> receipts
    }

    // Preload cloud bindings map (envelopeId -> bound)
    val boundIds: Set<Long> = remember(envelopes) {
        runBlocking {
            try {
                AppModule.provideDatabase(context).cloudBindingDao().getAll().map { it.envelopeId }.toSet()
            } catch (t: Throwable) { emptySet() }
        }
    }

    val tokens = KernelTheme.tokens
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(tokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
    ) {
        error?.let { msg ->
            item { Text("Error loading history: $msg") }
        }
        item {
            val tokens = KernelTheme.tokens
            Text("Receipts", style = tokens.typeScale.title)
            val connected = com.mfme.kernel.cloud.DriveServiceFactory.getAdapter(context) != null
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (connected) "Drive: Connected" else "Drive: Not connected") },
                    leadingIcon = {
                        if (connected) Icon(Icons.Filled.CloudDone, contentDescription = null) else Icon(Icons.Filled.CloudOff, contentDescription = null)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (connected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                )
            }
            Column(horizontalAlignment = Alignment.Start) {
                TextButton(onClick = { filter = 0 }) { Text("All") }
                TextButton(onClick = { filter = 1 }) { Text("Errors") }
                TextButton(onClick = { filter = 2 }) { Text("Ok") }
                TextButton(onClick = { filter = 3 }) { Text("SMS") }
            }
        }
        // Use namespaced keys so Receipt and Envelope ids can't collide in the same LazyColumn
        items(filtered, key = { "r-${it.id}" }) { r ->
            Column {
                val glyph = if (r.ok) "✅" else "⚠️"
                Text("$glyph ${r.code} · ${r.adapter} · ${r.tsUtcIso}")
                r.envelopeSha256?.let { Text(it) }
                r.message?.let { Text(it) }
            }
        }
        item { Spacer(modifier = Modifier.height(tokens.spacing.lg)) }
        item {
            val tokens = KernelTheme.tokens
            Text("Envelopes", style = tokens.typeScale.title)
        }
        items(envelopes, key = { "e-${it.id}" }) { e ->
            Column {
                Text("sha256: ${e.sha256}")
                Text("mime: ${e.mime ?: ""}")
                Text("filename: ${e.filename ?: ""}")
                val cloudStatus = run {
                    if (boundIds.contains(e.id)) "Uploaded"
                    else {
                        val last = receipts.filter { it.envelopeId == e.id && it.adapter == "uploader" }.maxByOrNull { it.id }
                        if (last == null) "Queued" else if (last.ok) "Queued" else "Failed"
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    when (cloudStatus) {
                        "Uploaded" -> AssistChip(onClick = {}, label = { Text("Uploaded") }, leadingIcon = { Icon(Icons.Filled.CloudDone, null) })
                        "Failed" -> AssistChip(onClick = {}, label = { Text("Failed") }, leadingIcon = { Icon(Icons.Filled.ErrorOutline, null) }, colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFFEBEE)))
                        else -> AssistChip(onClick = {}, label = { Text("Queued") }, leadingIcon = { Icon(Icons.Filled.CloudQueue, null) })
                    }
                }
            }
        }
    }
}
