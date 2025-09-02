package app.zero.inlet.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.zero.inlet.db.InletDatabase
import app.zero.inlet.db.InstantConverters
import java.io.File
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EnvelopeRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: InletDatabase
    private lateinit var repo: EnvelopeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, InletDatabase::class.java)
            .addTypeConverter(InstantConverters())
            .build()
        repo = EnvelopeRepositoryImpl(db, context.filesDir)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun duplicateSaveResultsInSingleRow() = runTest {
        val text = "hello world"
        val instant = Instant.now()
        val source = "test"
        val first = repo.saveEnvelope(text, null, source, instant)
        val second = repo.saveEnvelope(text, null, source, instant)
        assertTrue(first is EnvelopeRepository.EnvelopeResult.Success)
        assertTrue(second is EnvelopeRepository.EnvelopeResult.Duplicate)
        val all = repo.observeNewest().first()
        assertEquals(1, all.size)
        val file = File(all.first().bytes_path)
        assertEquals(text, file.readText(Charsets.UTF_8))
    }
}
