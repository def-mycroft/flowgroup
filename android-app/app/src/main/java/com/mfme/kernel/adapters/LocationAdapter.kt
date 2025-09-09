package com.mfme.kernel.adapters

import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Location adapter placeholder. A real implementation would query the
 * fused location provider. Here callers supply the JSON snapshot.
 */
class LocationAdapter(private val repo: KernelRepository) {
    suspend fun capture(json: String): SaveResult =
        withContext(Dispatchers.IO) { repo.saveFromLocation(json) }
}
