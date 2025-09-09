package com.mfme.kernel.adapters

import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Trivial camera adapter that delegates captured bytes to the repository.
 * In a full implementation this would integrate with CameraX and capture
 * real JPEG data and metadata.
 */
class CameraAdapter(private val repo: KernelRepository) {
    suspend fun capture(bytes: ByteArray, meta: Map<String, Any?> = emptyMap()): SaveResult =
        withContext(Dispatchers.IO) { repo.saveFromCamera(bytes, meta) }
}
