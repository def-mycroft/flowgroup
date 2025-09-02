package app.zero.inlet.repo

import android.content.Context
import androidx.room.Room
import app.zero.inlet.db.InletDatabase

object ServiceLocator {
    lateinit var repository: EnvelopeRepository
        private set

    fun init(context: Context) {
        val builder = Room.databaseBuilder(context, InletDatabase::class.java, "inlet.db")
            .addMigrations(InletDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
        val db = builder.build()
        repository = EnvelopeRepositoryImpl(db, context.filesDir)
    }
}
