package app.zero.inlet.share

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Shadows
import app.zero.inlet.core.Sha256Hasher
import app.zero.inlet.db.InletDatabase
import androidx.room.Room
import app.zero.inlet.db.EnvelopeDao
import java.io.ByteArrayInputStream
import java.io.File

class ShareReceiverTest {
    @Test
    fun duplicateSharesSingleEnvelope() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bytes = "hello world".toByteArray()
        val uri = Uri.parse("content://test/hello.txt")
        Shadows.shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("source_package", "com.example.sender")
        }

        val receiver = ShareReceiver()
        receiver.onReceive(context, intent)

        // Re-register stream for second share
        Shadows.shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))
        receiver.onReceive(context, intent)

        val sha = Sha256Hasher.hash(bytes)
        val db: InletDatabase = Room.databaseBuilder(context, InletDatabase::class.java, "inlet.db")
            .allowMainThreadQueries()
            .build()
        val envelopeDao: EnvelopeDao = db.envelopeDao()

        assertEquals(1, envelopeDao.count())
        val envelope = envelopeDao.findBySha(sha)!!
        assertEquals("hello.txt", envelope.filename)
        assertEquals("text/plain", envelope.mime)
        assertTrue(envelope.createdAtZ.endsWith("Z"))
        val expectedFile = File(File(context.filesDir, "ingest"), "$sha/hello.txt")
        assertTrue(expectedFile.exists())
        db.close()
    }
}
