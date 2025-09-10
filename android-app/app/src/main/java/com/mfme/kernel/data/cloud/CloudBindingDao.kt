package com.mfme.kernel.data.cloud

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CloudBindingDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(binding: CloudBinding): Long

    @Query("SELECT * FROM cloud_binding WHERE envelopeId = :envelopeId")
    suspend fun findByEnvelopeId(envelopeId: Long): CloudBinding?

    @Query("SELECT * FROM cloud_binding WHERE driveFileId = :driveFileId")
    suspend fun findByDriveFileId(driveFileId: String): CloudBinding?
}
