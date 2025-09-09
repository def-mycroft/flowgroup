package com.mfme.kernel.di

import android.content.Context
import androidx.room.Room
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.KernelRepositoryImpl
import kotlinx.coroutines.Dispatchers

object AppModule {
    fun provideDatabase(context: Context): KernelDatabase =
        Room.databaseBuilder(context, KernelDatabase::class.java, "kernel.db").build()

    fun provideRepository(context: Context, db: KernelDatabase): KernelRepository =
        KernelRepositoryImpl(context, db, Dispatchers.IO)
}
