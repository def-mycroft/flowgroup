package app.zero.inlet.share

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.Room
import app.zero.inlet.core.Sha256Hasher
import app.zero.inlet.core.UtcClock
import app.zero.inlet.db.EnvelopeEntity
import app.zero.inlet.db.InletDatabase
import app.zero.inlet.db.ReceiptEntity
import java.io.File

class ShareReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                uri?.let { listOf(it) } ?: emptyList()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
            }
            else -> emptyList()
        }

        if (uris.isEmpty()) return

        val resolver = context.contentResolver
        val mime = intent.type ?: ""
        val sourcePackage = intent.getStringExtra("source_package") ?: intent.`package` ?: ""
        val now = UtcClock.nowZ()

        val db = Room.databaseBuilder(context, InletDatabase::class.java, "inlet.db")
            .allowMainThreadQueries()
            .build()

        uris.forEach { uri ->
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@forEach
            val sha = Sha256Hasher.hash(bytes)
            val name = queryName(resolver, uri) ?: uri.lastPathSegment ?: "unknown"
            val dest = File(File(context.filesDir, "ingest"), "$sha/$name")
            if (!dest.exists()) {
                dest.parentFile?.mkdirs()
                dest.writeBytes(bytes)
            }
            val envelope = EnvelopeEntity(
                sha256 = sha,
                filename = name,
                mime = mime,
                sizeBytes = bytes.size.toLong(),
                createdAtZ = now,
                sourcePackage = sourcePackage
            )
            db.envelopeDao().insert(envelope)
            db.receiptDao().insert(
                ReceiptEntity(
                    type = "share",
                    envelopeSha256 = sha,
                    createdAtZ = now
                )
            )
        }
        db.close()
    }

    private fun queryName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return null
    }
}
