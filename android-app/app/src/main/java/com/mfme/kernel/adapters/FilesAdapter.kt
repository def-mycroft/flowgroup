package com.mfme.kernel.adapters

import android.net.Uri
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Placeholder Files adapter delegating to repository which performs the
 * actual persistence and hashing.
 */
class FilesAdapter(private val repo: KernelRepository) {
    suspend fun capture(uri: Uri, meta: Map<String, Any?> = emptyMap()): SaveResult =
        withContext(Dispatchers.IO) { repo.saveFromFile(uri, meta) }
}
