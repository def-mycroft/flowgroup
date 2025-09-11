package com.mfme.kernel.cloud

import android.content.Context
import app.zero.core.cloud.DriveAdapter
import app.zero.core.cloud.DriveFileRef
import app.zero.core.cloud.DriveFolderRef
import app.zero.core.cloud.UploadSpec
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import okio.buffer
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/** Google Drive REST-backed DriveAdapter. Minimal fields; resilient defaults. */
class GoogleDriveAdapter(
    private val context: Context,
    private val tokenProvider: TokenProvider
) : DriveAdapter {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    override suspend fun ensureFolder(pathSegments: List<String>): Result<DriveFolderRef> = runCatching {
        var parent: String? = null // null implies My Drive root
        for (seg in pathSegments) {
            val existing = querySingleFolder(seg, parent)
            parent = existing ?: createFolder(seg, parent)
        }
        DriveFolderRef(parent!!)
    }

    override suspend fun findBySha256(sha256: String, folderId: String): Result<DriveFileRef?> = runCatching {
        val q = "appProperties has { key='sha256' and value='${escape(sha256)}' } and '$folderId' in parents and trashed=false"
        val url = "https://www.googleapis.com/drive/v3/files?fields=files(id,md5Checksum,size)&q=" +
            URLEncoder.encode(q, "UTF-8")
        val req = builder().url(url).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val arr = JSONObject(resp.body!!.string()).optJSONArray("files") ?: return@use null
            if (arr.length() == 0) return@use null
            val f = arr.getJSONObject(0)
            DriveFileRef(
                id = f.getString("id"),
                md5 = f.optString("md5Checksum", null),
                bytes = f.optLong("size", -1).let { if (it >= 0) it else null }
            )
        }
    }

    override suspend fun getMetadata(fileId: String): Result<DriveFileRef?> = runCatching {
        val url = "https://www.googleapis.com/drive/v3/files/${fileId}?fields=id,md5Checksum,size"
        val req = builder().url(url).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val o = JSONObject(resp.body!!.string())
            DriveFileRef(
                id = o.getString("id"),
                md5 = o.optString("md5Checksum", null),
                bytes = o.optLong("size", -1).let { if (it >= 0) it else null }
            )
        }
    }

    override suspend fun uploadResumable(spec: UploadSpec): Result<DriveFileRef> = runCatching {
        val path = spec.localPath ?: throw IllegalArgumentException("local_path_missing")
        val file = File(path)
        if (!file.exists()) throw IllegalArgumentException("payload_missing")

        val meta = JSONObject().apply {
            put("name", buildName(spec))
            put("parents", listOf(spec.folderId))
            put("mimeType", spec.mime)
            put("appProperties", mapOf(
                "sha256" to spec.sha256,
                "bytes" to spec.bytes,
                "mime" to spec.mime,
                "receivedAtUtc" to spec.receivedAtUtc,
                "idempotencyKey" to spec.idempotencyKey
            ))
        }
        // Initiate resumable session
        val initReq = builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&fields=id,md5Checksum,size")
            .post(meta.toString().toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull()))
            .header("X-Upload-Content-Type", spec.mime)
            .header("X-Upload-Content-Length", spec.bytes.toString())
            .build()
        val uploadUrl = http.newCall(initReq).execute().use { resp ->
            if (!resp.isSuccessful) throw mapHttp(resp.code)
            resp.header("Location") ?: throw IllegalStateException("missing_upload_location")
        }
        // Upload media bytes
        val base: RequestBody = file.asRequestBody((spec.mime.ifBlank { "application/octet-stream" }).toMediaTypeOrNull())
        // Avoid smart-cast on cross-module public property by capturing locally
        val media: RequestBody = spec.onProgress?.let { ProgressRequestBody(base, spec.bytes, it) } ?: base
        val putReq = Request.Builder()
            .url(uploadUrl)
            .put(media)
            .header("Authorization", "Bearer ${tokenProvider.getAccessToken()}")
            .build()
        http.newCall(putReq).execute().use { resp ->
            if (!resp.isSuccessful) throw mapHttp(resp.code)
            val body = resp.body?.string().orEmpty().ifBlank { "{}" }
            val o = JSONObject(body)
            val id = o.optString("id").ifBlank { null } ?: run {
                // Fallback: query by sha if id not returned
                findBySha256(spec.sha256, spec.folderId).getOrNull()?.id
                    ?: throw IllegalStateException("upload_id_missing")
            }
            val md5 = o.optString("md5Checksum", null)
            val size = o.optLong("size", -1).let { if (it >= 0) it else null }
            return@use DriveFileRef(id = id, md5 = md5, bytes = size)
        }
    }

    override suspend fun probe(): Result<Unit> = runCatching {
        // Lightweight token fetch proves auth; Drive About would be heavier.
        tokenProvider.getAccessToken()
        Unit
    }

    private fun builder(): Request.Builder {
        val token = tokenProvider.getAccessToken()
        return Request.Builder().header("Authorization", "Bearer $token")
    }

    private fun escape(s: String): String = s.replace("'", "\\'")

    private fun querySingleFolder(name: String, parentId: String?): String? {
        val nameQ = "name='${escape(name)}' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val parentQ = parentId?.let { " and '$it' in parents" } ?: ""
        val url = "https://www.googleapis.com/drive/v3/files?fields=files(id,name)&q=" +
            URLEncoder.encode(nameQ + parentQ, StandardCharsets.UTF_8.name())
        val req = builder().url(url).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val arr = JSONObject(resp.body!!.string()).optJSONArray("files") ?: return null
            if (arr.length() == 0) return null
            return arr.getJSONObject(0).getString("id")
        }
    }

    private fun createFolder(name: String, parentId: String?): String {
        val o = JSONObject().apply {
            put("name", name)
            put("mimeType", "application/vnd.google-apps.folder")
            if (parentId != null) put("parents", listOf(parentId))
        }
        val req = builder()
            .url("https://www.googleapis.com/drive/v3/files?fields=id")
            .post(o.toString().toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull()))
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw mapHttp(resp.code)
            val id = JSONObject(resp.body!!.string()).getString("id")
            return id
        }
    }

    private fun buildName(spec: UploadSpec): String =
        spec.sha256 + (spec.ext?.let { if (it.isNotBlank()) ".${it.trim().lowercase()}" else "" } ?: "")

    private fun mapHttp(code: Int): Exception = when (code) {
        401, 403 -> SecurityException("auth:$code")
        409, 412 -> IllegalStateException("conflict:$code")
        in 500..599 -> java.io.IOException("server:$code")
        else -> java.io.IOException("http:$code")
    }

    private class ProgressRequestBody(
        private val delegate: RequestBody,
        private val total: Long,
        private val onProgress: (Long, Long) -> Unit
    ) : RequestBody() {
        override fun contentType() = delegate.contentType()
        override fun contentLength(): Long = delegate.contentLength()
        override fun writeTo(sink: okio.BufferedSink) {
            val forwarding = object : okio.ForwardingSink(sink) {
                var written: Long = 0
                override fun write(source: okio.Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    written += byteCount
                    // Throttle updates to avoid excessive churn
                    if (total > 0) onProgress(written.coerceAtMost(total), total)
                }
            }
            // Use Okio 3 extension instead of deprecated Okio.buffer(sink)
            val buffered = forwarding.buffer()
            delegate.writeTo(buffered)
            buffered.flush()
        }
    }
}
