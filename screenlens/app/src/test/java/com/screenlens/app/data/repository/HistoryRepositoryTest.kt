package com.screenlens.app.data.repository

import com.screenlens.app.data.db.HistoryDao
import com.screenlens.app.data.db.HistoryEntity
import com.screenlens.app.domain.HistoryItem
import com.screenlens.app.domain.HistoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** In-memory fake standing in for Room so repository logic can be tested without Android. */
private class FakeHistoryDao : HistoryDao {
    private val rows = MutableStateFlow<List<HistoryEntity>>(emptyList())
    private var nextId = 1L
    var lastTrimLimit: Int? = null

    override fun observeAll(): Flow<List<HistoryEntity>> = rows

    override fun observeFiltered(type: String?, query: String): Flow<List<HistoryEntity>> =
        rows.map { list ->
            list.filter { (type == null || it.type == type) }
                .filter { query.isBlank() || it.originalText.contains(query, true) || it.translatedText?.contains(query, true) == true }
        }

    override suspend fun getById(id: Long): HistoryEntity? = rows.value.find { it.id == id }

    override suspend fun insert(entity: HistoryEntity): Long {
        val withId = entity.copy(id = nextId++)
        rows.value = rows.value + withId
        return withId.id
    }

    override suspend fun delete(entity: HistoryEntity) {
        rows.value = rows.value.filterNot { it.id == entity.id }
    }

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun clearAll() {
        rows.value = emptyList()
    }

    override suspend fun count(): Int = rows.value.size

    override suspend fun trimToLimit(keepMostRecent: Int) {
        lastTrimLimit = keepMostRecent
        rows.value = rows.value.sortedByDescending { it.createdAt }.take(keepMostRecent)
    }
}

class HistoryRepositoryTest {

    private fun item(text: String, createdAt: Long = 1L) = HistoryItem(
        createdAt = createdAt,
        type = HistoryType.SCREEN,
        originalText = text,
        translatedText = null,
        sourceLanguage = null,
        targetLanguage = null,
        detectedLanguage = "en"
    )

    @Test
    fun `save with unlimited history does not trim`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepository(dao)

        repository.save(item("First"), historyLimit = 0)

        assertNull(dao.lastTrimLimit)
    }

    @Test
    fun `save with a limit trims down to that limit`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepository(dao)

        repository.save(item("First"), historyLimit = 5)

        assertEquals(5, dao.lastTrimLimit)
    }

    @Test
    fun `observe maps entities to domain items`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepository(dao)
        repository.save(item("Hello there"), historyLimit = 0)

        val results = repository.observe(HistoryType.SCREEN, "hello").first()

        assertEquals(1, results.size)
        assertEquals("Hello there", results.first().originalText)
    }

    @Test
    fun `observe with non-matching query returns empty list`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepository(dao)
        repository.save(item("Hello there"), historyLimit = 0)

        val results = repository.observe(null, "no match").first()

        assertEquals(0, results.size)
    }
}
