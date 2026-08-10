package com.screenlens.app.data.repository

import com.screenlens.app.data.db.HistoryDao
import com.screenlens.app.data.db.toDomain
import com.screenlens.app.data.db.toEntity
import com.screenlens.app.domain.HistoryItem
import com.screenlens.app.domain.HistoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val dao: HistoryDao) {

    fun observe(type: HistoryType?, query: String): Flow<List<HistoryItem>> =
        dao.observeFiltered(type?.name, query.trim()).map { list -> list.map { it.toDomain() } }

    fun observeRecent(limit: Int): Flow<List<HistoryItem>> =
        dao.observeAll().map { list -> list.take(limit).map { it.toDomain() } }

    suspend fun getById(id: Long): HistoryItem? = dao.getById(id)?.toDomain()

    /**
     * Saves an entry and trims the table down to [historyLimit] most-recent rows
     * (0 = unlimited). Returns the new row id.
     */
    suspend fun save(item: HistoryItem, historyLimit: Int): Long {
        val id = dao.insert(item.toEntity())
        if (historyLimit > 0) {
            dao.trimToLimit(historyLimit)
        }
        return id
    }

    suspend fun delete(item: HistoryItem) = dao.delete(item.toEntity())

    suspend fun clearAll() = dao.clearAll()
}
