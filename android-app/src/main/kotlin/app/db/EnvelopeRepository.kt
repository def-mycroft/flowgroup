package app.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EnvelopeRepository private constructor(private val dao: EnvelopeDao) {
    fun observeAll() = dao.observeAll()

    suspend fun insert(entity: EnvelopeEntity) = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    companion object {
        fun get(context: Context): EnvelopeRepository {
            val db = EnvelopeDatabase.get(context)
            return EnvelopeRepository(db.envelopeDao())
        }
    }
}
