package com.mfme.kernel.adapters

import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simple sensors adapter that forwards JSON snapshots to the repository.
 */
class SensorsAdapter(private val repo: KernelRepository) {
    suspend fun capture(json: String): SaveResult =
        withContext(Dispatchers.IO) { repo.saveFromSensors(json) }
}
