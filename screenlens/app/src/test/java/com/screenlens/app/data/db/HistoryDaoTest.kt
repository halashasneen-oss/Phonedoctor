package com.screenlens.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.historyDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(text: String, translated: String? = null, type: String = "SCREEN", createdAt: Long = 1L) =
        HistoryEntity(
            createdAt = createdAt,
            type = type,
            originalText = text,
            translatedText = translated,
            sourceLanguage = null,
            targetLanguage = null,
            detectedLanguage = "en"
        )

    @Test
    fun `search matches original and translated text`() = runTest {
        dao.insert(entity("Hello world", createdAt = 1))
        dao.insert(entity("Bonjour", translated = "Hello", createdAt = 2))
        dao.insert(entity("Completely unrelated", createdAt = 3))

        val results = dao.observeFiltered(null, "hello").first()

        assertEquals(2, results.size)
        assertTrue(results.any { it.originalText == "Hello world" })
        assertTrue(results.any { it.translatedText == "Hello" })
    }

    @Test
    fun `filter by type returns only matching rows`() = runTest {
        dao.insert(entity("Screen text", type = "SCREEN"))
        dao.insert(entity("Image text", type = "IMAGE"))
        dao.insert(entity("Another screen text", type = "SCREEN"))

        val results = dao.observeFiltered("SCREEN", "").first()

        assertEquals(2, results.size)
        assertTrue(results.all { it.type == "SCREEN" })
    }

    @Test
    fun `results are ordered newest first`() = runTest {
        dao.insert(entity("Oldest", createdAt = 1))
        dao.insert(entity("Newest", createdAt = 3))
        dao.insert(entity("Middle", createdAt = 2))

        val results = dao.observeAll().first()

        assertEquals(listOf("Newest", "Middle", "Oldest"), results.map { it.originalText })
    }

    @Test
    fun `trimToLimit keeps only the most recent rows`() = runTest {
        repeat(5) { i -> dao.insert(entity("Item $i", createdAt = i.toLong())) }

        dao.trimToLimit(2)
        val remaining = dao.observeAll().first()

        assertEquals(2, remaining.size)
        assertEquals(listOf("Item 4", "Item 3"), remaining.map { it.originalText })
    }

    @Test
    fun `clearAll removes every row`() = runTest {
        dao.insert(entity("A"))
        dao.insert(entity("B"))

        dao.clearAll()

        assertEquals(0, dao.observeAll().first().size)
    }
}
