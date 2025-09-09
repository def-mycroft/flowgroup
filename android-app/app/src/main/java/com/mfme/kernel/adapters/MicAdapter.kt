package com.mfme.kernel.adapters

import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Microphone adapter placeholder. Real implementation would record audio
 * and provide duration metadata. Here we simply pass given bytes to the
 * repository on an IO dispatcher.
 */
class MicAdapter(private val repo: KernelRepository) {
    suspend fun capture(bytes: ByteArray, meta: Map<String, Any?> = emptyMap()): SaveResult =
        withContext(Dispatchers.IO) { repo.saveFromMic(bytes, meta) }
}
