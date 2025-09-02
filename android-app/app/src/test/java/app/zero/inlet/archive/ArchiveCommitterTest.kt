package app.zero.inlet.archive

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import java.io.File
import java.nio.file.Files
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows

class ArchiveCommitterTest {

    private fun initWorkManager(context: Context): WorkManager {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        return WorkManager.getInstance(context)
    }

    private fun samplePlan(): ArchivePlan = ArchivePlan(
        dataPath = "archive/data.bin",
        sidecarPath = "archive/data.json",
        sha256 = "sha",
        target = "t",
        sidecarBytes = "{}".toByteArray()
    )

    @Test
    fun commitWritesAndCleansPartFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        initWorkManager(context)
        val root = Files.createTempDirectory("test").toFile()
        val committer = ArchiveCommitter(context, root)
        val plan = samplePlan()
        committer.commit(plan, "hello".toByteArray())

        val dataFile = File(root, plan.dataPath)
        val sidecarFile = File(root, plan.sidecarPath)
        assertTrue(dataFile.exists())
        assertTrue(sidecarFile.exists())

        val tmp = File(root, "tmp")
        assertFalse(File(tmp, dataFile.name + ".part").exists())
        assertFalse(File(tmp, sidecarFile.name + ".part").exists())
    }

    @Test
    fun failureLeavesNoFinalFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        initWorkManager(context)
        val root = Files.createTempDirectory("test").toFile()
        val committer = ArchiveCommitter(context, root)
        val plan = samplePlan()
        assertThrows(RuntimeException::class.java) {
            committer.commit(plan, "hello".toByteArray(), failAfterWrite = true)
        }
        assertFalse(File(root, plan.dataPath).exists())
        assertFalse(File(root, plan.sidecarPath).exists())
    }

    @Test
    fun enqueueIsUnique() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wm = initWorkManager(context)
        val root = Files.createTempDirectory("test").toFile()
        val committer = ArchiveCommitter(context, root)
        val plan = samplePlan()
        committer.commit(plan, "hi".toByteArray())
        committer.commit(plan, "hi".toByteArray())

        val workInfos = wm.getWorkInfosForUniqueWork("${plan.target}:${plan.sha256}").get()
        assertEquals(1, workInfos.size)
    }
}

