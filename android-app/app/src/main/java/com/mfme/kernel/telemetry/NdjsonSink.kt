package com.mfme.kernel.telemetry

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class NdjsonSink(private val appContext: Context) {
  suspend fun writeLine(tsUtcIso: String, receiptSha: String, line: String) = withContext(Dispatchers.IO) {
    val day = tsUtcIso.substring(0, 10)
    val dir = File(appContext.filesDir, "telemetry/$day").apply { mkdirs() }
    val tmp = File(dir, "$receiptSha.tmp")
    val out = File(dir, "${System.currentTimeMillis()}-$receiptSha.ndjson")

    FileOutputStream(tmp).use { fos ->
      fos.write(line.toByteArray(Charsets.UTF_8))
      fos.write('\n'.code)
      fos.fd.sync()
    }
    if (!tmp.renameTo(out)) {
      FileOutputStream(File(dir, "receipts.ndjson"), true).use { fos ->
        fos.write(line.toByteArray(Charsets.UTF_8));
        fos.write('\n'.code);
        fos.fd.sync()
      }
      tmp.delete()
    }
  }
}
