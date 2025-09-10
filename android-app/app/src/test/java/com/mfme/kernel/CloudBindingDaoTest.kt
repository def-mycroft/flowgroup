package com.mfme.kernel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.cloud.CloudBinding
import com.mfme.kernel.data.cloud.CloudBindingDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class CloudBindingDaoTest {
    private lateinit var db: KernelDatabase
    private lateinit var dao: CloudBindingDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KernelDatabase::class.java).build()
        dao = db.cloudBindingDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndFindBinding() = runBlocking {
        val binding = CloudBinding(
            envelopeId = 1L,
            driveFileId = "drive123",
            uploadedAtUtc = Instant.EPOCH,
            md5 = "md5",
            bytes = 123L
        )
        dao.insert(binding)
        val fetched = dao.findByEnvelopeId(1L)
        Assert.assertNotNull(fetched)
        Assert.assertEquals("drive123", fetched?.driveFileId)
    }
}
