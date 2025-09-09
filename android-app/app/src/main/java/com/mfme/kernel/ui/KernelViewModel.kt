package com.mfme.kernel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mfme.kernel.data.Envelope
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KernelViewModel(private val repo: KernelRepository): ViewModel() {
    val envelopes = repo.observeEnvelopes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val receipts  = repo.observeReceipts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(env: Envelope, onDone: (SaveResult) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.saveEnvelope(env)) }
    }

    fun saveFromCamera(bytes: ByteArray, meta: Map<String, Any?> = emptyMap(), onDone: (SaveResult) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.saveFromCamera(bytes, meta)) }
    }

    fun saveFromMic(bytes: ByteArray, meta: Map<String, Any?> = emptyMap(), onDone: (SaveResult) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.saveFromMic(bytes, meta)) }
    }

    fun saveFromFile(uri: android.net.Uri, meta: Map<String, Any?> = emptyMap(), onDone: (SaveResult) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.saveFromFile(uri, meta)) }
    }

    fun saveFromLocation(json: String, onDone: (SaveResult) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.saveFromLocation(json)) }
    }

    fun saveFromSensors(json: String, onDone: (SaveResult) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.saveFromSensors(json)) }
    }
}
