package com.mfme.kernel.data.cloud

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CloudBindingDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(binding: CloudBinding): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(binding: CloudBinding): Long

    @Query("SELECT * FROM cloud_binding WHERE envelopeId = :envelopeId")
    suspend fun findByEnvelopeId(envelopeId: Long): CloudBinding?

    @Query("SELECT * FROM cloud_binding WHERE driveFileId = :driveFileId")
    suspend fun findByDriveFileId(driveFileId: String): CloudBinding?

    @Query("DELETE FROM cloud_binding WHERE envelopeId = :envelopeId")
    suspend fun deleteByEnvelopeId(envelopeId: Long)

    @Query("SELECT * FROM cloud_binding")
    suspend fun getAll(): List<CloudBinding>

    @Query("SELECT cb.* FROM cloud_binding cb LEFT JOIN envelopes e ON cb.envelopeId = e.id WHERE e.id IS NULL")
    suspend fun findOrphans(): List<CloudBinding>
}
