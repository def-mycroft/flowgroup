package com.mfme.kernel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.Instant
import com.mfme.kernel.data.SaveResult

@Composable
fun CaptureScreen(viewModel: KernelViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun handleResult(result: SaveResult) {
        val msg = when (result) {
            is SaveResult.Success, is SaveResult.Duplicate -> "Saved"
            is SaveResult.Error -> "Capture failed"
        }
        scope.launch { snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Button(
                    onClick = {
                        viewModel.saveFromCamera("demo".toByteArray(), mapOf("tsUtc" to Instant.EPOCH), ::handleResult)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) { Text("Camera") }
            }
            item {
                Button(
                    onClick = {
                        viewModel.saveFromMic("mic".toByteArray(), mapOf("tsUtc" to Instant.EPOCH), ::handleResult)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) { Text("Mic") }
            }
            item {
                Button(
                    onClick = {
                        // Placeholder URI usage not available in demo; Snackbar will show failure
                        handleResult(SaveResult.Error(IllegalArgumentException("no_uri")))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) { Text("Files") }
            }
            item {
                Button(
                    onClick = {
                        viewModel.saveFromLocation("{}", ::handleResult)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) { Text("Location") }
            }
            item {
                Button(
                    onClick = {
                        viewModel.saveFromSensors("{}", ::handleResult)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) { Text("Sensors") }
            }
        }
    }
}
