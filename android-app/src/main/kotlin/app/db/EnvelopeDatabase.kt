package app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EnvelopeEntity::class], version = 1)
abstract class EnvelopeDatabase : RoomDatabase() {
    abstract fun envelopeDao(): EnvelopeDao

    companion object {
        @Volatile private var instance: EnvelopeDatabase? = null

        fun get(context: Context): EnvelopeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                EnvelopeDatabase::class.java,
                "envelopes.db"
            ).build().also { instance = it }
        }
    }
}
